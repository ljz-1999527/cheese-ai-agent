package com.cheese.cheeseaiagent.agent.model;

/**
 * 代理执行状态的枚举类
 * <p>
 * 描述代理（Agent）从创建到结束的完整生命周期状态，
 * 由 BaseAgent 在 run/runStream 执行流程中维护与切换。
 */
public enum AgentState {

    /**
     * 空闲状态：代理创建后的初始状态，只有空闲状态才能开始运行
     */
    IDLE,

    /**
     * 运行中状态：代理正在执行步骤循环
     */
    RUNNING,

    /**
     * 已完成状态：任务执行完成（包括达到最大步数或调用终止工具）
     */
    FINISHED,

    /**
     * 错误状态：执行过程中发生异常
     */
    ERROR
}
