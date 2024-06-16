package tup;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class BranchAndBound {
    private Problem problem;
    private AtomicInteger bestDistance = new AtomicInteger(Integer.MAX_VALUE);
    public int[][] bestSolution;
    Semaphore bestSolutionMutex = new Semaphore(1);
    private LowerBound lowerBound;
    private long nNodes = 0;
    private long startTime = System.currentTimeMillis();

    public BranchAndBound(Problem problem) throws InterruptedException {
        this.problem = problem;
        bestSolution = new int[problem.nUmpires][problem.nRounds];

        // Start lower bound calculation in a separate thread
        lowerBound = new LowerBound(problem);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            long StartTime = System.currentTimeMillis();
            try {
                lowerBound.solve();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Lower bound calculation time: " + (System.currentTimeMillis() - StartTime) / 1000.0 + "s");
        });
        executor.shutdown();
    }

    public void solve() throws InterruptedException {
        startTime = System.currentTimeMillis();
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
                visited[umpire++][-problem.opponents[0][team] - 1] = true;
            }
        }

        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        if (!problem.enableMultiThreading) {
            this.branchAndBound(path, 0, 1, 0, visited, numberOfUniqueVenuesVisited);
        }
        else {
            this.multiThreadedBranchAndBound(path, 0, 1, 0, visited, numberOfUniqueVenuesVisited);
        }
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
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Backtrack
                newPath[umpire][round] = 0;
                if (firstVisit) {
                    newVisited[umpire][allocation - 1] = false;
                    newNumberOfUniqueVenuesVisited[umpire]--;
                }

                System.out.println("Thread " + finalThreadNr + " finished");
            });
        }

        // Wait for all threads to finish
        System.out.println("Waiting for executor to finish");
        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.DAYS);
        System.out.println("Executor shutdown");

    }

    private void branchAndBound(int[][] path, int umpire, int round, int currentCost, boolean[][] visited, int[] numberOfUniqueVenuesVisited) throws InterruptedException {
        if (bestDistance.get() == lowerBound.getLowerBound(0)) {
            System.out.println("Returning because lower bound is equal to best distance");
            return;
        }

        if (round == this.problem.nRounds) {
            // Constructed a full feasible path
            if (currentCost < bestDistance.get()) {
                // The constructed path is better than the current best path! :)
                System.out.println("New BEST solution found in " + (System.currentTimeMillis() - startTime) / 60_000.0 + " min with cost " + currentCost + "! :)");
                //printPath(path);
                // Copy solution
                bestDistance.set(currentCost);
                bestSolutionMutex.acquire();
                for (int _umpire = 0; _umpire < this.problem.nUmpires; _umpire++) {
                    System.arraycopy(path[_umpire], 0, bestSolution[_umpire], 0, this.problem.nRounds);
                }
                bestSolutionMutex.release();
            }
            return;
        }

        List<Integer> feasibleAllocations = problem.getValidAllocations(path, umpire, round);
        if (!feasibleAllocations.isEmpty()) {
            for (Integer allocation : feasibleAllocations) {
                path[umpire][round] = allocation;

                boolean firstVisit = !visited[umpire][allocation - 1];
                if (firstVisit) {
                    visited[umpire][allocation - 1] = true;
                    numberOfUniqueVenuesVisited[umpire]++;
                }

                int prevHomeTeam = path[umpire][round - 1];
                int extraCost = this.problem.dist[prevHomeTeam - 1][allocation - 1];
                //if (problem.nTeams - numberOfUniqueVenuesVisited[umpire] < problem.nRounds - round && currentCost + extraCost + lowerBound.getLowerBound(round) < bestDistance) {
                if (!canPrune(path, umpire, round, currentCost + extraCost, numberOfUniqueVenuesVisited)) {
                    nNodes++;
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
    }


    private boolean canPrune(int[][] path, int umpire, int round, int currentCost, int[] numberOfUniqueVenuesVisited) throws InterruptedException {

        if (problem.nTeams - numberOfUniqueVenuesVisited[umpire] >= problem.nRounds - round) return true;

        int lb = this.lowerBound.getLowerBound(round);
        if (currentCost + lb >= bestDistance.get()) {
            //System.out.println("Pruned op LB");
            return true;
        }

        // TODO alles was juist behalve 10C. Die is nu ook juist door die laatste voorwaarde erbij te steken
        if (problem.enablePartialMatching && umpire < problem.nUmpires - 1 && currentCost + lb + lowerBound.getLowerBound2(round - 1, round) >= bestDistance.get()){
            // dit werkt correct wanneer we niet memoizeren
            //var m = this.problem.partialMatching.calculateDistanceGil(path, round, enableCaching);
            var m = this.problem.partialMatching.calculateDistance(path, round , problem.enableCaching);

            //System.out.println("Matching cost: " + m);
            //m >= problem.maxValue heb ik toegevoegd omdat er deelproblemen/subgraphs zijn die resulteren in niet toegestane routes e.g. [[100  MaxValue],[150  MaxValue]] en zelfs [MaxValue]
            if (m >= problem.maxValue || currentCost + lb + m >= bestDistance.get()) {
                //System.out.println("Pruned op matching");
                return true;
            }
        }

        return false;
    }

    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }

}