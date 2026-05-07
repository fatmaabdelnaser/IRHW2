package invertedIndex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

/**
 * WebCrawler
 *
 * A BFS web crawler that starts from the Wikipedia "List of pharaohs" seed URL
 * and visits up to MAX_PAGES = 10 Wikipedia pages.
 *
 * For each visited page it prints:
 *   - Page number, URL, and extracted title
 *   - The number of outgoing Wikipedia links found
 *   - A short preview of the visible text
 *
 * How to compile & run:
 *   javac invertedIndex/WebCrawler.java
 *   java  invertedIndex.WebCrawler
 */
public class WebCrawler {

    // ------------------------------------------------------------------
    // Configuration
    // ------------------------------------------------------------------
    private static final String SEED_URL  = "https://en.wikipedia.org/wiki/List_of_pharaohs";
    private static final int    MAX_PAGES = 10;   // crawl at most N pages

    // ------------------------------------------------------------------
    // Crawled-page record (simple container)
    // ------------------------------------------------------------------
    public static class CrawledPage {
        public int    docId;
        public String url;
        public String title;
        public String visibleText;   // full plain text
        public int    textLength;    // word count

        CrawledPage(int id, String u, String t, String txt) {
            docId       = id;
            url         = u;
            title       = t;
            visibleText = txt;
            // count words (split on whitespace)
            textLength  = txt.trim().isEmpty() ? 0 : txt.trim().split("\\s+").length;
        }
    }

    // ------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------
    /** All successfully crawled pages, in visit order. */
    private final List<CrawledPage> pages = new ArrayList<>();

    // ------------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------------

    /**
     * Runs the BFS crawl.  After this method returns, {@link #getPages()}
     * contains up to MAX_PAGES crawled pages.
     */
    public void crawl() {
        Queue<String> frontier = new LinkedList<>();
        Set<String>   visited  = new LinkedHashSet<>();

        frontier.add(SEED_URL);
        int docId = 0;

        System.out.println("=========================================");
        System.out.println("  Web Crawler – seed: " + SEED_URL);
        System.out.println("  Max pages : " + MAX_PAGES);
        System.out.println("=========================================\n");

        while (!frontier.isEmpty() && docId < MAX_PAGES) {

            String url = frontier.poll();

            // Skip if already visited
            if (visited.contains(url)) continue;
            visited.add(url);

            System.out.println("------------------------------------------");
            System.out.printf("[%d/%d] Fetching: %s%n", docId + 1, MAX_PAGES, url);

            try {
                // 1. Download HTML
                String html = fetchPage(url);

                // 2. Extract title and visible text
                String title = extractTitle(html);
                String text  = extractVisibleText(html);

                // 3. Store result
                CrawledPage cp = new CrawledPage(docId, url, title, text);
                pages.add(cp);

                // 4. Print summary
                System.out.println("   Title      : " + title);
                System.out.println("   Word count : " + cp.textLength);
                // Print first 200 characters of visible text as a preview
                String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
                System.out.println("   Preview    : " + preview);

                // 5. Extract outgoing Wikipedia links and enqueue them
                List<String> links = extractWikiLinks(html);
                System.out.println("   Links found: " + links.size());
                for (String link : links) {
                    if (!visited.contains(link)) {
                        frontier.add(link);
                    }
                }

                docId++;

                // Polite delay – avoid hammering the server
                Thread.sleep(500);

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                System.out.println("   Crawl interrupted.");
                break;
            } catch (Exception e) {
                System.out.println("   ERROR: " + e.getMessage() + " – skipping page.");
            }
        }

        System.out.println("\n==========================================");
        System.out.println("  Crawl complete.  Pages fetched: " + pages.size());
        System.out.println("==========================================\n");
    }

    /**
     * Returns the list of crawled pages (populated after {@link #crawl()}).
     */
    public List<CrawledPage> getPages() {
        return pages;
    }

    // ------------------------------------------------------------------
    // HTTP fetch
    // ------------------------------------------------------------------

