package com.secondbrain.util;

import com.secondbrain.entity.SensitiveWord;

import java.util.*;

/**
 * Aho-Corasick 多模式匹配器。
 *
 * 将敏感词列表编译为 Trie 自动机（含 failure links），
 * 一次文本扫描即可命中所有模式串，时间复杂度 O(text_length)。
 *
 * @author AI
 */
public final class SensitiveWordMatcher {

    private final TrieNode root;

    private SensitiveWordMatcher(TrieNode root) {
        this.root = root;
    }

    /**
     * 从敏感词列表构建 AC 自动机。
     *
     * @param words 敏感词列表
     * @return 编译后的匹配器
     */
    public static SensitiveWordMatcher compile(List<SensitiveWord> words) {
        TrieNode root = new TrieNode();

        for (SensitiveWord sw : words) {
            TrieNode node = root;
            for (char c : sw.getWord().toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new TrieNode());
            }
            node.isEndOfWord = true;
        }

        // BFS 构建 failure links
        Queue<TrieNode> queue = new LinkedList<>();
        for (TrieNode child : root.children.values()) {
            child.failureLink = root;
            queue.offer(child);
        }

        while (!queue.isEmpty()) {
            TrieNode current = queue.poll();
            for (Map.Entry<Character, TrieNode> entry : current.children.entrySet()) {
                char c = entry.getKey();
                TrieNode child = entry.getValue();
                queue.offer(child);

                TrieNode failure = current.failureLink;
                while (failure != root && !failure.children.containsKey(c)) {
                    failure = failure.failureLink;
                }
                if (failure.children.containsKey(c) && failure.children.get(c) != child) {
                    child.failureLink = failure.children.get(c);
                } else {
                    child.failureLink = root;
                }
            }
        }

        return new SensitiveWordMatcher(root);
    }

    /**
     * 检查文本是否命中任一敏感词。
     *
     * @param text 待检查文本
     * @return true 命中敏感词
     */
    public boolean matches(String text) {
        TrieNode node = root;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            while (node != root && !node.children.containsKey(c)) {
                node = node.failureLink;
            }

            if (node.children.containsKey(c)) {
                node = node.children.get(c);
            }

            // 沿 failure 链检查是否命中词尾
            for (TrieNode temp = node; temp != root; temp = temp.failureLink) {
                if (temp.isEndOfWord) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Trie 节点。
     * children 使用 HashMap 以支持中文等多字节字符。
     */
    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        TrieNode failureLink = null;
        boolean isEndOfWord = false;
    }
}