package tup;


import java.util.Arrays;

public class LowerBound {
    private Problem problem;
    private final int nrOfMatches;//aantal matches per ronde
    private double bestDistance;

    public LowerBound(Problem problem) {
        this.problem = problem;
        this.nrOfMatches= problem.nTeams / 2;

    }

    public void solve(){
        int[][] umpireAssignments = new int[problem.nUmpires][]; // rows = umpires and columns = array of assigned team per round
        //int[][] umpireAssignmentsHome = new int[nrOfUmpires][]; // rows = umpires and columns = array of assigned team per round
        //int[][] umpireAssignmentsAway = new int[nrOfUmpires][]; // rows = umpires and columns = array of assigned team per round
        // initialize to zeros
        for (int i = 0; i < problem.nUmpires; i++)
        {
            //umpireAssignmentsHome[i] = new int[nRounds];
            //umpireAssignmentsAway[i] = new int[nRounds];
            umpireAssignments[i] = new int[problem.nRounds];
        }
        // We start with assigning umpire 1 to game 1 = the first round
        int[] home = new int[problem.nUmpires];
        int[] away = new int[problem.nUmpires];
        getMatchesOfRound(problem.opponents[0], home, away);

        for (int i = 0; i < home.length; i++) {
            // umpireAssignmentsHome[i][0] = home[i];
            // umpireAssignmentsAway[i][0] = away[i];
            umpireAssignments[i][0] = home[i];
        }

        int[][] d = new int[problem.nTeams][];
        int[][] LBI = new int[problem.nRounds][problem.nRounds];

        for (int i = 0; i < problem.nRounds - 1; i++) {
            int fromRound = i;
            int toRound = i + 1;
            int totalDistance = 0;

            // init distance matrix to INT
            // .MAX_VALUE and replace the feasible distances
            for (int j = 0; j < problem.nTeams; j++) {
                d[j] = new int[problem.nTeams];
                Arrays.fill(d[j], 10000);
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
                    if (home1[j] == home2[k] || home1[j] == away2[k] || away1[j] == home2[k] || away1[j] == away2[k]) {
                        continue;
                    }

                    int from = home1[j];
                    int to = home2[k];

                    d[from - 1][to - 1] = problem.dist[from - 1][to - 1];
                }
            }

            //Hungarian hungarian = new Hungarian(d);
            Hungarian hungarian = new Hungarian();
            Hungarian.assignmentProblem(d);
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

            LBI[fromRound][toRound] = totalDistance;
           // System.out.println("Round " + fromRound + " to " + toRound + " has Mindistance " + totalDistance);
            //na de laatste iteratie heb je van alle rondes de min afstand tussen 2 rondes door hungarian
            //de hungarian houdt nog geen rekening met q-constraints enkel tussen 2 wedstrijden op zich
        }

        // now we try to strengthen the bounds using Algorithm 2
        double[][] S = new double[problem.nRounds][problem.nRounds]; // square matrix auto initialized with 0's -> contains the solutions for the subproblems
        double[][] LB = new double[problem.nRounds][problem.nRounds]; // square matrix auto initialized with 0's -> contains the lower bounds for all pairs of rounds

        int r_ix, r2_ix;
        for (int r = problem.nRounds - 1; r >= 1; r--) {
            r_ix = r - 1;
            S[r_ix][r_ix + 1] = LBI[r_ix][r_ix + 1];

            for (int r2 = r + 1; r2 <= problem.nRounds; r2++) {
                r2_ix = r2 - 1;
                LB[r_ix][r2_ix] = S[r_ix][r_ix + 1] + LB[r_ix + 1][r2_ix];
            }
        }
        //printArray(LB);//marker 1 voor LB "zie onder"
        for (int k = 2; k <= problem.nRounds - 1; k++) {
            int r = problem.nRounds - k;

            while (r >= 1) {
                for (int r3 = r + k - 2; r3 >= r; r3--) {
                    if (S[r3 - 1][r + k - 1] > 0) {
                        continue; // do not resolve already solved subproblems
                    }
                    if (S[r3 - 1][r + k - 1] == 0) {
                        //S[r3 - 1][r + k - 1] = valueOfSolutionOfSubproblemWithRounds(r3, r + k);//hier zit de fout
                    }

                    for (int r1 = r3; r1 >= 1; r1--) {
                        for (int r2 = r + k; r2 < problem.nRounds; r2++) {
                            LB[r1 - 1][r2 - 1] = Math.max(LB[r1 - 1][r2 - 1], LB[r1 - 1][r3 - 1] + S[r3 - 1][r + k - 1] + LB[r + k - 1][r2 - 1]);
                        }
                    }
                }

                r = r - k;
            }
        }
//        System.out.println();
//        System.out.println("Initial lower bounds");
//        printArrayInt(LBI);
//        System.out.println("Improved lower bounds");
//        printArray(LB);// hier zie je dat LB nog steeds hetzelfde is als marker 1 dus er klopt iets niet, wss omdat S niet aangepast wordt
//        System.out.println("S matrix");
//        printArray(S);
        bestDistance= LB[0][problem.nRounds - 1];


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
    void printArray(double[][] array) {
        for (double[] doubles : array) {
            for (double aDouble : doubles) {
                System.out.print(aDouble + "\t");
            }
            System.out.println();
        }
    }
    void printArrayInt(int[][] array) {
        for (int[] ints : array) {
            for (int anInt : ints) {
                System.out.print(anInt + "\t");
            }
            System.out.println();
        }
    }
    public double getLowerBound() {
        return bestDistance;
    }

}
