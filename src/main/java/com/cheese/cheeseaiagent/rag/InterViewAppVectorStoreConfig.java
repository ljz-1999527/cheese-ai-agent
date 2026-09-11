package com.cheese.cheeseaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 面试大师向量数据库配置（初始化基于内存的向量数据库 Bean）
 * <p>
 * 使用 SimpleVectorStore（内存向量库）承载面试知识库：
 * 启动时加载 Markdown 知识文档 → 用 EmbeddingModel 生成向量 → 存入向量库。
 * 同时注入关键词增强器为文档补充关键词元信息，提升检索质量。
 */
@Configuration
public class InterViewAppVectorStoreConfig {

    /** 文档加载器：负责加载 classpath 下的 Markdown 知识文档 */
    @Resource
    private InterViewAppDocumentLoader interViewAppDocumentLoader;

    /** 自定义 Token 切分器（备选方案，默认未启用） */
    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    /** 关键词元信息增强器：为文档自动补充关键词 */
    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    /**
     * 创建面试知识库向量存储 Bean
     *
     * @param ollamaEmbeddingModel ollama向量化（Embedding）模型
     * @return 初始化完成的内存向量库
     */
    @Bean
    VectorStore interViewAppVectorStore(EmbeddingModel ollamaEmbeddingModel) {
        // 构建基于内存的简单向量库
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(ollamaEmbeddingModel).build();
        // 加载文档：读取 classpath:document/*.md 知识文档
        List<Document> documentList = interViewAppDocumentLoader.loadMarkdowns();
        // 自主切分文档（备选方案：先按 Token 切分再入库），不推荐
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);
        // 自动补充关键词元信息：让检索更精准
        // 【本地环境适配】本机 CPU 推理 qwen3.5:9b 单次生成关键词需 2~2.5 分钟，
        // 启动时对每个文档分块串行调用会令应用启动耗时过长而无法接受，
        // 故临时跳过关键词增强，直接以原始文档向量化入库（RAG 检索功能不受影响）。
        // 如需恢复：在有 GPU 的机器上，或将 Ollama 模型换成 qwen2.5:0.5b 等轻量模型后，
        // 取消下行注释改用 enrichedDocuments 即可。
//        List<Document> enrichedDocuments = myKeywordEnricher.enrichDocuments(documentList);
        // 将文档向量化后写入向量库
        simpleVectorStore.add(documentList);
        return simpleVectorStore;
    }
}
