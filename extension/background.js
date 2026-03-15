let API_BASE_URL = "https://aisecondbrain.cn/api";
let DEBUG = false;

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === "getToken") {
    handleGetToken(sendResponse);
    return true;
  }
});

async function handleGetToken(sendResponse) {
  try {
    if (
      typeof chrome !== "undefined" &&
      chrome.storage &&
      chrome.storage.local
    ) {
      chrome.storage.local.get(["authToken"], (result) => {
        if (chrome.runtime.lastError) {
          sendResponse({
            success: false,
            error: chrome.runtime.lastError.message,
          });
        } else {
          if (result.authToken) {
            sendResponse({
              success: true,
              token: result.authToken,
            });
          } else {
            sendResponse({
              success: true,
              token: null,
            });
          }
        }
      });
    } else {
      sendResponse({
        success: false,
        error: "Chrome Storage API 不可用",
      });
    }
  } catch (error) {
    sendResponse({
      success: false,
      error: error.message,
    });
  }
}

chrome.runtime.onInstalled.addListener(() => {
  console.log("AI SecondBrain 插件已安装");
  console.log("Background service worker loaded");
});

chrome.runtime.onStartup.addListener(() => {
  console.log("AI SecondBrain 插件已启动");
});

chrome.runtime.onSuspend.addListener(() => {
  console.log("AI SecondBrain 插件即将挂起");
});
