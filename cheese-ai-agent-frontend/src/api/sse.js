/**
 * SSE 流式请求工具（基于原生 EventSource）
 *
 * 后端通过 Spring WebFlux Flux<String> / SseEmitter 以 text/event-stream 形式
 * 持续推送文本碎片。EventSource 会自动按 SSE 协议解析每个事件，data 字段内容
 * 通过 event.data 暴露，无需手动切分 \n\n 或剥离 "data:" 前缀。
 *
 * 每收到一个 data 事件 → 触发一次 onMessage(片段文本)；
 * 调用方应把片段追加到同一个 AI 气泡（打字机效果），直到 onDone 触发。
 *
 * @param {string} endpoint - 接口路径，如 /api/ai/inter_view_app/chat/sse
 * @param {Object} params - 查询参数，如 { message: '你好', chatId: 'xxx' }
 * @param {Object} handlers - 回调函数
 * @param {Function} handlers.onMessage - 每收到一段内容时触发，参数为该段文本
 * @param {Function} [handlers.onError] - 连接失败（如 404）时触发
 * @param {Function} [handlers.onDone] - 流结束（后端关闭连接或主动取消）时触发
 * @returns {{ abort: () => void }} 可调用 .abort() 主动取消
 */
export function streamSSE(endpoint, params, { onMessage, onError, onDone }) {
  const queryString = new URLSearchParams(params).toString()
  const url = queryString ? `${endpoint}?${queryString}` : endpoint

  let finished = false

  // 防止 onDone / onError 被重复触发
  const finish = (error = null) => {
    if (finished) return
    finished = true
    eventSource.close()
    if (error) {
      onError?.(error)
    }
    onDone?.()
  }

  const eventSource = new EventSource(url)

  // 每收到一个 SSE data 事件就回调一次（事件内容已由浏览器解析为 event.data）
  eventSource.onmessage = (event) => {
    onMessage?.(event.data)
  }

  eventSource.onerror = () => {
    if (finished) return
    // EventSource 在两种情况下会触发 onerror：
    //   1. 连接真正失败（如 404、网络错误）：readyState === CLOSED (2)
    //   2. 后端 Flux/SseEmitter 结束、连接被正常关闭：readyState === CONNECTING (0)
    //      此时 EventSource 会尝试自动重连，我们需要主动 close() 并当作流结束处理
    if (eventSource.readyState === EventSource.CLOSED) {
      finish(new Error('SSE 连接失败，请检查后端服务是否正常'))
    } else {
      finish()
    }
  }

  return {
    abort: () => finish(),
  }
}
