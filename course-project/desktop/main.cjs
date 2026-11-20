const { app, BrowserWindow, dialog, shell } = require('electron');
const path = require('node:path');
const fs = require('node:fs');
const { startLocalServer, parseApiOrigin } = require('./local-server.cjs');

let localServer;
let applicationOrigin;

function readApiOrigin() {
  const configurationPath = path.join(app.getPath('userData'), 'desktop-config.json');
  const configuration = fs.existsSync(configurationPath)
    ? JSON.parse(fs.readFileSync(configurationPath, 'utf8')) : {};
  return parseApiOrigin(process.env.FEYNMAN_API_ORIGIN || configuration.apiOrigin).origin;
}

async function createWindow() {
  const window = new BrowserWindow({
    width: 1440,
    height: 920,
    minWidth: 960,
    minHeight: 680,
    backgroundColor: '#f4f4ef', show: false,
    icon: path.join(__dirname, 'assets', 'icon.png'),
    webPreferences: {
      preload: path.join(__dirname, 'preload.cjs'),
      contextIsolation: true,
      nodeIntegration: false, sandbox: true
    }
  });

  const trusted = (url) => {
    try { return new URL(url).origin === applicationOrigin; } catch { return false; }
  };
  window.webContents.session.setPermissionCheckHandler((_contents, permission, requestingOrigin, details) =>
    permission === 'media' && details.mediaType !== 'video' && trusted(requestingOrigin));
  window.webContents.session.setPermissionRequestHandler((contents, permission, callback, details) => {
    const audioOnly = details.mediaTypes?.length > 0 && details.mediaTypes.every((type) => type === 'audio');
    callback(permission === 'media' && audioOnly && trusted(contents.getURL()) && trusted(details.requestingUrl));
  });
  window.webContents.on('will-navigate', (event, url) => {
    if (!trusted(url)) event.preventDefault();
  });
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (url.startsWith('https://')) shell.openExternal(url);
    return { action: 'deny' };
  });
  window.once('ready-to-show', () => window.show());
  await window.loadURL(applicationOrigin);
}

if (!app.requestSingleInstanceLock()) {
  app.quit();
} else {
  app.on('second-instance', () => {
    const window = BrowserWindow.getAllWindows()[0];
    if (window) { if (window.isMinimized()) window.restore(); window.focus(); }
  });
  app.whenReady().then(async () => {
    const root = app.isPackaged
      ? path.join(process.resourcesPath, 'web-dist') : path.join(__dirname, '../web/dist-desktop');
    // 同源本机服务兼容 History 路由、Worker 和录音，固定端口让重启后的登录存储保持同源。
    const result = await startLocalServer({ root, apiOrigin: readApiOrigin() });
    localServer = result.server;
    applicationOrigin = result.origin;
    await createWindow();
    app.on('activate', () => {
      if (!BrowserWindow.getAllWindows().length) createWindow().catch(showStartupError);
    });
  }).catch(showStartupError);
}

function showStartupError(error) {
  dialog.showErrorBox('费曼学习平台启动失败', error.code === 'EADDRINUSE'
    ? '本机端口 17463 已被占用，请关闭占用该端口的程序后重试。' : error.message);
  app.quit();
}

app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });
app.on('will-quit', () => localServer?.close());

