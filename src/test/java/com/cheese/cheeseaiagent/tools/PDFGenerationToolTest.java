package com.cheese.cheeseaiagent.tools;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PDFGenerationTool PDF 生成工具测试类
 * <p>
 * 验证 PDF 生成工具能否根据指定文件名和内容生成 PDF 文件。
 */
class PDFGenerationToolTest {

    /**
     * 测试生成 PDF
     */
    @Test
    void generatePDF() {
        // 创建 PDF 生成工具实例
        PDFGenerationTool tool = new PDFGenerationTool();
        // 指定文件名与内容
        String fileName = "程序员cheese的首页.pdf";
        String content = "程序员cheese的首页 https://blog.csdn.net/ljz66254";
        // 执行 PDF 生成
        String result = tool.generatePDF(fileName, content, List.of());
        // 断言返回结果不为空
        assertNotNull(result);
    }
}
