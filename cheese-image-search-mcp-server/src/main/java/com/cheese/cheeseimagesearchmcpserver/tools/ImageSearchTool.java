package com.cheese.cheeseimagesearchmcpserver.tools;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图片搜索工具类（基于 Pexels 图片库）
 * <p>
 * 通过 Pexels API 按关键词搜索网络图片，返回图片的中等尺寸 URL 列表。
 * 该工具作为 MCP 服务暴露给主应用调用，供 AI 智能体在对话中搜索图片。
 */
@Service
public class ImageSearchTool {

    // 替换为你的 Pexels API 密钥（需从官网申请）
    private static final String API_KEY = "l8t3Ut2dKDUwKAhtxGgKJudARDJeQglYA3U6UyyuSlpeUkQnDKjngFKj";

    // Pexels 常规搜索接口（请以文档为准）
    private static final String API_URL = "https://api.pexels.com/v1/search";

    /**
     * 搜索图片（AI 工具入口）
     * <p>
     * 该方法带有 @Tool 注解，会被注册为 MCP 工具供 AI 调用。
     *
     * @param query 搜索关键词
     * @return 图片 URL 列表（逗号分隔）；搜索失败时返回错误信息
     */
    @Tool(description = "search image from web")
    public String searchImage(@ToolParam(description = "Search query keyword") String query) {
        try {
            // 调用内部方法搜索图片，并将结果列表拼接为逗号分隔字符串
            return String.join(",", searchMediumImages(query));
        } catch (Exception e) {
            // 搜索失败时返回错误信息
            return "Error search image: " + e.getMessage();
        }
    }

    /**
     * 搜索中等尺寸的图片列表
     * <p>
     * 向 Pexels API 发送请求并解析响应，提取每张图片的中等尺寸 URL。
     *
     * @param query 搜索关键词
     * @return 图片中等尺寸 URL 列表
     */
    public List<String> searchMediumImages(String query) {
        // 设置请求头（包含API密钥）
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", API_KEY);

        // 设置请求参数（仅包含query，可根据文档补充page、per_page等参数）
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);

        // 发送 GET 请求
        String response = HttpUtil.createGet(API_URL)
                .addHeaders(headers)
                .form(params)
                .execute()
                .body();

        // 解析响应JSON（假设响应结构包含"photos"数组，每个元素包含"medium"字段）
        return JSONUtil.parseObj(response)
                .getJSONArray("photos")            // 获取图片数组
                .stream()                          // 转为流
                .map(photoObj -> (JSONObject) photoObj)   // 转 JSONObject
                .map(photoObj -> photoObj.getJSONObject("src"))  // 获取 src 对象
                .map(photo -> photo.getStr("medium"))          // 提取 medium 尺寸 URL
                .filter(StrUtil::isNotBlank)       // 过滤空值
                .collect(Collectors.toList());     // 收集为列表
    }
}
