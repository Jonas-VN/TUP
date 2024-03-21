package tup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Problem {
    final int q1;
    final int q2;
    final int nTeams;
    public final int nUmpires;
    public final int nRounds;
    final int[][] dist;
    final int[][] opponents;

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
            assignments[i][0] = Math.abs(opponents[0][i]);
        }

        // Wijs scheidsrechters toe voor de resterende rondes
        for (int round = 1; round < 2*nTeams - 2; round++) {
            for (int umpire = 0; umpire < nTeams; umpire++) {
                List<Integer> feasibleAllocations = getValidAllocations(umpire, round, assignments);
                if (!feasibleAllocations.isEmpty()){
                    for (Integer allocation : feasibleAllocations) {
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

    private List<Integer> getValidAllocations(int umpire, int round, int[][] assignments) {
    List<Integer> feasibleAllocations = new ArrayList<>();
    for (int j = 0; j < nTeams - 1; j++) {
        boolean isFeasible = true;
        for (int i = 1; i < q1 && round - i >= 0; i++) {
            if (Math.abs(opponents[round][j]) == Math.abs(assignments[umpire][round - i])) {
                isFeasible = false;
                break; // Skip to the next opponent if the condition is not met
            }
        }
        if (isFeasible) {
            feasibleAllocations.add(opponents[round][j]);
        }
    }

            // Check if the umpire has not refereed any of the teams during the previous q2 - 1 rounds
            /*
            for (int k = 1; k < q2 && round - k >= 0; k++) {//deze weet ik nog echt niet hoe ik moet doen
                if (opponents[umpire][game] == opponents[umpire][round - k]) {
                    return false;
                }
            }
             */
        System.out.println(feasibleAllocations);
        return feasibleAllocations;
    }

}
