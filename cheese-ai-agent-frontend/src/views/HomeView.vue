<script setup>
import { useRouter } from 'vue-router'
import interviewAvatar from '@/assets/avatars/interview-ai.svg'
import manusAvatar from '@/assets/avatars/manus-ai.svg'

const router = useRouter()

const apps = [
  {
    name: 'AI 面试大师',
    description: '模拟面试场景，与 AI 面试官进行实时对话，提升你的面试技巧。',
    accent: '#2563eb',
    route: '/interview',
    avatar: interviewAvatar,
    tag: '面试',
  },
  {
    name: 'AI 超级智能体',
    description: '强大的 AI 智能体助手，可以调用多种工具来帮你完成复杂任务。',
    accent: '#7c3aed',
    route: '/manus',
    avatar: manusAvatar,
    tag: '智能体',
  },
]

function goTo(route) {
  router.push(route)
}
</script>

<template>
  <div class="home">
    <div class="home-inner">
      <header class="home-header">
        <div class="brand">
          <div class="brand-logo">🧀</div>
          <h1>Cheese AI Agent</h1>
        </div>
        <p class="tagline">选择一个 AI 应用开始对话</p>
      </header>

      <div class="app-grid">
        <div
          v-for="app in apps"
          :key="app.name"
          class="app-card"
          @click="goTo(app.route)"
          :style="{ '--accent': app.accent }"
        >
          <div class="app-card-top">
            <div class="app-avatar">
              <img :src="app.avatar" :alt="app.name" />
            </div>
            <span class="app-tag">{{ app.tag }}</span>
          </div>
          <h2 class="app-name">{{ app.name }}</h2>
          <p class="app-desc">{{ app.description }}</p>
          <div class="app-footer">
            <span class="enter-link">进入应用</span>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="5" y1="12" x2="19" y2="12"/>
              <polyline points="12 5 19 12 12 19"/>
            </svg>
          </div>
        </div>
      </div>

      <footer class="home-footer">
        <p>© Cheese AI Agent · Powered by Spring Boot + Vue3</p>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.home {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 40px 0;
  background:
    radial-gradient(ellipse at top, #dbeafe 0%, transparent 50%),
    radial-gradient(ellipse at bottom right, #ede9fe 0%, transparent 50%),
    var(--bg-main);
}

.home-inner {
  width: 100%;
  max-width: 880px;
  padding: 0 24px;
}

.home-header {
  text-align: center;
  margin-bottom: 56px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.brand-logo {
  font-size: 32px;
}

.brand h1 {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}

.tagline {
  font-size: 15px;
  color: var(--text-secondary);
}

.app-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}

.app-card {
  --accent: var(--primary-color);
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: var(--radius-xl);
  padding: 28px;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s, border-color 0.25s;
  position: relative;
  overflow: hidden;
}

.app-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--accent);
  opacity: 0.8;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--accent);
}

.app-card:hover .enter-link,
.app-card:hover svg {
  color: var(--accent);
}

.app-card:hover svg {
  transform: translateX(4px);
}

.app-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.app-avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-tag {
  font-size: 12px;
  font-weight: 600;
  color: var(--accent);
  background: color-mix(in srgb, var(--accent) 10%, transparent);
  padding: 4px 10px;
  border-radius: 999px;
}

.app-name {
  font-size: 20px;
  font-weight: 600;
  color: var(--text-primary);
}

.app-desc {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--text-secondary);
}

.app-footer {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-top: 20px;
  font-size: 14px;
  font-weight: 500;
  color: var(--text-secondary);
  transition: color 0.2s;
}

.app-footer svg {
  transition: transform 0.2s, color 0.2s;
}

.home-footer {
  text-align: center;
  margin-top: 48px;
  color: var(--text-muted);
  font-size: 12px;
}

/* ---- 响应式 ---- */

/* 平板 */
@media (max-width: 1024px) {
  .app-grid {
    gap: 20px;
  }
}

/* 手机 */
@media (max-width: 640px) {
  .home {
    padding: 24px 0;
  }

  .home-header {
    margin-bottom: 36px;
  }

  .brand h1 {
    font-size: 24px;
  }

  .app-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .app-card {
    padding: 22px;
  }

  .app-avatar {
    width: 48px;
    height: 48px;
  }

  .app-name {
    font-size: 18px;
  }

  .home-footer {
    margin-top: 32px;
  }
}
</style>
