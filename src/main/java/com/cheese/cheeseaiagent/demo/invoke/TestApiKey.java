package com.cheese.cheeseaiagent.demo.invoke;

/**
 * 测试 API Key 常量（仅用于测试）
 * <p>
 * 各演示类（HttpAiInvoke / SdkAiInvoke / LangChainAiInvoke 等）统一从这里读取 API Key，
 * 方便集中配置。正式使用前请将 API_KEY 修改为你的真实密钥。
 */
public interface TestApiKey {

    // 修改为你的 API Key
    String API_KEY = "修改为你的 API Key";
}
