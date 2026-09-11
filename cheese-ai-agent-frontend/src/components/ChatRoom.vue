<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, computed } from 'vue'
import { useRouter } from 'vue-router'
import { streamSSE } from '@/api/sse'

const props = defineProps({
  /** 应用标题 */
  title: { type: String, required: true },
  /** 应用副标题 */
  subtitle: { type: String, default: '' },
  /** SSE 接口路径 */
  endpoint: { type: String, required: true },
  /** 是否需要自动生成并传递 chatId */
  useChatId: { type: Boolean, default: false },
  /** 流式模式:
   *  - append: 所有碎片追加到同一个气泡(打字机),AI 面试大师用
   *  - step:   每个 SSE 事件是独立步骤气泡,AI 超级智能体用
   */
  streaming: { type: String, default: 'append', validator: (v) => ['append', 'step'].includes(v) },
  /** AI 头像图片 URL */
  aiAvatar: { type: String, default: '' },
  /** 用户头像图片 URL */
  userAvatar: { type: String, default: '' },
  /** 应用主题色(用于强调) */
  accent: { type: String, default: '#2563eb' },
})

const router = useRouter()

const messages = ref([])
const inputText = ref('')
const isLoading = ref(false)
const chatId = ref('')
const messagesContainer = ref(null)
let abortController = null

onMounted(() => {
  if (props.useChatId) {
    chatId.value = generateChatId()
  }
})

onBeforeUnmount(() => {
  abortController?.abort()
})

