package preprocessing;

import datastructures.CustomHashTable;
import java.util.ArrayList;
import java.util.List;

/**
 * Text normalizer that tokenizes input text, filters punctuation, 
 * lowercases letters, and removes stop words using custom loops and data structures.
 * Zero external dependencies.
 */
public class TextNormalizer {

    // Hardcoded static array of common stop words
    private static final String[] STOP_WORDS = {
        "the", "is", "at", "which", "on", "a", "an", "and", "or", 
        "in", "to", "of", "for", "with", "as", "by", "that", "this", 
        "it", "its", "be", "are", "was", "were", "been", "has", "have", 
        "had", "do", "does", "did", "from", "but", "not", "he", "she", 
        "they", "we", "i", "you"
    };

    // Load stop words into our CustomHashTable for fast lookup
    private static final CustomHashTable STOP_WORDS_HASH = new CustomHashTable();
    static {
        for (String word : STOP_WORDS) {
            STOP_WORDS_HASH.put(word, 1);
        }
    }

    /**
     * Checks if the word is a stop word.
     */
    public static boolean isStopWord(String word) {
        return STOP_WORDS_HASH.containsKey(word);
    }

    /**
     * Tokenizes a text string into individual lowercased alphanumeric words,
     * filtering out punctuation and stop words.
     * Uses loops, char[], and StringBuilder.
     */
    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) return tokens;

        char[] charArray = text.toCharArray();
        StringBuilder wordBuilder = new StringBuilder();

        for (int i = 0; i < charArray.length; i++) {
            char c = charArray[i];

            // Check if alphanumeric
            if (isAlphanumeric(c)) {
                // Convert uppercase to lowercase on the fly
                if (c >= 'A' && c <= 'Z') {
                    c = (char) (c + ('a' - 'A'));
                }
                wordBuilder.append(c);
            } else {
                // Separator/punctuation: check if we have accumulated a word
                addWordIfValid(wordBuilder, tokens);
            }
        }

        // Process any trailing characters at the end of the text
        addWordIfValid(wordBuilder, tokens);

        return tokens;
    }

    /**
     * Checks if a character is alphanumeric (a-z, A-Z, 0-9).
     */
    private static boolean isAlphanumeric(char c) {
        return (c >= 'a' && c <= 'z') || 
               (c >= 'A' && c <= 'Z') || 
               (c >= '0' && c <= '9');
    }

    /**
     * Helper to check word validity (non-empty and not a stop word) and add to tokens list.
     */
    private static void addWordIfValid(StringBuilder sb, List<String> tokens) {
        if (sb.length() > 0) {
            String word = sb.toString();
            if (!isStopWord(word)) {
                tokens.add(word);
            }
            sb.setLength(0); // Reset for the next word
        }
    }
}
