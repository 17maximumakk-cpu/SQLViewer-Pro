// SQLViewerJava.java — SQL Viewer с историей запросов на Java (Swing)

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import com.google.gson.*;

public class SQLViewerJava extends JFrame {
    private Connection conn;
    private List<HistoryEntry> history = new ArrayList<>();
    private String historyFile = "sql_history.json";
    private JTextArea sqlArea;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel statusLabel;

    public SQLViewerJava() {
        setTitle("🗄️ SQLViewer Pro — Java");
        setSize(900, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        loadHistory();
        createUI();
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { saveHistory(); }
        });
    }

    private void createUI() {
        // Toolbar
        JPanel toolbar = new JPanel();
        JButton connectBtn = new JButton("Подключить БД");
        JButton execBtn = new JButton("Выполнить");
        JButton histBtn = new JButton("История");
        JButton exportBtn = new JButton("Экспорт CSV");
        JButton clearBtn = new JButton("Очистить историю");
        toolbar.add(connectBtn);
        toolbar.add(execBtn);
        toolbar.add(histBtn);
        toolbar.add(exportBtn);
        toolbar.add(clearBtn);
        add(toolbar, BorderLayout.NORTH);

        // SQL Editor
        sqlArea = new JTextArea(8, 80);
        sqlArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        JScrollPane sqlScroll = new JScrollPane(sqlArea);
        add(sqlScroll, BorderLayout.CENTER);

        // Table
        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        JScrollPane tableScroll = new JScrollPane(resultTable);
        add(tableScroll, BorderLayout.SOUTH);

        // Status
        statusLabel = new JLabel("Готов");
        add(statusLabel, BorderLayout.SOUTH);

        // Handlers
        connectBtn.addActionListener(e -> connectDB());
        execBtn.addActionListener(e -> executeQuery());
        histBtn.addActionListener(e -> showHistory());
        exportBtn.addActionListener(e -> exportCSV());
        clearBtn.addActionListener(e -> clearHistory());
        sqlArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_MASK), "execute");
        sqlArea.getActionMap().put("execute", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { executeQuery(); }
        });
    }

    private void connectDB() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite DB", "db", "sqlite"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:" + path);
                statusLabel.setText("Подключено к " + chooser.getSelectedFile().getName());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            }
        }
    }

    private void executeQuery() {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Сначала подключите БД");
            return;
        }
        String sql = sqlArea.getText().trim();
        if (sql.isEmpty()) return;
        try {
            Statement stmt = conn.createStatement();
            if (sql.toLowerCase().startsWith("select")) {
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                tableModel.setRowCount(0);
                tableModel.setColumnCount(0);
                for (int i = 1; i <= cols; i++) tableModel.addColumn(meta.getColumnName(i));
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++) row[i-1] = rs.getObject(i);
                    tableModel.addRow(row);
                }
                statusLabel.setText("Выполнено: " + tableModel.getRowCount() + " строк");
                rs.close();
            } else {
                int affected = stmt.executeUpdate(sql);
                statusLabel.setText("Выполнено: " + affected + " строк затронуто");
            }
            stmt.close();
            // Сохраняем историю
            history.add(new HistoryEntry(new Date(), sql));
            saveHistory();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Ошибка SQL: " + e.getMessage());
        }
    }

    private void showHistory() {
        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "История пуста");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (HistoryEntry h : history) {
            sb.append("[").append(h.time).append("] ").append(h.sql).append("\n");
        }
        JOptionPane.showMessageDialog(this, sb.toString(), "История запросов", JOptionPane.PLAIN_MESSAGE);
    }

    private void exportCSV() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Нет данных для экспорта");
            return;
        }
        JFileChooser chooser = new JFileChooser();
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            try (PrintWriter pw = new PrintWriter(path)) {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    pw.print(tableModel.getColumnName(i) + (i < tableModel.getColumnCount()-1 ? "," : ""));
                }
                pw.println();
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        pw.print(tableModel.getValueAt(r, c) + (c < tableModel.getColumnCount()-1 ? "," : ""));
                    }
                    pw.println();
                }
                statusLabel.setText("Экспортировано в " + chooser.getSelectedFile().getName());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Ошибка экспорта: " + e.getMessage());
            }
        }
    }

    private void clearHistory() {
        if (JOptionPane.showConfirmDialog(this, "Очистить историю?", "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            history.clear();
            saveHistory();
            statusLabel.setText("История очищена");
        }
    }

    private void loadHistory() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(historyFile)));
            Gson gson = new Gson();
            HistoryEntry[] arr = gson.fromJson(json, HistoryEntry[].class);
            for (HistoryEntry e : arr) history.add(e);
        } catch (IOException ignored) {}
    }

    private void saveHistory() {
        try (PrintWriter pw = new PrintWriter(historyFile)) {
            Gson gson = new Gson();
            pw.println(gson.toJson(history));
        } catch (IOException ignored) {}
    }

    static class HistoryEntry {
        Date time;
        String sql;
        HistoryEntry(Date time, String sql) { this.time = time; this.sql = sql; }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeLater(() -> new SQLViewerJava().setVisible(true));
    }
}
