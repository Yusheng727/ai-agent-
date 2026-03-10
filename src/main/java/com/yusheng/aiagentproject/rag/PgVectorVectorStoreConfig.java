package com.yusheng.aiagentproject.rag;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class PgVectorVectorStoreConfig {

    private static final Logger log = LoggerFactory.getLogger(PgVectorVectorStoreConfig.class);

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Bean
    public VectorStore pgVectorVectorStore(@Qualifier("pgVectorJdbcTemplate") JdbcTemplate jdbcTemplate,
                                           EmbeddingModel dashscopeEmbeddingModel) {
        VectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashscopeEmbeddingModel)
                .dimensions(1024)
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(true)
                .schemaName("public")
                .vectorTableName("vector_store")
                .maxDocumentBatchSize(10)
                .build();

        Integer existingCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM public.vector_store", Integer.class);
        int documentCount = existingCount == null ? 0 : existingCount;
        log.info("PGVector existing row count: {}", documentCount);

        if (documentCount == 0) {
            List<Document> documents = loveAppDocumentLoader.loadMarkdowns();
            log.info("PGVector seed documents size: {}", documents.size());
            for (int i = 0; i < documents.size(); i += 10) {
                int end = Math.min(i + 10, documents.size());
                List<Document> batch = documents.subList(i, end);
                vectorStore.add(batch);
            }
            log.info("PGVector seed completed");
        }
        else {
            log.info("PGVector seed skipped because table already contains data");
        }

        return vectorStore;
    }
}