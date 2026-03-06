package AlgorithmFrame.nsgaii;

import AlgorithmFrame.visualization.PrinterLayoutVisualizer;
import AlgorithmFrame.visualization.ScheduleVisualizer;
import ProblemFrame.CaculateFitness;
import ProblemFrame.Chromosome;
import ProblemFrame.MOIndividual;
import ProgramEntity.Item;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.Problem;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * NSGA-II结果导出器
 *
 * 负责导出所有Pareto前沿解的完整信息，包括：
 * 1. Pareto前沿图（能耗 vs Cmax）
 * 2. 最优Cmax解的甘特图
 * 3. 最优Cmax解的打印批次排布图
 * 4. 零件加工顺序记录
 * 5. 机器加工顺序记录
 * 6. Pareto前沿解集详细数据
 *
 * @author AI Assistant
 * @version 1.0
 */
public class NSGAIIResultExporter {

    private final Problem problem;
    private final Operation[][] operationMatrix;

    public NSGAIIResultExporter(Problem problem, Operation[][] operationMatrix) {
        this.problem = problem;
        this.operationMatrix = operationMatrix;
    }

    /**
     * 导出所有结果
     *
     * @param paretoFront Pareto前沿解集
     * @param outputDir 输出目录
     */
    public void exportAllResults(List<MOIndividual> paretoFront, String outputDir) throws IOException {
        System.out.println("\n" + repeatString("=", 80));
        System.out.println("开始导出NSGA-II结果...");
        System.out.println(repeatString("=", 80));

        // 创建输出目录
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 1. 导出Pareto前沿图
        System.out.println("\n[1/6] 生成Pareto前沿图...");
        exportParetoFrontChart(paretoFront, outputDir + "/pareto_front.png");

        // 2. 导出Pareto前沿解集数据
        System.out.println("\n[2/6] 导出Pareto前沿解集数据...");
        exportParetoFrontData(paretoFront, outputDir + "/pareto_front_data.txt");

        // 3. 找到Cmax最小的解
        MOIndividual bestCmaxSolution = findBestCmaxSolution(paretoFront);
        System.out.println("\n[3/6] 找到最优Cmax解: Cmax=" + bestCmaxSolution.objectives[0] +
                          ", 能耗=" + bestCmaxSolution.objectives[1] + " kWh");

        // 4. 导出最优解的甘特图
        System.out.println("\n[4/6] 生成最优解甘特图...");
        exportGanttChart(bestCmaxSolution, outputDir + "/best_cmax_gantt.png");

        // 5. 导出最优解的打印批次排布图
        System.out.println("\n[5/6] 生成打印批次排布图...");
        exportPrinterLayout(bestCmaxSolution, outputDir + "/best_cmax_printer_layout.png");

        // 6. 导出最优解的详细调度记录
        System.out.println("\n[6/6] 导出详细调度记录...");
        exportScheduleRecords(bestCmaxSolution, outputDir + "/best_cmax_schedule_records.txt");

        System.out.println("\n" + repeatString("=", 80));
        System.out.println("✅ 所有结果已导出到: " + outputDir);
        System.out.println(repeatString("=", 80) + "\n");
    }

    /**
     * 导出Pareto前沿图
     */
    private void exportParetoFrontChart(List<MOIndividual> paretoFront, String outputPath) throws IOException {
        ParetoFrontVisualizer.generateParetoFrontChart(paretoFront, outputPath, 1200, 800);
    }

    /**
     * 导出Pareto前沿解集数据
     */
    private void exportParetoFrontData(List<MOIndividual> paretoFront, String outputPath) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("Pareto前沿解集\n");
            writer.write(repeatString("=", 80) + "\n\n");
            writer.write(String.format("解的数量: %d\n\n", paretoFront.size()));

            writer.write(repeatString("-", 80) + "\n");
            writer.write(String.format("%-8s %-20s %-20s %-15s %-10s\n",
                "序号", "Cmax", "能耗(kWh)", "装箱质量", "批次数"));
            writer.write(repeatString("-", 80) + "\n");

