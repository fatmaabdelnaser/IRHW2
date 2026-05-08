package invertedIndex;

import java.util.*;

/**
 * Task 3: Get a query and convert it into a numerical TF-IDF vector.
 */
public class QueryProcessor {

    // Must match the stop-words used in InvertedIndex exactly
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the","and","for","are","was","were","has","have","had",
            "his","her","its","their","this","that","these","those",
            "with","from","into","onto","upon","over","under","about",
            "also","been","they","them","then","than","when","where",
            "which","while","who","whom","how","all","any","but","not",
            "can","may","will","would","could","should","shall","does",
            "did","being","each","more","most","one","two","three","four",
            "five","six","seven","eight","nine","ten","new","old","now",
            "out","per","yet","two","use","used","using","known","made"
    ));

    // We need the index to look up 'N' (total docs) and 'DF' (document frequency)
    private final InvertedIndex index;

    public QueryProcessor(InvertedIndex index) {
        this.index = index;
    }

    /**
     * Prompts the user and processes the query.
     * @return A Map representing the query vector: {term -> TF-IDF weight}
     */
    public Map<String, Double> getUserQueryVector() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("\nEnter your search query: ");
        String rawQuery = scanner.nextLine();

        if (rawQuery.trim().isEmpty()) {
            return new HashMap<>();
        }

        return calculateTfIdfVector(rawQuery);
    }

    /**
     *  Convert query into a numerical vector using TF-IDF.
     */
    private Map<String, Double> calculateTfIdfVector(String text) {
        // Step 1: Clean text and count raw Query TF
        Map<String, Integer> queryTf = new HashMap<>();
        String[] tokens = text.toLowerCase().split("[^a-z]+");

        for (String token : tokens) {
            if (token.length() < 3) continue;
            if (STOP_WORDS.contains(token)) continue;
            queryTf.merge(token, 1, Integer::sum);
        }

        // Step 2: Calculate TF-IDF for the query terms
        Map<String, Double> queryVector = new HashMap<>();
        int totalDocsN = index.getTotalDocs();
        Map<String, Map<Integer, Integer>> invIndex = index.getInvertedIndex();

        for (Map.Entry<String, Integer> entry : queryTf.entrySet()) {
            String term = entry.getKey();
            int tf = entry.getValue();

            // Find DF: How many documents contain this term?
            int df = 0;
            if (invIndex.containsKey(term)) {
                df = invIndex.get(term).size();
            }

            // Calculate IDF: log(N / DF)
            // Note: If df is 0 (word doesn't exist in any document), idf is 0.
            double idf = 0.0;
            if (df > 0) {
                // Using Math.log10 (Base 10) or Math.log (Natural log) works.
                // Math.log is standard natural log, which is commonly used.
                idf = Math.log((double) totalDocsN / df);
            }

            // Calculate final weight: TF * IDF
            double weight = tf * idf;
            queryVector.put(term, weight);
        }

        return queryVector;
    }
}
