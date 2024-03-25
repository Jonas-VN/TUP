package tup;

import java.util.Arrays;
import java.util.List;

public class BranchAndBound {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    private int[][] bestSolution;

    public BranchAndBound(Problem problem) {
        this.problem = problem;
        bestSolution = new int[problem.nTeams][problem.nTeams];
    }

    public void solve() {
        int[][] path = new int[problem.nUmpires][problem.nRounds];

        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        int i = -1;
        for (int team = 0; team < problem.nTeams; team++) {
            if (problem.opponents[0][team] > 0) {
                path[++i][0] = problem.opponents[0][team];
            }
            //path[i][0] = problem.opponents[0][i]; //note: in debuggen krijgt referee '0' wedstrijd '0' toegewezen, kan misschien verwarring brengen :)
        }
        printPath(path);
        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
        // branchAndBound(path, 0, 0);
        //path = problem.assignUmpires();
        path = this.branchAndBound(path, 0, 1);
        printPath(path);
    }

    private int[][] branchAndBound(int[][] path, int umpire, int round) {
        System.out.printf("Umpire: %d\t Round: %d\n", umpire, round);
        if (round == this.problem.nRounds) {
            // Constructed a full feasible path
            printPath(path);
            // TODO: Tijdelijke exit na eerste oplossing
            System.exit(0);
        }
        List<Integer> feasibleAllocations = this.problem.getValidAllocations(path, umpire, round);
        if (!feasibleAllocations.isEmpty()) {
            // TODO: Sort feasibleAllocations on lowest distance
//            feasibleAllocations.sort((a, b) -> {
//                int prevLocation = path[umpire][round - 1];
//                int aDistance = this.problem.dist[prevLocation - 1][a - 1];
//                int bDistance = this.problem.dist[prevLocation - 1][b - 1];
//                return Integer.compare(aDistance, bDistance);
//            });
            for (Integer allocation : feasibleAllocations) {
                path[umpire][round] = allocation;
                if (umpire == this.problem.nUmpires - 1) {
                    printPath(path);
                    this.branchAndBound(path, 0, round + 1);
                }
                else this.branchAndBound(path, umpire + 1, round);
            }
        }
        return path;
    }
    /*
        private void branchAndBound(int[][] path, int umpire, int round) {
            int uPlus = (umpire % problem.nTeams) + 1; //umpire te bekijken in volgende iteratie
            int rPlus;
            if (umpire + 1 == problem.nTeams) {
                rPlus = round + 1;
            } else {
                rPlus = round;
            }
            List<Integer> A = problem.getFeasibleAllocations(umpire, round);
            for (Integer a : A) {
                    if (!problem.canBePruned(a)) {
                        //path[umpire][round] = a;
                        printPath(path);
                        if (round < problem.nTeams - 1) {
                            branchAndBound(path, uPlus, rPlus);
                        } else {
                            int totalDistance = calculateTotalDistance(path);//Hier moet local search worden gedaan
                            if (totalDistance < bestDistance) {
                                bestDistance = totalDistance;
                                for (int i = 0; i < problem.nTeams; i++) {
                                }
                            }
                        }
                        path[umpire][round] = -1;
                    }
                }
                printPath(path);
            }




     */
    private int calculateTotalDistance(int[][] path) {
        int totalDistance = 0;
        for (int i = 0; i < problem.nTeams; i++) {
            for (int j = 0; j < problem.nTeams - 1; j++) {
                totalDistance += problem.dist[path[i][j]][path[i][j + 1]];
            }
        }
        return totalDistance;
    }



    public String getBestSolution() {
        return Arrays.deepToString(bestSolution);
    }


    public int getBestDistance() {
        return bestDistance;
    }





    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }
}