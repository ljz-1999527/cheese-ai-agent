package com.cheese.cheeseimagesearchmcpserver.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * ImageSearchTool 图片搜索工具测试类
 * <p>
 * 验证图片搜索功能能否正常调用 Pexels API 并返回结果。
 */
@SpringBootTest
public class ImageSearchToolTest {

    /** 注入图片搜索工具 Bean */
    @Resource
    private ImageSearchTool imageSearchTool;

    /**
     * 测试搜索图片
     */
    @Test
    void searchImage() {
        // 搜索关键词为 computer
        String result = imageSearchTool.searchImage("computer");
        // 断言返回结果不为空
        Assertions.assertNotNull(result);
    }
}
