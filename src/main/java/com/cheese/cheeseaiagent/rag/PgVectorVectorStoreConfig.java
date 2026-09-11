package com.cheese.cheeseaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * PgVector 向量数据库配置（基于 PostgreSQL 的向量存储）
 * <p>
 * 使用 PgVector 插件将知识库向量存储在 PostgreSQL 中，支持持久化与大规模存储。
 * 默认处于注释状态（不注册为 Bean），需要时取消 @Configuration 注释即可启用，
 * 并确保 PostgreSQL 已安装 pgvector 插件。
 */
// 为方便开发调试和部署，临时注释，如果需要使用 PgVector 存储知识库，取消注释即可
//@Configuration
public class PgVectorVectorStoreConfig {

    /** 文档加载器：负责加载 classpath 下的 Markdown 知识文档 */
    @Resource
    private InterViewAppDocumentLoader interViewAppDocumentLoader;

    /**
     * 创建 PgVector 向量存储 Bean
     *
     * @param jdbcTemplate             JDBC 模板（连接 PostgreSQL）
     * @param ollamaEmbeddingModel  ollama向量化（Embedding）模型
     * @return 初始化完成的 PgVector 向量库
     */
    @Bean
    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel ollamaEmbeddingModel) {
        // 构建 PgVector 向量存储：配置维度、距离类型、索引类型与表结构
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, ollamaEmbeddingModel)
                .dimensions(1536)                    // Optional: defaults to model dimensions or 1536
                .distanceType(COSINE_DISTANCE)       // Optional: defaults to COSINE_DISTANCE
                .indexType(HNSW)                     // Optional: defaults to HNSW
                .initializeSchema(true)              // Optional: defaults to false
                .schemaName("public")                // Optional: defaults to "public"
                .vectorTableName("vector_store")     // Optional: defaults to "vector_store"
                .maxDocumentBatchSize(10000)         // Optional: defaults to 10000
                .build();
        // 加载文档：读取知识库 Markdown 文档
        List<Document> documents = interViewAppDocumentLoader.loadMarkdowns();
        // 文档向量化后写入数据库
        vectorStore.add(documents);
        return vectorStore;
    }
}
