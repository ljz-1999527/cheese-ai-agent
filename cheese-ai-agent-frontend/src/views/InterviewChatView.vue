<script setup>
import ChatRoom from '@/components/ChatRoom.vue'
import interviewAvatar from '@/assets/avatars/interview-ai.svg'
</script>

<template>
  <ChatRoom
    title="AI 面试大师"
    subtitle="模拟面试 · 实时对话"
    endpoint="/api/ai/inter_view_app/chat/sse"
    :use-chat-id="true"
    streaming="append"
    :ai-avatar="interviewAvatar"
    accent="#2563eb"
  />
</template>
