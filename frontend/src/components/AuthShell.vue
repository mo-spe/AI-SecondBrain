<template>
  <main class="auth-shell">
    <section class="auth-story" aria-label="AI-SecondBrain 产品介绍">
      <router-link class="auth-brand" to="/login" aria-label="AI-SecondBrain 首页">
        <span class="brand-mark"><BrainIcon :size="24" color="#f8f4e8" /></span>
        <span><strong>SecondBrain</strong><small>KNOWLEDGE STUDIO</small></span>
      </router-link>
      <div class="story-copy">
        <span class="story-index">01 / LEARN WITH CONTEXT</span>
        <h1>让知识不只被收藏，<br /><em>而是持续生长。</em></h1>
        <p>从一次提问、一篇资料到长期可复用的知识网络。研究、整理、复习与分享，在同一个工作空间自然衔接。</p>
      </div>
      <div class="knowledge-orbit" aria-hidden="true">
        <span class="orbit orbit-one"></span><span class="orbit orbit-two"></span>
        <span class="node node-main">AI</span><span class="node node-a">研究</span>
        <span class="node node-b">知识</span><span class="node node-c">复习</span><span class="node node-d">连接</span>
      </div>
      <div class="story-proof">
        <div><strong>01</strong><span>捕获信息</span></div><div><strong>02</strong><span>形成理解</span></div><div><strong>03</strong><span>连接他人</span></div>
      </div>
    </section>
    <section class="auth-form-side">
      <div class="mobile-brand"><span class="brand-mark"><BrainIcon :size="22" color="#f8f4e8" /></span><strong>SecondBrain</strong></div>
      <div class="auth-form-card"><slot /></div>
      <p class="auth-footnote">你的私人知识默认保持私密，只有主动发布的内容才会进入社区。</p>
    </section>
  </main>
</template>

<script setup>
import BrainIcon from "@/components/BrainIcon.vue";
</script>

<style scoped>
.auth-shell { display: grid; grid-template-columns: minmax(460px, 1.08fr) minmax(460px, .92fr); min-height: 100vh; background: #f6f2e8; color: #1d241f; }
.auth-story { position: relative; display: flex; min-height: 100vh; flex-direction: column; justify-content: space-between; overflow: hidden; padding: clamp(32px, 4vw, 64px); background: #173d32; color: #f8f4e8; isolation: isolate; }
.auth-story::before { position: absolute; inset: 0; z-index: -2; background-image: linear-gradient(rgba(248,244,232,.045) 1px, transparent 1px), linear-gradient(90deg, rgba(248,244,232,.045) 1px, transparent 1px); background-size: 34px 34px; content: ""; }
.auth-story::after { position: absolute; right: -20%; bottom: -30%; z-index: -1; width: 650px; height: 650px; border-radius: 50%; background: radial-gradient(circle, rgba(184,114,58,.28), transparent 67%); content: ""; }
.auth-brand { display: inline-flex; align-items: center; align-self: flex-start; gap: 12px; color: inherit; text-decoration: none; }.brand-mark { display: grid; width: 42px; height: 42px; place-items: center; border: 1px solid rgba(248,244,232,.3); border-radius: 10px; background: rgba(248,244,232,.08); }
.auth-brand > span:last-child { display: flex; flex-direction: column; }.auth-brand strong { font-family: var(--font-family-display); font-size: 21px; }.auth-brand small { margin-top: 1px; color: rgba(248,244,232,.55); font: 600 9px/1 var(--font-family-ui); letter-spacing: .16em; }
.story-copy { position: relative; z-index: 1; max-width: 680px; margin: 11vh 0 6vh; }.story-index { color: #dca56f; font: 700 11px/1 var(--font-family-ui); letter-spacing: .18em; }
.story-copy h1 { margin: 22px 0; color: #f8f4e8; font-size: clamp(45px, 5vw, 76px); font-weight: 500; letter-spacing: -.04em; line-height: 1.02; }.story-copy h1 em { color: #dca56f; font-style: italic; }.story-copy p { max-width: 570px; margin: 0; color: rgba(248,244,232,.72); font-size: 18px; line-height: 1.8; }
.knowledge-orbit { position: absolute; right: clamp(-100px, -4vw, -30px); top: 49%; width: 360px; height: 360px; transform: translateY(-50%); opacity: .72; }.orbit { position: absolute; inset: 50%; border: 1px solid rgba(248,244,232,.24); border-radius: 50%; transform: translate(-50%, -50%); }.orbit-one { width: 210px; height: 210px; }.orbit-two { width: 340px; height: 340px; }
.node { position: absolute; display: grid; width: 54px; height: 54px; place-items: center; border: 1px solid rgba(248,244,232,.25); border-radius: 50%; background: #214d40; color: rgba(248,244,232,.85); font: 600 12px/1 var(--font-family-ui); box-shadow: 0 14px 30px rgba(0,0,0,.16); }.node-main { left: 153px; top: 153px; background: #b8723a; color: white; }.node-a { left: 16px; top: 70px; }.node-b { right: 6px; top: 90px; }.node-c { left: 55px; bottom: 20px; }.node-d { right: 45px; bottom: 10px; }
.story-proof { display: grid; grid-template-columns: repeat(3, 1fr); max-width: 560px; border-top: 1px solid rgba(248,244,232,.18); }.story-proof div { display: flex; flex-direction: column; gap: 3px; padding: 20px 18px 0 0; }.story-proof strong { color: #dca56f; font: 500 22px/1 var(--font-family-display); }.story-proof span { color: rgba(248,244,232,.58); font: 500 12px/1.4 var(--font-family-ui); }
.auth-form-side { display: flex; min-height: 100vh; align-items: center; justify-content: center; flex-direction: column; padding: clamp(28px, 5vw, 80px); }.auth-form-card { width: min(100%, 470px); }.auth-footnote { max-width: 470px; margin: 30px 0 0; padding-top: 18px; border-top: 1px solid #ddd5c6; color: #7d766a; font-size: 12px; line-height: 1.6; text-align: center; }.mobile-brand { display: none; }
@media (max-width: 960px) { .auth-shell { grid-template-columns: 1fr; }.auth-story { display: none; }.auth-form-side { padding: 28px 22px 46px; }.mobile-brand { display: flex; align-items: center; align-self: flex-start; gap: 10px; margin-bottom: 48px; color: #173d32; }.mobile-brand .brand-mark { width: 38px; height: 38px; background: #173d32; }.mobile-brand strong { font: 600 20px/1 var(--font-family-display); }.auth-form-card { margin: auto; } }
@media (prefers-reduced-motion: reduce) { *, *::before, *::after { animation: none !important; transition: none !important; } }
</style>
