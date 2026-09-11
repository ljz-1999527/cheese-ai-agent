package com.cheese.cheeseaiagent.advisor;

import org.jspecify.annotations.NonNull;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * 自定义 Re2 Advisor
 * 可提高大型语言模型的推理能力
 * <p>
 * Re2（Read again / Re-read）是一种提示词增强技巧：
 * 在原始问题后追加一句 "Read the question again: <原问题>"，
 * 引导大模型重新阅读问题，从而减少理解偏差、提升推理准确率。
 * 本类同时支持同步调用与流式调用两种场景。
 */
public class ReReadingAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 执行请求前，改写 Prompt
     * <p>
     * 将原始用户问题保存到上下文（context）中，并把用户消息改写成
     * "原问题 + Read the question again: 原问题" 的形式后重新构造请求。
     *
     * @param chatClientRequest 原始请求
     * @return 改写后的请求
     */
    private ChatClientRequest before(ChatClientRequest chatClientRequest) {
        // 取出用户消息中的原始文本
        String userText = chatClientRequest.prompt().getUserMessage().getText();
        // 添加上下文参数：把原始问题放入 context，方便后续流程使用
        chatClientRequest.context().put("re2_input_query", userText);
        // 修改用户提示词：使用文本块拼接"原文 + 重新阅读指令"
        String newUserText = """
                %s
                Read the question again: %s
                """.formatted(userText, userText);
        // 用改写后的用户消息增强原 Prompt，生成新的 Prompt
        Prompt newPrompt = chatClientRequest.prompt().augmentUserMessage(newUserText);
        // 构造携带新 Prompt 的请求对象并返回
        return new ChatClientRequest(newPrompt, chatClientRequest.context());
    }

    /**
     * 同步调用场景：先改写提示词，再继续调用链
     *
     * @param chatClientRequest 原始请求
     * @param chain             调用链
     * @return 大模型响应
     */
    @Override
    public ChatClientResponse adviseCall(@NonNull ChatClientRequest chatClientRequest, CallAdvisorChain chain) {
        // 改写请求后继续执行
        return chain.nextCall(this.before(chatClientRequest));
    }

    /**
     * 流式调用场景：先改写提示词，再继续调用链
     *
     * @param chatClientRequest 原始请求
     * @param chain             调用链
     * @return 流式响应 Flux
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest chatClientRequest, StreamAdvisorChain chain) {
        // 改写请求后继续执行
        return chain.nextStream(this.before(chatClientRequest));
    }

    /**
     * 返回 Advisor 执行顺序（数值越小越先执行）
     *
     * @return 顺序值 0
     */
    @Override
    public int getOrder() {
        return 0;
    }

    /**
     * 返回 Advisor 名称
     *
     * @return 当前类的简单类名
     */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
