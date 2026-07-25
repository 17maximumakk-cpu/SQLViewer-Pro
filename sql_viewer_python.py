# sql_viewer_python.py — SQL Viewer с историей запросов на Python (Tkinter)

import tkinter as tk
from tkinter import ttk, scrolledtext, messagebox, filedialog
import sqlite3
import json
import os
from datetime import datetime

class SQLViewer:
    def __init__(self, root):
        self.root = root
        self.root.title("🗄️ SQLViewer Pro — Python")
        self.root.geometry("900x700")
        self.conn = None
        self.cursor = None
        self.history = []
        self.history_file = "sql_history.json"
        self.load_history()
        self.create_widgets()
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)

    def create_widgets(self):
        # Верхняя панель
        top = tk.Frame(self.root)
        top.pack(fill=tk.X, pady=5)
        tk.Button(top, text="Подключить БД", command=self.connect_db).pack(side=tk.LEFT, padx=5)
        tk.Button(top, text="Выполнить", command=self.execute_query).pack(side=tk.LEFT, padx=5)
        tk.Button(top, text="История", command=self.show_history).pack(side=tk.LEFT, padx=5)
        tk.Button(top, text="Экспорт CSV", command=self.export_csv).pack(side=tk.LEFT, padx=5)
        tk.Button(top, text="Очистить историю", command=self.clear_history).pack(side=tk.LEFT, padx=5)

        # Редактор SQL
        self.sql_editor = scrolledtext.ScrolledText(self.root, height=6, font=("Courier", 12))
        self.sql_editor.pack(fill=tk.X, padx=10, pady=5)
        self.sql_editor.bind("<Control-Return>", lambda e: self.execute_query())

        # Результаты
        self.result_tree = ttk.Treeview(self.root)
        self.result_tree.pack(fill=tk.BOTH, expand=True, padx=10, pady=5)

        # Статус
        self.status = tk.Label(self.root, text="Готов", anchor=tk.W)
        self.status.pack(fill=tk.X, padx=10)

        # Горячие клавиши
        self.root.bind("<Control-r>", lambda e: self.execute_query())
        self.root.bind("<Control-h>", lambda e: self.show_history())

    def connect_db(self):
        filename = filedialog.askopenfilename(filetypes=[("SQLite DB", "*.db *.sqlite")])
        if filename:
            try:
                self.conn = sqlite3.connect(filename)
                self.cursor = self.conn.cursor()
                self.status.config(text=f"Подключено к {os.path.basename(filename)}")
            except Exception as e:
                messagebox.showerror("Ошибка", f"Не удалось подключиться: {e}")

    def execute_query(self):
        if not self.conn:
            messagebox.showwarning("Предупреждение", "Сначала подключите базу данных")
            return
        sql = self.sql_editor.get("1.0", tk.END).strip()
        if not sql:
            return
        try:
            self.cursor.execute(sql)
            if sql.lower().strip().startswith("select"):
                rows = self.cursor.fetchall()
                columns = [desc[0] for desc in self.cursor.description]
                self.display_results(columns, rows)
                self.status.config(text=f"Выполнено: {len(rows)} строк")
            else:
                self.conn.commit()
                self.status.config(text=f"Выполнено: {self.cursor.rowcount} строк затронуто")
            # Сохраняем в историю
            self.history.append({"time": datetime.now().isoformat(), "sql": sql})
            self.save_history()
        except Exception as e:
            messagebox.showerror("Ошибка SQL", str(e))

    def display_results(self, columns, rows):
        self.result_tree.delete(*self.result_tree.get_children())
        self.result_tree["columns"] = columns
        self.result_tree["show"] = "headings"
        for col in columns:
            self.result_tree.heading(col, text=col)
            self.result_tree.column(col, width=100)
        for row in rows:
            self.result_tree.insert("", "end", values=row)

    def show_history(self):
        if not self.history:
            messagebox.showinfo("История", "История пуста")
            return
        win = tk.Toplevel(self.root)
        win.title("История запросов")
        win.geometry("600x400")
        listbox = tk.Listbox(win, font=("Courier", 10))
        listbox.pack(fill=tk.BOTH, expand=True)
        for entry in self.history:
            listbox.insert(tk.END, f"[{entry['time']}] {entry['sql'][:80]}...")
        def load_selected():
            sel = listbox.curselection()
            if sel:
                idx = sel[0]
                self.sql_editor.delete("1.0", tk.END)
                self.sql_editor.insert("1.0", self.history[idx]["sql"])
                win.destroy()
        tk.Button(win, text="Загрузить запрос", command=load_selected).pack(pady=5)

    def export_csv(self):
        if not self.result_tree.get_children():
            messagebox.showinfo("Экспорт", "Нет данных для экспорта")
            return
        filename = filedialog.asksaveasfilename(defaultextension=".csv", filetypes=[("CSV", "*.csv")])
        if filename:
            import csv
            columns = self.result_tree["columns"]
            with open(filename, 'w', newline='') as f:
                writer = csv.writer(f)
                writer.writerow(columns)
                for item in self.result_tree.get_children():
                    row = self.result_tree.item(item)["values"]
                    writer.writerow(row)
            self.status.config(text=f"Экспортировано в {filename}")

    def clear_history(self):
        if messagebox.askyesno("Очистка", "Очистить всю историю?"):
            self.history = []
            self.save_history()
            self.status.config(text="История очищена")

    def load_history(self):
        if os.path.exists(self.history_file):
            with open(self.history_file, 'r') as f:
                self.history = json.load(f)

    def save_history(self):
        with open(self.history_file, 'w') as f:
            json.dump(self.history, f, indent=2)

    def on_close(self):
        if self.conn:
            self.conn.close()
        self.save_history()
        self.root.destroy()

if __name__ == "__main__":
    root = tk.Tk()
    app = SQLViewer(root)
    root.mainloop()
