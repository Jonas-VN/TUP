package tup;

import java.util.List;

public class BranchAndBoundSub {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    private int firstRound;
    private int lastRound;
    private int[][] bestSolution;


    public BranchAndBoundSub(Problem problem, int firstRound, int lastRound) {
        this.problem = problem;
        this.firstRound = firstRound;
        this.lastRound = lastRound;
        bestSolution = new int[problem.nUmpires][lastRound-firstRound+1];

    }

    public int solve() {
        int[][] path = new int[problem.nUmpires][lastRound-firstRound+1];
        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        int umpire = 0;
        for (int team = 0; team < problem.nTeams; team++) {
            if (problem.opponents[0][team] < 0) {
                path[umpire++][0] = -problem.opponents[0][team];
            }
        }

        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        path = this.branchAndBound(path, 0, 1);
        System.out.println("Best solution: ");
        System.out.println("Distance: " + bestDistance);
        return bestDistance;
    }

    private int[][] branchAndBound(int[][] path, int umpire, int round) {
        if (round > lastRound) {
            // Constructed a full feasible path
            int cost = calculateTotalDistance(path);
            if (cost < bestDistance) {
                System.out.println("New BEST solution found with cost " + cost + "! :)");
                bestDistance = cost;
                for (int _umpire = 0; _umpire < this.problem.nUmpires; _umpire++) {
                    System.arraycopy(path[_umpire], 0, bestSolution[_umpire], 0, lastRound-firstRound+1);
                }
            }
            return path;
        }
        List<Integer> feasibleAllocations = this.problem.getValidAllocations(path, umpire, round);
        if (!feasibleAllocations.isEmpty()) {
            for (Integer allocation : feasibleAllocations) {
                path[umpire][round - firstRound] = allocation;
                if (umpire == this.problem.nUmpires - 1) {
                    this.branchAndBound(path, 0, round + 1);
                } else {
                    this.branchAndBound(path, umpire + 1, round);
                }
            }
        }
        path[umpire][round - firstRound] = 0;
        return path;
    }

    private int calculateTotalDistance(int[][] path) {
        int totalDistance = 0;
        for (int umpire = 0; umpire < problem.nUmpires; umpire++) {
            for (int round = 1; round < problem.nRounds; round++) {
                int prevRound = round - 1;
                int prevHomeTeam = path[umpire][prevRound];

                int currHomeTeam = path[umpire][round];
                totalDistance += problem.dist[prevHomeTeam - 1][currHomeTeam - 1];
            }
        }
        return totalDistance;
    }

}
