package com.cheese.cheeseaiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

/**
 * ReAct (Reasoning and Acting) 模式的代理抽象类
 * 实现了思考-行动的循环模式
 * <p>
 * ReAct 模式将大模型的推理（Reasoning）与行动（Acting）交替进行：
 * 每轮先通过 think() 让大模型观察当前状态并决定是否需要行动，
 * 需要行动时再通过 act() 执行具体动作，如此循环直至任务完成。
 * 子类需要实现 think() 与 act() 两个抽象方法。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public abstract class ReActAgent extends BaseAgent {

    /**
     * 处理当前状态并决定下一步行动（抽象方法，由子类实现）
     *
     * @return 是否需要执行行动，true 表示需要执行，false 表示不需要执行
     */
    public abstract boolean think();

    /**
     * 执行决定的行动（抽象方法，由子类实现）
     *
     * @return 行动执行结果
     */
    public abstract String act();

    /**
     * 执行单个步骤：思考和行动
     * <p>
     * 这是对 BaseAgent.step() 的具体实现，遵循 ReAct 循环：
     * 先思考（think）决定是否行动，若需要行动则调用 act() 执行，否则直接返回。
     *
     * @return 步骤执行结果
     */
    @Override
    public String step() {
        try {
            // 先思考：调用子类实现的 think() 判断是否需要行动
            boolean shouldAct = think();
            if (!shouldAct) {
                // 大模型认为无需行动，直接结束本轮步骤
                return "思考完成 - 无需行动";
            }
            // 再行动：调用子类实现的 act() 执行具体动作
            return act();
        } catch (Exception e) {
            // 记录异常日志，并返回友好的失败信息（不中断整个代理循环）
            e.printStackTrace();
            return "步骤执行失败：" + e.getMessage();
        }
    }

}
