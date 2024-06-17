package tup;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class BranchAndBound {
    public int[][] bestSolution;
    private final Problem problem;
    private final LowerBound lowerBound;
    private final long startTime = System.currentTimeMillis();
    private final AtomicLong nNodes = new AtomicLong(0);
    private final Semaphore bestSolutionMutex = new Semaphore(1);
    private final AtomicInteger bestDistance = new AtomicInteger(Integer.MAX_VALUE);

    public BranchAndBound(Problem problem) {
        this.problem = problem;
        this.bestSolution = new int[problem.nUmpires][problem.nRounds];

        // Start lower bound calculation in a separate thread
        this.lowerBound = new LowerBound(problem);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long StartTime = System.currentTimeMillis();
            try {
                this.lowerBound.solve();
            } catch (InterruptedException ignored) {}
            System.out.println("Lower bound calculation time: " + (System.currentTimeMillis() - StartTime) / 1000.0 + "s");
            this.lowerBound.printLowerBounds();
        });
        executor.shutdown();
    }

    public void solve() throws InterruptedException {
        int[][] path = new int[problem.nUmpires][problem.nRounds];
        int[] numberOfUniqueVenuesVisited = new int[problem.nUmpires];
        boolean[][] visited = new boolean[problem.nUmpires][problem.nTeams];

        // init arrays
        for (int i = 0; i < problem.nUmpires; i++) {
            numberOfUniqueVenuesVisited[i] = 0;
            for (int j = 0; j < problem.nTeams; j++) {
                visited[i][j] = false;
            }
        }

        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        int umpire = 0;
        for (int team = 0; team < problem.nTeams; team++) {
            if (problem.opponents[0][team] < 0) {
                path[umpire][0] = -problem.opponents[0][team];
                numberOfUniqueVenuesVisited[umpire]++;
                visited[umpire][-problem.opponents[0][team] - 1] = true;
                umpire++;
            }
        }

        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        if (problem.enableMultiThreading) this.multiThreadedBranchAndBound(path, 0, 1, 0, visited, numberOfUniqueVenuesVisited);
        else this.branchAndBound(path, 0, 1, 0, visited, numberOfUniqueVenuesVisited);

        // Dirty fix to shut down the lower bound thread when it is in a big B&B SUB tree
        this.lowerBound.shutdown = true;

        System.out.println("Best solution: ");
        printPath(bestSolution);
        System.out.println("Distance: " + bestDistance);
        System.out.println("Number of nodes: " + nNodes);
        System.out.println("Time: " + (System.currentTimeMillis() - startTime) / 1_000.0 + "s");
    }

    public void printLowerBounds(){
        lowerBound.printLowerBounds();
    }

    public void printLowerBound(){
        var lb = lowerBound.getLowerBound2(0, problem.nRounds-1);
        System.out.println("LB: " + lb);
    }

    private void multiThreadedBranchAndBound(int[][] path, int umpire, int round, int currentCost, boolean[][] visited, int[] numberOfUniqueVenuesVisited) throws InterruptedException {
        List<Integer> feasibleAllocations = problem.getValidAllocations(path, umpire, round);
        ExecutorService executor = Executors.newFixedThreadPool(feasibleAllocations.size());
        int threadNr = 0;
        for (Integer allocation : feasibleAllocations) {
            threadNr++;
            int finalThreadNr = threadNr;
            executor.submit(() -> {
                System.out.println("Thread " + finalThreadNr + " started");

                // Each thread has its own copy of the path, visited and numberOfUniqueVenuesVisited arrays
                int[][] newPath = Arrays.stream(path).map(int[]::clone).toArray(int[][]::new);
                boolean[][] newVisited = Arrays.stream(visited).map(boolean[]::clone).toArray(boolean[][]::new);
                int[] newNumberOfUniqueVenuesVisited = numberOfUniqueVenuesVisited.clone();

                // Basically the same as in the branchAndBound method
                newPath[umpire][round] = allocation;
                boolean firstVisit = !newVisited[umpire][allocation - 1];
                if (firstVisit) {
                    newVisited[umpire][allocation - 1] = true;
                    newNumberOfUniqueVenuesVisited[umpire]++;
                }

                int prevHomeTeam = newPath[umpire][round - 1];
                int extraCost = this.problem.dist[prevHomeTeam - 1][allocation - 1];
                try {
                    if (umpire == this.problem.nUmpires - 1) {
                        this.branchAndBound(newPath, 0, round + 1, currentCost + extraCost, newVisited, newNumberOfUniqueVenuesVisited);
                    }
                    else this.branchAndBound(newPath, umpire + 1, round, currentCost + extraCost, newVisited, newNumberOfUniqueVenuesVisited);
                } catch (InterruptedException ignored) {}

                System.out.println("Thread " + finalThreadNr + " finished");
            });
        }

        // Wait for all threads to finish
        System.out.println("Waiting for B&B threads to finish");
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.DAYS);
        System.out.println("B&B executor shutdown");
    }

    private void branchAndBound(int[][] path, int umpire, int round, int currentCost, boolean[][] visited, int[] numberOfUniqueVenuesVisited) throws InterruptedException {
        if (round == this.problem.nRounds) {
            // Constructed a full feasible path
            if (currentCost < this.bestDistance.get()) {
                // The constructed path is better than the current best path! :)
                System.out.println("New BEST solution found in " + (System.currentTimeMillis() - startTime) / 60_000.0 + " min with cost " + currentCost + "! :)");
                // Copy solution
                this.bestDistance.set(currentCost);
                this.bestSolutionMutex.acquire();
                this.bestSolution = Arrays.stream(path).map(int[]::clone).toArray(int[][]::new);
                this.bestSolutionMutex.release();
            }
            return;
        }

        List<Integer> feasibleAllocations = problem.getValidAllocations(path, umpire, round);
        for (Integer allocation : feasibleAllocations) {
            path[umpire][round] = allocation;

            boolean firstVisit = !visited[umpire][allocation - 1];
            if (firstVisit) {
                visited[umpire][allocation - 1] = true;
                numberOfUniqueVenuesVisited[umpire]++;
            }

            int prevHomeTeam = path[umpire][round - 1];
            int extraCost = this.problem.dist[prevHomeTeam - 1][allocation - 1];
            if (!canPrune(path, umpire, round, currentCost + extraCost, numberOfUniqueVenuesVisited)) {
                this.nNodes.getAndIncrement();
                if (umpire == this.problem.nUmpires - 1) {
                    this.branchAndBound(path, 0, round + 1, currentCost + extraCost, visited, numberOfUniqueVenuesVisited);
                }
                else this.branchAndBound(path, umpire + 1, round, currentCost + extraCost, visited, numberOfUniqueVenuesVisited);
            }

            // Backtrack
            path[umpire][round] = 0;
            if (firstVisit) {
                visited[umpire][allocation - 1] = false;
                numberOfUniqueVenuesVisited[umpire]--;
            }
        }
    }

    private boolean canPrune(int[][] path, int umpire, int round, int currentCost, int[] numberOfUniqueVenuesVisited) throws InterruptedException {
        // Prune if the umpire can't visit all venues anymore
        if (this.problem.nTeams - numberOfUniqueVenuesVisited[umpire] >= this.problem.nRounds - round)
            return true;

        // Prune based on lower bound
        int lb = this.lowerBound.getLowerBound(round);
        if (currentCost + lb >= bestDistance.get())
            return true;

        // Prune based on partial matching
        if (this.canPruneOnPartialMatching(umpire, currentCost, lb, round)) {
            var m = this.problem.partialMatching.calculateDistance(path, round , problem.enableCaching);

            //m >= problem.maxValue heb ik toegevoegd omdat er deelproblemen/subgraphs zijn die resulteren in niet toegestane routes e.g. [[100  MaxValue],[150  MaxValue]] en zelfs [MaxValue]
            return m >= problem.maxValue || currentCost + lb + m >= bestDistance.get();
        }

        return false;
    }

    private boolean canPruneOnPartialMatching(int umpire, int currentCost, int lb, int round) {
        return  this.problem.enablePartialMatching &&
                umpire < this.problem.nUmpires - 1 &&
                currentCost + lb + this.lowerBound.getLowerBound2(round - 1, round) >= this.bestDistance.get();
    }

    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }
}