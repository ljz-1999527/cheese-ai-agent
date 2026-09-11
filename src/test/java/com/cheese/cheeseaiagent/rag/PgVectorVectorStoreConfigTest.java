package com.cheese.cheeseaiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

/**
 * PgVector 向量存储测试类
 * <p>
 * 验证 PGVector 向量数据库的文档入库与相似度检索能力：
 * 先添加测试文档，再通过查询词进行语义相似度搜索。
 */
@SpringBootTest
class PgVectorVectorStoreConfigTest {

    /** 注入 PGVector 向量存储 Bean */
    @Resource
    private VectorStore pgVectorVectorStore;

    /**
     * 测试向量存储的写入与查询
     */
    @Test
    void pgVectorVectorStore() {
        // 构造三个测试文档（内容 + 元数据）
        List<Document> documents = List.of(
                new Document("面试八股有什么用？学java啊，做项目啊", Map.of("meta1", "meta1")),
                new Document("程序员cheese的原创项目教程"),
                new Document("cheese这小伙子比较帅气", Map.of("meta2", "meta2")));
        // 添加文档：将文档向量化后写入向量库
        pgVectorVectorStore.add(documents);
        // 相似度查询：按语义检索最相关的前 3 条文档
        List<Document> results = pgVectorVectorStore.similaritySearch(SearchRequest.builder().query("怎么学java啊").topK(3).build());
        // 断言查询结果不为空
        Assertions.assertNotNull(results);
    }
}
