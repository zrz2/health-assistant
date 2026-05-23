package com.healthassistant.module.knowledge.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TextChunker {

    private static final Logger log = LoggerFactory.getLogger(TextChunker.class);
    private static final int MAX_CHUNK_CHARS = 800;
    private static final int MIN_CHUNK_CHARS = 200;
    private static final int OVERLAP_CHARS = 80;

    // Heading patterns for Chinese medical documents
    private static final Pattern[] HEADING_PATTERNS = {
            Pattern.compile("^第[一二三四五六七八九十百]+章\\s*.*"),         // 第一章 xxx
            Pattern.compile("^第[一二三四五六七八九十百]+节\\s*.*"),           // 第一节 xxx
            Pattern.compile("^[一二三四五六七八九十]、\\s*(.+)"),              // 一、xxx
            Pattern.compile("^[（(]\\s*[一二三四五六七八九十]\\s*[）)]\\s*(.+)"), // (一) xxx
            Pattern.compile("^(\\d+)\\.(\\d+)\\s+(.+)"),                    // 1.1 xxx
            Pattern.compile("^(\\d+)\\.\\s+(.+)"),                          // 1. xxx
            Pattern.compile("^(\\d+)、\\s*(.+)"),                            // 1、xxx
            Pattern.compile("^[（(]\\s*(\\d+)\\s*[）)]\\s*(.+)"),            // (1) xxx
            Pattern.compile("^([A-Z])\\.\\s+(.+)"),                         // A. xxx
    };

    private static final Pattern BLANK_LINE = Pattern.compile("\\n\\s*\\n");

    public ChunkResult chunkByMedicalSections(String document, String parentDocId) {
        if (document == null || document.isBlank()) {
            return new ChunkResult(Collections.emptyList(), Collections.emptyList());
        }

        List<Section> sections = parseSections(document);

        List<Chunk> searchChunks = new ArrayList<>();
        List<Chunk> parentChunks = new ArrayList<>();
        int chunkIndex = 0;

        for (Section section : sections) {
            String sectionText = section.text();
            String sectionPath = section.path();
            int headingLevel = section.level();

            if (sectionText.length() <= MAX_CHUNK_CHARS) {
                // Section fits in one chunk
                String chunkId = parentDocId + "_chunk_" + chunkIndex++;
                searchChunks.add(new Chunk(chunkId, sectionText, sectionPath, headingLevel, parentDocId));
            } else {
                // Split long sections by paragraphs, with overlap
                String[] paragraphs = sectionText.split("\\n\\s*\\n");
                StringBuilder current = new StringBuilder();
                int paraStart = 0;

                for (int i = 0; i < paragraphs.length; i++) {
                    String para = paragraphs[i].trim();
                    if (para.isEmpty()) continue;

                    if (current.length() + para.length() > MAX_CHUNK_CHARS && current.length() >= MIN_CHUNK_CHARS) {
                        String chunkId = parentDocId + "_chunk_" + chunkIndex++;
                        searchChunks.add(new Chunk(chunkId, current.toString().trim(),
                                sectionPath, headingLevel, parentDocId));
                        // Keep last paragraph as overlap
                        current.setLength(0);
                        if (paraStart < i - 1) {
                            String overlap = paragraphs[i - 1].trim();
                            if (overlap.length() < OVERLAP_CHARS * 2) {
                                current.append(overlap).append("\n\n");
                            }
                        }
                    }
                    current.append(para).append("\n\n");
                }

                if (!current.isEmpty()) {
                    String chunkId = parentDocId + "_chunk_" + chunkIndex++;
                    searchChunks.add(new Chunk(chunkId, current.toString().trim(),
                            sectionPath, headingLevel, parentDocId));
                }
            }

            // Parent chunk: whole section
            if (sectionText.length() >= MIN_CHUNK_CHARS || !searchChunks.isEmpty()) {
                String parentChunkId = parentDocId + "_parent_" + sectionPath.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "_");
                parentChunks.add(new Chunk(parentChunkId, sectionText, sectionPath, headingLevel, parentDocId));
            }
        }

        // Fallback: if no sections detected, chunk by paragraphs
        if (searchChunks.isEmpty()) {
            chunkIndex = 0;
            String[] paragraphs = document.split("\\n\\s*\\n");
            StringBuilder current = new StringBuilder();
            for (String para : paragraphs) {
                para = para.trim();
                if (para.isEmpty()) continue;
                if (current.length() + para.length() > MAX_CHUNK_CHARS && current.length() >= MIN_CHUNK_CHARS) {
                    String chunkId = parentDocId + "_chunk_" + chunkIndex++;
                    searchChunks.add(new Chunk(chunkId, current.toString().trim(),
                            "", 0, parentDocId));
                    current.setLength(0);
                }
                current.append(para).append("\n\n");
            }
            if (!current.isEmpty()) {
                String chunkId = parentDocId + "_chunk_" + chunkIndex++;
                searchChunks.add(new Chunk(chunkId, current.toString().trim(),
                        "", 0, parentDocId));
            }
        }

        log.debug("Chunked doc {} into {} search chunks, {} parent chunks",
                parentDocId, searchChunks.size(), parentChunks.size());
        return new ChunkResult(searchChunks, parentChunks);
    }

    private List<Section> parseSections(String document) {
        List<Section> sections = new ArrayList<>();
        String[] lines = document.split("\\n");
        List<String> currentLines = new ArrayList<>();
        String currentPath = "";
        int currentLevel = 0;

        for (String line : lines) {
            HeadingMatch match = matchHeading(line.trim());
            if (match != null) {
                // Flush current section
                if (!currentLines.isEmpty()) {
                    sections.add(new Section(String.join("\n", currentLines),
                            currentPath, currentLevel));
                }
                currentPath = currentPath.isEmpty()
                        ? match.title()
                        : currentPath + " > " + match.title();
                currentLevel = match.level();
                currentLines = new ArrayList<>();
                currentLines.add(line);
            } else {
                currentLines.add(line);
            }
        }

        // Flush last section
        if (!currentLines.isEmpty()) {
            sections.add(new Section(String.join("\n", currentLines),
                    currentPath.isEmpty() ? "正文" : currentPath,
                    currentLevel));
        }

        return sections;
    }

    private HeadingMatch matchHeading(String line) {
        for (int p = 0; p < HEADING_PATTERNS.length; p++) {
            Matcher m = HEADING_PATTERNS[p].matcher(line);
            if (m.find()) {
                String title = line.length() > 60 ? line.substring(0, 60) : line;
                int level = (p < 3) ? 1 : (p < 6) ? 2 : 3;
                return new HeadingMatch(title.trim(), level);
            }
        }
        return null;
    }

    public record Chunk(String chunkId, String content, String sectionPath,
                        int headingLevel, String parentDocId) {}

    public record ChunkResult(List<Chunk> searchChunks, List<Chunk> parentChunks) {}

    private record Section(String text, String path, int level) {}

    private record HeadingMatch(String title, int level) {}
}
