package com.cheese.cheeseaiagent.demo.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 查询扩展器 Demo（RAG 演示类）
 * <p>
 * 演示 Spring AI 的 MultiQueryExpander（多查询扩展）能力：
 * 在 RAG 检索前，让大模型把用户的一个问题改写成多个不同角度/表述的查询，
 * 再用这些查询分别检索，从而提升知识库召回的准确率与覆盖率。
 */
//@Component
public class MultiQueryExpanderDemo {

    /** ChatClient 构建器：用于驱动大模型生成扩展查询 */
    private final ChatClient.Builder chatClientBuilder;

    /**
     * 构造方法：注入通义千问大模型并构建 ChatClient.Builder
     *
     * @param ollamaChatModel 通义千问大模型实例
     */
    public MultiQueryExpanderDemo(ChatModel ollamaChatModel) {
        this.chatClientBuilder = ChatClient.builder(ollamaChatModel);
    }

    /**
     * 将原始查询扩展为多个查询
     *
     * @param query 原始查询文本（演示中实际未使用，内部固定为示例问题）
     * @return 扩展后的查询列表
     */
    public List<Query> expand(String query) {
        // 构建多查询扩展器：指定生成 3 个扩展查询
        MultiQueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .build();
        // 对示例问题执行扩展
        List<Query> queries = queryExpander.expand(new Query("谁是程序员cheese啊？"));
        return queries;
    }
}
