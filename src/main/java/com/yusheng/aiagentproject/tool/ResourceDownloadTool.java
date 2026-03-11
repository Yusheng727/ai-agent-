package com.yusheng.aiagentproject.tool;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import com.yusheng.aiagentproject.constant.FileConstant;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ResourceDownloadTool {

    /**
     * 下载文件根目录：所有下载文件都限制在该目录下。
     */
    private static final Path DOWNLOAD_DIR = Paths.get(FileConstant.FILE_SAVE_DIR, "download")
            .toAbsolutePath()
            .normalize();

    /**
     * 下载请求超时时间，网络波动时给足缓冲。
     */
    private static final int REQUEST_TIMEOUT_MILLIS = 30_000;

    @Tool(description = "Download a resource from a given URL")
    public String downloadResource(
            @ToolParam(description = "URL of the resource to download") String url,
            @ToolParam(description = "Name of the file to save the downloaded resource") String fileName) {

        try {
            validateHttpUrl(url);
            Path targetPath = resolveSafePath(fileName);
            FileUtil.mkdir(DOWNLOAD_DIR.toString());

            try (HttpResponse response = HttpUtil.createGet(url)
                    .timeout(REQUEST_TIMEOUT_MILLIS)
                    .setFollowRedirects(true)
                    .execute()) {
                if (!response.isOk()) {
                    return "下载资源失败：HTTP 状态码 " + response.getStatus();
                }
                response.writeBody(new File(targetPath.toString()));
            }
            return "资源下载成功：" + targetPath;
        } catch (Exception e) {
            return "下载资源失败：" + e.getMessage();
        }
    }

    /**
     * 仅允许 http / https，避免误用 file:// 等协议。
     */
    private void validateHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("下载地址不能为空");
        }
        URI uri = URI.create(url.trim());
        String scheme = uri.getScheme();
        if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
            throw new IllegalArgumentException("仅支持 http/https 下载地址");
        }
    }

    /**
     * 将文件名解析为安全路径，并限制在 DOWNLOAD_DIR 内。
     */
    private Path resolveSafePath(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        Path targetPath = DOWNLOAD_DIR.resolve(fileName).normalize();
        if (!targetPath.startsWith(DOWNLOAD_DIR)) {
            throw new IllegalArgumentException("非法文件路径，不允许访问目录外文件");
        }
        return targetPath;
    }
}