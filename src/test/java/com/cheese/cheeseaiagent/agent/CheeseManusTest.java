package com.cheese.cheeseaiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * YuManus 自主智能体测试类
 * <p>
 * 验证 YuManus 智能体能否完整执行一个涉及
 * 联网搜索、图片查找、PDF 生成的复合任务。
 */
@SpringBootTest
class CheeseManusTest {

    /** 注入 YuManus 智能体 Bean */
    @Resource
    private CheeseManus cheeseManus;

    /**
     * 运行一个完整的复合任务测试：
     * 让智能体根据用户诉求制定约会计划并以 PDF 输出。
     */
    @Test
    public void run() {
        // 构造用户提示词：包含位置限定、任务目标与输出格式要求
        String userPrompt = """
                我的朋友住在深圳光明区，请帮我找到 5 公里内合适的吃饭地点，
                并结合一些网络图片，制定一份详细的旅游计划，
                并以 PDF 格式输出""";
        // 调用智能体执行任务
        String answer = cheeseManus.run(userPrompt);
        // 断言返回结果不为空
        Assertions.assertNotNull(answer);
    }
}
