package com.yusheng.aiagentproject.tool;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationToolTest {

    @Test
    void testWriteAndReadFile() {
        FileOperationTool tool = new FileOperationTool();
        String fileName = "编程导航.txt";
        String content = "https://www.codefather.cn 程序员编程学习交流社区";

        String writeResult = tool.writeFile(fileName, content);
        assertTrue(writeResult.contains("文件写入成功"), "写入应成功，实际返回：" + writeResult);

        String readResult = tool.readFile(fileName);
        assertTrue(readResult.contains("codefather.cn"), "读取内容应包含站点地址，实际返回：" + readResult);
    }

    @Test
    void testRejectPathTraversal() {
        FileOperationTool tool = new FileOperationTool();
        String result = tool.readFile("../secret.txt");
        assertTrue(result.contains("非法文件路径"), "应拒绝目录穿越，实际返回：" + result);
    }
}