🗄️ SQLViewer Pro — просмотр SQL с историей запросов
Интерактивный инструмент для выполнения SQL-запросов с сохранением истории, автодополнением, экспортом и поддержкой нескольких СУБД.
Реализован на 7 языках программирования для демонстрации работы с базами данных и пользовательскими интерфейсами.

https://img.shields.io/github/repo-size/yourname/sqlviewer
https://img.shields.io/github/stars/yourname/sqlviewer?style=social
https://img.shields.io/badge/License-MIT-blue.svg

🧠 Концепция
SQLViewer Pro — это мощный инструмент для работы с базами данных, который позволяет:

✅ Подключаться к SQLite, PostgreSQL, MySQL (в зависимости от версии).

✅ Выполнять SQL-запросы с отображением результатов в таблице.

✅ Сохранять историю всех выполненных запросов с временными метками.

✅ Просматривать историю, удалять записи, экспортировать в CSV/JSON.

✅ Автодополнение ключевых слов SQL (в GUI-версиях).

✅ Подсветка синтаксиса SQL (в некоторых версиях).

✅ Экспорт результатов в CSV, JSON, HTML.

✅ Горячие клавиши для быстрого доступа к функциям.

✅ Кроссплатформенность — работает на Windows, Linux, macOS.

🚀 Как запустить
Для каждой версии требуются соответствующие библиотеки. Инструкции по установке и запуску:

Python
bash
pip install sqlite3 (встроен) tkinter
python sql_viewer_python.py
C++ (Qt)
bash
# Требуется Qt5 и SQLite (встроен)
qmake && make
./sql_viewer_cpp
Java
bash
javac SQLViewerJava.java && java SQLViewerJava
C# (.NET Core)
bash
dotnet add package System.Data.SQLite
dotnet run
Go
bash
go get github.com/mattn/go-sqlite3
go run sql_viewer_go.go
Rust
bash
cargo add rusqlite
cargo run
JavaScript (Node.js)
bash
npm install sqlite3
node sql_viewer_js.js
🧩 Пример сессии (консольная версия)
text
$ sqlviewer connect test.db
✅ Подключено к test.db

> query SELECT * FROM users;
ID | NAME     | AGE
1  | Alice    | 25
2  | Bob      | 30
3  | Charlie  | 28

> history
  1  [2025-01-20 10:00] SELECT * FROM users;
  2  [2025-01-20 10:05] SELECT name FROM users WHERE age > 25;

> export history.csv
✅ История сохранена в history.csv

> exit
📦 Содержимое репозитория
Файл	Язык	Особенности
sql_viewer_python.py	Python	Tkinter GUI, SQLite, история в JSON, автодополнение
sql_viewer_cpp.cpp	C++	Qt Widgets, QSqlQueryModel, история, экспорт CSV
SQLViewerJava.java	Java	Swing, JDBC, история, экспорт
SQLViewerCSharp.cs	C#	WPF, SQLite, история, автодополнение
sql_viewer_go.go	Go	консоль, sqlite3, история, команды
sql_viewer_rs.rs	Rust	консоль, rusqlite, история, команды
sql_viewer_js.js	JavaScript	Node.js, sqlite3, интерактивный режим
🔮 Расширенные функции
Поддержка PostgreSQL и MySQL (опционально).

Визуализация данных в виде графиков (в планах).

Экспорт в Excel (в некоторых версиях).

Синхронизация с облачными сервисами (в планах).

📜 Лицензия
MIT — свободно используйте, модифицируйте и распространяйте.

🤝 Вклад
Приветствуются пул-реквесты с улучшениями, поддержкой новых платформ и расширением функциональности.
