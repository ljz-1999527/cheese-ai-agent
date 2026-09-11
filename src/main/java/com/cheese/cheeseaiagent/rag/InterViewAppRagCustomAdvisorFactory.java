package com.cheese.cheeseaiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

/**
 * 创建自定义的 RAG 检索增强顾问的工厂
 * <p>
 * 与 Spring AI 内置的 QuestionAnswerAdvisor 不同，本工厂构建的 Advisor
 * 支持"按状态过滤 + 相似度阈值 + 自定义上下文增强器"，可实现更精细的
 * RAG 检索控制（如只检索"单身"状态的恋爱知识文档）。
 */
public class InterViewAppRagCustomAdvisorFactory {

    /**
     * 创建自定义的 RAG 检索增强顾问
     *
     * @param vectorStore 向量存储（知识库）
     * @param status      状态（用于过滤文档，如"单身"、"恋爱"、"已婚"）
     * @return 自定义的 RAG 检索增强顾问
     */
    public static Advisor createInterViewAppRagCustomAdvisor(VectorStore vectorStore, String status) {
        // 过滤特定状态的文档：构建 status == xxx 的过滤表达式
        Filter.Expression expression = new FilterExpressionBuilder()
                .eq("status", status)
                .build();
        // 创建文档检索器：从向量库检索相似度高于 0.5 的 Top 3 文档
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression) // 过滤条件
                .similarityThreshold(0.5) // 相似度阈值
                .topK(3) // 返回文档数量
                .build();
        // 构建检索增强顾问：检索 + 自定义上下文增强（空上下文时返回预设提示）
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .queryAugmenter(InterViewAppContextualQueryAugmenterFactory.createInstance())
                .build();
    }
}
