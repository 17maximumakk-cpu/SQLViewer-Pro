// sql_viewer_cpp.cpp — SQL Viewer с историей запросов на C++ (Qt)

#include <QApplication>
#include <QMainWindow>
#include <QWidget>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QPushButton>
#include <QTextEdit>
#include <QTableView>
#include <QSqlDatabase>
#include <QSqlQuery>
#include <QSqlQueryModel>
#include <QFileDialog>
#include <QMessageBox>
#include <QSettings>
#include <QJsonDocument>
#include <QJsonArray>
#include <QJsonObject>
#include <QFile>
#include <QTextStream>
#include <QDateTime>
#include <QHeaderView>

class SQLViewer : public QMainWindow {
    Q_OBJECT
public:
    SQLViewer(QWidget *parent = nullptr) : QMainWindow(parent) {
        setWindowTitle("🗄️ SQLViewer Pro — C++");
        resize(900, 700);
        loadHistory();
        createUI();
    }

private slots:
    void connectDB() {
        QString filename = QFileDialog::getOpenFileName(this, "Выберите базу данных", "", "SQLite (*.db *.sqlite)");
        if (filename.isEmpty()) return;
        db = QSqlDatabase::addDatabase("QSQLITE");
        db.setDatabaseName(filename);
        if (db.open()) {
            statusLabel->setText("Подключено к " + QFileInfo(filename).fileName());
        } else {
            QMessageBox::warning(this, "Ошибка", "Не удалось подключиться к БД");
        }
    }

    void executeQuery() {
        if (!db.isOpen()) {
            QMessageBox::warning(this, "Предупреждение", "Сначала подключите базу данных");
            return;
        }
        QString sql = sqlEdit->toPlainText().trimmed();
        if (sql.isEmpty()) return;
        QSqlQuery query;
        if (query.exec(sql)) {
            QSqlQueryModel *model = new QSqlQueryModel;
            model->setQuery(query);
            tableView->setModel(model);
            tableView->horizontalHeader()->setSectionResizeMode(QHeaderView::Stretch);
            statusLabel->setText("Выполнено");
            // Сохраняем историю
            QJsonObject entry;
            entry["time"] = QDateTime::currentDateTime().toString(Qt::ISODate);
            entry["sql"] = sql;
            history.append(entry);
            saveHistory();
        } else {
            QMessageBox::warning(this, "Ошибка SQL", query.lastError().text());
        }
    }

    void showHistory() {
        if (history.isEmpty()) {
            QMessageBox::information(this, "История", "История пуста");
            return;
        }
        QString text;
        for (const auto &entry : history) {
            text += QString("[%1] %2\n").arg(entry["time"].toString()).arg(entry["sql"].toString());
        }
        QMessageBox msgBox;
        msgBox.setWindowTitle("История запросов");
        msgBox.setText(text);
        msgBox.exec();
    }

    void exportCSV() {
        QSqlQueryModel *model = qobject_cast<QSqlQueryModel*>(tableView->model());
        if (!model || model->rowCount() == 0) {
            QMessageBox::information(this, "Экспорт", "Нет данных для экспорта");
            return;
        }
        QString filename = QFileDialog::getSaveFileName(this, "Сохранить CSV", "", "CSV (*.csv)");
        if (filename.isEmpty()) return;
        QFile file(filename);
        if (file.open(QIODevice::WriteOnly | QIODevice::Text)) {
            QTextStream out(&file);
            // Заголовки
            for (int i = 0; i < model->columnCount(); ++i) {
                out << model->headerData(i, Qt::Horizontal).toString() << (i < model->columnCount()-1 ? "," : "");
            }
            out << "\n";
            // Данные
            for (int r = 0; r < model->rowCount(); ++r) {
                for (int c = 0; c < model->columnCount(); ++c) {
                    out << model->data(model->index(r, c)).toString() << (c < model->columnCount()-1 ? "," : "");
                }
                out << "\n";
            }
            file.close();
            statusLabel->setText("Экспортировано в " + filename);
        }
    }

    void clearHistory() {
        if (QMessageBox::question(this, "Очистка", "Очистить всю историю?") == QMessageBox::Yes) {
            history.clear();
            saveHistory();
            statusLabel->setText("История очищена");
        }
    }

private:
    QSqlDatabase db;
    QTextEdit *sqlEdit;
    QTableView *tableView;
    QLabel *statusLabel;
    QJsonArray history;
    QString historyFile = "sql_history.json";

    void createUI() {
        QWidget *central = new QWidget(this);
        setCentralWidget(central);
        QVBoxLayout *mainLayout = new QVBoxLayout(central);

        // Toolbar
        QHBoxLayout *toolbar = new QHBoxLayout();
        QPushButton *connectBtn = new QPushButton("Подключить БД");
        QPushButton *execBtn = new QPushButton("Выполнить");
        QPushButton *histBtn = new QPushButton("История");
        QPushButton *exportBtn = new QPushButton("Экспорт CSV");
        QPushButton *clearBtn = new QPushButton("Очистить историю");
        toolbar->addWidget(connectBtn);
        toolbar->addWidget(execBtn);
        toolbar->addWidget(histBtn);
        toolbar->addWidget(exportBtn);
        toolbar->addWidget(clearBtn);
        mainLayout->addLayout(toolbar);

        // SQL Editor
        sqlEdit = new QTextEdit;
        sqlEdit->setFont(QFont("Courier", 12));
        sqlEdit->setFixedHeight(150);
        mainLayout->addWidget(sqlEdit);

        // Table
        tableView = new QTableView;
        mainLayout->addWidget(tableView);

        // Status
        statusLabel = new QLabel("Готов");
        mainLayout->addWidget(statusLabel);

        connect(connectBtn, &QPushButton::clicked, this, &SQLViewer::connectDB);
        connect(execBtn, &QPushButton::clicked, this, &SQLViewer::executeQuery);
        connect(histBtn, &QPushButton::clicked, this, &SQLViewer::showHistory);
        connect(exportBtn, &QPushButton::clicked, this, &SQLViewer::exportCSV);
        connect(clearBtn, &QPushButton::clicked, this, &SQLViewer::clearHistory);
    }

    void loadHistory() {
        QFile file(historyFile);
        if (file.open(QIODevice::ReadOnly)) {
            QJsonDocument doc = QJsonDocument::fromJson(file.readAll());
            if (doc.isArray()) history = doc.array();
            file.close();
        }
    }

    void saveHistory() {
        QFile file(historyFile);
        if (file.open(QIODevice::WriteOnly)) {
            file.write(QJsonDocument(history).toJson());
            file.close();
        }
    }
};

int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    SQLViewer w;
    w.show();
    return app.exec();
}

#include "sql_viewer_cpp.moc"
