package com.healthassistant.module.admin.service;

import com.healthassistant.common.constant.ErrorCode;
import com.healthassistant.common.exception.BusinessException;
import com.healthassistant.module.admin.entity.SensitiveWord;
import com.healthassistant.module.admin.repository.SensitiveWordRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class SensitiveWordService {

    private static final Logger log = LoggerFactory.getLogger(SensitiveWordService.class);

    private static final String[] SEED_WORDS = {
            "海洛因", "冰毒", "摇头丸", "K粉", "大麻", "可卡因",
            "吗啡", "杜冷丁", "美沙酮", "安非他命", "甲基苯丙胺",
            "氯胺酮", "麦角酸", "二乙酰胺", "芬太尼", "曲马多",
            "安定", "利他林", "麻黄碱", "伪麻黄碱",
            "苯丙胺", "可待因", "罂粟", "鸦片",
            "毒品", "吸毒", "贩毒", "制毒"
    };

    private final SensitiveWordRepository repository;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private TrieNode root = new TrieNode();

    public SensitiveWordService(SensitiveWordRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        // Seed built-in words
        for (String word : SEED_WORDS) {
            if (!repository.existsByWord(word)) {
                SensitiveWord sw = new SensitiveWord();
                sw.setWord(word);
                repository.save(sw);
            }
        }
        rebuildTrie();
        log.info("SensitiveWordService initialized with {} words", countWords());
    }

    public void addWord(String word) {
        if (repository.existsByWord(word)) {
            throw new BusinessException(ErrorCode.SENSITIVE_WORD_EXISTS, "敏感词已存在");
        }
        SensitiveWord sw = new SensitiveWord();
        sw.setWord(word);
        repository.save(sw);

        lock.writeLock().lock();
        try {
            addToTrie(word);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void deleteWord(Long id) {
        SensitiveWord sw = repository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONFIG_NOT_FOUND, "敏感词不存在"));
        repository.delete(sw);
        rebuildTrie();
    }

    public List<SensitiveWord> listAll() {
        return repository.findAll();
    }

    /**
     * Check text for sensitive words using DFA algorithm.
     * @return detected sensitive words (empty if clean)
     */
    public List<String> checkContent(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }
        List<String> hits = new ArrayList<>();
        lock.readLock().lock();
        try {
            int len = text.length();
            for (int i = 0; i < len; i++) {
                TrieNode node = root;
                for (int j = i; j < len; j++) {
                    char c = text.charAt(j);
                    TrieNode child = node.children.get(c);
                    if (child == null) break;
                    node = child;
                    if (node.isEnd) {
                        hits.add(text.substring(i, j + 1));
                        break;
                    }
                }
            }
        } finally {
            lock.readLock().unlock();
        }
        return hits;
    }

    public boolean containsSensitiveWord(String text) {
        return !checkContent(text).isEmpty();
    }

    private int countWords() {
        lock.readLock().lock();
        try {
            return countNodes(root);
        } finally {
            lock.readLock().unlock();
        }
    }

    private int countNodes(TrieNode node) {
        int count = node.isEnd ? 1 : 0;
        for (TrieNode child : node.children.values()) {
            count += countNodes(child);
        }
        return count;
    }

    private void rebuildTrie() {
        lock.writeLock().lock();
        try {
            root = new TrieNode();
            List<SensitiveWord> words = repository.findAll();
            for (SensitiveWord sw : words) {
                addToTrie(sw.getWord());
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void addToTrie(String word) {
        TrieNode node = root;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            node = node.children.computeIfAbsent(c, k -> new TrieNode());
        }
        node.isEnd = true;
    }

    private static class TrieNode {
        Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd;
    }
}
