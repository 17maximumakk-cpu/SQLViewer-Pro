// sql_viewer_go.go — SQL Viewer с историей запросов на Go (консоль)

package main

import (
	"bufio"
	"database/sql"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	_ "github.com/mattn/go-sqlite3"
)

type HistoryEntry struct {
	Time string `json:"time"`
	SQL  string `json:"sql"`
}

type App struct {
	db       *sql.DB
	history  []HistoryEntry
	histFile string
}

func NewApp() *App {
	return &App{histFile: "sql_history.json"}
}

func (a *App) loadHistory() {
	data, err := ioutil.ReadFile(a.histFile)
	if err == nil {
		json.Unmarshal(data, &a.history)
	}
}

func (a *App) saveHistory() {
	data, _ := json.MarshalIndent(a.history, "", "  ")
	ioutil.WriteFile(a.histFile, data, 0644)
}

func (a *App) connect(dbPath string) error {
	db, err := sql.Open("sqlite3", dbPath)
	if err != nil {
		return err
	}
	a.db = db
	return nil
}

func (a *App) query(sqlStr string) error {
	if a.db == nil {
		return fmt.Errorf("база данных не подключена")
	}
	rows, err := a.db.Query(sqlStr)
	if err != nil {
		return err
	}
	defer rows.Close()
	cols, _ := rows.Columns()
	fmt.Println(strings.Join(cols, " | "))
	vals := make([]interface{}, len(cols))
	valPtrs := make([]interface{}, len(cols))
	for i := range vals {
		valPtrs[i] = &vals[i]
	}
	count := 0
	for rows.Next() {
		rows.Scan(valPtrs...)
		row := make([]string, len(cols))
		for i, v := range vals {
			if v == nil {
				row[i] = "NULL"
			} else {
				row[i] = fmt.Sprintf("%v", v)
			}
		}
		fmt.Println(strings.Join(row, " | "))
		count++
	}
	fmt.Printf("Всего строк: %d\n", count)
	// Сохраняем историю
	a.history = append(a.history, HistoryEntry{Time: time.Now().Format(time.RFC3339), SQL: sqlStr})
	a.saveHistory()
	return nil
}

func (a *App) exec(sqlStr string) error {
	if a.db == nil {
		return fmt.Errorf("база данных не подключена")
	}
	res, err := a.db.Exec(sqlStr)
	if err != nil {
		return err
	}
	affected, _ := res.RowsAffected()
	fmt.Printf("Затронуто строк: %d\n", affected)
	a.history = append(a.history, HistoryEntry{Time: time.Now().Format(time.RFC3339), SQL: sqlStr})
	a.saveHistory()
	return nil
}

func (a *App) showHistory() {
	if len(a.history) == 0 {
		fmt.Println("История пуста")
		return
	}
	for i, h := range a.history {
		fmt.Printf("%d. [%s] %s\n", i+1, h.Time, h.SQL)
	}
}

func (a *App) exportCSV(sqlStr string) {
	// Для простоты выведем в консоль
	fmt.Println("Экспорт в CSV (только консольный вывод)")
}

func (a *App) clearHistory() {
	a.history = nil
	a.saveHistory()
	fmt.Println("История очищена")
}

func (a *App) run() {
	a.loadHistory()
	scanner := bufio.NewScanner(os.Stdin)
	fmt.Println("🗄️ SQLViewer Pro — Go Edition")
	fmt.Println("Команды: connect <file>, query <sql>, exec <sql>, history, export, clear, exit")
	for {
		fmt.Print("> ")
		if !scanner.Scan() {
			break
		}
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}
		parts := strings.SplitN(line, " ", 2)
		cmd := parts[0]
		arg := ""
		if len(parts) > 1 {
			arg = parts[1]
		}
		switch cmd {
		case "connect":
			if err := a.connect(arg); err != nil {
				fmt.Println("Ошибка:", err)
			} else {
				fmt.Println("Подключено к", arg)
			}
		case "query":
			if arg == "" {
				fmt.Println("Укажите SQL-запрос")
				continue
			}
			if err := a.query(arg); err != nil {
				fmt.Println("Ошибка:", err)
			}
		case "exec":
			if arg == "" {
				fmt.Println("Укажите SQL-команду")
				continue
			}
			if err := a.exec(arg); err != nil {
				fmt.Println("Ошибка:", err)
			}
		case "history":
			a.showHistory()
		case "export":
			a.exportCSV(arg)
		case "clear":
			a.clearHistory()
		case "exit":
			a.saveHistory()
			if a.db != nil {
				a.db.Close()
			}
			fmt.Println("До свидания!")
			return
		default:
			fmt.Println("Неизвестная команда")
		}
	}
}

func main() {
	NewApp().run()
}
