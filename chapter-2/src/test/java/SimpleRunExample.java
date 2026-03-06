package test;

import AlgorthmFrame.ga.GA;
import ProblemFrame.Solution;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;

/**
 * Simple algorithm running example
 */
public class SimpleRunExample {

    public static void main(String[] args) {
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\test_instance_small_2m_5j.txt";

        System.out.println("====================================================");
        System.out.println("3D Printing Workshop Scheduling - GA Test");
        System.out.println("====================================================");

        try {
            System.out.println("\n[Step 1] Reading instance...");
            File file = new File(instancePath);
            if (!file.exists()) {
                System.err.println("ERROR: File not found: " + instancePath);
                return;
            }

            Input input = new Input(file);
            Problem problem = input.getProblemDesFromFile();

            System.out.println("OK Problem loaded:");
            System.out.println("  Machines: " + problem.getMachineCount());
            System.out.println("  Jobs: " + problem.getJobCount());
            System.out.println("  Operations: " + problem.getTotalOperationCount());

            // Debug info
            int[] machineCountArr = problem.getMachineCountArr();
            System.out.print("  machineCountArr: ");
            for (int i = 0; i < Math.min(machineCountArr.length, 10); i++) {
                System.out.print(machineCountArr[i] + " ");
            }
            System.out.println();

            System.out.println("\n[Step 2] Running GA...");
            GA ga = new GA(problem);

            long start = System.currentTimeMillis();
            Solution solution = ga.solve();
            long end = System.currentTimeMillis();

            System.out.println("OK GA completed!");
            System.out.println("Time: " + (end - start) + " ms");

            if (solution != null) {
                System.out.println("Solution found!");
            } else {
                System.out.println("No solution found");
            }

        } catch (Exception e) {
            System.err.println("\nERROR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

