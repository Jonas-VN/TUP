package tup;


import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Semaphore;

public class LowerBound {
    private Problem problem;
    private final int nrOfMatches;//aantal matches per ronde
    private int[][] LB; // square matrix auto initialized with 0's -> contains the lower bounds for all pairs of rounds
    public boolean shutdown = false;
    private Semaphore[] mutexes;

    public LowerBound(Problem problem) {
        this.problem = problem;
        this.nrOfMatches= problem.nTeams / 2;
        this.LB = new int[problem.nRounds][problem.nRounds];
        this.mutexes = new Semaphore[problem.nRounds];
        for (int i = 0; i < problem.nRounds; i++) {
            mutexes[i] = new Semaphore(1);
        }

        initializeLowerBound();
    }

    private void initializeLowerBound(){
        int[][] umpireAssignments = new int[problem.nUmpires][]; // rows = umpires and columns = array of assigned team per round

        // initialize to zeros
        for (int i = 0; i < problem.nUmpires; i++)
        {
            umpireAssignments[i] = new int[problem.nRounds];
        }
        // We start with assigning umpire 1 to game 1 = the first round
        int[] home = new int[problem.nUmpires];
        int[] away = new int[problem.nUmpires];
        getMatchesOfRound(problem.opponents[0], home, away);

        for (int i = 0; i < home.length; i++) {
            umpireAssignments[i][0] = home[i];
        }

        int[][] d = new int[problem.nTeams][];
        // Use LB from the start
        //int[][] LBI = new int[problem.nRounds][problem.nRounds];

        for (int i = 0; i < problem.nRounds - 1; i++) {
            int fromRound = i;
            int toRound = i + 1;
            int totalDistance = 0;

            // init distance matrix to INT
            // .MAX_VALUE and replace the feasible distances
            for (int j = 0; j < problem.nTeams; j++) {
                d[j] = new int[problem.nTeams];
                Arrays.fill(d[j], problem.maxValue); // TODO Waarom 10000 -> zeker dat dit genoeg is
                //Arrays.fill(d[j], Integer.MAX_VALUE);
            }

            int[] round1 = problem.opponents[i];
            int[] round2 = problem.opponents[i + 1];
            int[] home1 = new int[problem.nUmpires];
            int[] away1 = new int[problem.nUmpires];
            int[] home2 = new int[problem.nUmpires];
            int[] away2 = new int[problem.nUmpires];

            getMatchesOfRound(round1, home1, away1);
            getMatchesOfRound(round2, home2, away2);

            for (int j = 0; j < nrOfMatches; j++) {
                for (int k = 0; k < nrOfMatches; k++) {
                    // going to the same teams (or venue = home team)
                    if (problem.q2 > 1 && (home1[j] == home2[k] || home1[j] == away2[k] || away1[j] == home2[k] || away1[j] == away2[k])) {
                        continue;
                    }

                    if (problem.q1 > 1 && home1[j] == home2[k]) {
                        continue;
                    }

                    int from = home1[j];
                    int to = home2[k];

                    d[from - 1][to - 1] = problem.dist[from - 1][to - 1];
                }
            }

            //Hungarian hungarian = new Hungarian(d);
            Hungarian hungarian = new Hungarian();
            hungarian.assignmentProblem(d); // Gil -> hier werd Hungarian.assignmentProblem gebruikt en dus de niet de instance. static issue!!
            int [] solution = hungarian.xy;

            // Meaning of solution: -> 1,5,2,4,6,3,0,7 for the first to second round
            // home1[] = 1,6,7,4 corresponds with teams A, F, G, D
            // to find where the umpire who is currently in home1[0] = 1 = A must go to,
            // we have to look into solution[A] = solution[0] => 1 => this is an index so + 1 = 2 = B
            // =>  use these values as index-1 in solution[] to see where the umpire should go to next

            for (int from : home1) {
                int to = solution[from - 1]; // to is the index for dest matrix to know the distance
                int distance = d[from - 1][to];
                totalDistance += distance;

                // we update the umpireAssignment logging
                // we need to search for the umpire who was in the FromRound in this venue
                int umpIx = 0;
                for (int x = 0; x < problem.nUmpires; x++) {
                    if (umpireAssignments[x][fromRound] == from) {
                        umpIx = x;
                        break;
                    }
                }
                umpireAssignments[umpIx][toRound] = to + 1; // +1 because this contains not idx but teamNr
            }

            LB[fromRound][toRound] = totalDistance;
            // System.out.println("Round " + fromRound + " to " + toRound + " has Mindistance " + totalDistance);
            //na de laatste iteratie heb je van alle rondes de min afstand tussen 2 rondes door hungarian
            //de hungarian houdt nog geen rekening met q-constraints enkel tussen 2 wedstrijden op zich
        }
    }

