package com.cheese.cheeseaiagent.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ollama 原生 /api/chat 流式对话服务（thinking / content 隔离版）
 * <p>
 * 背景：Spring AI 1.0.0 的 OllamaApi.Message 只解析 content/images/toolCalls、不解析 thinking 字段；
 * 而 qwen3.5:9b 是思考型模型，流式 chunk 中 message.content 恒为空、token 全落在 message.thinking，
 * 导致走 Spring AI ChatClient 的 SSE 无正文输出，且思考 token 会耗尽 num_predict 额度（done_reason=length）。
 * <p>
 * 本服务绕过 Spring AI 流式管道，直接调用 Ollama 原生 /api/chat 流式接口（NDJSON 逐行 JSON），
 * 将 thinking 增量与 content 增量拆分为不同 SSE 事件（event:thinking / event:content / event:done）推送，
 * 并通过 options.num_predict 设置生成 token 预算上限，避免思考 token 过量挤占 content 额度。
 */
@Component
@Slf4j
public class OllamaNativeChatService {

    /** Ollama 服务地址（来自 application.yml 的 spring.ai.ollama.base-url） */
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;

    /** 使用的模型名（来自 application.yml 的 spring.ai.ollama.chat.model） */
    @Value("${spring.ai.ollama.chat.model:qwen3.5:9b}")
    private String model;

    /** 上下文窗口（与现有 Spring AI 配置一致，避免长历史 + 长思考被截断） */
    @Value("${spring.ai.ollama.chat.options.num-ctx:16384}")
    private int numCtx;

    /** 默认生成 token 预算上限：思考 + 正文合计不超过该值，防止思考挤占正文额度 */
    private static final int DEFAULT_NUM_PREDICT = 1024;

    /** 内存会话历史上限（最近 N 条），防止多轮历史无限膨胀顶满 num_ctx */
    private static final int MAX_HISTORY_MESSAGES = 12;

    /** Jackson：解析 NDJSON 响应与序列化请求 / done 事件 */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 复用全局 HttpClient（连接复用，减少握手开销） */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    /** 内存会话历史：chatId -> 消息列表（role/content），多轮记忆拼接用；如需跨重启持久化可替换为 FileBasedChatMemory */
    private final Map<String, List<Map<String, String>>> chatHistories = new ConcurrentHashMap<>();

