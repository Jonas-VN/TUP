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


    public List<Integer> getFeasibleAllocations(int umpire, int round) {
        List<Integer> feasibleAllocations = new ArrayList<>();
        for (int i = 0; i < nTeams; i++) {
                if (isValidAllocation(umpire, round, i)) {
                    feasibleAllocations.add(i);
                }
            }
            return feasibleAllocations;
        }
        private boolean isValidAllocation(int umpire, int round, int game) {
            // Controleer of de scheidsrechter niet dezelfde locatie heeft bezocht in de vorige q1 - 1 rondes
            for (int i = 1; i < q1 && round - i >= 0; i++) {
                if (dist[umpire][game] == dist[umpire][round - i]) {
                    return false;
                }
            }

            // Controleer of de scheidsrechter geen van de teams heeft gefloten tijdens de vorige q2 - 1 rondes
            for (int i = 1; i < q2 && round - i >= 0; i++) {
                if (opponents[umpire][game] == opponents[umpire][round - i]) {
                    return false;
                }
            }

            return true;
        }

    public boolean canBePruned(Integer a) {//deze snap ik nog niet helemaal
        return false;
    }
}
