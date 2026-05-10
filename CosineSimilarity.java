package invertedIndex;
import java.util.*;

public class CosineSimilarity {

    //take parameters : a doc vector {(word : weight) , () ,....} , query vector {(word : weight) , () ,....}
    public static double cosineSimilarity(Map<String, Double> vec1,
                                          Map<String, Double> vec2) {

        double dot = dotProduct(vec1, vec2);
        double mag1 = magnitude(vec1);
        double mag2 = magnitude(vec2);

        if (mag1 == 0 || mag2 == 0) {
            return 0;
        }

        return dot / (mag1 * mag2);
    }

    
    //  Dot product: sum(A[i] * B[i])
     
    private static double dotProduct(Map<String, Double> v1,
                                     Map<String, Double> v2) {

        double sum = 0.0;

        for (String term : v1.keySet()) {
            if (v2.containsKey(term)) {
                sum += v1.get(term) * v2.get(term);
            }
        }
        return sum;
    }

    
    //magnitude for a vector: sqrt(sum(x^2))
     
    private static double magnitude(Map<String, Double> vec) {

        double sum = 0.0;

        for (double value : vec.values()) {
            sum += value * value;
        }
        return Math.sqrt(sum);
    }

    /**
     * Compute cosine similarity for ALL documents
     *
     * param queryVector TF-IDF query
     * param docVectors  docId → TF-IDF vector
     * return docId → similarity score
     */
    public static Map<Integer, Double> computeAllScores(
            Map<String, Double> queryVector,
            Map<Integer, Map<String, Double>> docVectors) {

        Map<Integer, Double> scores = new HashMap<>(); // hash bec >> no need for ordering (it is a later step) so didnot use TreeMap or linked list

       for (Integer docId : docVectors.keySet()) {

            Map<String, Double> docVector = docVectors.get(docId);

            double score = cosineSimilarity(queryVector, docVector);

            scores.put(docId, score);
        }

        return scores;
    }

    
    
}
