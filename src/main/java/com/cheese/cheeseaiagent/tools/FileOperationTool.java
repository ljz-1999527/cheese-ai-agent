package com.cheese.cheeseaiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.cheese.cheeseaiagent.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 文件操作工具类（提供文件读写能力，供 AI 智能体调用）
 * <p>
 * 将文件读写能力封装为 Spring AI 的 @Tool，让大模型在对话中
 * 能够自主执行"读文件 / 写文件"操作。文件统一保存在
 * 项目运行目录下的 tmp/file 文件夹中。
 */
public class FileOperationTool {

    /** 文件保存目录：项目运行目录/tmp/file */
    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";

    /**
     * 读取指定文件的内容
     *
     * @param fileName 要读取的文件名（相对 FILE_DIR 目录）
     * @return 文件内容；读取失败时返回错误信息
     */
    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of a file to read") String fileName) {
        // 拼接文件完整路径
        String filePath = FILE_DIR + "/" + fileName;
        try {
            // 使用 Hutool 读取 UTF-8 文本内容
            return FileUtil.readUtf8String(filePath);
        } catch (Exception e) {
            // 读取失败时返回错误信息，便于 AI 理解并处理
            return "Error reading file: " + e.getMessage();
        }
    }

    /**
     * 将内容写入指定文件（不存在则创建，已存在则覆盖）
     *
     * @param fileName 要写入的文件名（相对 FILE_DIR 目录）
     * @param content  要写入的文件内容
     * @return 写入成功提示或错误信息
     */
    @Tool(description = "Write content to a file. Supports all text-based formats (.txt, .md, .html, .csv, .json, .xml, .yaml, .sql, etc.); the file extension in fileName determines the format. Use this tool for general text/file output. For PDF output, use the generatePDF tool instead.")
    public String writeFile(@ToolParam(description = "Name of the file to write") String fileName,
                            @ToolParam(description = "Content to write to the file") String content
    ) {
        // 拼接文件完整路径
        String filePath = FILE_DIR + "/" + fileName;

        try {
            // 创建目录（目录不存在时自动创建）
            FileUtil.mkdir(FILE_DIR);
            // 将内容以 UTF-8 写入文件
            FileUtil.writeUtf8String(content, filePath);
            return "File written successfully to: " + filePath;
        } catch (Exception e) {
            // 写入失败时返回错误信息
            return "Error writing to file: " + e.getMessage();
        }
    }
}
