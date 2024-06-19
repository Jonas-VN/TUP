package tup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class PartialMatching {

    public final Problem problem;

    private static final int HASH_SIZE_LIMIT = ( int ) 5e8;

    private AtomicLong counter = new AtomicLong(0);
    private Map<Long, Integer> partialMatchingResults = new ConcurrentHashMap<>();

    public PartialMatching(Problem problem) {
        this.problem = problem;
    }


    private long getHashCode(int[][] assignments, int round){

        int prevRound = round - 1;

        // used and usedPrev keep, respectively, the unconnected games between the current and the next round
        boolean used[] = new boolean[problem.nUmpires];
        boolean usedPrev[] = new boolean[problem.nUmpires];

        for (int i = 0; i < problem.nUmpires; i++) {
            int gameNrInRound = 1;
            int gameNrInPrevRound = 1;
            int teamPrevRound = assignments[i][prevRound];
            int teamCurrentRound = assignments[i][round];

            // only handle the assigned teams
            if (teamCurrentRound > 0) {
                gameNrInRound = problem.opponentsGames[round][teamCurrentRound-1];
                gameNrInPrevRound = problem.opponentsGames[prevRound][teamPrevRound-1];
                usedPrev[gameNrInPrevRound-1] = true;
                used[gameNrInRound-1] = true;
            }
        }

        long hashCode = 17;
        for (int i = 0; i < used.length; i++) {
            hashCode = 31 * hashCode + (usedPrev[i]? 1 : 0);
            hashCode = 31 * hashCode + (used[i]? 2 : 0);
        }
        hashCode = 31 * hashCode + round;

        //System.out.println("roundix " + round + ": " + hashCode);

        return hashCode;
    }



    public int calculateDistance(int[][] assignments, int round, boolean cache) {

        long hashCode = 0;
        if (cache) {
            hashCode = getHashCode(assignments, round);

            if (partialMatchingResults.containsKey(hashCode)) {
                //System.out.println("Partial match memoization hit on key: " + key + " : " + partialMatchingResults.get(key));
                return partialMatchingResults.get(hashCode);
            } else if (counter.get() >= HASH_SIZE_LIMIT) {
                return 0;
            }
        }

        List<Integer> currentRoundVisitedTeams = new ArrayList<>();// teams gevisited in deze ronde
        List<Integer> previousRoundHomeTeams = new ArrayList<>(); //teams visited in previous round for which no umpire has been assigned yet in this round

        for (int i = 0; i < problem.nUmpires; i++) {
            if (assignments[i][round] == 0) {
                previousRoundHomeTeams.add(assignments[i][round-1]);
            }
            else {
                currentRoundVisitedTeams.add(assignments[i][round]);
            }
        }


        List<Integer> currentRoundHomeTeams = new ArrayList<>(); //teams to visite in this round
        List<Integer> currentRoundAwayTeams = new ArrayList<>(); //teams to visite in this round

        for (int i = 0; i < problem.nTeams; i++) {
            if (problem.opponents[round][i] < 0) {
                int homeTeam = -problem.opponents[round][i];
                int awayTeam = i + 1;
                if (!currentRoundVisitedTeams.contains(homeTeam)) {
                    currentRoundHomeTeams.add(homeTeam);
                    currentRoundAwayTeams.add(awayTeam);
                }
            }
        }

        int size = previousRoundHomeTeams.size();
        int[][] subgraph = new int[size][size];

        for (int i = 0; i < size; i++) {
            int prevRoundHomeTeam = previousRoundHomeTeams.get(i);
            int prevRoundAwayTeam = problem.opponents[round-1][prevRoundHomeTeam-1];

            for (int j = 0; j < size; j++) {
                // Vul de kosten in voor de werkelijke teams
                int currentRoundHomeTeam = currentRoundHomeTeams.get(j);
                int currentRoundAwayTeam = currentRoundAwayTeams.get(j);

                if (problem.q2 > 1 && (prevRoundHomeTeam == currentRoundHomeTeam ||
                        prevRoundAwayTeam == currentRoundHomeTeam ||
                        prevRoundHomeTeam == currentRoundAwayTeam ||
                        prevRoundAwayTeam == currentRoundAwayTeam)
                ) {
                    subgraph[i][j] = problem.maxValue; //Integer.MAX_VALUE;
                } else if (problem.q1 > 1 && prevRoundHomeTeam == currentRoundHomeTeam) {
                    subgraph[i][j] = problem.maxValue; //Integer.MAX_VALUE;
                } else {
                    subgraph[i][j] = problem.dist[previousRoundHomeTeams.get(i) - 1][currentRoundHomeTeams.get(j) - 1];
                }
            }
        }

        int m = 0;
        try{
            // solve with hungarian algorithm
            m = solveMatchingProblem(subgraph);
        } catch (Exception e) {
            //throw new RuntimeException(e);
            System.out.println("Munkres algorithm crashed");
            m = 0; // this is as if we don't do a partial matching for this case
        }

        // is niet langer nodig denk ik omdat we niet langer Integer.MaxValue in Hungarian gebruiken waardoor die niet langer negatieve waarden geeft!
        if (m < 0){
            System.out.println("Munkres algorithm returned negative value");
            m = Integer.MAX_VALUE;
        }

        if (cache) {
            counter.incrementAndGet();
            partialMatchingResults.put(hashCode, m);
        }

        return m;
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

}