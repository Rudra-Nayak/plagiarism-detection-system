package algorithms;

import java.util.ArrayList;
import java.util.List;

/**
 * Knuth-Morris-Pratt (KMP) string matching algorithm implementation from scratch.
 * Resolves exact matches of sentence segments and n-grams from the suspicious document
 * in the source document, returning the precise index spans.
 * Zero external dependencies.
 */
public class KMPMatcher {

    /**
     * Represents a matching span of text between the suspicious document and the source document.
     */
    public static class MatchSpan {
        public final String text;
        public final int suspiciousStart;
        public final int suspiciousEnd;
        public final int sourceStart;
        public final int sourceEnd;

        public MatchSpan(String text, int suspiciousStart, int suspiciousEnd, int sourceStart, int sourceEnd) {
            this.text = text;
            this.suspiciousStart = suspiciousStart;
            this.suspiciousEnd = suspiciousEnd;
            this.sourceStart = sourceStart;
            this.sourceEnd = sourceEnd;
        }
    }

    /**
     * Compiles the Longest Prefix Suffix (LPS) array for the given pattern.
     * Case-insensitive version.
     */
    private static int[] computeLPSArray(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;
        lps[0] = 0; // Base case: first character has no proper prefix/suffix

        while (i < m) {
            char cI = Character.toLowerCase(pattern.charAt(i));
            char cLen = Character.toLowerCase(pattern.charAt(len));

            if (cI == cLen) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    /**
     * Performs KMP pattern matching on the source text to find all instances
     * of the pattern. Returns the list of starting indices.
     * Case-insensitive match.
     */
    public static List<Integer> search(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        if (text == null || pattern == null || pattern.length() == 0 || text.length() < pattern.length()) {
            return matches;
        }

        int n = text.length();
        int m = pattern.length();
        int[] lps = computeLPSArray(pattern);

        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < n) {
            char charText = Character.toLowerCase(text.charAt(i));
            char charPattern = Character.toLowerCase(pattern.charAt(j));

            if (charPattern == charText) {
                i++;
                j++;
            }

            if (j == m) {
                // Match found: add starting index
                matches.add(i - j);
                j = lps[j - 1]; // Roll back using LPS
            } else if (i < n && charPattern != charText) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return matches;
    }

    /**
     * Analyzes both documents, identifying matching spans based on sentence splits 
     * or sliding n-grams (phrases of a given word count) from the suspicious text.
     */
    public static List<MatchSpan> findMatches(String source, String suspicious) {
        List<MatchSpan> spans = new ArrayList<>();
        if (source == null || suspicious == null || source.isEmpty() || suspicious.isEmpty()) {
            return spans;
        }

        int length = suspicious.length();
        int start = 0;

        while (start < length) {
            // Skip leading whitespace/newlines
            while (start < length && isWhitespace(suspicious.charAt(start))) {
                start++;
            }
            if (start >= length) break;

            // Segment sentences by delimiters: '.', '?', '!', '\n', '\r'
            int end = start;
            while (end < length) {
                char c = suspicious.charAt(end);
                if (c == '.' || c == '?' || c == '!' || c == '\n' || c == '\r') {
                    end++;
                    break;
                }
                end++;
            }

            String sentence = suspicious.substring(start, end);
            String trimmedSentence = sentence.trim();

            if (trimmedSentence.length() >= 15) {
                // Step 1: Try to search for the full sentence
                List<Integer> sentenceMatches = search(source, trimmedSentence);
                if (!sentenceMatches.isEmpty()) {
                    int suspiciousStartIdx = start + sentence.indexOf(trimmedSentence);
                    int suspiciousEndIdx = suspiciousStartIdx + trimmedSentence.length();
                    for (int srcIdx : sentenceMatches) {
                        spans.add(new MatchSpan(
                            trimmedSentence,
                            suspiciousStartIdx,
                            suspiciousEndIdx,
                            srcIdx,
                            srcIdx + trimmedSentence.length()
                        ));
                    }
                } else {
                    // Step 2: Fallback to n-grams (sub-phrases) of the sentence to catch partial plagiarism
                    searchSubPhrases(source, suspicious, start, trimmedSentence, spans);
                }
            }

            start = end;
        }

        return spans;
    }

    /**
     * Splits a sentence into overlapping n-gram sub-phrases and runs KMP on them.
     */
    private static void searchSubPhrases(String source, String suspicious, int sentenceOffset, String sentenceText, List<MatchSpan> spans) {
        // Simple tokenizer for sub-phrases: find indices of words in the sentence
        List<Integer> wordStarts = new ArrayList<>();
        List<Integer> wordEnds = new ArrayList<>();
        
        int len = sentenceText.length();
        int i = 0;
        while (i < len) {
            while (i < len && isWhitespace(sentenceText.charAt(i))) {
                i++;
            }
            if (i >= len) break;
            wordStarts.add(i);
            while (i < len && !isWhitespace(sentenceText.charAt(i))) {
                i++;
            }
            wordEnds.add(i);
        }

        int wordCount = wordStarts.size();
        int nGramWords = 6; // Sliding window of 6 words

        if (wordCount >= nGramWords) {
            for (int w = 0; w <= wordCount - nGramWords; w++) {
                int startCharIdx = wordStarts.get(w);
                int endCharIdx = wordEnds.get(w + nGramWords - 1);
                String subPhrase = sentenceText.substring(startCharIdx, endCharIdx);

                if (subPhrase.length() >= 15) {
                    List<Integer> phraseMatches = search(source, subPhrase);
                    if (!phraseMatches.isEmpty()) {
                        int suspiciousStartIdx = sentenceOffset + startCharIdx;
                        int suspiciousEndIdx = sentenceOffset + endCharIdx;
                        
                        // Avoid duplicates or highly overlapping same-phrase spans
                        boolean alreadyCovered = false;
                        for (MatchSpan existing : spans) {
                            if (existing.suspiciousStart <= suspiciousStartIdx && existing.suspiciousEnd >= suspiciousEndIdx) {
                                alreadyCovered = true;
                                break;
                            }
                        }

                        if (!alreadyCovered) {
                            for (int srcIdx : phraseMatches) {
                                spans.add(new MatchSpan(
                                    subPhrase,
                                    suspiciousStartIdx,
                                    suspiciousEndIdx,
                                    srcIdx,
                                    srcIdx + subPhrase.length()
                                ));
                            }
                        }
                    }
                }
            }
        }
    }

    private static boolean isWhitespace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r';
    }
}
