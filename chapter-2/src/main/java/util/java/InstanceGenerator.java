package util.java;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 算例生成器
 * 生成遗传算法测试所需的算例文件
 */
public class InstanceGenerator {

    private Random random;
    private int seed;

    public InstanceGenerator(int seed) {
        this.seed = seed;
        this.random = new Random(seed);
    }

    /**
     * 生成算例文件
     */
    public void generateInstance(int jobCount, int printerCount, int batchCount,
                               int discreteCount, String filename) throws IOException {

        int totalMachines = printerCount + batchCount + discreteCount;
        List<String> lines = new ArrayList<>();

        // 第一行：总机器数 工件数
        lines.add(totalMachines + " " + jobCount);

        // 第二行：打印机配置
        StringBuilder printerLine = new StringBuilder(String.valueOf(printerCount));
        for (int i = 0; i < printerCount; i++) {
            int length = (random.nextInt(6) + 6) * 100;     // 600-1200
            int width = (random.nextInt(2) + 4) * 100;      // 400-600
            int height = (random.nextInt(2) + 4) * 100;     // 400-600
            double layerHeight = Math.round((random.nextDouble() * 0.095 + 0.025) * 1000.0) / 1000.0; // 0.025-0.12
            int switchTime = random.nextInt(301) + 300;      // 300-600
            int printTime = random.nextInt(21) + 10;         // 10-30
            printerLine.append(String.format(" %d %d %d %d %.3f %d %d",
                    i + 1, length, width, height, layerHeight, switchTime, printTime));
        }
        lines.add(printerLine.toString());

        // 第三行：批处理机配置
        StringBuilder batchLine = new StringBuilder(String.valueOf(batchCount));
        for (int i = 0; i < batchCount; i++) {
            int processTime = (random.nextInt(61) + 60) * 10; // 600-1200
            batchLine.append(String.format(" %d %d", i + 1, processTime));
        }
        lines.add(batchLine.toString());

        // 生成工件数据
        for (int jobId = 0; jobId < jobCount; jobId++) {
            // 工件尺寸
            int length = random.nextInt(290) + 10;  // 10-600
            int width = random.nextInt(290) + 10;   // 10-600
            int height = random.nextInt(100) + 10;  // 10-400

            // 离散工序数 (1-8)
            int discreteOps = random.nextInt(8) + 1;
            int totalOps = 2 + discreteOps;  // 打印 + 批处理 + 离散

            StringBuilder jobLine = new StringBuilder();
            jobLine.append(String.format("%d %d %d %d", totalOps, length, width, height));

            // 打印工序：1台打印机，时间0
            int printerId = (jobId % printerCount) + 1;
            jobLine.append(String.format(" %d 1 %d 0", totalOps, printerId));

            // 批处理工序：1台批处理机，时间0
            int batchId = (jobId % batchCount) + 1 + printerCount;
            jobLine.append(String.format(" 1 %d 0", batchId));

            // 离散工序
            for (int op = 0; op < discreteOps; op++) {
                // 每工序可选机器数 (2-5台)
                int machineCount = Math.min(5, Math.max(2, random.nextInt(discreteCount - 1) + 2));
                jobLine.append(" ").append(machineCount);

                // 随机选择机器
                List<Integer> availableMachines = new ArrayList<>();
                for (int m = printerCount + batchCount + 1; m <= totalMachines; m++) {
                    availableMachines.add(m);
                }
                Collections.shuffle(availableMachines, random);

                for (int i = 0; i < machineCount; i++) {
                    int machineId = availableMachines.get(i);
                    int processTime = random.nextInt(601) + 600; // 600-1200
                    jobLine.append(String.format(" %d %d", machineId, processTime));
                }
            }

            lines.add(jobLine.toString());
        }

        // 写入文件
        try (FileWriter writer = new FileWriter(filename)) {
            for (String line : lines) {
                writer.write(line + "\n");
            }
        }

        System.out.println("已生成算例：" + filename);
    }

    /**
     * 生成多个算例文件
     */
    public void generateMultipleInstances(int jobCount, int printerCount, int batchCount,
                                        int discreteCount, int instanceCount, String baseFilename) throws IOException {

        for (int i = 1; i <= instanceCount; i++) {
            // 为每个实例设置不同的种子
            InstanceGenerator generator = new InstanceGenerator(seed + i * 1000);
            String filename = String.format("%s//J%dP%dB%dD%d_%02d.txt", baseFilename,jobCount, printerCount, batchCount, discreteCount, i);
            generator.generateInstance(jobCount, printerCount, batchCount, discreteCount, filename);
        }
    }

    /**
     * 主函数示例
     */
    public static void main(String[] args) {
        try {
            InstanceGenerator generator = new InstanceGenerator(42);

            // 生成单个算例
            //generator.generateInstance(20, 3, 2, 5, "instance/J20P3B2D5_test.txt");

            // 生成多个算例
            generator.generateMultipleInstances(50, 3, 3, 10, 2, "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J50");

            System.out.println("算例生成完成！");
        } catch (IOException e) {
            System.err.println("生成算例时出错: " + e.getMessage());
        }
    }
}
