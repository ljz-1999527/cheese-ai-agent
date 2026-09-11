package com.cheese.cheeseaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止工具（供 AI 智能体调用）
 * <p>
 * 作用是让自主规划智能体（如 YuManus）能够合理地中断任务：
 * 当任务已完成、或 AI 无法继续推进时，调用该工具结束工作，
 * 避免陷入无限循环或长时间空转。
 */
public class TerminateTool {

    /**
     * 结束当前任务
     *
     * @return 固定返回"任务结束"提示
     */
    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            "When you have finished all the tasks, call this tool to end the work.
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
