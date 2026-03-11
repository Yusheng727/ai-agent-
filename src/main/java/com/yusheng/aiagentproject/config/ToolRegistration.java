package com.yusheng.aiagentproject.config;

import com.yusheng.aiagentproject.tool.FileOperationTool;
import com.yusheng.aiagentproject.tool.PDFGenerationTool;
import com.yusheng.aiagentproject.tool.ResourceDownloadTool;
import com.yusheng.aiagentproject.tool.TerminalOperationTool;
import com.yusheng.aiagentproject.tool.WebScrapingTool;
import com.yusheng.aiagentproject.tool.WebSearchTool;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbacks;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {

    @Value("${searchapi.api-key:}")
    private String searchApiKey;

    /**
     * 注册统一的工具回调数组，供 LoveApp#doChatWithTools 注入使用。
     */
    @Bean
    public ToolCallback[] allTools() {
        FileOperationTool fileOperationTool = new FileOperationTool();
        WebSearchTool webSearchTool = new WebSearchTool(searchApiKey);
        WebScrapingTool webScrapingTool = new WebScrapingTool();
        ResourceDownloadTool resourceDownloadTool = new ResourceDownloadTool();
        TerminalOperationTool terminalOperationTool = new TerminalOperationTool();
        PDFGenerationTool pdfGenerationTool = new PDFGenerationTool();
        return ToolCallbacks.from(
                fileOperationTool,
                webSearchTool,
                webScrapingTool,
                resourceDownloadTool,
                terminalOperationTool,
                pdfGenerationTool
        );
    }
}