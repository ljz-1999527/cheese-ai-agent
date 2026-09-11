package com.cheese.cheeseaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * LoveAppDocumentLoader 文档加载器测试类
 * <p>
 * 验证 Markdown 知识库文档的加载流程能否正常执行。
 */
@SpringBootTest
class InterViewAppDocumentLoaderTest {

    /** 注入文档加载器 Bean */
    @Resource
    private InterViewAppDocumentLoader interViewAppDocumentLoader;

    /**
     * 测试加载 Markdown 文档
     */
    @Test
    void loadMarkdowns() {
        // 执行加载：将知识库 Markdown 文档加载为可检索的文档集合
        interViewAppDocumentLoader.loadMarkdowns();
    }
}
