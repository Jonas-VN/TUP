package tup;

import java.util.Arrays;
import java.util.List;

public class BranchAndBound {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    private int[][] bestSolution;
    private int[] numberOfUniqueVenuesVisited;
    private boolean[][] visited;
    private LowerBound lowerBound;

    public BranchAndBound(Problem problem) {
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

        lowerBound = new LowerBound(problem);
        lowerBound.solve();
        System.out.println("Lower bound finished");
    }

    public void solve() {
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
        path = this.branchAndBound(path, 0, 1, 0);
        System.out.println("Best solution: ");
        printPath(bestSolution);
        System.out.println("Distance: " + bestDistance);
    }

    private int[][] branchAndBound(int[][] path, int umpire, int round, int currentCost) {
        if (round == this.problem.nRounds ) {
            // Constructed a full feasible path
            if (currentCost < bestDistance) {
                // The constructed path is better than the current best path! :)
                System.out.println("New BEST solution found with cost " + currentCost + "! :)");
                printPath(path);
                // Copy solution
                bestDistance = currentCost;
                for (int _umpire = 0; _umpire < this.problem.nUmpires; _umpire++) {
                    System.arraycopy(path[_umpire], 0, bestSolution[_umpire], 0, this.problem.nRounds);
                }
            }
            return path;
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
                //if (currentCost + extraCost + lowerBound.getLowerBound(round) >= bestDistance) System.out.println("Pruned by lower bound!");
                if (problem.nTeams - numberOfUniqueVenuesVisited[umpire] < problem.nRounds - round||
                        currentCost + extraCost + lowerBound.getLowerBound(round) >= bestDistance) {
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
        return path;
    }

    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }
}