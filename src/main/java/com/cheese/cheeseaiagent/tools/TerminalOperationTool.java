package com.cheese.cheeseaiagent.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 终端操作工具（供 AI 智能体调用）
 * <p>
 * 允许 AI 在对话中执行终端命令（Windows 下使用 cmd.exe），
 * 并返回命令执行的标准输出结果。
 */
public class TerminalOperationTool {

    /**
     * 在终端中执行命令
     *
     * @param command 要执行的终端命令
     * @return 命令的标准输出；执行失败时包含错误信息
     */
    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        // 用于收集命令输出
        StringBuilder output = new StringBuilder();
        try {
            // 通过 cmd.exe /c 执行命令（备选方案：直接 Runtime.exec）
            ProcessBuilder builder = new ProcessBuilder("cmd.exe", "/c", command);
//            Process process = Runtime.getRuntime().exec(command);
            // 启动进程
            Process process = builder.start();
            // 逐行读取标准输出
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            // 等待命令执行完成并获取退出码
            int exitCode = process.waitFor();
            // 退出码非 0 表示执行失败
            if (exitCode != 0) {
                output.append("Command execution failed with exit code: ").append(exitCode);
            }
        } catch (IOException | InterruptedException e) {
            // 执行异常时返回错误信息
            output.append("Error executing command: ").append(e.getMessage());
        }
        return output.toString();
    }
}
