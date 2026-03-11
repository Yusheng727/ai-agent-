package com.yusheng.aiagentproject.tool;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;
import java.net.URI;

public class WebScrapingTool {

    private static final int TIMEOUT_MILLIS = 30_000;
    private static final int MAX_HTML_LENGTH = 20_000;

    @Tool(description = "Scrape the content of a web page")
    public String scrapeWebPage(@ToolParam(description = "URL of the web page to scrape") String url) {
        try {
            validateHttpUrl(url);
            Document doc = Jsoup.connect(url)
                    .timeout(TIMEOUT_MILLIS)
                    .userAgent("Mozilla/5.0 (compatible; AiAgentProject/1.0)")
                    .get();
            return trimHtml(doc.html());
        } catch (IOException e) {
            return "抓取网页失败：" + e.getMessage();
        } catch (Exception e) {
            return "抓取网页失败：" + e.getMessage();
        }
    }

    /**
     * 仅允许 http/https，避免误抓取本地协议。
     */
    private void validateHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("网页地址不能为空");
        }
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("仅支持 http/https 地址");
        }
    }

    /**
     * 控制返回体积，避免工具调用时上下文过大。
     */
    private String trimHtml(String html) {
        if (html == null) {
            return "";
        }
        if (html.length() <= MAX_HTML_LENGTH) {
            return html;
        }
        return html.substring(0, MAX_HTML_LENGTH) + "\n<!-- html truncated -->";
    }
}