import AlgorthmFrame.ga.ChromosomeOperation;
import AlgorthmFrame.ga.GA;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;
import java.util.Random;

public class CrossOverTest {
    public static void main (String[] s) {
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


        int[] o1 = new int[]{0, 1, 2, 3, 4, 0, 1, 2, 3, 4};
        int[] m1 = new int[]{1, 1, 1, 1, 1, 6, 5, 6, 5, 6};
        int[] o2 = new int[]{0, 1, 2, 3, 4, 0, 4, 2, 1, 3};
        int[] m2 = new int[]{1, 1, 1, 1, 1, 6, 5, 6, 5, 6};

        ChromosomeOperation chromosomeOperation = new ChromosomeOperation(new Random(), problem);
        chromosomeOperation.operSeqCrossoverPOX(o1, m1, m2, o2, 1);
    } catch (Exception e) {
        throw e;
    }
    }

}
