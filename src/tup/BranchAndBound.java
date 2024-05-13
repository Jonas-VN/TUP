package tup;

import java.util.*;
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
    private long startTime = System.currentTimeMillis();

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
        startTime = System.currentTimeMillis();
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
        System.out.println("Time: " + (System.currentTimeMillis() - startTime) / 60_000.0 + "min");
    }

    private void branchAndBound(int[][] path, int umpire, int round, int currentCost) throws InterruptedException {
        if (round == this.problem.nRounds) {
            // Constructed a full feasible path
            if (currentCost < bestDistance) {
                // The constructed path is better than the current best path! :)
                //System.out.println("New BEST solution found in " + (System.currentTimeMillis() - startTime) / 60_000.0 + " min with cost " + currentCost + "! :)");
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
//                int[][] subgraph = generateSubgraph(path, round, allocation);
//                if (subgraph != null) {
//                   int m = solveMatchingProblem(subgraph);
//               }


                path[umpire][round] = allocation;
                boolean firstVisit = !visited[umpire][allocation - 1];
                if (firstVisit) {
                    visited[umpire][allocation - 1] = true;
                    numberOfUniqueVenuesVisited[umpire]++;
                }

                int prevHomeTeam = path[umpire][round - 1];
                int extraCost = this.problem.dist[prevHomeTeam - 1][allocation - 1];
                //if (problem.nTeams - numberOfUniqueVenuesVisited[umpire] < problem.nRounds - round && currentCost + extraCost + lowerBound.getLowerBound(round) < bestDistance) {
                if (!canPrune(path, umpire, round, allocation, currentCost + extraCost)) {
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

    private int solveMatchingProblem(int[][] subgraph){
        Hungarian hungarian = new Hungarian();
        hungarian.assignmentProblem(subgraph);
        int[] solution = hungarian.xy;

        int cost = 0;
        for (int i = 0; i < solution.length; i++) {
            cost += subgraph[i][solution[i]];
        }

        return cost;
    }


    private int[][] generateSubgraph(int[][] assignments, int round, int allocation) {
        List<Integer> visitedTeams = new ArrayList<>();//aantal teams gevisited in deze ronde
        List<Integer> previousTeams = new ArrayList<>();//teams visited in de vorige ronde
        List<Integer> currentRoundTeams = new ArrayList<>();//overige nog te bezoeken teams in deze ronde
        for (int i = 0; i < problem.nUmpires; i++) {
            if (assignments[i][round] == 0) {
            } else {
                visitedTeams.add(assignments[i][round]);
            }
        }

       // System.out.println("Generating subgraph for round " + round +1);
        if (visitedTeams.isEmpty()) {
            return null;
        }
        for (int i = visitedTeams.size()+1; i < problem.nUmpires; i++) {
           previousTeams.add(assignments[i][round-1]);

        }
        for (int i = 0; i < problem.nTeams; i++) {
            if (problem.opponents[round][i] < 0) {
                int homeTeam = -problem.opponents[round][i];
                int awayTeam = i + 1;
                if (!visitedTeams.contains(homeTeam) && !visitedTeams.contains(awayTeam) && homeTeam != allocation) {
                    currentRoundTeams.add(homeTeam);
                }
            }
        }

        int size = Math.max(previousTeams.size(), currentRoundTeams.size());
        int[][] subgraph = new int[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i < previousTeams.size() && j < currentRoundTeams.size()) {
                    // Vul de kosten in voor de werkelijke teams
                    subgraph[i][j] = problem.dist[previousTeams.get(i)-1][currentRoundTeams.get(j)-1];
                } else {
                    // Vul de kosten in voor de dummy-knopen
                    subgraph[i][j] = Integer.MAX_VALUE;
                }
            }
        }



        return subgraph;
    }
    private boolean canPrune(int[][] path, int umpire, int round, int allocation, int currentCost) throws InterruptedException {
        // Controleer of het team al is toegewezen in deze ronde
//        for (int i = 0; i < umpire; i++) {
//            if (path[i][round] == allocation) {
//                return true;
//            }
//        }
        if (problem.nTeams - numberOfUniqueVenuesVisited[umpire] >= problem.nRounds - round) return true;


        if (currentCost + lowerBound.getLowerBound(round) >= bestDistance) {
            //System.out.println("Pruned op LB");
            return true;
        }

//        int[][] subgraph = generateSubgraph(path, round, allocation);
//        if (subgraph != null) {
//            int m = solveMatchingProblem(subgraph);
//            if (currentCost + lowerBound.getLowerBound(round) + m >= bestDistance) {
//                System.out.println("Pruned op matching");
//                return true;
//            }
//        }



        return false;
    }




    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }

}