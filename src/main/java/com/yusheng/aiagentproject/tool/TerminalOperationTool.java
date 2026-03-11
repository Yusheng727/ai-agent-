package com.yusheng.aiagentproject.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TerminalOperationTool {

    /**
     * 允许执行的命令白名单（最小权限原则）。
     */
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "java",
            "mvn",
            "mvnw",
            "git",
            "whoami",
            "hostname"
    );

    private static final int COMMAND_TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_LENGTH = 8_000;

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        try {
            List<String> tokens = parseCommand(command);
            if (tokens.isEmpty()) {
                return "命令执行失败：命令不能为空";
            }

            String executable = normalizeExecutable(tokens.get(0));
            if (!ALLOWED_COMMANDS.contains(executable)) {
                return "命令执行被拒绝：仅允许执行白名单命令 " + ALLOWED_COMMANDS;
            }

            ProcessBuilder builder = new ProcessBuilder(tokens);
            builder.redirectErrorStream(true);
            Process process = builder.start();

            boolean completed = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                return "命令执行超时：超过 " + COMMAND_TIMEOUT_SECONDS + " 秒";
            }

            String output = readProcessOutput(process);
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                return trimOutput(output) + System.lineSeparator() + "命令执行失败，退出码：" + exitCode;
            }
            return trimOutput(output);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "命令执行被中断：" + e.getMessage();
        } catch (Exception e) {
            return "命令执行失败：" + e.getMessage();
        }
    }

    /**
     * 支持简单的引号分词，避免把带空格参数拆错。
     */
    private List<String> parseCommand(String command) {
        List<String> tokens = new ArrayList<>();
        if (command == null || command.isBlank()) {
            return tokens;
        }

        Pattern pattern = Pattern.compile("\"([^\"]*)\"|(\\S+)");
        Matcher matcher = pattern.matcher(command.trim());
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                tokens.add(matcher.group(1));
            } else {
                tokens.add(matcher.group(2));
            }
        }
        return tokens;
    }

    /**
     * 统一可执行程序名，兼容 Windows 下的 mvnw.cmd / mvnw.bat。
     */
    private String normalizeExecutable(String executable) {
        String value = executable.toLowerCase();
        if (value.endsWith(".cmd") || value.endsWith(".bat") || value.endsWith(".exe")) {
            int index = value.lastIndexOf('.');
            if (index > 0) {
                return value.substring(0, index);
            }
        }
        return value;
    }

    private String readProcessOutput(Process process) throws IOException {
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }
        }
        return output.toString();
    }

    private String trimOutput(String output) {
        if (output == null || output.isBlank()) {
            return "命令执行完成，无输出";
        }
        if (output.length() <= MAX_OUTPUT_LENGTH) {
            return output;
        }
        return output.substring(0, MAX_OUTPUT_LENGTH) + System.lineSeparator() + "...输出已截断";
    }
}