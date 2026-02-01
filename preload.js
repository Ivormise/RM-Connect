/**
 * Preload скрипт для безопасного взаимодействия между процессами
 * В текущей реализации Web Serial API доступен напрямую в renderer процессе
 * благодаря enableBlinkFeatures: 'Serial'
 */

const { contextBridge } = require('electron');

// Экспорт полезных функций в renderer процесс (опционально)
contextBridge.exposeInMainWorld('electronAPI', {
  platform: process.platform,
  versions: {
    node: process.versions.node,
    chrome: process.versions.chrome,
    electron: process.versions.electron
  }
});

// Логирование для отладки
console.log('Preload скрипт загружен');
console.log('Platform:', process.platform);
console.log('Web Serial API доступен через navigator.serial');
