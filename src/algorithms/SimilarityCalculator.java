package algorithms;

import datastructures.CustomHashTable;
import preprocessing.TextNormalizer;
import java.util.List;

/**
 * SimilarityCalculator computes the Cosine Similarity index between two texts 
 * using our CustomHashTable for token frequency mapping and manually performing 
 * vector operations.
 * Zero external dependencies.
 */
public class SimilarityCalculator {

    /**
     * Calculates the Cosine Similarity percentage between two strings.
     * Records the total hash table collisions encountered during vector building in the first element of collisionContainer.
     * 
     * @param text1 The first document (e.g. source)
     * @param text2 The second document (e.g. suspicious)
     * @param collisionContainer A single-element array to store the sum of collisions in the frequency maps.
     * @return Cosine Similarity score in the range [0.0, 100.0]
     */
    public static double calculateCosineSimilarity(String text1, String text2, int[] collisionContainer) {
        if (text1 == null || text2 == null || text1.trim().isEmpty() || text2.trim().isEmpty()) {
            if (collisionContainer != null && collisionContainer.length > 0) {
                collisionContainer[0] = 0;
            }
            return 0.0;
        }

        // 1. Preprocess and tokenize both documents
        List<String> tokens1 = TextNormalizer.tokenize(text1);
        List<String> tokens2 = TextNormalizer.tokenize(text2);

        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            if (collisionContainer != null && collisionContainer.length > 0) {
                collisionContainer[0] = 0;
            }
            return 0.0;
        }

        // 2. Map word frequencies for text1 using CustomHashTable
        CustomHashTable freqTable1 = new CustomHashTable();
        for (String token : tokens1) {
            int currentFreq = freqTable1.get(token);
            freqTable1.put(token, currentFreq + 1);
        }

        // 3. Map word frequencies for text2 using CustomHashTable
        CustomHashTable freqTable2 = new CustomHashTable();
        for (String token : tokens2) {
            int currentFreq = freqTable2.get(token);
            freqTable2.put(token, currentFreq + 1);
        }

        // 4. Capture total collisions from both hash tables
        if (collisionContainer != null && collisionContainer.length > 0) {
            collisionContainer[0] = freqTable1.getCollisionCount() + freqTable2.getCollisionCount();
        }

        // 5. Calculate Dot Product and Vector Magnitudes manually
        double dotProduct = 0.0;
        double sumSquares1 = 0.0;
        double sumSquares2 = 0.0;

        // Iterate through all unique keys in the first document vector
        List<String> keys1 = freqTable1.keys();
        for (String key : keys1) {
            double freq1 = freqTable1.get(key);
            sumSquares1 += freq1 * freq1;

            // If the word also exists in the second document, compute dot product part
            if (freqTable2.containsKey(key)) {
                double freq2 = freqTable2.get(key);
                dotProduct += freq1 * freq2;
            }
        }

        // Iterate through all unique keys in the second document vector for its magnitude
        List<String> keys2 = freqTable2.keys();
        for (String key : keys2) {
            double freq2 = freqTable2.get(key);
            sumSquares2 += freq2 * freq2;
        }

        // 6. Calculate Cosine Similarity: DotProduct / (magnitude1 * magnitude2)
        double magnitude1 = Math.sqrt(sumSquares1);
        double magnitude2 = Math.sqrt(sumSquares2);

        if (magnitude1 == 0.0 || magnitude2 == 0.0) {
            return 0.0;
        }

        double similarity = dotProduct / (magnitude1 * magnitude2);
        
        // Return score as a percentage [0.0, 100.0]
        return similarity * 100.0;
    }
}