    /**
     * Downloads the page at {@code urlStr} and returns its raw HTML.
     * Follows up to one HTTP redirect automatically.
     *
     * @param urlStr absolute URL to fetch
     * @return raw HTML content as a String
     * @throws IOException on network or HTTP errors
     */
    private String fetchPage(String urlStr) throws IOException {
        HttpURLConnection conn =
                (HttpURLConnection) new URL(urlStr).openConnection();

        // Identify ourselves politely
        conn.setRequestProperty("User-Agent",
                "Mozilla/5.0 (compatible; UniversityCrawler/1.0; educational use)");
        conn.setConnectTimeout(8_000);   // 8 s connect timeout
        conn.setReadTimeout(12_000);     // 12 s read timeout
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();

        // Manual redirect handling (covers 301 / 302 / 303 / 307 / 308)
        if (status == HttpURLConnection.HTTP_MOVED_PERM  ||
                status == HttpURLConnection.HTTP_MOVED_TEMP  ||
                status == 303 || status == 307 || status == 308) {
            String newUrl = conn.getHeaderField("Location");
            conn.disconnect();
            if (newUrl != null && !newUrl.equals(urlStr)) {
                return fetchPage(newUrl);   // follow once
            }
        }

        if (status != HttpURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new IOException("HTTP " + status + " for " + urlStr);
        }

        // Read the response body
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append('\n');
            }
        }
        conn.disconnect();
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // HTML parsing helpers
    // ------------------------------------------------------------------

    /**
     * Extracts the text content of the first {@code <title>} element.
     *
     * @param html raw HTML
     * @return page title, or "Untitled" if no title tag is found
     */
    private String extractTitle(String html) {
        int start = html.indexOf("<title>");
        int end   = html.indexOf("</title>", start);
        if (start < 0 || end < 0) return "Untitled";
        return html.substring(start + 7, end).trim();
    }

    /**
     * Strips all HTML markup and returns plain visible text.
     *
     * Steps:
     *  1. Remove {@code <script>} blocks entirely.
     *  2. Remove {@code <style>} blocks entirely.
     *  3. Strip every remaining HTML tag.
     *  4. Decode the most common HTML entities.
     *  5. Collapse consecutive whitespace to single spaces.
     *
     * @param html raw HTML
     * @return plain text suitable for indexing or display
     */
    private String extractVisibleText(String html) {
        // Remove script blocks
        String text = html.replaceAll("(?is)<script[^>]*>.*?</script>", " ");
        // Remove style blocks
        text = text.replaceAll("(?is)<style[^>]*>.*?</style>", " ");
        // Strip all remaining tags
        text = text.replaceAll("<[^>]+>", " ");
        // Decode common HTML entities
        text = text.replace("&amp;",  "&")
                .replace("&lt;",   "<")
                .replace("&gt;",   ">")
                .replace("&quot;", "\"")
                .replace("&#39;",  "'")
                .replace("&nbsp;", " ")
                .replace("&#160;", " ");
        // Collapse whitespace
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    /**
     * Extracts all absolute Wikipedia article URLs from {@code href="/wiki/..."}
     * attributes, excluding special-namespace pages (those containing {@code :}
     * or {@code #}).
     *
     * @param html raw HTML of a Wikipedia page
     * @return list of unique absolute Wikipedia article URLs found on the page
     */
    private List<String> extractWikiLinks(String html) {
        List<String>  links = new ArrayList<>();
        Set<String>   seen  = new LinkedHashSet<>();   // deduplicate per page
        int pos = 0;

        while (pos < html.length()) {
            // Look for  href="/wiki/
            int hrefIdx = html.indexOf("href=\"/wiki/", pos);
            if (hrefIdx < 0) break;

            int valueStart = hrefIdx + 6;              // skip  href="
            int valueEnd   = html.indexOf('"', valueStart);
            if (valueEnd < 0) break;

            String path = html.substring(valueStart, valueEnd);

            // Exclude Wikipedia meta-pages (File:, Help:, Wikipedia:, #anchor, …)
            if (!path.contains(":") && !path.contains("#")) {
                String fullUrl = "https://en.wikipedia.org" + path;
                if (seen.add(fullUrl)) {               // only add if not already seen
                    links.add(fullUrl);
                }
            }
            pos = valueEnd + 1;
        }
        return links;
    }

    // ------------------------------------------------------------------
    // main – demonstration
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        WebCrawler crawler = new WebCrawler();

        // Run the crawl
        crawler.crawl();

        // Print a final summary table of all crawled pages
        System.out.println("=== Summary of Crawled Pages ===");
        System.out.printf("%-6s  %-55s  %-8s  %s%n",
                "DocID", "URL (truncated)", "Words", "Title");
        System.out.println("-".repeat(110));

        for (CrawledPage cp : crawler.getPages()) {
            // Truncate URL for display
            String shortUrl = cp.url.length() > 55
                    ? cp.url.substring(0, 52) + "..."
                    : cp.url;
            System.out.printf("%-6d  %-55s  %-8d  %s%n",
                    cp.docId, shortUrl, cp.textLength, cp.title);
        }
    }
}