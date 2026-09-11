package com.cheese.cheeseaiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 自定义基于 Token 的切词器
 * <p>
 * 封装 Spring AI 的 TokenTextSplitter，将长文档按 Token 数量切分为小段，
 * 便于向量化入库与检索。提供默认切分与自定义参数切分两种方式。
 */
@Component
class MyTokenTextSplitter {

    /**
     * 使用默认参数切分文档
     *
     * @param documents 原始文档列表
     * @return 切分后的文档片段列表
     */
    public List<Document> splitDocuments(List<Document> documents) {
        // 使用 Spring AI 默认参数的 Token 切分器
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.apply(documents);
    }

    /**
     * 使用自定义参数切分文档
     * <p>
     * 参数含义：默认块大小 200 token、最小块大小 100 token、
     * 重叠大小 10 token、最大块大小 5000 token、保留标题标记。
     *
     * @param documents 原始文档列表
     * @return 切分后的文档片段列表
     */
    public List<Document> splitCustomized(List<Document> documents) {
        // 使用自定义参数的 Token 切分器
        TokenTextSplitter splitter = new TokenTextSplitter(200, 100, 10, 5000, true);
        return splitter.apply(documents);
    }
}
