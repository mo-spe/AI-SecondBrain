let pendingRuntime;

export const loadCesium = () => {
  if (window.Cesium) return Promise.resolve(window.Cesium);
  if (pendingRuntime) return pendingRuntime;
  const baseUrl = window.location.protocol === 'file:'
    ? new URL('./cesium/', document.baseURI)
    : new URL(`${import.meta.env.BASE_URL}cesium/`, window.location.origin);
  // 引擎和Worker共用资源根路径，兼容网页路由及Electron的file入口。
  window.CESIUM_BASE_URL = baseUrl.href;
  pendingRuntime = new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.src = new URL('Cesium.js', baseUrl).href;
    script.async = true;
    script.onload = () => {
      if (window.Cesium) resolve(window.Cesium);
      else { script.remove(); reject(new Error('数字地球引擎初始化失败，请刷新页面重试。')); }
    };
    script.onerror = () => { script.remove(); reject(new Error('数字地球引擎加载失败，请刷新页面重试。')); };
    document.head.append(script);
  }).catch((error) => {
    pendingRuntime = undefined;
    throw error;
  });
  return pendingRuntime;
};
