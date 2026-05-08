package invertedIndex;

import java.util.*;

/**
 * InvertedIndex
 *
 * Builds an inverted index from a list of CrawledPage objects produced by WebCrawler.
 *
 * Data structures:
 *   invertedIndex : Map<String, Map<Integer, Integer>>
 *                   term  →  { docId → TF (raw term frequency) }
 *
 *   termFrequency : Map<Integer, Map<String, Integer>>
 *                   docId →  { term  → TF }          (same data, different view)
 *
 * Usage (from main or another class):
 *   WebCrawler crawler = new WebCrawler();
 *   crawler.crawl();
 *   InvertedIndex index = new InvertedIndex(crawler.getPages());
 *   index.build();
 *   index.printIndex(20);   // print first 20 terms
 */
public class InvertedIndex {

    // Core data structures

    /**
     * The inverted index:
     *   term  →  Map( docId → TF )
     *
     * TF = raw count of how many times the term appears in that document.
     */
    private final Map<String, Map<Integer, Integer>> invertedIndex = new TreeMap<>();

    /**
     * Per-document term-frequency view (needed later for TF-IDF):
     *   docId →  Map( term → TF )
     */
    private final Map<Integer, Map<String, Integer>> termFrequency = new HashMap<>();

    /** Total number of documents indexed (= N used in IDF formula). */
    private int totalDocs = 0;

    /** The pages we received from the crawler. */
    private final List<WebCrawler.CrawledPage> pages;

    // Constructor

    /**
     * @param pages list of crawled pages returned by {@link WebCrawler#getPages()}
     */
    public InvertedIndex(List<WebCrawler.CrawledPage> pages) {
        this.pages = pages;
    }

    // Build

    /**
     * Processes every crawled page and populates {@link #invertedIndex}
     * and {@link #termFrequency}.
     *
     * Steps for each page:
     *   1. Tokenise  – lower-case, keep only alphabetic tokens, drop stop-words.
     *   2. Count TF  – count how many times each token appears in this document.
     *   3. Post      – post each (term, docId, TF) triple into the inverted index.
     */
    public void build() {
        System.out.println("Building inverted index for " + pages.size() + " pages...\n");

        for (WebCrawler.CrawledPage page : pages) {
            totalDocs++;

            // --- Step 1 & 2: tokenise and count ---
            Map<String, Integer> tf = computeTF(page.visibleText);

            // Store per-document view
            termFrequency.put(page.docId, tf);

            // --- Step 3: post into inverted index ---
            for (Map.Entry<String, Integer> entry : tf.entrySet()) {
                String term  = entry.getKey();
                int    count = entry.getValue();

                // Get (or create) the posting list for this term
                invertedIndex
                        .computeIfAbsent(term, k -> new HashMap<>())
                        .put(page.docId, count);
            }
        }

        System.out.println("Inverted index built successfully.");
        System.out.println("  Total unique terms : " + invertedIndex.size());
        System.out.println("  Total documents    : " + totalDocs);
        System.out.println();
    }

    // Tokenisation helpers

    /**
     * Tokenises {@code text}, removes stop-words, and returns a map of
     * term → raw frequency for this single document.
     *
     * @param text plain visible text of one page
     * @return map of term → TF
     */
    private Map<String, Integer> computeTF(String text) {
        Map<String, Integer> tf = new HashMap<>();

        // Split on any non-alphabetic character; lower-case every token
        String[] tokens = text.toLowerCase().split("[^a-z]+");

        for (String token : tokens) {
            // Skip very short tokens and stop-words
            if (token.length() < 3) continue;
            if (STOP_WORDS.contains(token)) continue;

            tf.merge(token, 1, Integer::sum);  // tf[token]++
        }
        return tf;
    }

    // Public getters (used by later classes: TF-IDF, CosineSimilarity)

    /** @return the full inverted index (term → { docId → TF }) */
    public Map<String, Map<Integer, Integer>> getInvertedIndex() {
        return invertedIndex;
    }

    /** @return per-document TF map (docId → { term → TF }) */
    public Map<Integer, Map<String, Integer>> getTermFrequency() {
        return termFrequency;
    }

    /** @return N = total number of indexed documents */
    public int getTotalDocs() {
        return totalDocs;
    }

    // Printing / display

    /**
     * Prints the inverted index to standard output.
     *
     * @param maxTerms maximum number of terms to print (use -1 for all)
     */
    public void printIndex(int maxTerms) {
        System.out.println("============================================================");
        System.out.println("  INVERTED INDEX  (term → posting list)");
        System.out.println("============================================================");
        System.out.printf("%-25s  %s%n", "TERM", "DocID:TF  pairs");
        System.out.println("------------------------------------------------------------");

        int count = 0;
        for (Map.Entry<String, Map<Integer, Integer>> entry : invertedIndex.entrySet()) {

            if (maxTerms > 0 && count >= maxTerms) {
                System.out.println("  ... (showing first " + maxTerms + " terms) ...");
                break;
            }

            String                  term        = entry.getKey();
            Map<Integer, Integer>   postingList = entry.getValue();

            // Build readable posting list string: "docId:TF, docId:TF, ..."
            StringBuilder sb = new StringBuilder();
            // Sort by docId for consistent output
            postingList.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(e -> sb.append("doc").append(e.getKey())
                            .append(":").append(e.getValue()).append("  "));

            System.out.printf("%-25s  %s%n", term, sb.toString().trim());
            count++;
        }

        System.out.println("============================================================\n");
    }

    /**
     * Prints per-document TF statistics for a specific document.
     *
     * @param docId the document whose TF map to display
     * @param topN  show only the top-N most frequent terms
     */
    public void printDocumentTF(int docId, int topN) {
        Map<String, Integer> tf = termFrequency.get(docId);
        if (tf == null) {
            System.out.println("Document " + docId + " not found.");
            return;
        }

        // Find the page title for context
        String title = pages.stream()
                .filter(p -> p.docId == docId)
                .map(p -> p.title)
                .findFirst().orElse("Unknown");

        System.out.println("------------------------------------------------------------");
        System.out.println("  TF for Doc " + docId + ": " + title);
        System.out.println("------------------------------------------------------------");

        tf.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topN)
                .forEach(e -> System.out.printf("  %-20s %d%n", e.getKey(), e.getValue()));

        System.out.println();
    }

    // Stop-words list  (common English words that carry no meaning)
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

    // main – standalone demo (crawls + builds index + prints sample)
    public static void main(String[] args) {
        // 1. Crawl
        WebCrawler crawler = new WebCrawler();
        crawler.crawl();

        // 2. Build the inverted index
        InvertedIndex index = new InvertedIndex(crawler.getPages());
        index.build();

        // 3. Print a sample of the index (first 30 terms, alphabetical)
        index.printIndex(30);

        // 4. Print top-10 terms by TF for each crawled document
        System.out.println("=== Top-10 terms per document ===\n");
        for (WebCrawler.CrawledPage page : crawler.getPages()) {
            index.printDocumentTF(page.docId, 10);
        }
    }
}