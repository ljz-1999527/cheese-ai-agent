package com.cheese.cheeseaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * WebScrapingTool 网页抓取工具测试类
 * <p>
 * 验证网页抓取功能能否获取到指定网页的内容。
 */
class WebScrapingToolTest {

    /**
     * 测试抓取网页
     */
    @Test
    void scrapeWebPage() {
        // 创建网页抓取工具实例
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        // 指定要抓取的网页地址
        String url = "https://blog.csdn.net/ljz66254";
        // 执行抓取
        String result = webScrapingTool.scrapeWebPage(url);
        // 断言返回结果不为空
        Assertions.assertNotNull(result);
    }
}
