const { app, BrowserWindow, Menu } = require('electron');
const path = require('path');

let mainWindow;

// Скрыть стандартное меню браузера
Menu.setApplicationMenu(null);

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    minWidth: 800,
    minHeight: 600,
    webPreferences: {
      // Безопасность
      nodeIntegration: false,
      contextIsolation: true,
      // Включение Web Serial API
      enableBlinkFeatures: 'Serial',
      // Preload скрипт (опционально)
      preload: path.join(__dirname, 'preload.js')
    },
    backgroundColor: '#e0e7ff',
    show: false // Не показывать окно до полной загрузки
  });

  // Загрузка локального HTML файла
  mainWindow.loadFile('index.html');

  // Показать окно когда готово
  mainWindow.once('ready-to-show', () => {
    mainWindow.show();
  });

  // Обработка выбора последовательного порта
  mainWindow.webContents.session.on('select-serial-port', (event, portList, webContents, callback) => {
    // Предотвращаем стандартное поведение
    event.preventDefault();
    
    // Логирование доступных портов (для отладки)
    console.log('Доступные serial порты:', portList);
    
    // Если есть порты, автоматически выбираем первый
    // В продакшене можно добавить диалог выбора порта
    if (portList && portList.length > 0) {
      callback(portList[0].portId);
    } else {
      console.log('Serial порты не найдены');
      callback(''); // Пустая строка означает отмену
    }
  });

  // Обработка добавления нового порта
  mainWindow.webContents.session.on('serial-port-added', (event, port) => {
    console.log('Добавлен новый serial порт:', port);
  });

  // Обработка удаления порта
  mainWindow.webContents.session.on('serial-port-removed', (event, port) => {
    console.log('Удален serial порт:', port);
  });

  // Обработка предоставления доступа к устройству
  mainWindow.webContents.session.setDevicePermissionHandler((details) => {
    console.log('Запрос доступа к устройству:', details);
    // Автоматически разрешаем доступ к serial устройствам
    if (details.deviceType === 'serial') {
      return true;
    }
    return false;
  });

  // DevTools для отладки (закомментируйте в продакшене)
  // mainWindow.webContents.openDevTools();

  mainWindow.on('closed', () => {
    mainWindow = null;
  });
}

// Создать окно когда Electron готов
app.whenReady().then(() => {
  createWindow();

  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow();
    }
  });
});

// Выход когда все окна закрыты (кроме macOS)
app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

// Обработка необработанных исключений
process.on('uncaughtException', (error) => {
  console.error('Необработанное исключение:', error);
});
