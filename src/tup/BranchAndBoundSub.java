package tup;

import java.util.ArrayList;
import java.util.List;

public class BranchAndBoundSub {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    private int firstRound;
    private int lastRound;
    private int[][] bestSolution;
    private LowerBound lowerbound;


    public BranchAndBoundSub(Problem problem, int firstRound, int lastRound, LowerBound lowerbound) {
        this.problem = problem;
        this.firstRound = firstRound;
        this.lastRound = lastRound;
        bestSolution = new int[problem.nUmpires][lastRound-firstRound+1];
        this.lowerbound = lowerbound;
    }

    public int solve() {
        int[][] path = new int[problem.nUmpires][lastRound-firstRound+1];
        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        int umpire = 0;
        for (int team = 0; team < problem.nTeams; team++) {
            if (problem.opponents[this.firstRound-1][team] < 0) {
                path[umpire++][0] = -problem.opponents[this.firstRound-1][team];
            }
        }

        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        this.branchAndBound(path, 0, firstRound,0);
        return bestDistance;
    }

    private void branchAndBound(int[][] path, int umpire, int round, int currentCost) {
        if (lowerbound.shutdown) return;
        if (round == lastRound ) {
            // Constructed a full feasible path
            if (currentCost < bestDistance) {
                bestDistance = currentCost;
                for (int _umpire = 0; _umpire < this.problem.nUmpires; _umpire++) {
                    System.arraycopy(path[_umpire], 0, bestSolution[_umpire], 0, lastRound-firstRound+1);
                }
            }
            return;
        }

        List<Integer> feasibleAllocations = this.getValidAllocations(path, umpire, round);
        if (!feasibleAllocations.isEmpty()) {
            for (Integer allocation : feasibleAllocations) {
                path[umpire][round-firstRound+1] = allocation;

                int prevHomeTeam = path[umpire][round-firstRound+1 - 1];
                int extraCost = this.problem.dist[prevHomeTeam - 1][allocation - 1];
                if (currentCost + extraCost + lowerbound.getLowerBound(round + 1, lastRound) < bestDistance) {
                    if (umpire == this.problem.nUmpires - 1) {
                        this.branchAndBound(path, 0, round + 1, currentCost + extraCost);
                    }
                    else this.branchAndBound(path, umpire + 1, round, currentCost + extraCost);
                }

                // Backtrack
                path[umpire][round-firstRound+1] = 0;

            }
        }
    }

    public List<Integer> getValidAllocations(int[][] assignments, int umpire, int round) {
        // feasibleAllocations contains all feasible home teams (NO INDEXES)
        List<Integer> feasibleAllocations = new ArrayList<>();
        List<Integer> previousLocations = getPreviousLocations(assignments, round, umpire);
        List<Integer> previousTeams = getPreviousTeams(assignments, round, umpire);
        for (int i = 0; i < problem.nTeams; i++) {
            if (problem.opponents[round][i] < 0 ) {
                int homeTeam = -problem.opponents[round][i];
                int awayTeam = i+1;

                if (!previousTeams.contains(awayTeam) && !previousTeams.contains(homeTeam) && !previousLocations.contains(homeTeam))
                    feasibleAllocations.add(homeTeam);
            }
        }

        List<Integer> alreadyUsedThisRound = new ArrayList<>();
        for (int i = 0; i < problem.nUmpires; i++){
            if (assignments[i][round-firstRound+1] != 0){
                alreadyUsedThisRound.add(assignments[i][round-firstRound+1]);
            }
        }
        feasibleAllocations.removeIf(alreadyUsedThisRound::contains);

        // Sort on distance to previous location
        feasibleAllocations.sort((a, b) -> {
            int prevLocation = assignments[umpire][round - firstRound +1 - 1];
            int aDistance = problem.dist[prevLocation - 1][a - 1];
            int bDistance = problem.dist[prevLocation - 1][b - 1];
            return Integer.compare(aDistance, bDistance);
        });

        return feasibleAllocations;
    }

    public List<Integer> getPreviousLocations(int [][] assignments, int round, int umpire) {
        List<Integer> previousLocations = new ArrayList<>();
        for (int i = 1; i < problem.q1 && round - i -firstRound+1 >= 0; i++) {
            int homeTeam = assignments[umpire][round - i - firstRound+1];
            previousLocations.add(homeTeam);
        }
        return previousLocations;
    }

    private List<Integer> getPreviousTeams(int [][] assignments, int round, int umpire) {
        List<Integer> previousTeams = new ArrayList<>();
        for (int i = 1; i < problem.q2 && round - i - firstRound+1 >= 0; i++) {
            int homeTeam = assignments[umpire][round - i-firstRound+1];
            int awayTeam = problem.opponents[round - i][homeTeam - 1];
            previousTeams.add(homeTeam);
            previousTeams.add(awayTeam);
        }
        return previousTeams;
    }
}