    /**
     * thinking / content 隔离的流式对话（Ollama 原生 /api/chat，NDJSON）
     *
     * @param message    用户输入
     * @param chatId     会话 ID（为空则单轮；非空则拼接内存历史做多轮）
     * @param numPredict 生成 token 预算上限（null 或非法值用默认 1024）
     * @return SSE 流：event:thinking（思考增量）/ event:content（正文增量）/ event:done（统计信息）
     */
    public Flux<ServerSentEvent<String>> chatStreamIsolated(String message, String chatId, Integer numPredict) {
        // token 预算：非法值回退默认
        int numPredictValue = (numPredict != null && numPredict > 0) ? numPredict : DEFAULT_NUM_PREDICT;

        // 组装 Ollama messages：多轮拼接历史 + 当前用户消息
        Map<String, String> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        List<Map<String, String>> messages = new ArrayList<>();
        if (chatId != null && !chatId.isBlank()) {
            List<Map<String, String>> history = chatHistories.get(chatId);
            if (history != null && !history.isEmpty()) {
                messages.addAll(history);
            }
        }
        messages.add(userMessage);

        // 使用 Flux.push 将阻塞式 HTTP 流转换为响应式流
        return Flux.push((FluxSink<ServerSentEvent<String>> sink) -> {
            AtomicReference<InputStream> bodyRef = new AtomicReference<>();
            // 客户端断开时关闭底层输入流，中断阻塞读
            sink.onDispose(() -> {
                InputStream in = bodyRef.get();
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // 关闭失败不影响主流程
                    }
                }
            });
            try {
                HttpRequest request = buildRequest(messages, numPredictValue);
                HttpResponse<InputStream> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofInputStream());
                bodyRef.set(response.body());
                if (response.statusCode() != 200) {
                    sink.error(new RuntimeException("Ollama 原生接口返回异常状态码: " + response.statusCode()));
                    return;
                }
                streamAndEmit(response.body(), sink, chatId, userMessage);
                sink.complete();
            } catch (IOException e) {
                if (sink.isCancelled()) {
                    // 客户端已断开导致的读中断，属正常结束
                    log.debug("客户端断开，停止读取 Ollama 流: {}", e.getMessage());
                } else {
                    log.error("读取 Ollama 流失败", e);
                    sink.error(e);
                }
            } catch (Exception e) {
                log.error("Ollama 原生流式调用失败", e);
                sink.error(e);
            }
        });
    }

    /**
     * 构造 Ollama /api/chat 请求体（stream=true，携带 num_predict 预算与 num_ctx）
     */
    private HttpRequest buildRequest(List<Map<String, String>> messages, int numPredict) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("stream", true);
        Map<String, Object> options = new LinkedHashMap<>();
        // 生成 token 预算上限：思考 + 正文合计不超过该值，避免思考 token 挤占 content 额度
        options.put("num_predict", numPredict);
        // 上下文窗口：与现有 Spring AI 配置一致
        options.put("num_ctx", numCtx);
        payload.put("options", options);

        String body = objectMapper.writeValueAsString(payload);
        String url = (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl) + "/api/chat";
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMinutes(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
    }

    /**
     * 逐行读取 NDJSON 响应流，将 thinking / content 增量拆分为独立 SSE 事件推送，done 时回报统计并保存记忆
     */
    private void streamAndEmit(InputStream body, FluxSink<ServerSentEvent<String>> sink,
                               String chatId, Map<String, String> userMessage) throws IOException {
        long thinkingChars = 0;
        long contentChars = 0;
        StringBuilder fullContent = new StringBuilder();
        String doneReason = "unknown";
        long evalCount = 0;
        long promptEvalCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(body, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sink.isCancelled()) {
                    return;
                }
                if (line.isBlank()) {
                    continue;
                }
                JsonNode node = objectMapper.readTree(line);
                JsonNode messageNode = node.path("message");
                if (messageNode.isObject()) {
                    // 思考增量：兼容 thinking / reasoning_content / reasoning 三种字段命名
                    String thinking = extractText(messageNode, "thinking", "reasoning_content", "reasoning");
                    if (thinking != null && !thinking.isEmpty()) {
                        thinkingChars += thinking.length();
                        sink.next(ServerSentEvent.<String>builder().event("thinking").data(thinking).build());
                    }
                    // 正文增量
                    String content = extractText(messageNode, "content");
                    if (content != null && !content.isEmpty()) {
                        contentChars += content.length();
                        fullContent.append(content);
                        sink.next(ServerSentEvent.<String>builder().event("content").data(content).build());
                    }
                }
                // 结束标记：携带 done_reason / eval_count 等统计
                if (node.path("done").asBoolean(false)) {
                    doneReason = node.path("done_reason").asText("unknown");
                    evalCount = node.path("eval_count").asLong(0);
                    promptEvalCount = node.path("prompt_eval_count").asLong(0);
                    break;
                }
            }
        }

        // done 事件：回报统计信息（eval_count 为 Ollama 权威总生成 token；thinking/content 分项按字符估算）
        sink.next(ServerSentEvent.<String>builder().event("done")
                .data(buildDonePayload(doneReason, evalCount, promptEvalCount, thinkingChars, contentChars))
                .build());

        // 保存多轮记忆：仅保存正文（thinking 是思考过程，不注入上下文，避免挤占 num_ctx）
        saveHistory(chatId, userMessage, fullContent.toString());
    }

    /**
     * 构造 done 事件数据：回报 thinking / content 各自消耗情况与总 token
     */
    private String buildDonePayload(String doneReason, long evalCount, long promptEvalCount,
                                    long thinkingChars, long contentChars) {
        Map<String, Object> done = new LinkedHashMap<>();
        done.put("event", "done");
        done.put("model", model);
        done.put("done_reason", doneReason);
        done.put("eval_count", evalCount);                 // Ollama 官方：本次生成总 token 数（思考 + 正文）
        done.put("prompt_eval_count", promptEvalCount);    // Ollama 官方：输入 token 数
        done.put("thinking_chars", thinkingChars);         // 思考实际字符数（增量累计）
        done.put("content_chars", contentChars);           // 正文实际字符数（增量累计）
        done.put("thinking_tokens_est", Math.round(thinkingChars / 4.0));  // 思考 token 估算（约 4 字符/token）
        done.put("content_tokens_est", Math.round(contentChars / 4.0));    // 正文 token 估算
        done.put("total_tokens_est", Math.round((thinkingChars + contentChars) / 4.0));
        try {
            return objectMapper.writeValueAsString(done);
        } catch (JsonProcessingException e) {
            log.warn("done 事件序列化失败，降级为最小结构: {}", e.getMessage());
            return "{\"event\":\"done\",\"done_reason\":\"" + doneReason + "\"}";
        }
    }

    /**
     * 保存多轮记忆到内存（线程安全），仅存正文，并限制条数防止上下文膨胀
     */
    private void saveHistory(String chatId, Map<String, String> userMessage, String assistantContent) {
        if (chatId == null || chatId.isBlank()) {
            return;
        }
        List<Map<String, String>> history = chatHistories.computeIfAbsent(chatId, k -> new ArrayList<>());
        synchronized (history) {
            history.add(userMessage);
            // 正文为空时跳过 assistant 记录（避免空字符串消息干扰后续轮次）
            if (!assistantContent.isEmpty()) {
                Map<String, String> assistant = new LinkedHashMap<>();
                assistant.put("role", "assistant");
                assistant.put("content", assistantContent);
                history.add(assistant);
            }
            // 仅保留最近 N 条，防止多轮历史顶满 num_ctx
            if (history.size() > MAX_HISTORY_MESSAGES) {
                chatHistories.put(chatId, new ArrayList<>(history.subList(history.size() - MAX_HISTORY_MESSAGES, history.size())));
            }
        }
    }

    /**
     * 从 message 节点按字段名依次取值（兼容不同模型的思考字段命名），返回首个非空文本
     */
    private String extractText(JsonNode messageNode, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode value = messageNode.get(fieldName);
            if (value != null && value.isTextual() && !value.asText().isEmpty()) {
                return value.asText();
            }
        }
        return null;
    }
}
