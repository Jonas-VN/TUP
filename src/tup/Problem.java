package tup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Problem {
    final int q1;
    final int q2;
    final int nTeams;
    final int[][] dist;
    final int[][] opponents;

    public Problem(int nTeams, int[][] dist, int[][] opponents, int q1, int q2) {
        this.nTeams = nTeams;
        this.dist = dist;
        this.opponents = opponents;
        this.q1 = q1;
        this.q2 = q2;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Problem{");
        sb.append("q1=").append(q1);
        sb.append(",\n q2=").append(q2);
        sb.append(",\n nTeams=").append(nTeams);
        sb.append(",\n dist=").append(Arrays.deepToString(dist));
        sb.append(",\n opponents=").append(Arrays.deepToString(opponents));
        sb.append('}');
        return sb.toString();
    }
    public void assignUmpires() {
        int[][] assignments = new int[nTeams][2*nTeams - 2];

        // Wijs in de eerste ronde elke scheidsrechter toe aan een thuisveld
        for (int i = 0; i < nTeams; i++) {
            assignments[i][0] = Math.abs(opponents[0][i]);
        }

        // Wijs scheidsrechters toe voor de resterende rondes
        for (int round = 1; round < 2*nTeams - 2; round++) {
            for (int umpire = 0; umpire < nTeams; umpire++) {
                List<Integer> feasibleAllocations = getFeasibleAllocations(umpire, round, assignments);
                if (!feasibleAllocations.isEmpty()){
                    for (Integer allocation : feasibleAllocations) {
                        assignments[umpire][round] = Math.abs(opponents[umpire][allocation]);
                        break;
                    }
                }
                else assignments[umpire][round] = 99;//hier moet je dan eigenlijk backtracken denk ik

            }
        }
    }


    public boolean canBePruned(Integer a) {//deze snap ik nog niet helemaal
        return false;
    }


    public List<Integer> getFeasibleAllocations(int umpire, int round, int[] [] assignments) {
        List<Integer> feasibleAllocations = new ArrayList<>();
        for (int i = 0; i < nTeams; i++) {
            if (isValidAllocation(umpire, round, i, assignments)) {
                feasibleAllocations.add(i);
            }
        }
        return feasibleAllocations;
    }

    private boolean isValidAllocation(int umpire, int round, int game, int[][] assignments) {
        // Check if the umpire has not visited the same location in the previous q1 - 1 rounds
        for (int j =0; j< opponents.length;j++){
            for (int i = 1; i < q1 && round - i >= 0; i++) {
                if (opponents[round][j] == assignments[umpire][round - i]) {
                    return false;
                }
            }
            // Check if the umpire has not refereed any of the teams during the previous q2 - 1 rounds
            for (int k = 1; k < q2 && round - k >= 0; k++) {//deze weet ik nog echt niet hoe ik moet doen
                if (opponents[umpire][game] == opponents[umpire][round - k]) {
                    return false;
                }
            }
        }

        return true;
    }

}