function generateChatId() {
  return `chat_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

/** 创建消息对象 */
function makeUserMsg(text) {
  return {
    id: Date.now(),
    role: 'user',
    content: text,
  }
}

/** 创建 AI 占位消息 */
function makeAiMsg() {
  return {
    id: Date.now() + Math.random(),
    role: 'ai',
    content: '',
    thinking: true,
    /** step 模式下的步骤编号 */
    stepIndex: null,
    /** step 模式: 当前步骤是否仍在流式输出 */
    streaming: false,
  }
}

function handleSend() {
  const text = inputText.value.trim()
  if (!text || isLoading.value) return

  inputText.value = ''
  messages.value.push(makeUserMsg(text))

  isLoading.value = true
  scrollToBottom()

  const params = { message: text }
  if (props.useChatId && chatId.value) {
    params.chatId = chatId.value
  }

  let stepCounter = 0
  /** append 模式下复用的那个 AI 消息 */
  let appendAiMsg = null

  abortController = streamSSE(props.endpoint, params, {
    onMessage: (data) => {
      if (props.streaming === 'append') {
        // append 模式:第一次收到数据时创建一个 AI 气泡,后续碎片都追加到这一个气泡
        if (!appendAiMsg) {
          appendAiMsg = makeAiMsg()
          appendAiMsg.thinking = false
          messages.value.push(appendAiMsg)
        }
        appendAiMsg.content += data
        scrollToBottom()
      } else {
        // step 模式:每个 SSE 事件 = 一个新的步骤气泡
        const stepMsg = makeAiMsg()
        stepMsg.thinking = false
        stepMsg.stepIndex = ++stepCounter
        stepMsg.streaming = true
        stepMsg.content = data
        messages.value.push(stepMsg)
        scrollToBottom()
      }
    },
    onError: (error) => {
      if (props.streaming === 'append') {
        if (!appendAiMsg) {
          appendAiMsg = makeAiMsg()
          messages.value.push(appendAiMsg)
        }
        appendAiMsg.thinking = false
        if (!appendAiMsg.content) {
          appendAiMsg.content = '抱歉，发生了错误，请重试。'
        }
      } else {
        // step 模式:关闭所有还在 streaming 的气泡
        messages.value.forEach((m) => {
          if (m.role === 'ai') {
            m.thinking = false
            m.streaming = false
            if (!m.content) m.content = '抱歉，发生了错误，请重试。'
          }
        })
      }
      isLoading.value = false
      scrollToBottom()
    },
    onDone: () => {
      if (props.streaming === 'append') {
        if (!appendAiMsg) {
          appendAiMsg = makeAiMsg()
          appendAiMsg.thinking = false
          messages.value.push(appendAiMsg)
        } else {
          appendAiMsg.thinking = false
        }
        if (!appendAiMsg.content) appendAiMsg.content = '(空回复)'
      } else {
        messages.value.forEach((m) => {
          if (m.role === 'ai') {
            m.thinking = false
            m.streaming = false
            if (!m.content) m.content = '(空回复)'
          }
        })
      }
      isLoading.value = false
      scrollToBottom()
    },
  })
}

function goHome() {
  router.push('/')
}

/** 格式化时间戳 */
function formatTime(ts) {
  const d = new Date(ts)
  const hh = String(d.getHours()).padStart(2, '0')
  const mm = String(d.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}
</script>

<template>
  <div class="chat-room">
    <!-- 顶部标题栏 -->
    <header class="chat-header">
      <button class="back-btn" @click="goHome">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor"
             stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="19" y1="12" x2="5" y2="12"/>
          <polyline points="12 19 5 12 12 5"/>
        </svg>
        <span>返回</span>
      </button>
      <div class="header-info">
        <h1 class="header-title">{{ title }}</h1>
        <p v-if="subtitle" class="header-subtitle">{{ subtitle }}</p>
      </div>
      <span v-if="chatId" class="chat-id-tag">会话 ID: {{ chatId }}</span>
    </header>

    <!-- 聊天记录区域 -->
    <div class="chat-messages" ref="messagesContainer">
      <div v-if="messages.length === 0" class="empty-hint">
        <div class="empty-avatar" :style="{ background: accent }">
          <img v-if="aiAvatar" :src="aiAvatar" :alt="title" />
        </div>
        <p>开始和 {{ title }} 对话吧</p>
      </div>

      <div
        v-for="msg in messages"
        :key="msg.id"
        :class="[
          'message-row',
          msg.role === 'user' ? 'message-user' : 'message-ai',
          msg.streaming ? 'message-streaming' : '',
        ]"
      >
        <!-- 头像 -->
        <div class="avatar">
          <template v-if="msg.role === 'user'">
            <img v-if="userAvatar" :src="userAvatar" alt="我" />
            <span v-else>我</span>
          </template>
          <template v-else>
            <img v-if="aiAvatar" :src="aiAvatar" :alt="title" />
            <span v-else>AI</span>
          </template>
        </div>

        <!-- 气泡 -->
        <div class="bubble">
          <!-- step 模式:步骤标题 -->
          <div v-if="msg.role === 'ai' && msg.stepIndex !== null" class="step-header">
            <span class="step-badge" :style="{ background: accent }">Step {{ msg.stepIndex }}</span>
            <span v-if="msg.streaming" class="step-status running">执行中</span>
            <span v-else class="step-status done">已完成</span>
          </div>

          <!-- thinking 状态:打字指示器 -->
          <div v-if="msg.thinking" class="typing-hint">
            <span>正在输入</span>
            <span class="dots"></span>
          </div>

          <!-- 正常内容 -->
          <template v-else>
            <p class="bubble-text">{{ msg.content }}</p>
          </template>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <footer class="chat-input">
      <textarea
        v-model="inputText"
        :placeholder="isLoading ? 'AI 回复中...' : '输入消息，按 Enter 发送，Shift + Enter 换行'"
        :disabled="isLoading"
        rows="1"
        @keydown.enter.exact.prevent="handleSend"
        ref="textarea"
      ></textarea>
      <button class="send-btn" :disabled="isLoading || !inputText.trim()" @click="handleSend">
        <svg v-if="isLoading" width="18" height="18" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
             class="spin">
          <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
        </svg>
        <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none"
             stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <line x1="22" y1="2" x2="11" y2="13"/>
          <polygon points="22 2 15 22 11 13 2 9 22 2"/>
        </svg>
      </button>
    </footer>
  </div>
</template>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: var(--bg-main);
}

/* ---- Header ---- */
.chat-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 24px;
  background: var(--bg-card);
  border-bottom: 1px solid var(--border-color);
  box-shadow: var(--shadow-sm);
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  font-size: 14px;
  color: var(--text-secondary);
  border-radius: var(--radius-sm);
  transition: background 0.2s;
}

.back-btn:hover {
  background: var(--bg-hover);
}

.header-info {
  flex: 1;
  min-width: 0;
}

.header-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.header-subtitle {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-id-tag {
  font-size: 11px;
  color: var(--text-muted);
  background: var(--bg-main);
  padding: 3px 8px;
  border-radius: 999px;
  white-space: nowrap;
  font-family: 'SF Mono', 'Menlo', monospace;
}

/* ---- Messages ---- */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-hint {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  margin-top: 60px;
  color: var(--text-secondary);
  font-size: 15px;
}

.empty-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.empty-avatar img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(4px); }
  to   { opacity: 1; transform: translateY(0); }
}

.message-user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.message-ai {
  align-self: flex-start;
}

.message-streaming .bubble {
  box-shadow: var(--shadow-md);
}

.avatar {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 13px;
  font-weight: 600;
  background: #e2e8f0;
  color: var(--text-primary);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-user .avatar {
  background: var(--primary-color);
  color: #fff;
}

.bubble {
  padding: 10px 16px;
  border-radius: var(--radius-lg);
  font-size: 15px;
  line-height: 1.6;
  word-break: break-word;
  transition: box-shadow 0.2s;
}

.message-user .bubble {
  background: var(--primary-color);
  color: var(--user-text);
  border-top-right-radius: 4px;
}

.message-ai .bubble {
  background: var(--ai-bubble);
  color: var(--ai-text);
  border: 1px solid var(--border-color);
  border-top-left-radius: 4px;
}

.bubble-text {
  white-space: pre-wrap;
}

/* ---- Step 模式样式 ---- */
.step-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding-bottom: 8px;
  border-bottom: 1px solid var(--border-light);
}

.step-badge {
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 4px;
  letter-spacing: 0.5px;
}

.step-status {
  font-size: 12px;
  font-weight: 500;
}

.step-status.running {
  color: var(--primary-color);
}

.step-status.running::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary-color);
  margin-right: 4px;
  animation: pulse 1.2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

.step-status.done {
  color: var(--text-muted);
}

.step-status.done::before {
  content: '✓';
  color: var(--success-color);
  margin-right: 4px;
}

/* ---- Thinking 状态 ---- */
.typing-hint {
  display: flex;
  align-items: center;
  gap: 2px;
  color: var(--text-secondary);
  font-size: 14px;
}

.typing-hint .dots::after {
  content: '.';
  animation: dots-cycle 1.4s infinite;
}

@keyframes dots-cycle {
  0%   { content: ''; }
  20%  { content: '.'; }
  40%  { content: '..'; }
  60%  { content: '...'; }
  80%  { content: ''; }
}

/* ---- Input ---- */
.chat-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 24px;
  background: var(--bg-card);
  border-top: 1px solid var(--border-color);
  flex-shrink: 0;
}

.chat-input textarea {
  flex: 1;
  padding: 10px 14px;
  font-size: 15px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: var(--bg-main);
  color: var(--text-primary);
  resize: none;
  min-height: 40px;
  max-height: 120px;
  transition: border-color 0.2s, background 0.2s;
  line-height: 1.5;
}

.chat-input textarea:focus {
  border-color: var(--primary-color);
  background: #fff;
}

.chat-input textarea:disabled {
  opacity: 0.6;
}

.send-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: #fff;
  background: var(--primary-color);
  border-radius: var(--radius-md);
  transition: background 0.2s, transform 0.1s;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  background: var(--primary-dark);
}

.send-btn:active:not(:disabled) {
  transform: scale(0.96);
}

.send-btn:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}

.spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* ---- 响应式 ---- */

/* 平板 */
@media (max-width: 1024px) {
  .message-row {
    max-width: 85%;
  }
}

/* 手机 */
@media (max-width: 640px) {
  .chat-header {
    padding: 10px 14px;
    gap: 10px;
  }

  .back-btn span {
    display: none;
  }

  .chat-id-tag {
    display: none;
  }

  .chat-messages {
    padding: 16px 12px;
    gap: 12px;
  }

  .message-row {
    max-width: 90%;
    gap: 8px;
  }

  .avatar {
    width: 30px;
    height: 30px;
    font-size: 11px;
  }

  .bubble {
    padding: 9px 12px;
    font-size: 14px;
  }

  .chat-input {
    padding: 10px 12px;
    gap: 8px;
  }

  .send-btn {
    width: 38px;
    height: 38px;
  }

  .step-badge {
    font-size: 10px;
    padding: 2px 6px;
  }
}
</style>
