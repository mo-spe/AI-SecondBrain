class AIChatCollector {
  constructor() {
    this.supportedSites = [
      "chatgpt.com",
      "chat.openai.com",
      "chat.deepseek.com",
      "www.kimi.com",
      "kimi.moonshot.cn",
      "kimi.ai",
      "www.doubao.com",
      "www.zhipuai.cn",
      "www.qianwen.com",
    ];
    this.currentSite = this.detectSite();
    console.log("AI SecondBrain Collector initialized");
    console.log("Current site:", this.currentSite);
    console.log("Hostname:", window.location.hostname);
    console.log("URL:", window.location.href);
    console.log("Supported sites:", this.supportedSites);
  }

  detectSite() {
    const hostname = window.location.hostname;
    console.log("检测网站，当前 hostname:", hostname);

    const foundSite = this.supportedSites.find(
      (site) => hostname === site || hostname.endsWith("." + site),
    );
    console.log("检测到的网站:", foundSite || "不支持的网站");

    return foundSite || null;
  }

  isSupportedSite() {
    const supported = this.currentSite !== null;
    console.log("网站是否支持:", supported);
    return supported;
  }

  extractChatContent() {
    if (!this.currentSite) {
      console.log("当前网站不支持自动采集");
      return null;
    }

    let content = "";
    let platform = this.currentSite;

    try {
      switch (this.currentSite) {
        case "chatgpt.com":
        case "chat.openai.com":
          content = this.extractChatGPTContent();
          break;
        case "chat.deepseek.com":
          content = this.extractDeepSeekContent();
          break;
        case "www.kimi.com":
        case "kimi.moonshot.cn":
        case "kimi.ai":
          content = this.extractKimiContent();
          break;
        case "www.doubao.com":
          content = this.extractDoubaoContent();
          break;
        case "www.zhipuai.cn":
          content = this.extractZhipuContent();
          break;
        case "www.qianwen.com":
          content = this.extractQianwenContent();
          break;
        default:
          console.log("未知的网站:", this.currentSite);
          return null;
      }

      if (content && content.length > 0) {
        return {
          content: content,
          platform: platform,
          timestamp: new Date().toISOString(),
        };
      }

      return null;
    } catch (error) {
      console.error("提取对话内容失败:", error);
      return null;
    }
  }

  extractChatGPTContent() {
    const messages = document.querySelectorAll(
      '[data-testid^="conversation-turn"]',
    );
    const chatContent = [];

    messages.forEach((msg) => {
      const role = msg.querySelector('[data-testid^="user-message"]')
        ? "user"
        : "assistant";
      const textElement = msg.querySelector(".markdown, .prose");
      const text = textElement ? textElement.innerText.trim() : "";

      if (text) {
        chatContent.push({
          role: role,
          content: text,
        });
      }
    });

    return this.formatChatContent(chatContent);
  }

  extractDeepSeekContent() {
    console.log("开始提取 DeepSeek 对话内容...");

    const chatContent = [];

    try {
      const possibleSelectors = [
        '[class*="message"]',
        '[class*="chat"]',
        '[class*="dialog"]',
        '[class*="conversation"]',
        "article",
        '[role="article"]',
        "[data-message]",
      ];

      let messages = [];

      for (const selector of possibleSelectors) {
        const elements = document.querySelectorAll(selector);
        console.log(`选择器 "${selector}" 找到 ${elements.length} 个元素`);

        if (elements.length > 0) {
          messages = Array.from(elements).filter((el) => {
            const text = el.innerText.trim();
            return text.length > 10 && text.length < 10000;
          });

          if (messages.length > 0) {
            console.log(
              `使用选择器 "${selector}" 找到 ${messages.length} 个有效消息`,
            );
            break;
          }
        }
      }

      if (messages.length === 0) {
        console.error("未能找到任何对话消息");

        const bodyText = document.body.innerText;
        console.log("页面文本长度:", bodyText.length);
        console.log("页面文本预览:", bodyText.substring(0, 500));

        return "";
      }

      console.log(`总共找到 ${messages.length} 个消息元素`);

      messages.forEach((msg, index) => {
        const text = msg.innerText.trim();
        console.log(
          `消息 ${index + 1}: 长度=${text.length}, 内容预览=${text.substring(0, 50)}`,
        );

        if (text.length > 10) {
          const isUser = this.detectUserMessage(msg, text, chatContent.length);
          const role = isUser ? "user" : "assistant";

          chatContent.push({
            role: role,
            content: text,
          });
        }
      });

      console.log(`成功提取 ${chatContent.length} 条对话`);
    } catch (error) {
      console.error("提取 DeepSeek 内容时出错:", error);
      return "";
    }

    return this.formatChatContent(chatContent);
  }

  detectUserMessage(element, text, currentCount) {
    const elementText = element.innerText.toLowerCase();
    const elementClasses = element.className.toLowerCase();

    const userIndicators = ["user", "you", "我", "发送", "send"];
    const assistantIndicators = [
      "assistant",
      "ai",
      "bot",
      "model",
      "助手",
      "回复",
      "response",
    ];

    const hasUserIndicator = userIndicators.some(
      (indicator) =>
        elementClasses.includes(indicator) || elementText.includes(indicator),
    );

    const hasAssistantIndicator = assistantIndicators.some(
      (indicator) =>
        elementClasses.includes(indicator) || elementText.includes(indicator),
    );

    if (hasUserIndicator && !hasAssistantIndicator) {
      return true;
    }

    if (hasAssistantIndicator && !hasUserIndicator) {
      return false;
    }

    return currentCount % 2 === 0;
  }

  extractKimiContent() {
    const messages = document.querySelectorAll(
      '[class*="message"], [class*="chat"]',
    );
    const chatContent = [];

    messages.forEach((msg) => {
      const role = msg.classList.contains("user") ? "user" : "assistant";
      const text = msg.innerText.trim();

      if (text) {
        chatContent.push({
          role: role,
          content: text,
        });
      }
    });

    return this.formatChatContent(chatContent);
  }

  extractDoubaoContent() {
    const messages = document.querySelectorAll(
      '[class*="message"], [class*="chat"], [class*="dialog"]',
    );
    const chatContent = [];

    messages.forEach((msg) => {
      const role =
        msg.classList.contains("user") || msg.classList.contains("you")
          ? "user"
          : "assistant";
      const text = msg.innerText.trim();

      if (text) {
        chatContent.push({
          role: role,
          content: text,
        });
      }
    });

    return this.formatChatContent(chatContent);
  }

  extractZhipuContent() {
    const messages = document.querySelectorAll(
      '[class*="message"], [class*="chat"], [class*="dialog"]',
    );
    const chatContent = [];

    messages.forEach((msg) => {
      const role =
        msg.classList.contains("user") || msg.classList.contains("you")
          ? "user"
          : "assistant";
      const text = msg.innerText.trim();

      if (text) {
        chatContent.push({
          role: role,
          content: text,
        });
      }
    });

    return this.formatChatContent(chatContent);
  }

  extractQianwenContent() {
    const messages = document.querySelectorAll(
      '[class*="message"], [class*="chat"], [class*="dialog"]',
    );
    const chatContent = [];

    messages.forEach((msg) => {
      const role =
        msg.classList.contains("user") || msg.classList.contains("you")
          ? "user"
          : "assistant";
      const text = msg.innerText.trim();

      if (text) {
        chatContent.push({
          role: role,
          content: text,
        });
      }
    });

    return this.formatChatContent(chatContent);
  }

  formatChatContent(messages) {
    if (messages.length === 0) return "";

    let formatted = "";
    messages.forEach((msg) => {
      const roleLabel = msg.role === "user" ? "用户" : "AI";
      formatted += `${roleLabel}: ${msg.content}\n\n`;
    });

    return formatted.trim();
  }

  injectCollectButton() {
    console.log("========================================");
    console.log("开始注入采集按钮...");
    console.log("========================================");
    console.log("Is supported site:", this.isSupportedSite());
    console.log("Current site:", this.currentSite);
    console.log("Hostname:", window.location.hostname);
    console.log("Document ready state:", document.readyState);
    console.log("Document body exists:", !!document.body);

    if (!this.isSupportedSite()) {
      console.log("❌ 当前网站不支持，跳过按钮注入");
      return;
    }

    if (!document.body) {
      console.log("❌ document.body 不存在，等待页面加载...");
      setTimeout(() => this.injectCollectButton(), 1000);
      return;
    }

    if (document.getElementById("ai-secondbrain-collect-btn")) {
      console.log("✅ 采集按钮已存在，跳过注入");
      return;
    }

    console.log("🔧 开始创建采集按钮...");
    const button = document.createElement("button");
    button.id = "ai-secondbrain-collect-btn";
    button.innerHTML = "📥 采集到知识库";
    button.style.cssText = `
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 999999;
      padding: 12px 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border: none;
      border-radius: 8px;
      cursor: pointer;
      font-size: 14px;
      font-weight: 600;
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
      transition: all 0.3s ease;
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
    `;

    button.addEventListener("mouseenter", () => {
      button.style.transform = "translateY(-2px)";
      button.style.boxShadow = "0 6px 16px rgba(102, 126, 234, 0.5)";
    });

    button.addEventListener("mouseleave", () => {
      button.style.transform = "translateY(0)";
      button.style.boxShadow = "0 4px 12px rgba(102, 126, 234, 0.4)";
    });

    button.addEventListener("click", () => {
      console.log("🔘 采集按钮被点击");
      this.collectAndSend();
    });

    try {
      document.body.appendChild(button);
      console.log("✅ AI SecondBrain 采集按钮已注入成功");
      console.log("按钮 ID:", button.id);
      console.log("按钮位置:", button.style.position);
      console.log("按钮文本:", button.innerHTML);
    } catch (error) {
      console.error("❌ 按钮注入失败:", error);
      console.error("错误详情:", error.message);
      console.error("错误堆栈:", error.stack);
    }
  }

  tryInjectButton(maxRetries = 5, delay = 1000) {
    console.log(`尝试注入按钮，剩余重试次数: ${maxRetries}`);

    if (maxRetries <= 0) {
      console.error("❌ 按钮注入失败，已达到最大重试次数");
      return;
    }

    this.injectCollectButton();

    if (!document.getElementById("ai-secondbrain-collect-btn")) {
      console.log(`按钮未注入成功，${delay}ms 后重试...`);
      setTimeout(
        () => this.tryInjectButton(maxRetries - 1, delay * 1.5),
        delay,
      );
    } else {
      console.log("✅ 按钮注入成功");
    }
  }

  async collectAndSend() {
    console.log("开始采集...");
    const button = document.getElementById("ai-secondbrain-collect-btn");
    button.innerHTML = "⏳ 采集中...";
    button.disabled = true;

    try {
      const chatData = this.extractChatContent();
      console.log("提取的对话数据:", chatData);

      if (!chatData) {
        alert("未能提取到对话内容，请确保页面已加载完成");
        this.resetButton();
        return;
      }

      console.log("准备发送采集请求...");

      const API_BASE_URL = "http://localhost:8080/api";

      try {
        const token = await this.getStoredToken();
        console.log("获取到的 Token:", token ? "存在" : "不存在");

        if (!token) {
          alert("❌ 请先在插件弹窗中登录");
          this.resetButton();
          return;
        }

        console.log("发送采集请求到后端...");
        const response = await fetch(`${API_BASE_URL}/chat/collect`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            content: chatData.content,
            platform: chatData.platform,
          }),
        });

        console.log("后端响应状态:", response.status);
        const result = await response.json();
        console.log("后端响应数据:", result);

        if (response.ok && result.code === 200) {
          alert("✅ 采集成功！知识点已保存到您的知识库");
        } else {
          alert("❌ 采集失败：" + (result.message || "未知错误"));
        }
      } catch (apiError) {
        console.error("API 请求失败:", apiError);
        alert("❌ 采集失败：" + apiError.message);
      }
    } catch (error) {
      console.error("采集失败:", error);
      alert("❌ 采集失败：" + error.message);
    } finally {
      this.resetButton();
    }
  }

  async getStoredToken() {
    console.log("========================================");
    console.log("Content Script 开始获取 Token...");
    console.log("========================================");
    console.log("typeof chrome:", typeof chrome);
    console.log(
      "chrome.runtime:",
      typeof chrome !== "undefined" ? chrome.runtime : "undefined",
    );
    console.log(
      "chrome.runtime.sendMessage:",
      typeof chrome !== "undefined" && chrome.runtime
        ? typeof chrome.runtime.sendMessage
        : "undefined",
    );

    return new Promise((resolve) => {
      if (
        typeof chrome !== "undefined" &&
        chrome.runtime &&
        chrome.runtime.sendMessage
      ) {
        console.log(
          "✅ Chrome Runtime API 可用，发送消息到 Background Script 获取 Token...",
        );

        chrome.runtime.sendMessage({ action: "getToken" }, (response) => {
          console.log("========================================");
          console.log("Background Script 响应:");
          console.log("========================================");
          console.log("完整响应对象:", response);
          console.log("响应类型:", typeof response);

          if (chrome.runtime.lastError) {
            console.error(
              "❌ Chrome Runtime 消息发送失败:",
              chrome.runtime.lastError,
            );

            if (
              chrome.runtime.lastError.message ===
              "Extension context invalidated."
            ) {
              console.error("❌ 扩展上下文已失效，需要刷新页面");
              alert("❌ 扩展已更新，请刷新页面后重试");
            }

            console.error(
              "错误详情:",
              JSON.stringify(chrome.runtime.lastError),
            );
            resolve(null);
          } else {
            console.log("✅ 消息发送成功");

            if (response) {
              console.log("response.success:", response.success);
              console.log("response.token:", response.token);
              console.log("response.token 类型:", typeof response.token);

              if (response.success) {
                console.log("✅ 响应成功");

                if (response.token) {
                  console.log("✅ Token 存在");
                  console.log("Token 长度:", response.token.length);
                  console.log("Token 类型:", typeof response.token);

                  if (
                    typeof response.token === "string" &&
                    response.token.length > 0
                  ) {
                    console.log(
                      "Token 预览:",
                      response.token.substring(0, 30) + "...",
                    );
                    resolve(response.token);
                  } else {
                    console.error("❌ Token 格式不正确");
                    resolve(null);
                  }
                } else {
                  console.error("❌ Token 为空");
                  resolve(null);
                }
              } else {
                console.error("❌ 响应失败");
                console.error("错误信息:", response.error || "未知错误");
                resolve(null);
              }
            } else {
              console.error("❌ 响应为空");
              resolve(null);
            }
          }
        });
      } else {
        console.error("❌ Chrome Runtime API 不可用");
        console.error("详细检查:");
        console.error("- typeof chrome:", typeof chrome);
        console.error(
          "- chrome.runtime:",
          typeof chrome !== "undefined" ? chrome.runtime : "undefined",
        );
        console.error(
          "- chrome.runtime.sendMessage:",
          typeof chrome !== "undefined" && chrome.runtime
            ? typeof chrome.runtime.sendMessage
            : "undefined",
        );
        resolve(null);
      }
    });
  }

  resetButton() {
    const button = document.getElementById("ai-secondbrain-collect-btn");
    button.innerHTML = "📥 采集到知识库";
    button.disabled = false;
  }
}

