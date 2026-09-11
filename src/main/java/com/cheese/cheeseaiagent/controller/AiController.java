package com.cheese.cheeseaiagent.controller;

import com.cheese.cheeseaiagent.agent.CheeseManus;
import com.cheese.cheeseaiagent.agent.YuManus;
import com.cheese.cheeseaiagent.app.InterViewApp;
import com.cheese.cheeseaiagent.app.OllamaNativeChatService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

/**
 * AI 功能控制器（HTTP 入口层）
 * <p>
 * 统一暴露 AI 相关 REST 接口，供前端调用。所有接口统一以 /ai 为前缀，
 * 内部委托给 InterViewApp（面试大师应用）与 CheeseManus（超级智能体）处理。
 * 包含同步调用、SSE 流式调用、Server-Sent-Event 封装、SseEmitter 封装等多种调用方式演示。
 */
@RestController
@RequestMapping("/ai")
public class AiController {

    /** 面试大师应用（负责对话、RAG、工具调用、MCP 等业务能力） */
    @Resource
    private InterViewApp interViewApp;

    /** 项目注册的全部工具回调（用于构造 CheeseManus 智能体） */
    @Resource
    private ToolCallback[] allTools;
    @Resource
    private ToolCallbackProvider mcpToolCallbackProvider;

    /** 通义千问大模型实例（用于构造 CheeseManus 智能体） */
    @Resource
    private ChatModel ollamaChatModel;

    /** Ollama 原生流式对话服务（thinking / content 隔离，直连 /api/chat） */
    @Resource
    private OllamaNativeChatService ollamaNativeChatService;

    /**
     * 同步调用 AI 面试大师应用
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return AI 回复文本
     */
    @GetMapping("/inter_view_app/chat/sync")
    public String doChatWithInterViewAppSync(String message, String chatId) {
        // 委托给 InterViewApp 的同步对话方法
        return interViewApp.doChat(message, chatId);
    }

    /**
     * SSE 流式调用 AI 面试大师应用
     * <p>
     * 使用 Spring WebFlux 的 Flux<String> 返回，配合 produces=TEXT_EVENT_STREAM_VALUE
     * 实现标准的 Server-Sent Events 流式推送。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return 流式文本片段
     */
    @GetMapping(value = "/inter_view_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithInterViewAppSSE(String message, String chatId) {
        // 委托给 InterViewApp 的流式对话方法（返回 Flux 流）
        return interViewApp.doChatByStream(message, chatId);
    }

    /**
     * SSE 流式调用 AI 面试大师应用（thinking / content 隔离版）
     * <p>
     * 绕过 Spring AI 流式管道，直连 Ollama 原生 /api/chat 流式接口（NDJSON 逐行 JSON）。
     * 针对 qwen3.5:9b 这类思考型模型（token 全落在 message.thinking、content 恒为空）的问题，
     * 将思考增量与正文增量拆分为不同 SSE 事件推送，前端可分别消费：
     * - event:thinking  思考过程增量
     * - event:content   正文增量
     * - event:done      结束统计（done_reason / eval_count / thinking、content 各自 token 估算）
     * 通过 numPredict 设置生成 token 预算上限（默认 1024），避免思考 token 挤占正文额度。
     *
     * @param message    用户输入的消息
     * @param chatId     会话 ID（可选，传入则携带内存多轮历史）
     * @param numPredict 生成 token 预算上限（可选，默认 1024）
     * @return SSE 事件流（thinking / content / done 分离）
     */
    @GetMapping(value = "/inter_view_app/chat/sse_isolated", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithInterViewAppThinkingIsolated(
            @RequestParam("message") String message,
            @RequestParam(value = "chatId", required = false) String chatId,
            @RequestParam(value = "numPredict", required = false) Integer numPredict) {
        // 委托给 Ollama 原生流式服务：thinking 与 content 隔离推送
        return ollamaNativeChatService.chatStreamIsolated(message, chatId, numPredict);
    }

    /**
     * SSE 流式调用 AI 面试大师应用（ServerSentEvent 封装版）
     * <p>
     * 与上一个接口的区别：将每个文本片段包装为 ServerSentEvent 对象，
     * 便于前端读取事件中的结构化数据。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return 包装后的 ServerSentEvent 流
     */
    @GetMapping(value = "/inter_view_app/chat/server_sent_event")
    public Flux<ServerSentEvent<String>> doChatWithInterViewAppServerSentEvent(String message, String chatId) {
        // 将文本流映射为 ServerSentEvent 对象流
        return interViewApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 面试大师应用（SseEmitter 封装版）
     * <p>
     * 使用 Servlet 时代的 SseEmitter 实现流式推送（兼容传统 MVC 场景），
     * 通过订阅 Flux 流将每个片段手动推送到 SseEmitter。
     *
     * @param message 用户输入的消息
     * @param chatId  会话 ID
     * @return SseEmitter 对象
     */
    @GetMapping(value = "/inter_view_app/chat/sse_emitter")
    public SseEmitter doChatWithInterViewAppServerSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter（3 分钟超时）
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3 分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        interViewApp.doChatByStream(message, chatId)
                .subscribe(chunk -> {
                    // 每收到一个片段就推送给前端
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        // 推送失败则以异常结束
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        // 返回 SseEmitter 给 Spring MVC 处理
        return sseEmitter;
    }

    /**
     * 流式调用 Manus 超级智能体
     *
     * @param message 用户输入的消息
     * @return SseEmitter 对象，逐步推送智能体的每一步执行结果
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        // 每次请求创建新的 CheeseManus 智能体实例（注入全部工具与通义千问模型）
        CheeseManus cheeseManus = new CheeseManus(allTools,mcpToolCallbackProvider, ollamaChatModel);
        // 以流式方式运行智能体，返回 SSE 推送对象
        return cheeseManus.runStream(message);
    }
}