    public void solve() throws InterruptedException {

        // now we try to strengthen the bounds using Algorithm 2
        int[][] S = new int[problem.nRounds][problem.nRounds]; // square matrix auto initialized with 0's -> contains the solutions for the subproblems


        int r_ix, r2_ix;
        for (int r = problem.nRounds - 1; r >= 1; r--) {
            r_ix = r - 1;
            S[r_ix][r_ix + 1] = LB[r_ix][r_ix + 1];

            for (int r2 = r + 1; r2 <= problem.nRounds; r2++) {
                r2_ix = r2 - 1;
                mutexes[r_ix].acquire();
                LB[r_ix][r2_ix] = S[r_ix][r_ix + 1] + LB[r_ix + 1][r2_ix];
                mutexes[r_ix].release();
            }
        }


        for (int k = 2; k < problem.nRounds; k++) {
            //System.out.println(String.format("k = %d", k));

            int start = problem.nRounds - 1 - k; //ix
            int end = start + k; //ix

            while (start >= 0) {
                for (int first = end - 2; first >= start; first--) {
                    if (shutdown) return;

                    //System.out.println(String.format("Bnb Sub from round %d to round %d", first, end));

                    if (S[first][end] > 0) {
                        continue; // do not resolve already solved subproblems
                    }

                    if (S[first][end] == 0) {
                        // bbSub needs roundnr's and not ix's => +1
                        BranchAndBoundSub bbsub = new BranchAndBoundSub(problem, first + 1 , end + 1, this);
                        var m = bbsub.solve();
                        S[first][end] = m; // S is also our cache to skip already resolved subproblems

                        int delta = m - getLowerBound2(first, end);

                        // only update LB if we get stronger lower bounds (delta < 0 should never occur because we are making the bounds stronger)
                        if (delta > 0){
                            //S[first][end] = m;

                            //propagate to round 0
                            for (int i = first; i >= 0; i--) {
                                for (int j = end; j < problem.nRounds; j++) {
                                    mutexes[i].acquire();
                                    mutexes[end].acquire();
                                    LB[i][j] = Math.max(LB[i][j], LB[i][first] + S[first][end] + LB[end][j]);
                                    mutexes[i].release();
                                    mutexes[end].release();
                                }
                            }
                        }
                    }
                }


                //if (start == 0) break; // overbodig want als start 0 is en je doet -k dan zal de while loop stoppen vermits start >= 0 moet zijn
                end = start; // dit was vergeten waardoor er vééééél meer berekeningen worden uitgevoerd
                start -= k;
            }
        }
        //printLowerBounds();
    }

    void getMatchesOfRound(int[] r, int[] h, int[] a) {
        // Create a copy of the round array because we do not want to change the original array
        int[] round = new int[r.length];
        System.arraycopy(r, 0, round, 0, round.length);

        int umpireIx = 0;
        for (int i = 0; i < round.length; i++) {
            int opponent = round[i];

            if (opponent == 0) {
                continue; // we already covered this game
            }

            if (opponent > 0) { // Home
                h[umpireIx] = i + 1;
                a[umpireIx] = Math.abs(opponent);
            } else {
                h[umpireIx] = Math.abs(opponent);
                a[umpireIx] = i + 1;
            }

            round[i] = 0;
            round[Math.abs(opponent) - 1] = 0;

            umpireIx++;
        }

    }

    /**
     *
     * @param round index of the round to start from
     * @return
     * @throws InterruptedException
     */
    public int getLowerBound(int round) throws InterruptedException {
        mutexes[round].acquire();
        int ret =  LB[round][problem.nRounds - 1];
        mutexes[round].release();
        return ret;
    }

    /**
     *
     * @param fromRound nr of the round to start from (not the index)
     * @param toRound nr of the round to end (not the index)
     * @return
     */
    public int getLowerBound(int fromRound, int toRound) {
        mutexes[fromRound - 1].acquireUninterruptibly();
        int ret = LB[fromRound - 1][toRound - 1];
        mutexes[fromRound - 1].release();
        return ret;
    }

    /**
     *
     * @param fromRound index of the round to start from
     * @param toRound index of the round to end
     * @return
     */
    public int getLowerBound2(int fromRound, int toRound) {
        mutexes[fromRound].acquireUninterruptibly();
        int ret = LB[fromRound][toRound];
        mutexes[fromRound].release();
        return ret;
    }

    public void printLowerBounds(){
        int rows = LB.length; // Get the number of rows
        int cols = LB[0].length; // Get the number of columns

        System.out.println();

        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                System.out.printf("%08d ", LB[i][j]); // Print each element followed by a space
            }
            System.out.println();
        }

        System.out.println();
        System.out.println();
    }

}