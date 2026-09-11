package com.cheese.cheeseaiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 面试大师应用文档加载器
 * <p>
 * 负责从 classpath 的 document 目录加载全部 Markdown 知识文档，
 * 并转换为 Spring AI 的 Document 对象（含文件名、状态等元信息），
 * 供向量库初始化时写入知识库。
 */
@Component
@Slf4j
public class InterViewAppDocumentLoader {

    /** 资源模式解析器：用于按通配符匹配 classpath 下的资源文件 */
    private final ResourcePatternResolver resourcePatternResolver;

    /**
     * 构造方法：注入资源模式解析器
     *
     * @param resourcePatternResolver Spring 提供的资源模式解析器
     */
    public InterViewAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载多篇 Markdown 文档
     *
     * @return 全部文档的 Document 对象列表（含元信息）
     */
    public List<Document> loadMarkdowns() {
        // 存放所有加载成功的文档
        List<Document> allDocuments = new ArrayList<>();
        try {
            // 匹配 classpath:document/interview/ 下的所有 .md 文件
            Resource[] resources = resourcePatternResolver.getResources("classpath:document/interview/*.md");
            // 逐个读取 Markdown 文档
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                // 提取文档倒数第 3 和第 2 个字作为标签（如 "xxx基础篇.md" 提取 "基础"）
                String status = filename.substring(filename.length() - 6, filename.length() - 4);
                // 配置 Markdown 读取器：按水平分隔线分块、过滤代码块与引用块，并附加元信息
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("status", status)
                        .build();
                // 读取当前文档并加入结果列表
                MarkdownDocumentReader markdownDocumentReader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
            }
        } catch (IOException e) {
           log.error("Markdown 文档加载失败", e);
        }
        return allDocuments;
    }
}
