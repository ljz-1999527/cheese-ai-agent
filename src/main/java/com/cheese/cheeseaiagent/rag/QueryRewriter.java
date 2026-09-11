package com.cheese.cheeseaiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

/**
 * 查询重写器
 * <p>
 * 在 RAG 检索前，让大模型将用户的自然语言问题改写为更适合向量检索的查询语句，
 * 从而提升知识库检索的准确率。
 */
@Component
public class QueryRewriter {

    /** 查询转换器：负责执行查询重写 */
    private final QueryTransformer queryTransformer;

    /**
     * 构造方法：构建查询重写转换器
     *
     * @param ollamaChatModel 通义千问大模型实例
     */
    public QueryRewriter(ChatModel ollamaChatModel) {
        // 构建 ChatClient Builder（驱动大模型完成改写）
        ChatClient.Builder builder = ChatClient.builder(ollamaChatModel);
        // 创建查询重写转换器
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(builder)
                .build();
    }

    /**
     * 执行查询重写
     *
     * @param prompt 用户原始查询文本
     * @return 重写后的查询文本
     */
    public String doQueryRewrite(String prompt) {
        // 将文本包装为 Query 对象
        Query query = new Query(prompt);
        // 执行查询重写
        Query transformedQuery = queryTransformer.transform(query);
        // 输出重写后的查询
        return transformedQuery.text();
    }
}
