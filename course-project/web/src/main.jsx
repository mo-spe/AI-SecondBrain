import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext.jsx';
import App from './App.jsx';
import PwaStatus from './components/PwaStatus.jsx';
import './styles.css';

if ('serviceWorker' in navigator && import.meta.env.PROD && !window.feynmanDesktop) {
  window.addEventListener('load', () => navigator.serviceWorker.register('/sw.js', { updateViaCache: 'none' })
    .catch((error) => console.warn('离线功能暂时不可用', error.name)));
}

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <PwaStatus />
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>
);
