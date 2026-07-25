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
      document.getElementById("currentUser").textContent = user.username || user.nickname || "已登录用户";
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

  const btn = document.getElementById("saveSettingsBtn");
  const originalText = btn.textContent;
  btn.textContent = "已保存";
  btn.style.background = "#059669";
  setTimeout(() => {
    btn.textContent = originalText;
    btn.style.background = "";
  }, 1200);
}

async function handleLogin() {
  const username = document.getElementById("username").value.trim();
  const password = document.getElementById("password").value;
  const loginBtn = document.getElementById("loginBtn");

  if (!username || !password) {
    alert("请输入用户名和密码");
    return;
  }

  loginBtn.innerHTML = '<span class="loading"></span> 登录中...';
  loginBtn.disabled = true;

  try {
    const response = await apiRequest("/auth/login", "POST", { username, password });
    console.log("Login response:", JSON.stringify(response, null, 2));

    if (response.success && response.data.code === 200) {
      const result = response.data;

      if (!result.data) {
        alert("登录成功但未获取到 Token，请检查后端响应");
        return;
      }

      const token = result.data.token;

      if (!token) {
        alert("登录成功但未获取到 Token 字段，请检查后端响应");
        return;
      }

      await storeToken(token);
      showLoggedInSection(token);
      await loadWorkspaces(token);
      showSettingsSection();
      await loadCollectSettings();
      alert("登录成功！");
    } else {
      const errorMsg = response.data?.message || response.error || "用户名或密码错误";
      alert("登录失败：" + errorMsg);
    }
  } catch (error) {
    alert("登录失败：" + (error.message || "网络请求失败"));
  } finally {
    loginBtn.innerHTML = "登录";
    loginBtn.disabled = false;
  }
}

async function handleLogout() {
  if (confirm("确定要退出登录吗？")) {
    await removeToken();
    showLoginSection();
    alert("已退出登录");
  }
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
