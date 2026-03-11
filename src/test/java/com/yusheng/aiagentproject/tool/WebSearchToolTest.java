package com.yusheng.aiagentproject.tool;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * WebSearchTool 真实联网测试。
 * <p>按你的要求：必须访问真实 SearchAPI，不使用 Mock。
 */
class WebSearchToolTest {

    @Test
    void testSearchWebRealNetwork() {
        String searchApiKey = firstNonBlank(
                System.getProperty("searchapi.api-key"),
                System.getenv("SEARCHAPI_API_KEY"),
                readConfigValue("searchapi.api-key")
        );

        WebSearchTool tool = new WebSearchTool(searchApiKey);
        String query = "程序员鱼皮编程导航 codefather.cn";

        WebSearchTool.WebSearchResponse response = tool.searchWeb(query, 5, 1, 0);

        assertNotNull(response);
        assertTrue(response.success(), "真实联网调用失败: " + response.errorMessage());
        assertTrue(response.hasAnyResult(), "未拿到有效搜索结果");
    }

    /**
     * 优先读取 application.yml 与 application-local.yml（若存在）。
     */
    private String readConfigValue(String key) {
        List<Resource> resources = new ArrayList<>();

        ClassPathResource application = new ClassPathResource("application.yml");
        if (application.exists()) {
            resources.add(application);
        }

        ClassPathResource applicationLocal = new ClassPathResource("application-local.yml");
        if (applicationLocal.exists()) {
            resources.add(applicationLocal);
        }

        if (resources.isEmpty()) {
            return null;
        }

        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(resources.toArray(new Resource[0]));
        Properties properties = yamlFactory.getObject();
        if (properties == null) {
            return null;
        }
        return properties.getProperty(key);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}