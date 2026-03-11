package com.yusheng.aiagentproject.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TerminalOperationToolTest {

    @Test
    void executeTerminalCommand() {
        TerminalOperationTool tool = new TerminalOperationTool();
        String result = tool.executeTerminalCommand("java -version");

        assertTrue(
                result.toLowerCase().contains("version") || result.contains("命令执行完成"),
                "白名单命令应成功执行，实际返回：" + result
        );
    }

    @Test
    void rejectNonWhitelistedCommand() {
        TerminalOperationTool tool = new TerminalOperationTool();
        String result = tool.executeTerminalCommand("del /f /q important.txt");

        assertTrue(result.contains("命令执行被拒绝"), "非白名单命令应被拒绝，实际返回：" + result);
    }
}