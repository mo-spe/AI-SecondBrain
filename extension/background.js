let API_BASE_URL = "http://localhost:8080/api";

chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  console.log("Background 收到消息:", request);

  if (request.action === "getToken") {
    handleGetToken(sendResponse);
    return true;
  }
});

async function handleGetToken(sendResponse) {
  console.log("========================================");
  console.log("Background 开始获取 Token...");
  console.log("========================================");

  try {
    if (
      typeof chrome !== "undefined" &&
      chrome.storage &&
      chrome.storage.local
    ) {
      console.log("✅ Chrome Storage API 可用");
      console.log("开始读取 authToken...");

      chrome.storage.local.get(["authToken"], (result) => {
        console.log("========================================");
        console.log("Storage 读取结果:");
        console.log("========================================");
        console.log("完整结果对象:", result);
        console.log("result 类型:", typeof result);

        if (chrome.runtime.lastError) {
          console.error(
            "❌ Background 读取 Token 失败:",
            chrome.runtime.lastError,
          );
          console.error("错误详情:", JSON.stringify(chrome.runtime.lastError));
          sendResponse({
            success: false,
            error: chrome.runtime.lastError.message,
          });
        } else {
          console.log("✅ Storage 读取成功");
          console.log("result.authToken:", result.authToken);
          console.log("result.authToken 类型:", typeof result.authToken);

          if (result.authToken) {
            console.log("✅ authToken 存在");
            console.log("Token 长度:", result.authToken.length);
            console.log("Token 类型:", typeof result.authToken);
            console.log(
              "Token 预览:",
              result.authToken.substring(0, 30) + "...",
            );

            sendResponse({
              success: true,
              token: result.authToken,
            });
          } else {
            console.error("❌ authToken 不存在");
            console.error("Storage 中的键:", Object.keys(result));

            sendResponse({
              success: true,
              token: null,
            });
          }
        }
      });
    } else {
      console.error("❌ Chrome Storage API 不可用");
      console.error("详细检查:");
      console.error("- typeof chrome:", typeof chrome);
      console.error(
        "- chrome.storage:",
        typeof chrome !== "undefined" ? chrome.storage : "undefined",
      );
      console.error(
        "- chrome.storage.local:",
        typeof chrome !== "undefined" && chrome.storage
          ? chrome.storage.local
          : "undefined",
      );

      sendResponse({
        success: false,
        error: "Chrome Storage API 不可用",
      });
    }
  } catch (error) {
    console.error("❌ Background 获取 Token 时出错:", error);
    console.error("错误详情:", error.message);
    console.error("错误堆栈:", error.stack);

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
