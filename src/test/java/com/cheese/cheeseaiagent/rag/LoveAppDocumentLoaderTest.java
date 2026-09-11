package com.cheese.cheeseaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LoveAppDocumentLoader 文档加载器测试类
 * <p>
 * 验证 Markdown 知识库文档的加载流程能否正常执行。
 */
@SpringBootTest
class LoveAppDocumentLoaderTest {

    /** 注入文档加载器 Bean */
    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    /**
     * 测试加载 Markdown 文档
     */
    @Test
    void loadMarkdowns() {
        // 执行加载：将知识库 Markdown 文档加载为可检索的文档集合
        loveAppDocumentLoader.loadMarkdowns();
    }
}
