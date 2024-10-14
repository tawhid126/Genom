import java.util.*;
import java.util.concurrent.*;

class DNARead {
    private String read;

    public DNARead(String read) {
        this.read = read;
    }

    public String getRead() {
        return read;
    }

    public void setRead(String read) {
        this.read = read;
    }
}

class OverlapTask implements Callable<int[]> {
    private DNARead read1;
    private DNARead read2;

    public OverlapTask(DNARead read1, DNARead read2) {
        this.read1 = read1;
        this.read2 = read2;
    }

    @Override
    public int[] call() {
        String r1 = read1.getRead();
        String r2 = read2.getRead();
        int maxOverlap = 0;

        for (int i = 1; i <= Math.min(r1.length(), r2.length()); i++) {
            if (r1.endsWith(r2.substring(0, i))) {
                maxOverlap = i;
            }
        }

        return new int[]{maxOverlap, maxOverlap > 0 ? 1 : 0};
    }
}

public class GenomeAssembly {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        List<String> reads = Arrays.asList("ATGC", "GCAT", "TGCA", "CATG");
        List<DNARead> dnaReads = new ArrayList<>();
        for (String read : reads) {
            dnaReads.add(new DNARead(read));
        }

        ExecutorService executor = Executors.newFixedThreadPool(5);

        while (dnaReads.size() > 1) {
            int maxOverlap = -1;
            int bestI = -1;
            int bestJ = -1;
            String mergedRead = "";

            List<Future<int[]>> futures = new ArrayList<>();
            for (int i = 0; i < dnaReads.size(); i++) {
                for (int j = 0; j < dnaReads.size(); j++) {
                    if (i != j) {
                        futures.add(executor.submit(new OverlapTask(dnaReads.get(i), dnaReads.get(j))));
                    }
                }
            }

            int index = 0;
            for (int i = 0; i < dnaReads.size(); i++) {
                for (int j = 0; j < dnaReads.size(); j++) {
                    if (i != j) {
                        int[] result = futures.get(index++).get();
                        int overlap = result[0];
                        if (overlap > maxOverlap) {
                            maxOverlap = overlap;
                            bestI = i;
                            bestJ = j;
                            mergedRead = dnaReads.get(i).getRead() + dnaReads.get(j).getRead().substring(overlap);
                        }
                    }
                }
            }

            if (maxOverlap > 0) {
                dnaReads.set(bestI, new DNARead(mergedRead));
                dnaReads.remove(bestJ);
            } else {
                break;
            }
        }

        executor.shutdown();

        if (dnaReads.size() == 1) {
            System.out.println(dnaReads.get(0).getRead());
        } else {
            System.out.println("No single contig could be formed.");
        }
    }
}