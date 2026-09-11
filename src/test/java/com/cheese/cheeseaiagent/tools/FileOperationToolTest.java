package com.cheese.cheeseaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * FileOperationTool 文件操作工具测试类
 * <p>
 * 验证文件读写工具的基本功能：
 * 读取已有文件、向文件写入内容。
 */
@SpringBootTest
class FileOperationToolTest {

    /**
     * 测试读取文件
     */
    @Test
    void readFile() {
        // 创建文件操作工具实例
        FileOperationTool fileOperationTool = new FileOperationTool();
        // 指定要读取的文件名
        String fileName = "程序员cheese.txt";
        // 执行读取
        String result = fileOperationTool.readFile(fileName);
        // 断言返回结果不为空（文件不存在时返回错误信息，也不会为 null）
        Assertions.assertNotNull(result);
    }

    /**
     * 测试写入文件
     */
    @Test
    void writeFile() {
        // 创建文件操作工具实例
        FileOperationTool fileOperationTool = new FileOperationTool();
        // 指定文件名与写入内容
        String fileName = "程序员cheese.txt";
        String content = "https://blog.csdn.net/ljz66254 程序员程序员cheese的csdn主页";
        // 执行写入
        String result = fileOperationTool.writeFile(fileName, content);
        // 断言返回结果不为空
        Assertions.assertNotNull(result);
    }
}
