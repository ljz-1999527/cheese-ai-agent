package com.cheese.cheeseaiagent.rag;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetriever;
import com.alibaba.cloud.ai.dashscope.rag.DashScopeDocumentRetrieverOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 自定义基于阿里云知识库服务的 RAG 增强顾问配置
 * <p>
 * 将知识库托管在阿里云百炼的云端知识库（Data Management）中，
 * 通过 DashScopeDocumentRetriever 在对话时从云端检索相关知识，
 * 再经 RetrievalAugmentationAdvisor 注入大模型上下文。
 * 该 Bean 默认不启用，需要在 LoveApp 中显式使用。
 */
@Configuration
@Slf4j
public class LoveAppRagCloudAdvisorConfig {

    /** 从配置文件读取 DashScope API Key（默认空，本地未配置时返回 null 不注册 Bean） */
    @Value("${spring.ai.dashscope.api-key:}")
    private String dashScopeApiKey;

    /**
     * 创建基于阿里云知识库的 RAG Advisor Bean
     *
     * @return 配置好的 RAG 检索增强顾问；未配置 api-key 时返回 null（不注册 Bean）
     */
    @Bean
    public Advisor loveAppRagCloudAdvisor() {
        // 未配置 key 时不启用云 RAG（本地默认用 Ollama），上线填写 key 后自动生效
        if (dashScopeApiKey == null || dashScopeApiKey.trim().isEmpty()) {
            log.info("未配置 spring.ai.dashscope.api-key，跳过云 RAG Advisor 注册");
            return null;
        }
        // 构建 DashScope API 客户端
        DashScopeApi dashScopeApi = DashScopeApi.builder()
                .apiKey(dashScopeApiKey)
                .build();
        // 指定云端知识库名称（需提前在阿里云控制台创建）
        final String KNOWLEDGE_INDEX = "恋爱大师";
        // 创建云端文档检索器：绑定知识库索引
        DocumentRetriever dashScopeDocumentRetriever = new DashScopeDocumentRetriever(dashScopeApi,
                DashScopeDocumentRetrieverOptions.builder()
                        .withIndexName(KNOWLEDGE_INDEX)
                        .build());
        // 构建检索增强顾问：对话时自动检索云端知识库
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(dashScopeDocumentRetriever)
                .build();
    }
}
