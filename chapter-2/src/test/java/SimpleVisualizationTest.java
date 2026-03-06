import AlgorthmFrame.visualization.ScheduleVisualizer;
import ProblemFrame.Solution;
import ProblemFrame.Chromosome;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import ProgramEntity.Operation;

import java.io.File;

/**
 * 简单的可视化功能测试
 * 不运行GA，直接测试可视化组件
 */
public class SimpleVisualizationTest {

    public static void main(String[] args) {
        System.out.println("=== 简单可视化功能测试 ===\n");

        try {
            // 加载算例
            String instancePath = "src/main/resources/instance/J20P3B2D5_01.txt";
            File instanceFile = new File(instancePath);

            if (!instanceFile.exists()) {
                System.err.println("错误：找不到算例文件: " + instancePath);
                return;
            }

            System.out.println("加载算例: " + instancePath);
            Input input = new Input(instanceFile);
            Problem problem = input.getProblemDesFromFile();

            // 创建模拟的Solution和Chromosome对象用于测试
            System.out.println("创建测试数据...");

            // 创建空的operationMatrix
            Operation[][] operationMatrix = new Operation[problem.getJobCount()][];
            for (int i = 0; i < operationMatrix.length; i++) {
                operationMatrix[i] = new Operation[problem.getOperationCountArr()[i]];
                for (int j = 0; j < operationMatrix[i].length; j++) {
                    operationMatrix[i][j] = new Operation();
                    operationMatrix[i][j].jobNo = i;
                    operationMatrix[i][j].task = j;
                    operationMatrix[i][j].machineNo = j % problem.getMachineCount();
                    operationMatrix[i][j].startTime = j * 10.0;
                    operationMatrix[i][j].endTime = (j + 1) * 10.0;
                }
            }

            // 创建Chromosome - 使用简单构造器
            Chromosome chromosome = new Chromosome(new java.util.Random(42));
            // 初始化必要的属性用于测试
            chromosome.fitness = 1.0; // 虚拟适应度值

            // 初始化printSolution数组（模拟打印批次）
            java.util.List<ProgramEntity.Solution>[] printSolutions = new java.util.List[problem.getPrintMachineCount()];
            for (int i = 0; i < printSolutions.length; i++) {
                printSolutions[i] = new java.util.ArrayList<>();
            }
            chromosome.printSolution = printSolutions;

            // 为第一个打印机创建模拟批次
            if (printSolutions.length > 0) {
                java.util.List<ProgramEntity.Solution> printerBatches = printSolutions[0];

                // 创建第一个批次
                ProgramEntity.Solution batch1 = new ProgramEntity.Solution();
                batch1.startTime = 0.0;
                batch1.endTime = 25.0;
                batch1.placeItemList = new java.util.ArrayList<>();

                // 添加零件到批次
                ProgramEntity.PlaceItem item1 = new ProgramEntity.PlaceItem();
                item1.name = "0"; // 工件0
                item1.x = 10.0;
                item1.y = 10.0;
                item1.l = 50.0; // 长
                item1.w = 30.0; // 宽
                item1.h = 20.0; // 高
                item1.isRotate = false;
                batch1.placeItemList.add(item1);

                ProgramEntity.PlaceItem item2 = new ProgramEntity.PlaceItem();
                item2.name = "1"; // 工件1
                item2.x = 70.0;
                item2.y = 10.0;
                item2.l = 40.0;
                item2.w = 40.0;
                item2.h = 15.0;
                item2.isRotate = false;
                batch1.placeItemList.add(item2);

                printerBatches.add(batch1);

                // 创建第二个批次
                ProgramEntity.Solution batch2 = new ProgramEntity.Solution();
                batch2.startTime = 30.0;
                batch2.endTime = 55.0;
                batch2.placeItemList = new java.util.ArrayList<>();

                ProgramEntity.PlaceItem item3 = new ProgramEntity.PlaceItem();
                item3.name = "2"; // 工件2
                item3.x = 15.0;
                item3.y = 50.0;
                item3.l = 35.0;
                item3.w = 45.0;
                item3.h = 25.0;
                item3.isRotate = false; // 不旋转，测试正常情况
                batch2.placeItemList.add(item3);

                // 添加一个旋转的零件用于测试
                ProgramEntity.PlaceItem item4 = new ProgramEntity.PlaceItem();
                item4.name = "3"; // 工件3
                item4.x = 60.0;
                item4.y = 50.0;
                item4.l = 25.0;
                item4.w = 35.0;
                item4.h = 20.0;
                item4.isRotate = true; // 旋转，测试旋转标记
                batch2.placeItemList.add(item4);

                printerBatches.add(batch2);
            }

            // 创建Solution
            Solution solution = new Solution(operationMatrix, chromosome, problem, 100.0);

            // 测试可视化
            System.out.println("生成可视化图表...");
            String outputDir = "test_visualization_results";

            // 创建一个模拟的迭代历史记录（测试用）
            java.util.List<Double> testHistory = new java.util.ArrayList<>();
            for (int i = 0; i <= 10; i++) {
                testHistory.add(1000.0 - i * 50.0);  // 模拟makespan从1000降到500
            }

            ScheduleVisualizer visualizer = new ScheduleVisualizer(solution, chromosome, problem, operationMatrix, testHistory);
            visualizer.generateAllCharts(outputDir);

            System.out.println("\n✅ 可视化测试完成！");
            System.out.println("检查目录: " + outputDir);

        } catch (Exception e) {
            System.err.println("\n❌ 测试失败:");
            e.printStackTrace();
        }
    }
}
