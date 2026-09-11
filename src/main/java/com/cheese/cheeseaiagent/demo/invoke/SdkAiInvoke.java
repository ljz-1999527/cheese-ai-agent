package com.cheese.cheeseaiagent.demo.invoke;// 建议dashscope SDK的版本 >= 2.12.0
import java.util.Arrays;
import java.lang.System;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.JsonUtils;

/**
 * 阿里云灵积 AI SDK 调用（演示类）
 * <p>
 * 演示使用阿里云官方 DashScope SDK（com.alibaba.dashscope）直接调用大模型，
 * 是最底层的官方调用方式之一：
 * 1. 通过 Generation 类发起生成请求
 * 2. 使用 GenerationParam.Builder 构建参数（模型、消息、返回格式）
 * 3. 返回 GenerationResult 结构化结果
 * <p>
 * 运行方式：直接执行 main 方法。
 */
public class SdkAiInvoke {

    /**
     * 使用官方 SDK 发起一次对话调用
     *
     * @return 大模型生成结果对象 GenerationResult
     * @throws ApiException            阿里云接口调用异常
     * @throws NoApiKeyException       未配置 API Key 异常
     * @throws InputRequiredException  缺少必要输入参数异常
     */
    public static GenerationResult callWithMessage() throws ApiException, NoApiKeyException, InputRequiredException {
        // 创建生成器实例
        Generation gen = new Generation();
        // 构建系统消息：定义 AI 的角色
        Message systemMsg = Message.builder()
                .role(Role.SYSTEM.getValue())
                .content("You are a helpful assistant.")
                .build();
        // 构建用户消息：用户提问内容
        Message userMsg = Message.builder()
                .role(Role.USER.getValue())
                .content("你好，我是程序员鱼皮，正在带大家开发编程导航 codefather.cn 最新的原创项目 - AI 超级智能体")
                .build();
        // 构建生成参数
        GenerationParam param = GenerationParam.builder()
                // 若没有配置环境变量，请用百炼API Key将下行替换为：.apiKey("sk-xxx")
                .apiKey(TestApiKey.API_KEY)
                // 此处以qwen-plus为例，可按需更换模型名称。模型列表：https://help.aliyun.com/zh/model-studio/getting-started/models
                .model("qwen-plus")
                // 传入消息列表
                .messages(Arrays.asList(systemMsg, userMsg))
                // 指定返回格式为消息结构
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();
        // 发起调用并返回结果
        return gen.call(param);
    }

    /**
     * 程序入口：调用大模型并输出结果
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        try {
            // 调用大模型并转换为 JSON 字符串输出
            GenerationResult result = callWithMessage();
            System.out.println(JsonUtils.toJson(result));
        } catch (ApiException | NoApiKeyException | InputRequiredException e) {
            // 使用日志框架记录异常信息
            System.err.println("An error occurred while calling the generation service: " + e.getMessage());
        }
        // 显式退出程序
        System.exit(0);
    }
}
