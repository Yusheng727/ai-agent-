package com.yusheng.aiagentproject;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local") // 测试时启用本地配置，确保 MySQL 数据源可用
class AiAgentProjectApplicationTests {

    @Test
    void contextLoads() {
    }
}