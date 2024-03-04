package tup;

import java.util.Arrays;

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
}
