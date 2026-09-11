<script setup>
import ChatRoom from '@/components/ChatRoom.vue'
import manusAvatar from '@/assets/avatars/manus-ai.svg'
</script>

<template>
  <ChatRoom
    title="AI 超级智能体"
    subtitle="多工具智能体 · 实时对话"
    endpoint="/api/ai/manus/chat"
    :use-chat-id="false"
    streaming="step"
    :ai-avatar="manusAvatar"
    accent="#7c3aed"
  />
</template>
