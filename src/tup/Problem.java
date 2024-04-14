package tup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Problem {
    final int q1;
    final int q2;
    final int nTeams;
    public final int nUmpires;
    public final int nRounds;
    public final int[][] dist;
    public final int[][] opponents;

    public Problem(int nTeams, int[][] dist, int[][] opponents, int q1, int q2) {
        this.nTeams = nTeams;
        this.nUmpires = nTeams / 2;
        this.nRounds = (nTeams - 1) * 2;
        this.dist = dist;
        this.opponents = opponents;
        this.q1 = q1;
        this.q2 = q2;
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

    public int[] [] assignUmpires() {
        int[][] assignments = new int[nTeams][2*nTeams - 2];

        // Wijs in de eerste ronde elke scheidsrechter toe aan een thuisveld
        for (int i = 0; i < nTeams; i++) {
            assignments[i][0] = (opponents[0][i]);
        }

        // Wijs scheidsrechters toe voor de resterende rondes
        for (int round = 1; round < 2*nTeams - 2; round++) {
            for (int umpire = 0; umpire < nTeams; umpire++) {
                List<Integer> feasibleAllocations = getValidAllocations(assignments, umpire, round);
                if (!feasibleAllocations.isEmpty()){
                    for (Integer allocation : feasibleAllocations) { // Loop is onnodig?
                        assignments[umpire][round] = allocation;
                        break;
                    }
                }
                else assignments[umpire][round] = 99;//hier moet je dan eigenlijk backtracken denk ik

            }
        }
        return assignments;
    }


    public boolean canBePruned(Integer a) {//deze snap ik nog niet helemaal
        return false;
    }

    public List<Integer> getValidAllocations(int[][] assignments, int umpire, int round) {
        List<Integer> feasibleAllocations = new ArrayList<>();
        List<Integer> previousLocations = getPreviousLocations(assignments, round, umpire);
        List<Integer> previousTeams = getPreviousTeams(assignments, round, umpire);
        for (int i = 0; i < nTeams; i++) {
            if (opponents[round][i] < 0 ) {
                int homeTeam = Math.abs(opponents[round][i]);
                int awayTeam = i+1;
                if(previousLocations.contains(homeTeam)
                    || previousTeams.contains(awayTeam) || previousTeams.contains(homeTeam)){
                    continue;
                }
            }
            else {
                int homeTeam = i+1;
                int awayTeam = Math.abs(opponents[round][i]);
                if (previousLocations.contains(homeTeam)
                        || previousTeams.contains(homeTeam)
                        || previousTeams.contains(awayTeam)){

                    continue;
                }
            }
            feasibleAllocations.add(opponents[round][i]);
        }
        System.out.println(feasibleAllocations);
        return feasibleAllocations;
    }
    public List<Integer> getPreviousLocations(int [][] assignments, int round, int umpire) {
        List<Integer> previousLocations = new ArrayList<>();
        for (int i = 1; i < q1 && round - i >= 0; i++) {
            if (assignments[umpire][round - i] > 0){
                int team = assignments[umpire][round - i];
                previousLocations.add(Math.abs(opponents[round-i][team-1]));
            }
            else previousLocations.add(Math.abs(assignments[umpire][round - i]));
        }
        return previousLocations;
    }
    private List<Integer> getPreviousTeams(int [][] assignments, int round, int umpire) {
        List<Integer> previousTeams = new ArrayList<>();
        for (int i = 1; i <= q2-1 && round - i >= 0; i++) {
            int previousGame = Math.abs(assignments[umpire][round - i]);
            previousTeams.add(Math.abs(opponents[round - i][previousGame - 1]));
            previousTeams.add(previousGame);
        }
        return previousTeams;
    }

}
