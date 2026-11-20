const { spawnSync } = require('node:child_process');
const path = require('node:path');

const webDirectory = path.join(__dirname, '../web');
const result = spawnSync(process.execPath, [
  path.join(webDirectory, 'node_modules/vite/bin/vite.js'), 'build', '--base=/', '--outDir=dist-desktop', '--logLevel=warn'
], {
  cwd: webDirectory, stdio: 'inherit',
  // 桌面 API 通过主进程代理切换，避免构建机 .env 把私有后端地址烘焙进安装包。
  env: { ...process.env, VITE_API_BASE_URL: '/api' }
});
process.exitCode = result.status ?? 1;
