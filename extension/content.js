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
    this.DEBUG = false;
  }

  detectSite() {
    const hostname = window.location.hostname;
    const foundSite = this.supportedSites.find(
      (site) => hostname === site || hostname.endsWith("." + site),
    );
    return foundSite || null;
  }

  isSupportedSite() {
    return this.currentSite !== null;
  }

  extractChatContent() {
    if (!this.currentSite) {
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

        if (elements.length > 0) {
          messages = Array.from(elements).filter((el) => {
            const text = el.innerText.trim();
            return text.length > 10 && text.length < 10000;
          });

          if (messages.length > 0) {
            break;
          }
        }
      }

      if (messages.length === 0) {
        return "";
      }

      messages.forEach((msg, index) => {
        const text = msg.innerText.trim();

        if (text.length > 10) {
          const isUser = this.detectUserMessage(msg, text, chatContent.length);
          const role = isUser ? "user" : "assistant";

          chatContent.push({
            role: role,
            content: text,
          });
        }
      });
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
    if (!this.isSupportedSite()) {
      return;
    }

    if (!document.body) {
      setTimeout(() => this.injectCollectButton(), 1000);
      return;
    }

    if (document.getElementById("ai-secondbrain-collect-btn")) {
      return;
    }

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
      this.collectAndSend();
    });

    try {
      document.body.appendChild(button);
    } catch (error) {
      console.error("按钮注入失败:", error);
    }
  }

  tryInjectButton(maxRetries = 5, delay = 1000) {
    if (maxRetries <= 0) {
      return;
    }

    this.injectCollectButton();

    if (!document.getElementById("ai-secondbrain-collect-btn")) {
      setTimeout(
        () => this.tryInjectButton(maxRetries - 1, delay * 1.5),
        delay,
      );
    }
  }

  async collectAndSend() {
    const button = document.getElementById("ai-secondbrain-collect-btn");
    button.innerHTML = "⏳ 采集中...";
    button.disabled = true;

    try {
      const chatData = this.extractChatContent();

      if (!chatData) {
        alert("未能提取到对话内容，请确保页面已加载完成");
        this.resetButton();
        return;
      }

      const API_BASE_URL = "https://aisecondbrain.cn/api";

      try {
        const token = await this.getStoredToken();

        if (!token) {
          alert("❌ 请先在插件弹窗中登录");
          this.resetButton();
          return;
        }

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

        const result = await response.json();

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
    return new Promise((resolve) => {
      if (
        typeof chrome !== "undefined" &&
        chrome.runtime &&
        chrome.runtime.sendMessage
      ) {
        chrome.runtime.sendMessage({ action: "getToken" }, (response) => {
          if (chrome.runtime.lastError) {
            console.error("Chrome Runtime 错误:", chrome.runtime.lastError);

            if (
              chrome.runtime.lastError.message ===
              "Extension context invalidated."
            ) {
              console.error("扩展上下文已失效");
              alert("❌ 扩展已更新，请刷新页面后重试");
              resolve(null);
            } else {
              console.error("其他错误:", chrome.runtime.lastError.message);
              alert("❌ 扩展通信失败，请刷新页面后重试");
              resolve(null);
            }
          } else {
            if (response && response.success && response.token) {
              console.log("Token 获取成功");
              resolve(response.token);
            } else {
              console.error("Token 获取失败:", response);
              alert("❌ 请先在插件弹窗中登录");
              resolve(null);
            }
          }
        });
      } else {
        console.error("Chrome Runtime API 不可用");
        alert("❌ 扩展API不可用，请检查扩展是否正常加载");
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

function initCollector() {
  if (collector.isSupportedSite()) {
    collector.tryInjectButton();
  }
}

if (document.readyState === "loading") {
  document.addEventListener("DOMContentLoaded", () => {
    initCollector();
  });
} else {
  initCollector();
}
