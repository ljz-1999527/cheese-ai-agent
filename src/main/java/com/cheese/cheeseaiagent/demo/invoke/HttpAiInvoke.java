package com.cheese.cheeseaiagent.demo.invoke;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

/**
 * HTTP 方式调用 AI（演示类）
 * <p>
 * 不使用任何 AI SDK，而是直接通过 HTTP 请求调用阿里云百炼（DashScope）的
 * 文本生成接口。用于演示大模型接口的底层调用原理：
 * 1. 构建包含 model / input.messages / parameters 的 JSON 请求体
 * 2. 通过 HTTP POST 发送到 DashScope 的 generation 接口
 * 3. 使用 Bearer Token（API Key）完成鉴权
 * <p>
 * 运行方式：直接执行 main 方法。
 */
public class HttpAiInvoke {

    /**
     * 程序入口：演示 HTTP 方式调用大模型
     *
     * @param args 命令行参数（未使用）
     */
    public static void main(String[] args) {
        // API密钥（从测试常量类中读取）
        String apiKey = TestApiKey.API_KEY;

        // 构建请求URL：DashScope 文本生成接口地址
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";

        // 构建请求JSON数据
        JSONObject inputJson = new JSONObject();
        JSONObject messagesJson = new JSONObject();

        // 添加系统消息：定义 AI 的角色
        JSONObject systemMessage = new JSONObject();
        systemMessage.set("role", "system");
        systemMessage.set("content", "You are a helpful assistant.");

        // 添加用户消息：用户提问内容
        JSONObject userMessage = new JSONObject();
        userMessage.set("role", "user");
        userMessage.set("content", "你是谁？");

        // 组装messages数组：将系统消息与用户消息放入数组
        messagesJson.set("messages", JSONUtil.createArray().set(systemMessage).set(userMessage));

        // 构建参数：指定返回格式为 message（结构化消息）
        JSONObject parametersJson = new JSONObject();
        parametersJson.set("result_format", "message");

        // 构建完整请求体：指定模型、输入消息与参数
        JSONObject requestJson = new JSONObject();
        requestJson.set("model", "qwen-plus");
        requestJson.set("input", messagesJson);
        requestJson.set("parameters", parametersJson);

        // 发送请求：POST 到接口，携带鉴权头与 JSON 请求体
        String result = HttpRequest.post(url)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(requestJson.toString())
                .execute()
                .body();

        // 输出结果：打印大模型返回的 JSON 字符串
        System.out.println(result);
    }
}
