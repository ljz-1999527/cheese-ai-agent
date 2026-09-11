package com.cheese.cheeseaiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 AI 的文档元信息增强器（为文档补充关键词元信息）
 * <p>
 * 使用大模型为每篇知识文档自动抽取关键词并写入文档 metadata，
 * 便于 RAG 检索时通过关键词提升匹配精度。
 */
@Component
public class MyKeywordEnricher {

    /** ollama大模型实例：用于生成关键词 */
    @Resource
    private ChatModel ollamaChatModel;

    /**
     * 为文档列表补充关键词元信息
     *
     * @param documents 原始文档列表
     * @return 增强后的文档列表（metadata 中新增关键词）
     */
    public List<Document> enrichDocuments(List<Document> documents) {
        // 创建关键词元信息增强器：每个文档抽取 5 个关键词
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(ollamaChatModel, 5);
        // 执行增强并返回结果
        return  keywordMetadataEnricher.apply(documents);
    }
}
