import java.util.*;
import java.util.concurrent.*;

public class MultithreadedOverlapDetection {

    public static void main(String[] args) {
        List<String> reads = Arrays.asList("ATGC", "GCAT", "TGCA", "CATG");
        List<Overlap> overlaps = detectOverlaps(reads);
        for (Overlap overlap : overlaps) {
            System.out.println(overlap);
        }
    }

    public static List<Overlap> detectOverlaps(List<String> reads) {
        List<Overlap> overlaps = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        for (int i = 0; i < reads.size(); i++) {
            for (int j = i + 1; j < reads.size(); j++) {
                String read1 = reads.get(i);
                String read2 = reads.get(j);
                executor.submit(() -> checkOverlap(read1, read2, overlaps));
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return overlaps;
    }

    private static void checkOverlap(String read1, String read2, List<Overlap> overlaps) {
        int overlapLength = getOverlapLength(read1, read2);
        if (overlapLength > 0) {
            overlaps.add(new Overlap(read1, read2, overlapLength));
        }
    }

    private static int getOverlapLength(String read1, String read2) {
        int maxOverlap = Math.min(read1.length(), read2.length());
        for (int i = 1; i <= maxOverlap; i++) {
            if (read1.endsWith(read2.substring(0, i))) {
                return i;
            }
        }
        return 0;
    }

    static class Overlap {
        String read1;
        String read2;
        int length;

        Overlap(String read1, String read2, int length) {
            this.read1 = read1;
            this.read2 = read2;
            this.length = length;
        }

        @Override
        public String toString() {
            return "(" + read1 + ", " + read2 + ", " + length + ")";
        }
    }
}

