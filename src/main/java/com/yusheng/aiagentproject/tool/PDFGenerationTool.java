package com.yusheng.aiagentproject.tool;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.yusheng.aiagentproject.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PDFGenerationTool {

    /**
     * PDF 生成根目录。
     */
    private static final Path PDF_DIR = Paths.get(FileConstant.FILE_SAVE_DIR, "pdf")
            .toAbsolutePath()
            .normalize();

    /**
     * 常见中文字体候选路径：按顺序尝试，找不到则回退到英文默认字体。
     */
    private static final List<String> CJK_FONT_CANDIDATES = List.of(
            "C:/Windows/Fonts/msyh.ttc,0",
            "C:/Windows/Fonts/simsun.ttc,0",
            "C:/Windows/Fonts/simhei.ttf",
            "/System/Library/Fonts/PingFang.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc"
    );

    @Tool(description = "Generate a PDF file with given content")
    public String generatePDF(
            @ToolParam(description = "Name of the file to save the generated PDF") String fileName,
            @ToolParam(description = "Content to be included in the PDF") String content) {
        try {
            FileUtil.mkdir(PDF_DIR.toString());
            Path filePath = resolveSafePdfPath(fileName);

            try (PdfWriter writer = new PdfWriter(filePath.toString());
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {

                document.setFont(createPreferredFont());
                document.add(new Paragraph(content == null ? "" : content));
            }
            return "PDF 生成成功：" + filePath;
        } catch (Exception e) {
            return "PDF 生成失败：" + e.getMessage();
        }
    }

    /**
     * 优先使用可显示中文的字体，避免 STSongStd-Light 在部分环境不可用导致报错。
     */
    private PdfFont createPreferredFont() throws Exception {
        for (String candidate : CJK_FONT_CANDIDATES) {
            String fontPath = candidate;
            int commaIndex = candidate.indexOf(',');
            if (commaIndex > -1) {
                fontPath = candidate.substring(0, commaIndex);
            }
            if (!Files.exists(Paths.get(fontPath))) {
                continue;
            }
            return PdfFontFactory.createFont(candidate, PdfEncodings.IDENTITY_H);
        }
        return PdfFontFactory.createFont(StandardFonts.HELVETICA);
    }

    /**
     * 校验并规范化 PDF 路径，限制在工具目录内。
     */
    private Path resolveSafePdfPath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        String normalizedFileName = fileName.endsWith(".pdf") ? fileName : fileName + ".pdf";
        Path targetPath = PDF_DIR.resolve(normalizedFileName).normalize();
        if (!targetPath.startsWith(PDF_DIR)) {
            throw new IllegalArgumentException("非法文件路径，不允许访问目录外文件");
        }
        return targetPath;
    }
}