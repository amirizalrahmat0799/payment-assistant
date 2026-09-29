package com.pgs.assistant.knowledge;

import com.pgs.assistant.config.AssistantProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Retrieval-augmented generation over the gateway's documentation in {@code resources/knowledge}.
 * <p>
 * Ingestion runs at startup and is incremental: each file's SHA-256 is stored, so unchanged files
 * are skipped (no re-embedding cost) and changed files have their old chunks replaced.
 */
@Component
public class KnowledgeBase implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBase.class);

    /** A retrieved chunk plus where it came from, so answers can cite their sources. */
    public record Snippet(String source, String title, String section, String text, double score) {
    }

    private final VectorStore vectorStore;
    private final JdbcClient jdbc;
    private final AssistantProperties properties;
    // ~350 tokens per chunk; sections shorter than that stay whole.
    private final TokenTextSplitter splitter = new TokenTextSplitter(350, 100, 5, 1000, true, List.of('.', ',', ';', ':', '!', '?', '\n'));

    public KnowledgeBase(VectorStore vectorStore, JdbcClient jdbc, AssistantProperties properties) {
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        jdbc.sql("""
                CREATE TABLE IF NOT EXISTS knowledge_ingest (
                    source       VARCHAR(200) PRIMARY KEY,
                    content_hash VARCHAR(64)  NOT NULL,
                    document_ids TEXT         NOT NULL,
                    ingested_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
                )""").update();

        Resource[] files = new PathMatchingResourcePatternResolver().getResources("classpath:knowledge/*.md");
        for (Resource file : files) {
            ingest(file.getFilename(), new String(file.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    void ingest(String source, String markdown) {
        String hash = sha256(markdown);
        Optional<String[]> existing = jdbc.sql("SELECT content_hash, document_ids FROM knowledge_ingest WHERE source = ?")
                .param(source)
                .query((rs, i) -> new String[]{rs.getString(1), rs.getString(2)})
                .optional();

        if (existing.isPresent() && existing.get()[0].equals(hash)) {
            log.debug("Knowledge {} unchanged", source);
            return;
        }
        existing.ifPresent(row -> vectorStore.delete(List.of(row[1].split(","))));

        List<Document> sections = new ArrayList<>();
        for (MarkdownSections.Section s : MarkdownSections.split(markdown)) {
            sections.add(new Document(s.heading() + "\n\n" + s.text(),
                    Map.of("source", source, "title", s.title(), "section", s.heading())));
        }
        List<Document> chunks = splitter.apply(sections);
        vectorStore.add(chunks);

        String ids = String.join(",", chunks.stream().map(Document::getId).toList());
        jdbc.sql("""
                        INSERT INTO knowledge_ingest (source, content_hash, document_ids) VALUES (?, ?, ?)
                        ON CONFLICT (source) DO UPDATE
                           SET content_hash = EXCLUDED.content_hash, document_ids = EXCLUDED.document_ids, ingested_at = now()
                        """)
                .params(source, hash, ids)
                .update();
        log.info("Ingested {} ({} chunks)", source, chunks.size());
    }

    public List<Snippet> search(String question) {
        List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                .query(question)
                .topK(properties.rag().topK())
                .similarityThreshold(properties.rag().similarityThreshold())
                .build());
        return docs.stream()
                .map(d -> new Snippet(
                        String.valueOf(d.getMetadata().get("source")),
                        String.valueOf(d.getMetadata().get("title")),
                        String.valueOf(d.getMetadata().get("section")),
                        d.getText(),
                        d.getScore() == null ? 0 : d.getScore()))
                .toList();
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
