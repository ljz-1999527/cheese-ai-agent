package com.cheese.cheeseaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * WebSearchTool 网页搜索工具测试类
 * <p>
 * 验证基于 SearchAPI 的百度搜索功能，
 * 需在配置文件中提供 search-api.api-key。
 */
@SpringBootTest
class WebSearchToolTest {

    /** 注入搜索 API Key */
    @Value("${search-api.api-key}")
    private String searchApiKey;

    /**
     * 测试网页搜索
     */
    @Test
    void searchWeb() {
        // 使用配置的 API Key 创建搜索工具实例
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        // 指定搜索关键词
        String query = "程序员cheese 的csdn主页 blog.csdn.net/ljz66254";
        // 执行搜索
        String result = webSearchTool.searchWeb(query);
        // 断言返回结果不为空
        Assertions.assertNotNull(result);
    }
}
