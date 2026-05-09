package invertedIndex;

import java.util.*;

public class Ranking {

    public static List<Map.Entry<Integer, Double>> getTopK(
            Map<Integer, Double> scores, int k) {

        List<Map.Entry<Integer, Double>> ranked = new ArrayList<>(scores.entrySet());

        ranked.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        int limit = Math.min(k, ranked.size());
        return new ArrayList<>(ranked.subList(0, limit));
    }

    public static void displayResults(List<Map.Entry<Integer, Double>> ranked) {
        if (ranked.isEmpty()) {
            System.out.println("No matching documents found.");
            return;
        }
        System.out.println("\n--- Top " + ranked.size() + " Ranked Documents ---");
        for (int i = 0; i < ranked.size(); i++) {
            Map.Entry<Integer, Double> entry = ranked.get(i);
            System.out.println("Rank " + (i + 1) + ": Doc ID = " + entry.getKey() +
                    " | Score = " + String.format("%.4f", entry.getValue()));
        }
    }
}
