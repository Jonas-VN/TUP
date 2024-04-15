package tup;

import java.util.Arrays;
import java.util.List;

public class BranchAndBound {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    private int[][] bestSolution;

    public BranchAndBound(Problem problem) {
        this.problem = problem;
        bestSolution = new int[problem.nUmpires][problem.nRounds];
    }

    public void solve() {
        int[][] path = new int[problem.nUmpires][problem.nRounds];

        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        int i = -1;
        for (int team = 0; team < problem.nTeams; team++) {
            if (problem.opponents[0][team] > 0) {
                path[++i][0] = problem.opponents[0][team];
            }
            //path[i][0] = problem.opponents[0][i]; //note: in debuggen krijgt referee '0' wedstrijd '0' toegewezen, kan misschien verwarring brengen :)
        }
        printPath(path);
        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        // branchAndBound(path, 0, 0);
        //path = problem.assignUmpires();
        path = this.branchAndBound(path, 0, 1);
        System.out.println("Best solution: ");
        printPath(bestSolution);
        System.out.println("Distance: " + bestDistance);
    }

    private int[][] branchAndBound(int[][] path, int umpire, int round) {
        //System.out.printf("Umpire: %d\t Round: %d\n", umpire, round);
        if (round == this.problem.nRounds) {
            // Constructed a full feasible path
            int cost = calculateTotalDistance(path);
            if (cost < bestDistance && testVenueFeasibility(path)) {
                System.out.println("New BEST solution found with cost " + cost + "! :)");
                printPath(path);

                bestDistance = cost;
                for (int _umpire = 0; _umpire < this.problem.nUmpires; _umpire++) {
                    if (this.problem.nRounds >= 0)
                        System.arraycopy(path[_umpire], 0, bestSolution[_umpire], 0, this.problem.nRounds);
                }
            }
            return path;
        }
        List<Integer> feasibleAllocations = this.problem.getValidAllocations(path, umpire, round);
        if (!feasibleAllocations.isEmpty()) {
            for (Integer allocation : feasibleAllocations) {
                path[umpire][round] = allocation;
                if (umpire == this.problem.nUmpires - 1) {
                    //printPath(path);
                    this.branchAndBound(path, 0, round + 1);
                }
                else this.branchAndBound(path, umpire + 1, round);
            }
        }
        path[umpire][round] = 0;
        return path;
    }

    boolean testVenueFeasibility(int[][] path) {
        for (int umpire = 0; umpire < this.problem.nUmpires; umpire++) {
            boolean[] visited = new boolean[this.problem.nTeams];
            Arrays.fill(visited, false);
            for (int round = 0; round < this.problem.nRounds; round++) {
                int venue = path[umpire][round] - 1;
                visited[venue] = true;
            }

            for (int i = 0; i < this.problem.nTeams; i++) {
                if (!visited[i]) return false;
            }
        }
        return true;
    }


    private int calculateTotalDistance(int[][] path) {
        int totalDistance = 0;
        for (int umpire = 0; umpire < problem.nUmpires; umpire++) {
            for (int round = 1; round < problem.nRounds; round++) {
                int prevLoc = path[umpire][round - 1] - 1;
                int currLoc = path[umpire][round] - 1;
                totalDistance += problem.dist[prevLoc][currLoc];
            }
        }
        return totalDistance;
    }



    public String getBestSolution() {
        return Arrays.deepToString(bestSolution);
    }


    public int getBestDistance() {
        return bestDistance;
    }





    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }
}