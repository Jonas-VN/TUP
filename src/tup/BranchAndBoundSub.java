package tup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class BranchAndBoundSub {
    private final int lastRound;
    private final int firstRound;
    private final Problem problem;
    private final LowerBound lowerbound;
    private final AtomicInteger bestDistance = new AtomicInteger(Integer.MAX_VALUE);

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

        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        if (problem.enableMultiThreadingSub) this.multiThreadedBranchAndBound(path, 0, firstRound,0);
        else this.branchAndBound(path, 0, firstRound,0);

        return bestDistance.get();
    }

    private void multiThreadedBranchAndBound(int[][] path, int umpire, int round, int currentCost) throws InterruptedException {
        List<Integer> feasibleAllocations = this.getValidAllocations(path, umpire, round);
        ExecutorService executor = Executors.newFixedThreadPool(feasibleAllocations.size());
        for (Integer allocation : feasibleAllocations) {
            executor.submit(() -> {
                // Each thread has its own copy of the path arrays
                int[][] newPath = Arrays.stream(path).map(int[]::clone).toArray(int[][]::new);
                newPath[umpire][round - firstRound + 1] = allocation;

                int prevHomeTeam = path[umpire][round - firstRound];
                int extraCost = this.problem.dist[prevHomeTeam - 1][allocation - 1];

                if (!this.canPrune(newPath, round, umpire,currentCost + extraCost)) {
                    try {
                        if (umpire == this.problem.nUmpires - 1) {
                            this.branchAndBound(newPath, 0, round + 1, currentCost + extraCost);
                        }
                        else this.branchAndBound(newPath, umpire + 1, round, currentCost + extraCost);
                    } catch (InterruptedException ignored) {}
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.DAYS);
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
        // Dirty fix to shut down the lower bound thread when it is in a big B&B SUB tree
        if (this.lowerbound.shutdown) return;

        // Constructed a full feasible path
        if (round == lastRound) {
            // The constructed path is better than the current best path! :)
            if (currentCost < bestDistance.get()) bestDistance.set(currentCost);
            return;
        }

        List<Integer> feasibleAllocations = this.getValidAllocations(path, umpire, round);
        if (!feasibleAllocations.isEmpty()) {
            for (Integer allocation : feasibleAllocations) {
                path[umpire][round - firstRound + 1] = allocation;

                int prevHomeTeam = path[umpire][round - firstRound];
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
    private boolean canPrune(int[][] path, int round, int umpire ,int currentCost) {
        // Prune based on lower bound
        int lb = this.lowerbound.getLowerBound(round + 1, this.lastRound);
        if (currentCost + lb >= this.bestDistance.get()) {
            return true;
        }

        // Prune based on partial matching
        if (this.problem.enableLowerBoundPartialMatching && umpire < this.problem.nUmpires - 1){
            // Create the new 2D array because Partial matching needs all the rounds
            int[][] fullPath = new int[this.problem.nUmpires][this.problem.nRounds];
            // Copy elements from the original array to the new array starting at a certain index
            int startIdx = firstRound - 1;
            for (int i = 0; i < path.length; i++) {
                // Copy each sub-array to the new sub-array
                System.arraycopy(path[i], 0, fullPath[i], startIdx, path[i].length);
            }

            var m = this.problem.partialMatching.calculateDistance(fullPath, round, problem.enableLowerBoundCaching);
            return m >= problem.maxValue || currentCost + lb + m >= bestDistance.get();
        }

        return false;
    }

    public List<Integer> getValidAllocations(int[][] assignments, int umpire, int round) {
        // feasibleAllocations contains all feasible home teams (NO INDEXES)
        List<Integer> feasibleAllocations = new ArrayList<>();
        List<Integer> previousLocations = getPreviousLocations(assignments, round, umpire);
        List<Integer> previousTeams = getPreviousTeams(assignments, round, umpire);
        for (int i = 0; i < this.problem.nTeams; i++) {
            if (this.problem.opponents[round][i] < 0 ) {
                int homeTeam = -this.problem.opponents[round][i];
                int awayTeam = i + 1;

                if (!previousTeams.contains(awayTeam) && !previousTeams.contains(homeTeam) && !previousLocations.contains(homeTeam))
                    feasibleAllocations.add(homeTeam);
            }
        }

        List<Integer> alreadyUsedThisRound = new ArrayList<>();
        for (int i = 0; i < this.problem.nUmpires; i++){
            if (assignments[i][round - this.firstRound + 1] != 0){
                alreadyUsedThisRound.add(assignments[i][round - this.firstRound + 1]);
            }
        }
        feasibleAllocations.removeIf(alreadyUsedThisRound::contains);

        // Sort on distance to previous location
        feasibleAllocations.sort((a, b) -> {
            int prevLocation = assignments[umpire][round - this.firstRound];
            int aDistance = problem.dist[prevLocation - 1][a - 1];
            int bDistance = problem.dist[prevLocation - 1][b - 1];
            return Integer.compare(aDistance, bDistance);
        });

        return feasibleAllocations;
    }

    public List<Integer> getPreviousLocations(int [][] assignments, int round, int umpire) {
        List<Integer> previousLocations = new ArrayList<>();
        for (int i = 1; i < problem.q1 && round - i - firstRound + 1 >= 0; i++) {
            int homeTeam = assignments[umpire][round - i - firstRound + 1];
            previousLocations.add(homeTeam);
        }
        return previousLocations;
    }

    private List<Integer> getPreviousTeams(int [][] assignments, int round, int umpire) {
        List<Integer> previousTeams = new ArrayList<>();
        for (int i = 1; i < problem.q2 && round - i - firstRound + 1 >= 0; i++) {
            int homeTeam = assignments[umpire][round - i - firstRound + 1];
            int awayTeam = problem.opponents[round - i][homeTeam - 1];
            previousTeams.add(homeTeam);
            previousTeams.add(awayTeam);
        }
        return previousTeams;
    }
}