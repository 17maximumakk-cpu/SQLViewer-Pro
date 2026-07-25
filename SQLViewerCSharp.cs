// SQLViewerCSharp.cs — SQL Viewer с историей запросов на C# (WPF)

using System;
using System.Collections.Generic;
using System.Data;
using System.Data.SQLite;
using System.IO;
using System.Linq;
using System.Text.Json;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

namespace SQLViewerWPF
{
    public partial class MainWindow : Window
    {
        private SQLiteConnection conn;
        private List<HistoryEntry> history = new List<HistoryEntry>();
        private string historyFile = "sql_history.json";
        private DataTable resultTable = new DataTable();

        private TextBox sqlBox;
        private DataGrid dataGrid;
        private Label statusLabel;

        public MainWindow()
        {
            InitializeComponent();
            LoadHistory();
            CreateUI();
            this.Closing += (s, e) => SaveHistory();
        }

        private void CreateUI()
        {
            Title = "🗄️ SQLViewer Pro — C#";
            Width = 900;
            Height = 700;
            var grid = new Grid();
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });
            grid.RowDefinitions.Add(new RowDefinition { Height = new GridLength(1, GridUnitType.Star) });
            grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            // Toolbar
            var toolbar = new StackPanel { Orientation = Orientation.Horizontal };
            var connectBtn = new Button { Content = "Подключить БД" };
            var execBtn = new Button { Content = "Выполнить" };
            var histBtn = new Button { Content = "История" };
            var exportBtn = new Button { Content = "Экспорт CSV" };
            var clearBtn = new Button { Content = "Очистить историю" };
            toolbar.Children.Add(connectBtn);
            toolbar.Children.Add(execBtn);
            toolbar.Children.Add(histBtn);
            toolbar.Children.Add(exportBtn);
            toolbar.Children.Add(clearBtn);
            Grid.SetRow(toolbar, 0);
            grid.Children.Add(toolbar);

            // SQL Editor
            sqlBox = new TextBox { AcceptsReturn = true, TextWrapping = TextWrapping.Wrap, Height = 100, FontFamily = new System.Windows.Media.FontFamily("Consolas") };
            sqlBox.PreviewKeyDown += (s, e) => {
                if (e.Key == Key.Enter && Keyboard.Modifiers == ModifierKeys.Control) ExecuteQuery();
            };
            Grid.SetRow(sqlBox, 1);
            grid.Children.Add(sqlBox);

            // DataGrid
            dataGrid = new DataGrid { AutoGenerateColumns = true };
            Grid.SetRow(dataGrid, 2);
            grid.Children.Add(dataGrid);

            // Status
            statusLabel = new Label { Content = "Готов" };
            Grid.SetRow(statusLabel, 3);
            grid.Children.Add(statusLabel);

            Content = grid;

            connectBtn.Click += (s, e) => ConnectDB();
            execBtn.Click += (s, e) => ExecuteQuery();
            histBtn.Click += (s, e) => ShowHistory();
            exportBtn.Click += (s, e) => ExportCSV();
            clearBtn.Click += (s, e) => ClearHistory();
        }

        private void ConnectDB()
        {
            var dialog = new Microsoft.Win32.OpenFileDialog { Filter = "SQLite (*.db;*.sqlite)|*.db;*.sqlite" };
            if (dialog.ShowDialog() == true)
            {
                try
                {
                    conn = new SQLiteConnection($"Data Source={dialog.FileName};Version=3;");
                    conn.Open();
                    statusLabel.Content = $"Подключено к {System.IO.Path.GetFileName(dialog.FileName)}";
                }
                catch (Exception e)
                {
                    MessageBox.Show($"Ошибка: {e.Message}");
                }
            }
        }

        private void ExecuteQuery()
        {
            if (conn == null) { MessageBox.Show("Сначала подключите БД"); return; }
            string sql = sqlBox.Text.Trim();
            if (string.IsNullOrEmpty(sql)) return;
            try
            {
                using var cmd = new SQLiteCommand(sql, conn);
                if (sql.ToLower().StartsWith("select"))
                {
                    var adapter = new SQLiteDataAdapter(cmd);
                    resultTable = new DataTable();
                    adapter.Fill(resultTable);
                    dataGrid.ItemsSource = resultTable.DefaultView;
                    statusLabel.Content = $"Выполнено: {resultTable.Rows.Count} строк";
                }
                else
                {
                    int affected = cmd.ExecuteNonQuery();
                    statusLabel.Content = $"Выполнено: {affected} строк затронуто";
                }
                history.Add(new HistoryEntry { Time = DateTime.Now, Sql = sql });
                SaveHistory();
            }
            catch (Exception e)
            {
                MessageBox.Show($"Ошибка SQL: {e.Message}");
            }
        }

        private void ShowHistory()
        {
            if (history.Count == 0) { MessageBox.Show("История пуста"); return; }
            var sb = new System.Text.StringBuilder();
            foreach (var h in history) sb.Append($"[{h.Time}] {h.Sql}\n");
            MessageBox.Show(sb.ToString(), "История запросов");
        }

        private void ExportCSV()
        {
            if (resultTable.Rows.Count == 0) { MessageBox.Show("Нет данных для экспорта"); return; }
            var dialog = new Microsoft.Win32.SaveFileDialog { Filter = "CSV (*.csv)|*.csv" };
            if (dialog.ShowDialog() == true)
            {
                using var sw = new StreamWriter(dialog.FileName);
                sw.WriteLine(string.Join(",", resultTable.Columns.Cast<DataColumn>().Select(c => c.ColumnName)));
                foreach (DataRow row in resultTable.Rows)
                    sw.WriteLine(string.Join(",", row.ItemArray));
                statusLabel.Content = $"Экспортировано в {System.IO.Path.GetFileName(dialog.FileName)}";
            }
        }

        private void ClearHistory()
        {
            if (MessageBox.Show("Очистить историю?", "Подтверждение", MessageBoxButton.YesNo) == MessageBoxResult.Yes)
            {
                history.Clear();
                SaveHistory();
                statusLabel.Content = "История очищена";
            }
        }

        private void LoadHistory()
        {
            if (File.Exists(historyFile))
            {
                string json = File.ReadAllText(historyFile);
                history = JsonSerializer.Deserialize<List<HistoryEntry>>(json) ?? new List<HistoryEntry>();
            }
        }

        private void SaveHistory()
        {
            string json = JsonSerializer.Serialize(history, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(historyFile, json);
        }

        public class HistoryEntry { public DateTime Time { get; set; } public string Sql { get; set; } }

        [STAThread]
        static void Main()
        {
            var app = new Application();
            app.Run(new MainWindow());
        }
    }
}
