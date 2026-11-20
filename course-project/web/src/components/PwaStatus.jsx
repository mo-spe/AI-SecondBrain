import { useEffect, useState } from 'react';
import { Download, WifiOff, X } from 'lucide-react';

export default function PwaStatus() {
  const [prompt, setPrompt] = useState(null);
  const [offline, setOffline] = useState(!navigator.onLine);
  const [installing, setInstalling] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [showHelp, setShowHelp] = useState(false);
  const standalone = window.matchMedia('(display-mode: standalone)').matches || navigator.standalone;
  const ios = /iphone|ipad|ipod/i.test(navigator.userAgent);

  useEffect(() => {
    const offer = (event) => { event.preventDefault(); setPrompt(event); };
    const online = () => setOffline(false);
    const disconnected = () => setOffline(true);
    const installed = () => { setPrompt(null); setDismissed(true); };
    window.addEventListener('beforeinstallprompt', offer);
    window.addEventListener('online', online);
    window.addEventListener('offline', disconnected);
    window.addEventListener('appinstalled', installed);
    return () => {
      window.removeEventListener('beforeinstallprompt', offer);
      window.removeEventListener('online', online);
      window.removeEventListener('offline', disconnected);
      window.removeEventListener('appinstalled', installed);
    };
  }, []);

  const install = async () => {
    if (!prompt) { setShowHelp(true); return; }
    setInstalling(true);
    try {
      await prompt.prompt();
      await prompt.userChoice;
    } finally { setPrompt(null); setInstalling(false); }
  };

  if (window.feynmanDesktop) return null;
  return <>
    {offline && <div className="pwa-offline" role="status"><WifiOff size={16} /> 当前离线，可打开已缓存的页面；登录、保存和 AI 功能需要联网。</div>}
    {!standalone && !dismissed && (prompt || ios) && <div className="pwa-install" aria-label="安装应用">
      <button type="button" onClick={install} disabled={installing}><Download size={18} />{installing ? '正在打开…' : '安装到主屏'}</button>
      <button type="button" aria-label="关闭安装提示" onClick={() => setDismissed(true)}><X size={18} /></button>
      {showHelp && <p role="status">在 Safari 中点击“分享”，选择“添加到主屏幕”。</p>}
    </div>}
  </>;
}
