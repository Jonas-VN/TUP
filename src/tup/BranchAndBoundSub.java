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

    /**
     * Branch and bound on a subproblem
     * @param problem
     * @param firstRound roundnr of the first round (not ix)
     * @param lastRound roundnr of the last round (not ix)
     * @param lowerbound lower bound instance
     */
    public BranchAndBoundSub(Problem problem, int firstRound, int lastRound, LowerBound lowerbound) {
        this.problem = problem;
        this.firstRound = firstRound;
        this.lastRound = lastRound;
        bestSolution = new int[problem.nUmpires][lastRound-firstRound+1];
        this.lowerbound = lowerbound;
    }

    public int solve() throws InterruptedException {
        int[][] path = new int[problem.nUmpires][lastRound-firstRound+1];
        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        int umpire = 0;
        for (int team = 0; team < problem.nTeams; team++) {
            if (problem.opponents[this.firstRound-1][team] < 0) {
                path[umpire++][0] = -problem.opponents[this.firstRound-1][team];
            }
        }
        //System.out.println("Starting branch and bound from round " + firstRound + " to " + lastRound);
        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        this.branchAndBound(path, 0, firstRound,0);
        return bestDistance;
    }

    /**
     *
     * @param path
     * @param umpire
     * @param round nr of the round not the index and it is the round we assigning umpires to -> it is if you consider it and ix
     * @param currentCost
     * @throws InterruptedException
     */
    private void branchAndBound(int[][] path, int umpire, int round, int currentCost) throws InterruptedException {
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

                if (!canPrune(path, round, umpire,currentCost + extraCost)) {
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

    /**
     *
     * @param path
     * @param round the roundnr (not the index) of the round we are assigning umpire to
     * @param currentCost
     * @return
     * @throws InterruptedException
     */
    private boolean canPrune(int[][] path, int round, int umpire ,int currentCost) throws InterruptedException {

        int lb = this.lowerbound.getLowerBound(round+1, lastRound); // deze overload vraagt geen indexen maar effectieve nummers
        //var currentRoundIx = round - 1;
        //int lb = this.lowerbound.getLowerBound(currentRoundIx + 1+1, lastRound-1+1); // minimum cost vanaf hier tot de laatste ronde
        if (currentCost + lb >= bestDistance) {
            //System.out.println("Pruned op LB");
            return true;
        }

        // Partial matching
        if (problem.enableLowerBoundPartialMatching && umpire < problem.nUmpires - 1){

            // Create the new 2D array because Partial matching needs all the rounds
            int[][] fullPath = new int[this.problem.nUmpires][this.problem.nRounds];
            // Copy elements from the original array to the new array starting at a certain index
            int startIdx = firstRound - 1;
            for (int i = 0; i < path.length; i++) {
                // Copy each sub-array to the new sub-array
                System.arraycopy(path[i], 0, fullPath[i], startIdx, path[i].length);
            }

            var m = this.problem.partialMatching.calculateDistance(fullPath, round, problem.enableLowerBoundCaching); // partial matching needs the index of round we are assigning umpire to!
            //System.out.println("Matching cost: " + m);
            if (m >= problem.maxValue || currentCost + lb + m >= bestDistance) {
                //System.out.println("Pruned op matching");
                return true;
            }
        }
        return false;
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
