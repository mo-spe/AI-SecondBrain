const API_BASE_URL = "http://localhost:8080/api";

document.addEventListener("DOMContentLoaded", () => {
  console.log("Popup loaded, initializing...");
  checkLoginStatus();

  const loginBtn = document.getElementById("loginBtn");
  const logoutBtn = document.getElementById("logoutBtn");
  const goToDashboardBtn = document.getElementById("goToDashboardBtn");

  console.log("Login button found:", !!loginBtn);
  console.log("Logout button found:", !!logoutBtn);
  console.log("Go to dashboard button found:", !!goToDashboardBtn);

  if (loginBtn) {
    loginBtn.addEventListener("click", handleLogin);
    console.log("Login button event listener attached");
  }

  if (logoutBtn) {
    logoutBtn.addEventListener("click", handleLogout);
    console.log("Logout button event listener attached");
  }

  if (goToDashboardBtn) {
    goToDashboardBtn.addEventListener("click", () => {
      chrome.tabs.create({ url: "http://localhost:3000" });
    });
    console.log("Go to dashboard button event listener attached");
  }
});

async function checkLoginStatus() {
  const token = await getStoredToken();

  if (token) {
    showLoggedInSection(token);
  } else {
    showLoginSection();
  }
}

function showLoginSection() {
  document.getElementById("loginSection").classList.remove("hidden");
  document.getElementById("loggedInSection").classList.add("hidden");
}

async function showLoggedInSection(token) {
  document.getElementById("loginSection").classList.add("hidden");
  document.getElementById("loggedInSection").classList.remove("hidden");

  console.log("显示已登录状态");
  document.getElementById("currentUser").textContent = "已登录用户";
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
    console.log("发送登录请求...");
    const response = await fetch(`${API_BASE_URL}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ username, password }),
    });

    console.log("登录响应状态:", response.status);
    const result = await response.json();
    console.log("登录响应数据:", result);

    if (response.ok && result.code === 200) {
      console.log("登录成功，准备存储 Token...");
      console.log("result.data:", result.data);
      console.log("result.data 类型:", typeof result.data);

      if (!result.data) {
        console.error("Token 不存在于响应中");
        alert("❌ 登录成功但未获取到 Token，请检查后端响应");
        return;
      }

      const token = result.data.token;
      console.log("提取的 Token:", token);
      console.log("Token 类型:", typeof token);

      if (!token) {
        console.error("Token 字段不存在于 result.data 中");
        console.error("result.data 的键:", Object.keys(result.data));
        alert("❌ 登录成功但未获取到 Token 字段，请检查后端响应");
        return;
      }

      await storeToken(token);
      showLoggedInSection(token);
      alert("✅ 登录成功！");
    } else {
      alert("❌ 登录失败：" + (result.message || "用户名或密码错误"));
    }
  } catch (error) {
    console.error("登录失败:", error);
    alert("❌ 登录失败：" + error.message);
  } finally {
    loginBtn.innerHTML = "登录";
    loginBtn.disabled = false;
  }
}

async function handleLogout() {
  if (confirm("确定要退出登录吗？")) {
    await removeToken();
    showLoginSection();
    alert("✅ 已退出登录");
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
    console.log("准备存储 Token...");
    console.log("Token 值:", token);
    console.log("Token 类型:", typeof token);

    if (!token) {
      console.error("Token 为空，无法存储");
      resolve();
      return;
    }

    console.log("Token 长度:", token.length);
    chrome.storage.local.set({ authToken: token }, () => {
      if (chrome.runtime.lastError) {
        console.error("存储 Token 失败:", chrome.runtime.lastError);
      } else {
        console.log("✅ Token 存储成功");
      }
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
