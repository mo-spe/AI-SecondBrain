# AI-SecondBrain 移动端

移动端使用 uni-app + Vue 3，一套源码产出 H5、微信小程序和 App。

> 注意：不要把 `mobile/` 或 `mobile/src/` 直接导入微信开发者工具。源码阶段没有 `app.json`；微信开发者工具应导入编译后的 `dist/build/mp-weixin` 目录。

## 开发与构建

```bash
npm install
npm run dev:h5
npm run dev:mp-weixin
npm run dev:app

npm run build:h5
npm run build:mp-weixin
npm run build:app
```

### Android APK

1. 使用 HBuilderX 打开 `D:\AI-SecondBrain\mobile`（打开源码目录，不是 `dist`）。
2. 在 `src/manifest.json` 设置 `appid`、应用图标、版本号和 Android 包名；没有 DCloud AppID 时可先在 HBuilderX 创建/绑定。
3. 真机联调前，将 `VITE_API_BASE_URL` 设置为电脑局域网 API 地址，例如 `http://192.168.1.20:8080/api`，不要使用手机无法访问的 `localhost`。
4. 快速预览：运行 `npm run dev:app`，或在 HBuilderX 选择“运行 → 运行到手机或模拟器”。
5. 生成 APK：在 HBuilderX 源码项目中选择“发行 → 原生 App-云打包”，勾选 Android、配置 Android 签名后下载 APK。`npm run build:app` 只负责校验 App 资源编译，不会直接生成可安装 APK。

> `npm run build:app` 已验证通过。未配置 API 地址时，App 会显示配置提示，不会使用假数据。

H5 开发服务器通过 Vite 代理访问 `http://localhost:8080/api`。App 和微信小程序需要在构建前设置 `VITE_API_BASE_URL`，生产环境必须使用可访问的 HTTPS 地址；示例见 `.env.example`。

### 小程序预览排查

- 本地联调优先执行 `npm run dev:mp-weixin`，然后在微信开发者工具导入 `mobile/dist/dev/mp-weixin`；该开发构建会使用 `http://localhost:8080/api`。
- `npm run build:mp-weixin` 生成的是生产包 `mobile/dist/build/mp-weixin`。如果没有设置 `VITE_API_BASE_URL`，页面会明确提示配置 API 地址，不会伪造知识点或工作区数据。
- 真机预览不能访问电脑的 `localhost`，请把 `VITE_API_BASE_URL` 改为电脑局域网 IP，并在微信开发者工具勾选“不校验合法域名”（仅开发阶段）。

## 微信小程序首次配置

1. 在 `src/manifest.json` 填写小程序 appid，或在开发者工具中使用对应项目配置。
2. 后端执行 `sql/V12__add_oauth_identities.sql`。
3. 后端注入 `WECHAT_APP_ID`、`WECHAT_APP_SECRET`，并在微信后台配置合法 request 域名。
4. 导入 `dist/build/mp-weixin` 到微信开发者工具进行真机/模拟器验收。

微信登录只把 openid 作为独立身份映射保存，`session_key` 不落库；未配置服务端密钥时不会创建伪账户。
