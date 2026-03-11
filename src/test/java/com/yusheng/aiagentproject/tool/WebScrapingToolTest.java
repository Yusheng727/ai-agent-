package com.yusheng.aiagentproject.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WebScrapingToolTest {

    @Test
    void testScrapeWebPage() {
        WebScrapingTool tool = new WebScrapingTool();
        List<String> candidates = List.of(
                "https://www.baidu.com",
                "https://www.example.com",
                "https://www.codefather.cn"
        );

        String result = tryScrape(tool, candidates);
        assertTrue(result.contains("<html") || result.contains("<!doctype html"), "应返回网页 HTML，实际返回：" + result);
    }

    @Test
    void testRejectInvalidProtocol() {
        WebScrapingTool tool = new WebScrapingTool();
        String result = tool.scrapeWebPage("file:///etc/passwd");

        assertTrue(result.contains("仅支持 http/https"), "应拒绝非 http/https 协议，实际返回：" + result);
    }

    /**
     * 真实联网环境可能出现超时，失败时自动切换候选站点。
     */
    private String tryScrape(WebScrapingTool tool, List<String> urls) {
        String lastResult = "";
        for (String url : urls) {
            String result = tool.scrapeWebPage(url);
            if (result.contains("<html") || result.contains("<!doctype html")) {
                return result;
            }
            lastResult = "URL=" + url + " -> " + result;
        }
        return lastResult;
    }
}