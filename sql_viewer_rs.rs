// sql_viewer_rs.rs — SQL Viewer с историей запросов на Rust (консоль)

use rusqlite::{Connection, Result};
use serde::{Deserialize, Serialize};
use std::fs;
use std::io::{self, Write, BufRead};
use std::time::{SystemTime, UNIX_EPOCH};

#[derive(Serialize, Deserialize, Clone)]
struct HistoryEntry {
    time: String,
    sql: String,
}

struct App {
    conn: Option<Connection>,
    history: Vec<HistoryEntry>,
    hist_file: String,
}

impl App {
    fn new() -> Self {
        App {
            conn: None,
            history: Vec::new(),
            hist_file: "sql_history.json".to_string(),
        }
    }

    fn load_history(&mut self) {
        if let Ok(data) = fs::read_to_string(&self.hist_file) {
            if let Ok(hist) = serde_json::from_str(&data) {
                self.history = hist;
            }
        }
    }

    fn save_history(&self) {
        if let Ok(json) = serde_json::to_string_pretty(&self.history) {
            let _ = fs::write(&self.hist_file, json);
        }
    }

    fn connect(&mut self, path: &str) -> Result<()> {
        let conn = Connection::open(path)?;
        self.conn = Some(conn);
        Ok(())
    }

    fn query(&mut self, sql: &str) -> Result<()> {
        if let Some(ref conn) = self.conn {
            let mut stmt = conn.prepare(sql)?;
            let cols = stmt.column_count();
            let mut rows = stmt.query([])?;
            // Печать заголовков
            for i in 0..cols {
                print!("{}", stmt.column_name(i).unwrap_or(""));
                if i < cols-1 { print!(" | "); }
            }
            println!();
            let mut count = 0;
            while let Some(row) = rows.next()? {
                for i in 0..cols {
                    let val: String = row.get(i).unwrap_or("NULL".to_string());
                    print!("{}", val);
                    if i < cols-1 { print!(" | "); }
                }
                println!();
                count += 1;
            }
            println!("Всего строк: {}", count);
            // Сохраняем историю
            let time = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs();
            self.history.push(HistoryEntry {
                time: time.to_string(),
                sql: sql.to_string(),
            });
            self.save_history();
        } else {
            println!("База данных не подключена");
        }
        Ok(())
    }

    fn exec(&mut self, sql: &str) -> Result<()> {
        if let Some(ref conn) = self.conn {
            let affected = conn.execute(sql, [])?;
            println!("Затронуто строк: {}", affected);
            let time = SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs();
            self.history.push(HistoryEntry {
                time: time.to_string(),
                sql: sql.to_string(),
            });
            self.save_history();
        } else {
            println!("База данных не подключена");
        }
        Ok(())
    }

    fn show_history(&self) {
        if self.history.is_empty() {
            println!("История пуста");
            return;
        }
        for (i, h) in self.history.iter().enumerate() {
            println!("{}. [{}] {}", i+1, h.time, h.sql);
        }
    }

    fn clear_history(&mut self) {
        self.history.clear();
        self.save_history();
        println!("История очищена");
    }

    fn run(&mut self) {
        self.load_history();
        let stdin = io::stdin();
        let mut reader = stdin.lock();
        println!("🗄️ SQLViewer Pro — Rust Edition");
        println!("Команды: connect <file>, query <sql>, exec <sql>, history, clear, exit");
        loop {
            print!("> ");
            io::stdout().flush().unwrap();
            let mut line = String::new();
            if reader.read_line(&mut line).is_err() { break; }
            let line = line.trim();
            if line.is_empty() { continue; }
            let parts: Vec<&str> = line.splitn(2, ' ').collect();
            let cmd = parts[0];
            let arg = if parts.len() > 1 { parts[1] } else { "" };
            match cmd {
                "connect" => {
                    if let Err(e) = self.connect(arg) {
                        println!("Ошибка: {}", e);
                    } else {
                        println!("Подключено к {}", arg);
                    }
                }
                "query" => {
                    if arg.is_empty() { println!("Укажите SQL-запрос"); continue; }
                    if let Err(e) = self.query(arg) {
                        println!("Ошибка: {}", e);
                    }
                }
                "exec" => {
                    if arg.is_empty() { println!("Укажите SQL-команду"); continue; }
                    if let Err(e) = self.exec(arg) {
                        println!("Ошибка: {}", e);
                    }
                }
                "history" => self.show_history(),
                "clear" => self.clear_history(),
                "exit" => {
                    self.save_history();
                    println!("До свидания!");
                    return;
                }
                _ => println!("Неизвестная команда"),
            }
        }
    }
}

fn main() {
    App::new().run();
}