            int index = 1;
            for (MOIndividual ind : paretoFront) {
                writer.write(String.format("%-8d %-20.2f %-20.2f %-15.4f %-10d\n",
                    index++,
                    ind.objectives[0],  // Cmax
                    ind.objectives[1],  // 能耗
                    ind.packingQ,       // 装箱质量
                    ind.batchCount      // 批次数
                ));
            }

            writer.write(repeatString("-", 80) + "\n\n");

            // 统计信息
            double minCmax = paretoFront.stream().mapToDouble(i -> i.objectives[0]).min().orElse(0);
            double maxCmax = paretoFront.stream().mapToDouble(i -> i.objectives[0]).max().orElse(0);
            double minEnergy = paretoFront.stream().mapToDouble(i -> i.objectives[1]).min().orElse(0);
            double maxEnergy = paretoFront.stream().mapToDouble(i -> i.objectives[1]).max().orElse(0);

            writer.write("统计信息:\n");
            writer.write(String.format("  Cmax范围: [%.2f, %.2f]\n", minCmax, maxCmax));
            writer.write(String.format("  能耗范围: [%.2f, %.2f] kWh\n", minEnergy, maxEnergy));
            writer.write(String.format("  Cmax改进: %.2f%%\n", (maxCmax - minCmax) / maxCmax * 100));
            writer.write(String.format("  能耗改进: %.2f%%\n", (maxEnergy - minEnergy) / maxEnergy * 100));
        }

        System.out.println("  ✓ Pareto前沿数据已保存: " + outputPath);
    }

    /**
     * 找到Cmax最小的解
     */
    private MOIndividual findBestCmaxSolution(List<MOIndividual> paretoFront) {
        return paretoFront.stream()
            .min((a, b) -> Double.compare(a.objectives[0], b.objectives[0]))
            .orElse(paretoFront.get(0));
    }

    /**
     * 导出甘特图
     */
    private void exportGanttChart(MOIndividual individual, String outputPath) throws IOException {
        // 转换为Chromosome并评估
        Chromosome chromosome = individual.toChromosome();

        // 使用CaculateFitness评估
        CaculateFitness fitnessCalculator = new CaculateFitness();
        double makespan = fitnessCalculator.evaluate(chromosome, problem, operationMatrix);

        // 创建ProblemFrame.Solution对象
        ProblemFrame.Solution solution = new ProblemFrame.Solution(
            operationMatrix, chromosome, problem, makespan
        );

        // 使用ScheduleVisualizer生成甘特图
        ScheduleVisualizer visualizer = new ScheduleVisualizer(
            solution, chromosome, problem, operationMatrix, null
        );
        visualizer.generateGanttChart(outputPath);

        System.out.println("  ✓ 甘特图已保存: " + outputPath);
    }

    /**
     * 导出打印批次排布图
     */
    private void exportPrinterLayout(MOIndividual individual, String outputPath) throws IOException {
        Chromosome chromosome = individual.toChromosome();

        // 确保printSolution已生成
        if (chromosome.printSolution == null) {
            CaculateFitness fitnessCalculator = new CaculateFitness();
            fitnessCalculator.evaluate(chromosome, problem, operationMatrix);
        }

        // 使用PrinterLayoutVisualizer生成打印布局图
        PrinterLayoutVisualizer layoutVisualizer = new PrinterLayoutVisualizer(
            chromosome, problem.getMachines()
        );
        layoutVisualizer.generatePrinterLayoutChart(outputPath);

        System.out.println("  ✓ 打印批次排布图已保存: " + outputPath);
    }

    /**
     * 导出详细调度记录
     */
    private void exportScheduleRecords(MOIndividual individual, String outputPath) throws IOException {
        Chromosome chromosome = individual.toChromosome();

        // 确保已评估
        CaculateFitness fitnessCalculator = new CaculateFitness();
        fitnessCalculator.evaluate(chromosome, problem, operationMatrix);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            writer.write("最优Cmax解的详细调度记录\n");
            writer.write(repeatString("=", 100) + "\n\n");

            // 1. 基本信息
            writer.write("【基本信息】\n");
            writer.write(repeatString("-", 100) + "\n");
            writer.write(String.format("Cmax (最大完工时间): %.2f\n", individual.objectives[0]));
            writer.write(String.format("能耗: %.2f kWh\n", individual.objectives[1]));
            writer.write(String.format("装箱质量: %.4f\n", individual.packingQ));
            writer.write(String.format("批次总数: %d\n", individual.batchCount));
            writer.write(String.format("工件数量: %d\n", problem.getJobCount()));
            writer.write(String.format("机器数量: %d (打印机:%d, 批处理机:%d, 离散机:%d)\n\n",
                problem.getMachineCount(),
                problem.getPrintMachineCount(),
                problem.getBatchMachineCount(),
                problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()
            ));

            // 2. 染色体编码
            writer.write("【染色体编码】\n");
            writer.write(repeatString("-", 100) + "\n");
            writer.write("工序序列 (OS): " + Arrays.toString(chromosome.gene_OS) + "\n");
            writer.write("机器选择 (MS): " + Arrays.toString(chromosome.gene_MS) + "\n\n");

            // 3. 零件加工顺序记录
            writer.write("【零件加工顺序记录】\n");
            writer.write(repeatString("-", 100) + "\n");
            exportJobProcessingOrder(writer, chromosome);

            // 4. 机器加工顺序记录
            writer.write("\n【机器加工顺序记录】\n");
            writer.write(repeatString("-", 100) + "\n");
            exportMachineProcessingOrder(writer, chromosome, individual);

            // 5. 打印批次详细信息
            writer.write("\n【打印批次详细信息】\n");
            writer.write(repeatString("-", 100) + "\n");
            exportPrintBatchDetails(writer, chromosome);
        }

        System.out.println("  ✓ 详细调度记录已保存: " + outputPath);
    }

    /**
     * 导出零件加工顺序记录（参考chapter-2）
     */
    private void exportJobProcessingOrder(BufferedWriter writer, Chromosome chromosome) throws IOException {
        writer.write(String.format("%-8s %-15s %-15s %-15s %-15s %-15s\n",
            "工件号", "工序号", "机器号", "开始时间", "结束时间", "加工时长"));
        writer.write(repeatString("-", 100) + "\n");

        for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
            writer.write(String.format("\n工件 %d:\n", jobIdx));

            for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                Operation op = operationMatrix[jobIdx][opIdx];
                if (op != null) {
                    String machineType = getMachineTypeName(op.machineNo);
                    writer.write(String.format("  %-8d %-15d %-15s %-15.2f %-15.2f %-15.2f\n",
                        op.jobNo,
                        opIdx,
                        machineType,
                        op.startTime,
                        op.endTime,
                        op.endTime - op.startTime
                    ));
                }
            }
        }
    }

    /**
     * 导出机器加工顺序记录（参考chapter-2）
     */
    private void exportMachineProcessingOrder(BufferedWriter writer, Chromosome chromosome, MOIndividual individual) throws IOException {
        int machineCount = problem.getMachineCount();

        for (int machineIdx = 0; machineIdx < machineCount; machineIdx++) {
            String machineType = getMachineTypeName(machineIdx);
            writer.write(String.format("\n%s:\n", machineType));
            writer.write(String.format("%-8s %-15s %-15s %-15s %-15s\n",
                "工件号", "工序号", "开始时间", "结束时间", "加工时长"));
            writer.write(repeatString("-", 80) + "\n");

            // 收集该机器上的所有工序
            List<Operation> machineOps = new ArrayList<>();
            for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
                for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                    Operation op = operationMatrix[jobIdx][opIdx];
                    if (op != null && op.machineNo == machineIdx) {
                        machineOps.add(op);
                    }
                }
            }

            // 按开始时间排序
            machineOps.sort((a, b) -> Double.compare(a.startTime, b.startTime));

            // 输出
            for (Operation op : machineOps) {
                writer.write(String.format("  %-8d %-15d %-15.2f %-15.2f %-15.2f\n",
                    op.jobNo,
                    op.task,
                    op.startTime,
                    op.endTime,
                    op.endTime - op.startTime
                ));
            }

            // 计算利用率
            double busyTime = machineOps.stream()
                .mapToDouble(op -> op.endTime - op.startTime)
                .sum();
            double totalTime = individual.objectives[0];  // Cmax
            double utilization = totalTime > 0 ? busyTime / totalTime * 100 : 0;

            writer.write(String.format("\n  机器利用率: %.2f%% (忙碌时间: %.2f / 总时间: %.2f)\n",
                utilization, busyTime, totalTime));
        }
    }

    /**
     * 导出打印批次详细信息
     */
    private void exportPrintBatchDetails(BufferedWriter writer, Chromosome chromosome) throws IOException {
        if (chromosome.printSolution == null) {
            writer.write("  (无打印批次信息)\n");
            return;
        }

        int printMachineCount = problem.getPrintMachineCount();
        Item[] items = problem.getItems();

        for (int printerIdx = 0; printerIdx < printMachineCount; printerIdx++) {
            List<ProgramEntity.Solution> batches = chromosome.printSolution[printerIdx];
            if (batches == null || batches.isEmpty()) {
                continue;
            }

            PrintMachine printer = (PrintMachine) problem.getMachines()[printerIdx];
            writer.write(String.format("\n打印机 %d (尺寸: %.1f x %.1f x %.1f):\n",
                printerIdx + 1, printer.L, printer.W, printer.H));
            writer.write(repeatString("-", 80) + "\n");

            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                ProgramEntity.Solution batch = batches.get(batchIdx);
                // 从placeItemList获取零件数
                int itemCount = batch.placeItemList != null ? batch.placeItemList.size() : 0;
                writer.write(String.format("  批次 %d: 零件数=%d, 占用率=%.2f%%\n",
                    batchIdx + 1,
                    itemCount,
                    batch.rate * 100
                ));

                // 列出批次中的零件
                if (batch.placeItemList != null) {
                    for (ProgramEntity.PlaceItem placeItem : batch.placeItemList) {
                        // 从PlaceItem的name中提取零件索引（假设name格式为"Item0", "Item1"等）
                        String itemName = placeItem.name;
                        if (itemName != null && itemName.startsWith("Item")) {
                            try {
                                int itemIdx = Integer.parseInt(itemName.substring(4));
                                if (itemIdx >= 0 && itemIdx < items.length) {
                                    Item item = items[itemIdx];
                                    writer.write(String.format("    - 零件 %d: 尺寸 %.1f x %.1f x %.1f\n",
                                        itemIdx, item.l, item.w, item.h));
                                }
                            } catch (NumberFormatException e) {
                                // 如果解析失败，使用PlaceItem的尺寸信息
                                writer.write(String.format("    - %s: 尺寸 %.1f x %.1f x %.1f\n",
                                    itemName, placeItem.l, placeItem.w, placeItem.h));
                            }
                        } else {
                            // 没有name或格式不对，使用PlaceItem的尺寸信息
                            writer.write(String.format("    - %s: 尺寸 %.1f x %.1f x %.1f\n",
                                itemName != null ? itemName : "未知", placeItem.l, placeItem.w, placeItem.h));
                        }
                    }
                }
            }
        }
    }

    /**
     * 获取机器类型名称
     */
    private String getMachineTypeName(int machineIdx) {
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();

        if (machineIdx < printMachineCount) {
            return "打印机" + (machineIdx + 1);
        } else if (machineIdx < printMachineCount + batchMachineCount) {
            return "批处理机" + (machineIdx - printMachineCount + 1);
        } else {
            return "离散机" + (machineIdx - printMachineCount - batchMachineCount + 1);
        }
    }

    /**
     * 重复字符串
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
}

