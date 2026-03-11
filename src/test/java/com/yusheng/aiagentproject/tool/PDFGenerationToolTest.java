package com.yusheng.aiagentproject.tool;

import com.yusheng.aiagentproject.constant.FileConstant;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFGenerationToolTest {

    @Test
    void generatePDF() {
        PDFGenerationTool tool = new PDFGenerationTool();
        String fileName = "编程导航原创项目.pdf";
        String content = "编程导航原创项目 https://www.codefather.cn";

        String result = tool.generatePDF(fileName, content);
        assertTrue(result.contains("PDF 生成成功"), "PDF 生成应成功，实际返回：" + result);

        Path expectedPath = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf", fileName)
                .toAbsolutePath()
                .normalize();
        assertTrue(Files.exists(expectedPath), "生成后文件应存在：" + expectedPath);
    }
}