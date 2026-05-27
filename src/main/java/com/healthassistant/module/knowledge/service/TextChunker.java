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

    // 中文标点句号、感叹号、问号、分号（作为句子边界）
    private static final Pattern SENTENCE_BOUNDARY = Pattern.compile("[。！?；]\\s*");

    // 标题匹配模式（保留原有）
    private static final Pattern[] HEADING_PATTERNS = {
            Pattern.compile("^第[一二三四五六七八九十百]+章\\s*.*"),
            Pattern.compile("^第[一二三四五六七八九十百]+节\\s*.*"),
            Pattern.compile("^[一二三四五六七八九十]、\\s*(.+)"),
            Pattern.compile("^[（(]\\s*[一二三四五六七八九十]\\s*[）)]\\s*(.+)"),
            Pattern.compile("^(\\d+)\\.(\\d+)\\s+(.+)"),
            Pattern.compile("^(\\d+)\\.\\s+(.+)"),
            Pattern.compile("^(\\d+)、\\s*(.+)"),
            Pattern.compile("^[（(]\\s*(\\d+)\\s*[）)]\\s*(.+)"),
            Pattern.compile("^([A-Z])\\.\\s+(.+)"),
    };

    /**
     * 切分医学文档
     * @param document 完整正文
     * @param parentDocId 知识条目ID
     * @return 切分结果（包含 search chunks 和 parent chunks）
     */
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

            // 1. 创建父 Chunk（完整章节，用于上下文扩展）
            String parentChunkId = parentDocId + "_parent_" + sectionPath.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fff]", "_");
            Chunk parentChunk = new Chunk(
                    parentChunkId,
                    sectionText,
                    sectionPath,
                    headingLevel,
                    parentDocId,
                    null,   // parentContent 为空，父 chunk 没有上层父内容
                    -1,
                    0,      // startChar
                    sectionText.length()
            );
            parentChunks.add(parentChunk);

            // 2. 创建检索子块（search chunks）
            List<Chunk> subChunks = splitSectionToChunks(sectionText, parentDocId, sectionPath, headingLevel, parentChunkId, chunkIndex);
            searchChunks.addAll(subChunks);
            chunkIndex += subChunks.size();
        }

        // Fallback: 如果没有解析出任何章节，则按段落切分整个文档
        if (searchChunks.isEmpty()) {
            log.warn("No sections detected for doc {}, falling back to paragraph splitting", parentDocId);
            Chunk fallbackParent = new Chunk(
                    parentDocId + "_parent_full",
                    document,
                    "",
                    0,
                    parentDocId,
                    null,
                    -1,
                    0,
                    document.length()
            );
            parentChunks.add(fallbackParent);
            searchChunks.addAll(splitPlainTextToChunks(document, parentDocId, fallbackParent.chunkId(), 0));
        }

        log.debug("Chunked doc {} into {} search chunks, {} parent chunks",
                parentDocId, searchChunks.size(), parentChunks.size());
        return new ChunkResult(searchChunks, parentChunks);
    }

    /**
     * 将一个章节切分成多个检索子块，每个子块携带父内容（当前章节完整文本）
     */
    private List<Chunk> splitSectionToChunks(String sectionText, String parentDocId,
                                              String sectionPath, int headingLevel,
                                              String parentChunkId, int startIndex) {
        List<Chunk> chunks = new ArrayList<>();
        if (sectionText.length() <= MAX_CHUNK_CHARS) {
            // 单块章节：直接作为一个 search chunk
            String chunkId = parentDocId + "_chunk_" + startIndex;
            chunks.add(new Chunk(
                    chunkId,
                    sectionText,
                    sectionPath,
                    headingLevel,
                    parentDocId,
                    sectionText,  // parentContent 就是章节全文
                    startIndex,
                    0,
                    sectionText.length()
            ));
            return chunks;
        }

        // 长章节：按句子边界切分，带重叠
        List<String> sentences = splitIntoSentences(sectionText);
        List<String> chunksText = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        int lastCut = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i);
            if (current.length() + sentence.length() > MAX_CHUNK_CHARS && current.length() >= MIN_CHUNK_CHARS) {
                chunksText.add(current.toString().trim());
                // 保留上一块的最后一个句子作为重叠
                String lastSentence = sentences.get(i - 1);
                current.setLength(0);
                if (lastSentence.length() < OVERLAP_CHARS * 2) {
                    current.append(lastSentence);
                }
            }
            current.append(sentence);
        }
        if (!current.isEmpty()) {
            chunksText.add(current.toString().trim());
        }

        // 为每个子块创建 Chunk 对象，携带父内容
        int idx = 0;
        int globalIdx = startIndex;
        int charPos = 0;
        for (String chunkText : chunksText) {
            String chunkId = parentDocId + "_chunk_" + (globalIdx++);
            // 近似计算起始位置（粗略，如需精确可改为基于原文索引）
            int startChar = charPos;
            int endChar = charPos + chunkText.length();
            chunks.add(new Chunk(
                    chunkId,
                    chunkText,
                    sectionPath,
                    headingLevel,
                    parentDocId,
                    sectionText,   // 父内容 = 整个章节文本
                    idx++,
                    startChar,
                    endChar
            ));
            charPos = endChar;
        }
        return chunks;
    }

    /**
     * 纯文本切分（无标题结构时的后备）
     */
    private List<Chunk> splitPlainTextToChunks(String text, String parentDocId,
                                                String parentChunkId, int startIndex) {
        List<Chunk> chunks = new ArrayList<>();
        List<String> sentences = splitIntoSentences(text);
        StringBuilder current = new StringBuilder();
        int chunkIdx = startIndex;
        for (String sentence : sentences) {
            if (current.length() + sentence.length() > MAX_CHUNK_CHARS && current.length() >= MIN_CHUNK_CHARS) {
                String chunkId = parentDocId + "_chunk_" + (chunkIdx++);
                chunks.add(new Chunk(
                        chunkId,
                        current.toString().trim(),
                        "",
                        0,
                        parentDocId,
                        text,   // 父内容就是整个文档
                        chunkIdx - 1,
                        0,
                        current.length()
                ));
                current.setLength(0);
            }
            current.append(sentence);
        }
        if (!current.isEmpty()) {
            String chunkId = parentDocId + "_chunk_" + (chunkIdx++);
            chunks.add(new Chunk(
                    chunkId,
                    current.toString().trim(),
                    "",
                    0,
                    parentDocId,
                    text,
                    chunkIdx - 1,
                    0,
                    current.length()
            ));
        }
        return chunks;
    }

    /**
     * 将文本按句子边界拆分（保留标点）
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        Matcher m = SENTENCE_BOUNDARY.matcher(text);
        int last = 0;
        while (m.find()) {
            String sentence = text.substring(last, m.end()).trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
            last = m.end();
        }
        if (last < text.length()) {
            String lastPart = text.substring(last).trim();
            if (!lastPart.isEmpty()) {
                sentences.add(lastPart);
            }
        }
        if (sentences.isEmpty()) {
            sentences.add(text);
        }
        return sentences;
    }

    // ------------------- 以下为原有的章节解析逻辑（保持不变） -------------------
    private List<Section> parseSections(String document) {
        List<Section> sections = new ArrayList<>();
        String[] lines = document.split("\\n");
        List<String> currentLines = new ArrayList<>();
        String currentPath = "";
        int currentLevel = 0;

        for (String line : lines) {
            HeadingMatch match = matchHeading(line.trim());
            if (match != null) {
                if (!currentLines.isEmpty()) {
                    sections.add(new Section(String.join("\n", currentLines),
                            currentPath, currentLevel));
                }
                currentPath = currentPath.isEmpty() ? match.title() : currentPath + " > " + match.title();
                currentLevel = match.level();
                currentLines = new ArrayList<>();
                currentLines.add(line);
            } else {
                currentLines.add(line);
            }
        }
        if (!currentLines.isEmpty()) {
            sections.add(new Section(String.join("\n", currentLines),
                    currentPath.isEmpty() ? "正文" : currentPath, currentLevel));
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

    // ------------------- 记录类定义 -------------------
    public record Chunk(String chunkId, String content, String sectionPath,
                        int headingLevel, String parentDocId, String parentContent,
                        int chunkIndex, int startChar, int endChar) {}

    public record ChunkResult(List<Chunk> searchChunks, List<Chunk> parentChunks) {}

    private record Section(String text, String path, int level) {}

    private record HeadingMatch(String title, int level) {}
}