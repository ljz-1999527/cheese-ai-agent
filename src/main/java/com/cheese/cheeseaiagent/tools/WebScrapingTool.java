package com.cheese.cheeseaiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 网页抓取工具（供 AI 智能体调用）
 * <p>
 * 使用 Jsoup 抓取指定 URL 网页的完整 HTML 内容，
 * 让 AI 能够获取网页上的具体信息（如文章正文、页面数据等）。
 */
public class WebScrapingTool {

    /**
     * 抓取网页内容
     *
     * @param url 要抓取的网页地址
     * @return 网页的完整 HTML 内容；抓取失败时返回错误信息
     */
    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            // 连接网页并获取 HTML 文档对象
            Document document = Jsoup.connect(url).get();
            // 返回完整 HTML
            return document.html();
        } catch (Exception e) {
            // 抓取失败时返回错误信息
            return "Error scraping web page: " + e.getMessage();
        }
    }
}
