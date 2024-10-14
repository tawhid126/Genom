import java.util.*;
import java.util.concurrent.*;

public class DeBruijnGraphAssembler {

    public static void main(String[] args) {
        List<String> reads = Arrays.asList("ATGC", "GCAT", "TGCA", "CATG");
        String assembledGenome = assembleGenome(reads, 3); 
        System.out.println(assembledGenome);
    }

    private static class DeBruijnGraph {
        private final Map<String, List<String>> adjacencyList = new ConcurrentHashMap<>();

        public void addEdge(String from, String to) {
            adjacencyList.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        }

        public List<String> getEdges(String node) {
            return adjacencyList.getOrDefault(node, new ArrayList<>());
        }

        public Set<String> getNodes() {
            return adjacencyList.keySet();
        }
    }

    private static DeBruijnGraph constructDeBruijnGraph(List<String> reads, int k) {
        DeBruijnGraph graph = new DeBruijnGraph();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<?>> futures = new ArrayList<>();
        for (String read : reads) {
            futures.add(executor.submit(() -> {
                for (int i = 0; i <= read.length() - k; i++) {
                    String kmer1 = read.substring(i, i + k - 1);
                    String kmer2 = read.substring(i + 1, i + k);
                    graph.addEdge(kmer1, kmer2);
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        return graph;
    }

    private static String traverseDeBruijnGraph(DeBruijnGraph graph) {
        StringBuilder genome = new StringBuilder();
        Set<String> visited = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

        List<Future<?>> futures = new ArrayList<>();
        for (String node : graph.getNodes()) {
            futures.add(executor.submit(() -> {
                if (!visited.contains(node)) {
                    StringBuilder path = new StringBuilder(node);
                    String current = node;
                    visited.add(current);
                    while (!graph.getEdges(current).isEmpty()) {
                        String next = graph.getEdges(current).remove(0);
                        path.append(next.charAt(next.length() - 1));
                        current = next;
                        visited.add(current);
                    }
                    synchronized (genome) {
                        genome.append(path);
                    }
                }
            }));
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        return genome.toString();
    }

    public static String assembleGenome(List<String> reads, int k) {
        DeBruijnGraph graph = constructDeBruijnGraph(reads, k);
        return traverseDeBruijnGraph(graph);
    }
}