package com.yusheng.aiagentproject.tool;

import com.yusheng.aiagentproject.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileOperationTool {
    /**
     * 文件操作根目录：所有读写都限制在该目录下，防止目录穿越。
     */
    private static final Path FILE_DIR = Paths.get(FileConstant.FILE_SAVE_DIR, "file")
            .toAbsolutePath()
            .normalize();

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        try {
            Path filePath = resolveSafePath(fileName);
            if (!Files.exists(filePath)) {
                return "读取文件失败：文件不存在 - " + filePath;
            }
            return Files.readString(filePath, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "读取文件失败：" + e.getMessage();
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(
            @ToolParam(description = "Name of the file to write") String fileName,
            @ToolParam(description = "Content to write to the file") String content) {
        try {
            Files.createDirectories(FILE_DIR);
            Path filePath = resolveSafePath(fileName);
            Files.writeString(filePath, content == null ? "" : content, StandardCharsets.UTF_8);
            return "文件写入成功：" + filePath;
        } catch (Exception e) {
            return "写入文件失败：" + e.getMessage();
        }
    }

    /**
     * 将用户输入的文件名解析为安全路径，并限制在 FILE_DIR 内。
     */
    private Path resolveSafePath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        Path targetPath = FILE_DIR.resolve(fileName).normalize();
        if (!targetPath.startsWith(FILE_DIR)) {
            throw new IllegalArgumentException("非法文件路径，不允许访问目录外文件");
        }
        return targetPath;
    }
}