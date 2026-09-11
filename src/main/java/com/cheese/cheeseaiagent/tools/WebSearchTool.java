package com.cheese.cheeseaiagent.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具（供 AI 智能体调用）
 * <p>
 * 通过 SearchAPI 服务（searchapi.io）调用百度搜索引擎，
 * 将搜索到的前 5 条结果返回给 AI，帮助 AI 获取实时网络信息。
 */
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    /** SearchAPI 的 API Key（由构造方法传入） */
    private final String apiKey;

    /**
     * 构造方法：传入搜索 API Key
     *
     * @param apiKey SearchAPI 的 API Key
     */
    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * 使用百度引擎搜索信息
     *
     * @param query 搜索关键词
     * @return 前 5 条搜索结果的 JSON 字符串；搜索失败时返回错误信息
     */
    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        // 构建请求参数：关键词、API Key、搜索引擎
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            // 发送 HTTP GET 请求
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            // 取出返回结果的前 5 条
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分（自然搜索结果列表）
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            // 只取前 5 条结果
            List<Object> objects = organicResults.subList(0, 5);
            // 拼接搜索结果为字符串：将每条结果转成 JSON 字符串并用逗号连接
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));
            return result;
        } catch (Exception e) {
            // 搜索失败时返回错误信息
            return "Error searching Baidu: " + e.getMessage();
        }
    }
}
