package com.cheese.cheeseaiagent.demo.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.rag.Query;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MultiQueryExpanderDemo 演示测试类
 * <p>
 * 验证多查询扩展组件的功能：
 * 输入一个查询词后，能否扩展出多个相关查询。
 */
@SpringBootTest
class MultiQueryExpanderDemoTest {

    /** 注入多查询扩展演示组件 */
    @Resource
    private MultiQueryExpanderDemo multiQueryExpanderDemo;

    /**
     * 测试查询扩展功能
     */
    @Test
    void expand() {
        // 传入一个口语化的查询语句
        List<Query> queries = multiQueryExpanderDemo.expand("啥是程序员cheese啊啊啊啊啊啊？！请回答我哈哈哈哈");
        // 断言扩展结果不为空
        Assertions.assertNotNull(queries);
    }
}
