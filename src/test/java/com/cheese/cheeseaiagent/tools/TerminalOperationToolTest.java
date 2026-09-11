package com.cheese.cheeseaiagent.tools;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TerminalOperationTool 终端操作工具测试类
 * <p>
 * 验证终端命令执行功能，测试命令为 Windows 的 dir。
 */
class TerminalOperationToolTest {

    /**
     * 测试执行终端命令
     */
    @Test
    void executeTerminalCommand() {
        // 创建终端操作工具实例
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        // 执行 dir 命令（列出当前目录内容）
        String command = "dir";
        String result = terminalOperationTool.executeTerminalCommand(command);
        // 断言返回结果不为空
        Assertions.assertNotNull(result);
    }
}
