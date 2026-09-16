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

  /* ============ SVG 图标 ============ */
  getIcon(name) {
    const icons = {
      collect: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>',
      check: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
      error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
      info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
    };
    return icons[name] || icons.info;
  }

  /* ============ Toast 通知 ============ */
  showToast(message, type = "info", duration = 3500) {
    const existing = document.querySelectorAll(".aisecondbrain-toast");
    existing.forEach((t) => t.remove());

    const toast = document.createElement("div");
    toast.className = `aisecondbrain-toast ${type}`;
    toast.innerHTML = `${this.getIcon(type)}<span>${message}</span>`;

    document.body.appendChild(toast);

    setTimeout(() => {
      toast.classList.add("fade-out");
      setTimeout(() => toast.remove(), 250);
    }, duration);
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
    button.className = "aisecondbrain-save-btn";
    button.innerHTML = `${this.getIcon("collect")} 采集到知识库`;

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
    button.innerHTML = '<span class="spinner"></span> 采集中...';
    button.disabled = true;

    try {
      const chatData = this.extractChatContent();

      if (!chatData) {
        this.showToast("未能提取到对话内容，请确保页面已加载完成", "error");
        this.resetButton();
        return;
      }

      const API_BASE_URL = "http://localhost:8080/api";

      try {
        const token = await this.getStoredToken();

        if (!token) {
          this.showToast("请先在插件弹窗中登录", "error");
          this.resetButton();
          return;
        }

        const settings = await this.getCollectSettings();

        const response = await fetch(`${API_BASE_URL}/chat/collect`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            content: chatData.content,
            platform: chatData.platform,
            workspaceId: settings.workspaceId || null,
            extractKnowledge: settings.extractKnowledge !== false,
            generateCards: settings.generateCards || false,
          }),
        });

        const result = await response.json();

        if (response.ok && result.code === 200) {
          if (settings.extractKnowledge !== false) {
            this.showToast("采集成功！AI 正在提取知识点，请前往知识库「待确认」页签审核", "success", 5000);
          } else {
            this.showToast("采集成功！对话已保存到知识库", "success");
          }
        } else {
          this.showToast("采集失败：" + (result.message || "未知错误"), "error");
        }
      } catch (apiError) {
        console.error("API 请求失败:", apiError);
        this.showToast("采集失败：" + apiError.message, "error");
      }
    } catch (error) {
      console.error("采集失败:", error);
      this.showToast("采集失败：" + error.message, "error");
    } finally {
      this.resetButton();
    }
  }

  async getCollectSettings() {
    return new Promise((resolve) => {
      const defaults = {
        workspaceId: null,
        extractKnowledge: true,
        generateCards: false,
      };

      if (
        typeof chrome !== "undefined" &&
        chrome.storage &&
        chrome.storage.local
      ) {
        chrome.storage.local.get(["collectSettings"], (result) => {
          resolve(result.collectSettings || defaults);
        });
      } else {
        resolve(defaults);
      }
    });
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
              this.showToast("扩展已更新，请刷新页面后重试", "error");
              resolve(null);
            } else {
              this.showToast("扩展通信失败，请刷新页面后重试", "error");
              resolve(null);
            }
          } else {
            if (response && response.success && response.token) {
              resolve(response.token);
            } else {
              this.showToast("请先在插件弹窗中登录", "error");
              resolve(null);
            }
          }
        });
      } else {
        this.showToast("扩展API不可用，请检查扩展是否正常加载", "error");
        resolve(null);
      }
    });
  }

  resetButton() {
    const button = document.getElementById("ai-secondbrain-collect-btn");
    if (button) {
      button.innerHTML = `${this.getIcon("collect")} 采集到知识库`;
      button.disabled = false;
    }
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
