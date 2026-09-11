package com.cheese.cheeseaiagent.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

/**
 * 自定义日志 Advisor
 * 打印 info 级别日志、只输出单次用户提示词和 AI 回复的文本
 * <p>
 * Advisor（顾问）是 Spring AI 提供的一种拦截器机制，可以在调用大模型的前后
 * 插入自定义逻辑。本类同时实现了 CallAdvisor（同步调用）与 StreamAdvisor（流式调用）
 * 两个接口，因此既支持普通对话的日志打印，也支持 SSE 流式对话的日志打印。
 */
@Slf4j
public class MyLoggerAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 返回 Advisor 的名称（用于日志与调试）
     *
     * @return 当前类的简单类名
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * 返回 Advisor 的执行顺序（数值越小优先级越高）
     *
     * @return 顺序值 0，表示最先执行
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 在请求发送给大模型之前执行，打印用户输入的提示词
     *
     * @param request 当前 ChatClient 请求（包含提示词、上下文等信息）
     * @return 原样返回请求，不做任何修改
     */
    private ChatClientRequest before(ChatClientRequest request) {
        // 打印完整的用户提示词内容，便于追踪每次 AI 调用的输入
        log.info("AI Request: {}", request.prompt());
        return request;
    }

    /**
     * 在收到大模型响应之后执行，打印 AI 回复的文本内容
     *
     * @param chatClientResponse ChatClient 响应对象（包含大模型返回的完整结果）
     */
    private void observeAfter(ChatClientResponse chatClientResponse) {
        // 从响应中取出最终生成的文本并打印
        assert chatClientResponse.chatResponse() != null;
        log.info("AI Response: {}", chatClientResponse.chatResponse().getResult().getOutput().getText());
    }

    /**
     * 同步调用场景下的 Advisor 拦截逻辑
     * <p>
     * 调用链：先记录请求日志，再交给调用链继续向后执行，最后记录响应日志。
     *
     * @param chatClientRequest 当前请求
     * @param chain             调用链，用于继续执行后续的 Advisor 或真正的大模型调用
     * @return 大模型返回的响应
     */
    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        log.info("AI adviseCall");
        // 请求前日志
        chatClientRequest = before(chatClientRequest);
        // 继续执行调用链（真正发起大模型调用）
        ChatClientResponse chatClientResponse = chain.nextCall(chatClientRequest);
        // 响应后日志
        observeAfter(chatClientResponse);
        return chatClientResponse;
    }

    /**
     * 流式调用场景下的 Advisor 拦截逻辑
     * <p>
     * 由于流式响应是异步的 Flux，直接观察不到完整结果，因此借助
     * ChatClientMessageAggregator 聚合完整响应后再打印日志。
     *
     * @param chatClientRequest 当前请求
     * @param chain             调用链
     * @return 流式响应 Flux（原样透传，不影响流式输出）
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        log.info("AI adviseStream");
        // 请求前日志
        chatClientRequest = before(chatClientRequest);
        // 继续执行调用链，拿到流式响应 Flux
        Flux<ChatClientResponse> chatClientResponseFlux = chain.nextStream(chatClientRequest);
        // 使用聚合器将流式片段聚合为完整响应，聚合完成后回调 observeAfter 打印日志
        return (new ChatClientMessageAggregator()).aggregateChatClientResponse(chatClientResponseFlux, this::observeAfter);
    }
}
