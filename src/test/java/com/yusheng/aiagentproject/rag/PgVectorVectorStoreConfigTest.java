package com.yusheng.aiagentproject.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

@SpringBootTest
@ActiveProfiles("local")
class PgVectorVectorStoreConfigTest {

    @Resource
    private VectorStore pgVectorVectorStore;

    @Test
    void pgVectorVectorStore() {
        List<Document> documents = List.of(
                new Document("yusheng是一个程序员，喜欢打乒乓球", Map.of("tag", "programmer_pingpong")),
                new Document("世界上喜欢乒乓球和折纸的人不多", Map.of("tag", "pingpong_origami")),
                new Document("yusheng其实还喜欢折纸", Map.of("tag", "origami"))
        );

        pgVectorVectorStore.add(documents);

        List<Document> results = pgVectorVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("程序员喜欢打乒乓球")
                        .topK(5)
                        .build()
        );

        Assertions.assertNotNull(results, "检索结果不能为 null");
        Assertions.assertFalse(results.isEmpty(), "检索结果不能为空");
        Assertions.assertTrue(results.size() <= 5, "返回结果数量不应超过 topK");

        Assertions.assertTrue(
                results.stream().anyMatch(doc -> doc.getText().contains("程序员")),
                "结果中应该至少有一条和“程序员”相关的文档"
        );

        Assertions.assertTrue(
                results.stream().anyMatch(doc -> doc.getText().contains("乒乓球")),
                "结果中应该至少有一条和“乒乓球”相关的文档"
        );

        Assertions.assertTrue(
                results.stream()
                        .limit(3)
                        .anyMatch(doc -> doc.getText().contains("yusheng是一个程序员，喜欢打乒乓球")),
                "前3条结果中应该包含目标文档"
        );

        Assertions.assertTrue(
                results.stream().allMatch(doc -> doc.getMetadata().containsKey("distance")),
                "每条检索结果都应该包含 distance 元数据"
        );
    }
}
