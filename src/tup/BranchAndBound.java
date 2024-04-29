package tup;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BranchAndBound {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    public int[][] bestSolution;
    private int[] numberOfUniqueVenuesVisited;
    private boolean[][] visited;
    private LowerBound lowerBound;
    private long nNodes = 0;

    public BranchAndBound(Problem problem) throws InterruptedException {
        this.problem = problem;
        bestSolution = new int[problem.nUmpires][problem.nRounds];
        numberOfUniqueVenuesVisited = new int[problem.nUmpires];
        visited = new boolean[problem.nUmpires][problem.nTeams];

        // init arrays
        for (int i = 0; i < problem.nUmpires; i++) {
            numberOfUniqueVenuesVisited[i] = 0;
            for (int j = 0; j < problem.nTeams; j++) {
                visited[i][j] = false;
            }
        }

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
        int[][] path = new int[problem.nUmpires][problem.nRounds];

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
        this.branchAndBound(path, 0, 1, 0);
        this.lowerBound.shutdown = true;
        System.out.println("Best solution: ");
        printPath(bestSolution);
        System.out.println("Distance: " + bestDistance);
        System.out.println("Number of nodes: " + nNodes);
    }

    private void branchAndBound(int[][] path, int umpire, int round, int currentCost) throws InterruptedException {
        if (round == this.problem.nRounds) {
            // Constructed a full feasible path
            if (currentCost < bestDistance) {
                // The constructed path is better than the current best path! :)
                System.out.println("New BEST solution found with cost " + currentCost + "! :)");
                //printPath(path);
                // Copy solution
                bestDistance = currentCost;
                for (int _umpire = 0; _umpire < this.problem.nUmpires; _umpire++) {
                    System.arraycopy(path[_umpire], 0, bestSolution[_umpire], 0, this.problem.nRounds);
                }
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
                if (problem.nTeams - numberOfUniqueVenuesVisited[umpire] < problem.nRounds - round && currentCost + extraCost + lowerBound.getLowerBound(round) < bestDistance) {
                    nNodes++;
                    if (umpire == this.problem.nUmpires - 1) {
                        this.branchAndBound(path, 0, round + 1, currentCost + extraCost);
                    }
                    else this.branchAndBound(path, umpire + 1, round, currentCost + extraCost);
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

    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }
}