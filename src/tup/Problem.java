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
        List<Integer> previousLocations = getPreviousLocations(umpire, round, assignments);
        for (int i = 0; i < nTeams - 1; i++) {
            boolean isFeasible = true;
            if (opponents[round][i] < 0 ) {
                if(previousLocations.contains(Math.abs(opponents[round][i]))){
                    isFeasible = false;
                    continue;
                }
            }
            else {
                int team = Math.abs(opponents[round][i]);
                if (previousLocations.contains(Math.abs(opponents[round][team-1]))){
                    isFeasible = false;
                    continue;
                }
            }
            if (isFeasible) {
                feasibleAllocations.add(opponents[round][i]);
            }
        }
        System.out.println(feasibleAllocations);
        return feasibleAllocations;
    }
    public List<Integer> getPreviousLocations(int umpire, int round, int[][] assignments) {
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

}