const collector = new AIChatCollector();

console.log("========================================");
console.log("Content script loaded");
console.log("========================================");
console.log("Document ready state:", document.readyState);
console.log("Chrome API 可用性检查:", typeof chrome !== "undefined");
console.log(
  "Chrome runtime 可用性检查:",
  typeof chrome !== "undefined" && chrome.runtime,
);

function initCollector() {
  console.log("========================================");
  console.log("开始初始化采集器...");
  console.log("========================================");
  console.log("Current site:", collector.currentSite);
  console.log("Is supported:", collector.isSupportedSite());

  if (collector.isSupportedSite()) {
    console.log("✅ 网站支持，开始注入按钮");
    collector.tryInjectButton();
  } else {
    console.log("❌ 网站不支持，跳过按钮注入");
  }

  console.log("========================================");
  console.log("采集器初始化完成");
  console.log("========================================");
}

if (document.readyState === "loading") {
  console.log("⏳ 等待 DOMContentLoaded...");
  document.addEventListener("DOMContentLoaded", () => {
    console.log("✅ DOMContentLoaded 事件触发");
    initCollector();
  });
} else {
  console.log("✅ DOM 已加载，立即初始化");
  initCollector();
}

console.log("========================================");
console.log("Content script initialization complete");
console.log("========================================");
