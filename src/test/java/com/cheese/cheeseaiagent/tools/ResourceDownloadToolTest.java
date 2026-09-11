package com.cheese.cheeseaiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ResourceDownloadTool 资源下载工具测试类
 * <p>
 * 验证从指定 URL 下载资源的流程能否正常执行。
 */
@SpringBootTest
public class ResourceDownloadToolTest {

    /**
     * 测试下载资源
     */
    @Test
    public void testDownloadResource() {
        // 创建资源下载工具实例
        ResourceDownloadTool tool = new ResourceDownloadTool();
        // 指定下载地址与保存文件名
        String url = "https://profile-avatar.csdnimg.cn/9bdc55993211497b924064e42884fda3_ljz66254.jpg";
        String fileName = "avatar.jpg";
        // 执行下载
        String result = tool.downloadResource(url, fileName);
        // 断言返回结果不为空
        assertNotNull(result);
    }
}
