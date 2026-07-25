// sql_viewer_js.js — SQL Viewer с историей запросов на JavaScript (Node.js)

const sqlite3 = require('sqlite3').verbose();
const readline = require('readline');
const fs = require('fs');

const rl = readline.createInterface({
    input: process.stdin,
    output: process.stdout,
    prompt: '> '
});

let db = null;
let history = [];
const histFile = 'sql_history.json';

function loadHistory() {
    try {
        if (fs.existsSync(histFile)) {
            const data = fs.readFileSync(histFile, 'utf8');
            history = JSON.parse(data);
        }
    } catch (e) {}
}

function saveHistory() {
    fs.writeFileSync(histFile, JSON.stringify(history, null, 2));
}

function connectDB(path) {
    return new Promise((resolve, reject) => {
        db = new sqlite3.Database(path, (err) => {
            if (err) reject(err);
            else resolve();
        });
    });
}

function querySQL(sql) {
    return new Promise((resolve, reject) => {
        if (!db) { reject(new Error('База данных не подключена')); return; }
        db.all(sql, (err, rows) => {
            if (err) reject(err);
            else resolve(rows);
        });
    });
}

function execSQL(sql) {
    return new Promise((resolve, reject) => {
        if (!db) { reject(new Error('База данных не подключена')); return; }
        db.run(sql, function(err) {
            if (err) reject(err);
            else resolve(this.changes);
        });
    });
}

async function processCommand(cmd, arg) {
    try {
        switch (cmd) {
            case 'connect':
                await connectDB(arg);
                console.log(`✅ Подключено к ${arg}`);
                break;
            case 'query':
                if (!arg) { console.log('Укажите SQL-запрос'); break; }
                const rows = await querySQL(arg);
                if (rows.length > 0) {
                    const cols = Object.keys(rows[0]);
                    console.log(cols.join(' | '));
                    rows.forEach(row => {
                        console.log(cols.map(c => row[c] !== null ? row[c] : 'NULL').join(' | '));
                    });
                }
                console.log(`Всего строк: ${rows.length}`);
                history.push({ time: new Date().toISOString(), sql: arg });
                saveHistory();
                break;
            case 'exec':
                if (!arg) { console.log('Укажите SQL-команду'); break; }
                const affected = await execSQL(arg);
                console.log(`Затронуто строк: ${affected}`);
                history.push({ time: new Date().toISOString(), sql: arg });
                saveHistory();
                break;
            case 'history':
                if (history.length === 0) { console.log('История пуста'); break; }
                history.forEach((h, i) => {
                    console.log(`${i+1}. [${h.time}] ${h.sql}`);
                });
                break;
            case 'clear':
                history = [];
                saveHistory();
                console.log('История очищена');
                break;
            case 'exit':
                if (db) db.close();
                saveHistory();
                console.log('До свидания!');
                process.exit(0);
                break;
            default:
                console.log('Неизвестная команда');
        }
    } catch (e) {
        console.error('Ошибка:', e.message);
    }
}

loadHistory();
console.log('🗄️ SQLViewer Pro — JavaScript Edition');
console.log('Команды: connect <file>, query <sql>, exec <sql>, history, clear, exit');
rl.prompt();

rl.on('line', async (line) => {
    const parts = line.trim().split(/\s+/);
    const cmd = parts[0];
    const arg = parts.slice(1).join(' ');
    await processCommand(cmd, arg);
    rl.prompt();
}).on('close', () => {
    if (db) db.close();
    saveHistory();
    process.exit(0);
});
