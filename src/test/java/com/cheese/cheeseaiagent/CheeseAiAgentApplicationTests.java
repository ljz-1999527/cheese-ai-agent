package com.cheese.cheeseaiagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 应用启动上下文测试类
 * <p>
 * 使用 @SpringBootTest 启动完整的 Spring 容器，
 * 验证项目配置、Bean 定义是否正确，能否正常启动。
 */
@SpringBootTest
class CheeseAiAgentApplicationTests {

    /**
     * 空测试方法：仅用于触发 Spring 容器加载
     * 若容器能成功启动且无异常，则测试通过。
     */
    @Test
    void contextLoads() {
    }

}
