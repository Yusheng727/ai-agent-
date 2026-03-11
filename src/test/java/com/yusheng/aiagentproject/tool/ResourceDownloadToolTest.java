package com.yusheng.aiagentproject.tool;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceDownloadToolTest {

    @Test
    void downloadResource() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        List<String> candidates = List.of(
                "https://www.baidu.com/favicon.ico",
                "https://www.google.com/favicon.ico",
                "https://www.codefather.cn/logo.png"
        );

        String result = tryDownload(tool, candidates, "logo.png");
        assertTrue(result.contains("资源下载成功"), "真实下载应成功，实际返回：" + result);
    }

    @Test
    void shouldRejectInvalidProtocol() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String result = tool.downloadResource("file:///etc/passwd", "x.txt");
        assertTrue(result.contains("仅支持 http/https"), "应拒绝非 http/https 协议，实际返回：" + result);
    }

    @Test
    void shouldRejectPathTraversal() {
        ResourceDownloadTool tool = new ResourceDownloadTool();
        String result = tool.downloadResource("https://www.baidu.com/favicon.ico", "../logo.png");
        assertTrue(result.contains("非法文件路径"), "应拒绝目录穿越，实际返回：" + result);
    }

    /**
     * 真实联网环境可能波动，这里按候选地址依次尝试，减少单站点偶发抖动导致的误报。
     */
    private String tryDownload(ResourceDownloadTool tool, List<String> urls, String fileName) {
        String lastResult = "";
        for (String url : urls) {
            String result = tool.downloadResource(url, fileName);
            if (result.contains("资源下载成功")) {
                return result;
            }
            lastResult = "URL=" + url + " -> " + result;
        }
        return lastResult;
    }
}