package tup;


import java.util.ArrayList;
import java.util.List;

public class Problem {
    final int q1;
    final int q2;
    final int nTeams;
    public final int nUmpires;
    public final int nRounds;
    public final int[][] dist;
    public final int[][] opponents;

    public final int maxValue = 100000;

    /*
    to keep track of the gamenr an opponent is in per round
     */
    public final int[][] opponentsGames;

    public final PartialMatching partialMatching;

    public Boolean enablePartialMatching = false;
    public Boolean enableCaching = false;

    public Boolean enableLowerBoundPartialMatching = false;
    public Boolean enableLowerBoundCaching = false;

    public Boolean enableMultiThreading = false;
    public Boolean enableMultiThreadingSub = false;

    public Problem(int nTeams, int[][] dist, int[][] opponents, int q1, int q2) {
        this.nTeams = nTeams;
        this.nUmpires = nTeams / 2;
        this.nRounds = (nTeams - 1) * 2;
        this.dist = dist;
        this.opponents = opponents;
        this.q1 = q1;
        this.q2 = q2;

        this.opponentsGames = new int[nRounds][nTeams];
        this.partialMatching = new PartialMatching(this);

        for (int round = 0; round < nRounds; round++) {
            int gameNrInRound = 1;
            for (int team = 0; team < nTeams; team++) {
                if (opponents[round][team] < 0){
                    opponentsGames[round][team] = gameNrInRound;
                    opponentsGames[round][-opponents[round][team] - 1] = gameNrInRound;
                    gameNrInRound++;
                }
            }
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Problem{\n");
        sb.append("q1=").append(q1).append(",\n");
        sb.append("q2=").append(q2).append(",\n");
        sb.append("nTeams=").append(nTeams).append(",\n");
        sb.append("nUmpires=").append(nUmpires).append(",\n");

        sb.append("dist=[\n");
        buildArray(sb, dist);
        sb.append("]\n");

        sb.append("opponents=[\n");
        buildArray(sb, opponents);
        sb.append("]\n");

        sb.append("}");
        return sb.toString();
    }

    private static void buildArray(StringBuilder sb, int[][] array2D) {
        int maxWidth = 0;
        for (int[] array1D : array2D) {
            for (int element : array1D) {
                int width = String.valueOf(element).length();
                if (width > maxWidth) maxWidth = width;
            }
        }
        for (int[] array1D : array2D) {
            sb.append("[");
            for (int element : array1D) {
                sb.append(String.format("%" + (maxWidth + 2) + "s", element));
            }
            sb.append("]\n");
        }
    }

    public List<Integer> getValidAllocations(int[][] assignments, int umpire, int round) {
        // feasibleAllocations contains all feasible home teams (NO INDEXES)
        ArrayList<Integer> feasibleAllocations = new ArrayList<>();
        ArrayList<Integer> previousLocations = getPreviousLocations(assignments, round, umpire);
        ArrayList<Integer> previousTeams = getPreviousTeams(assignments, round, umpire);
        for (int i = 0; i < this.nTeams; i++) {
            if (this.opponents[round][i] < 0 ) {
                int homeTeam = -this.opponents[round][i];
                int awayTeam = i+1;

                if (!previousTeams.contains(awayTeam) && !previousTeams.contains(homeTeam) && !previousLocations.contains(homeTeam))
                    feasibleAllocations.add(homeTeam);
            }
        }

        List<Integer> alreadyUsedThisRound = new ArrayList<>();
        for (int i = 0; i < this.nUmpires; i++){
            if (assignments[i][round] != 0){
                alreadyUsedThisRound.add(assignments[i][round]);
            }
        }
        feasibleAllocations.removeIf(alreadyUsedThisRound::contains);

        // Sort on distance to previous location
        feasibleAllocations.sort((a, b) -> {
            int prevLocation = assignments[umpire][round - 1];
            int aDistance = this.dist[prevLocation - 1][a - 1];
            int bDistance = this.dist[prevLocation - 1][b - 1];
            return Integer.compare(aDistance, bDistance);
        });

        return feasibleAllocations;
    }

    public ArrayList<Integer> getPreviousLocations(int [][] assignments, int round, int umpire) {
        ArrayList<Integer> previousLocations = new ArrayList<>();
        for (int i = 1; i < this.q1 && round - i >= 0; i++) {
            int homeTeam = assignments[umpire][round - i];
            previousLocations.add(homeTeam);
        }
        return previousLocations;
    }

    private ArrayList<Integer> getPreviousTeams(int [][] assignments, int round, int umpire) {
        ArrayList<Integer> previousTeams = new ArrayList<>();
        for (int i = 1; i < this.q2 && round - i >= 0; i++) {
            int homeTeam = assignments[umpire][round - i];
            int awayTeam = this.opponents[round - i][homeTeam - 1];
            previousTeams.add(homeTeam);
            previousTeams.add(awayTeam);
        }
        return previousTeams;
    }


    public void printArray(int[][] array){
        int rows = array.length; // Get the number of rows
        int cols = array[0].length; // Get the number of columns

        System.out.println();

        for (int i = 0; i < rows; i++)
        {
            for (int j = 0; j < cols; j++)
            {
                System.out.printf("%d ", array[i][j]); // Print each element followed by a space
            }
            System.out.println();
        }

        System.out.println();
        System.out.println();
    }

    public void printTournement(){
        System.out.println();
        for (int round = 0; round < this.nRounds; round++) {
            System.out.printf("Round%02d:\t\t", (round+1));
            for (int game = 0; game < this.nTeams; game++) {
                if (opponents[round][game] < 0) {
                    int homeTeam = -opponents[round][game];
                    int awayTeam = game+1;

                    System.out.print(homeTeam + "-" + awayTeam + "\t\t");
                }
            }
            System.out.println();
        }
        System.out.println();
    }
}

