const { contextBridge } = require('electron');

contextBridge.exposeInMainWorld('feynmanDesktop', {
  platform: process.platform,
  version: process.versions.electron
});

