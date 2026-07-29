document.addEventListener("DOMContentLoaded", () => {
  checkLoginStatus();

  const loginBtn = document.getElementById("loginBtn");
  const logoutBtn = document.getElementById("logoutBtn");
  const goToDashboardBtn = document.getElementById("goToDashboardBtn");
  const saveSettingsBtn = document.getElementById("saveSettingsBtn");

  if (loginBtn) {
    loginBtn.addEventListener("click", handleLogin);
  }

  if (logoutBtn) {
    logoutBtn.addEventListener("click", handleLogout);
  }

  if (goToDashboardBtn) {
    goToDashboardBtn.addEventListener("click", () => {
      chrome.tabs.create({ url: "http://localhost:5173" });
    });
  }

  if (saveSettingsBtn) {
    saveSettingsBtn.addEventListener("click", handleSaveSettings);
  }
});

/* ============ Toast 通知系统 ============ */
function showToast(message, type = "info", duration = 3500) {
  const container = document.getElementById("toastContainer");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast ${type}`;

  const icons = {
    success: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>',
    error: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>',
    info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><line x1="12" y1="16" x2="12" y2="12"/><line x1="12" y1="8" x2="12.01" y2="8"/></svg>',
  };

  toast.innerHTML = `${icons[type] || icons.info}<span>${message}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.classList.add("fade-out");
    setTimeout(() => toast.remove(), 250);
  }, duration);
}

async function apiRequest(path, method = "GET", body = null, headers = {}) {
  return new Promise((resolve) => {
    chrome.runtime.sendMessage(
      {
        action: "apiRequest",
        path,
        method,
        body,
        headers
      },
      (response) => {
        resolve(response);
      }
    );
  });
}

async function checkLoginStatus() {
  const token = await getStoredToken();

  if (token) {
    showLoggedInSection(token);
    await loadWorkspaces(token);
    showSettingsSection();
    await loadCollectSettings();
  } else {
    showLoginSection();
  }
}

function showLoginSection() {
  document.getElementById("loginSection").classList.remove("hidden");
  document.getElementById("loggedInSection").classList.add("hidden");
  document.getElementById("settingsSection").classList.add("hidden");
}

async function showLoggedInSection(token) {
  document.getElementById("loginSection").classList.add("hidden");
  document.getElementById("loggedInSection").classList.remove("hidden");

  try {
    const response = await apiRequest("/user/info", "GET", null, {
      Authorization: `Bearer ${token}`
    });

    if (response.success && response.data.code === 200) {
      const user = response.data.data;
      const displayName = user.username || user.nickname || "已登录用户";
      document.getElementById("currentUser").textContent = displayName;
      const avatar = document.getElementById("userAvatar");
      if (avatar) {
        avatar.textContent = displayName.charAt(0).toUpperCase();
      }
    } else {
      document.getElementById("currentUser").textContent = "已登录用户";
    }
  } catch {
    document.getElementById("currentUser").textContent = "已登录用户";
  }
}

function showSettingsSection() {
  document.getElementById("settingsSection").classList.remove("hidden");
}

async function loadWorkspaces(token) {
  const select = document.getElementById("workspaceSelect");
  select.innerHTML = '<option value="">个人空间</option>';

  try {
    const response = await apiRequest("/workspace", "GET", null, {
      Authorization: `Bearer ${token}`
    });

    if (response.success && response.data.code === 200) {
      const workspaces = response.data.data;
      if (Array.isArray(workspaces)) {
        workspaces.forEach((ws) => {
          const option = document.createElement("option");
          option.value = ws.id;
          option.textContent = ws.name;
          select.appendChild(option);
        });
      }
    }
  } catch (error) {
    console.error("加载工作区列表失败:", error);
  }
}

async function getCollectSettings() {
  return new Promise((resolve) => {
    chrome.storage.local.get(["collectSettings"], (result) => {
      const defaults = {
        workspaceId: null,
        extractKnowledge: true,
        generateCards: false,
      };
      resolve(result.collectSettings || defaults);
    });
  });
}

async function loadCollectSettings() {
  const settings = await getCollectSettings();

  const workspaceSelect = document.getElementById("workspaceSelect");
  const extractToggle = document.getElementById("extractKnowledge");
  const generateToggle = document.getElementById("generateCards");

  if (settings.workspaceId !== null && settings.workspaceId !== undefined) {
    workspaceSelect.value = settings.workspaceId;
  } else {
    workspaceSelect.value = "";
  }

  extractToggle.checked = settings.extractKnowledge !== false;
  generateToggle.checked = settings.generateCards === true;
}

async function handleSaveSettings() {
  const workspaceIdRaw = document.getElementById("workspaceSelect").value;
  const workspaceId = workspaceIdRaw === "" ? null : (isNaN(Number(workspaceIdRaw)) ? workspaceIdRaw : Number(workspaceIdRaw));
  const extractKnowledge = document.getElementById("extractKnowledge").checked;
  const generateCards = document.getElementById("generateCards").checked;

  const settings = { workspaceId, extractKnowledge, generateCards };

  await new Promise((resolve) => {
    chrome.storage.local.set({ collectSettings: settings }, resolve);
  });

  showToast("设置已保存", "success", 2000);
}

async function handleLogin() {
  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;
  const loginBtn = document.getElementById("loginBtn");

  if (!username || !password) {
    showToast("请输入用户名和密码", "error");
    return;
  }

  loginBtn.innerHTML = '<span class="loading"></span> 登录中...';
  loginBtn.disabled = true;

  try {
    const response = await apiRequest("/auth/login", "POST", { username, password });

    if (response.success && response.data.code === 200) {
      const result = response.data;

      if (!result.data) {
        showToast("登录成功但未获取到 Token，请检查后端响应", "error");
        return;
      }

      const token = result.data.token;

      if (!token) {
        showToast("登录成功但未获取到 Token 字段", "error");
        return;
      }

      await storeToken(token);
      showLoggedInSection(token);
      await loadWorkspaces(token);
      showSettingsSection();
      await loadCollectSettings();
      showToast("登录成功", "success");
    } else {
      const errorMsg = response.data?.message || response.error || "用户名或密码错误";
      showToast("登录失败：" + errorMsg, "error");
    }
  } catch (error) {
    showToast("登录失败：" + (error.message || "网络请求失败"), "error");
  } finally {
    loginBtn.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/><polyline points="10 17 15 12 10 7"/><line x1="15" y1="12" x2="3" y2="12"/></svg> 登录';
    loginBtn.disabled = false;
  }
}

async function handleLogout() {
  await removeToken();
  showLoginSection();
  showToast("已退出登录", "info", 2000);
}

async function getStoredToken() {
  return new Promise((resolve) => {
    chrome.storage.local.get(["authToken"], (result) => {
      resolve(result.authToken || null);
    });
  });
}

async function storeToken(token) {
  return new Promise((resolve) => {
    if (!token) {
      resolve();
      return;
    }

    chrome.storage.local.set({ authToken: token }, () => {
      resolve();
    });
  });
}

async function removeToken() {
  return new Promise((resolve) => {
    chrome.storage.local.remove(["authToken"], () => {
      resolve();
    });
  });
}
