package ProblemFrame;

import ProgramEntity.Operation;
import ProgramEntity.Problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Random;

public class Solution {
    public Operation[][] operationMatrix; // job i operation j
    public Chromosome chromosome;
    public Problem problem;
    public Operation[][] machineMatrix; // machine i operation j
    public double cost;
    public double algrithmTimeCost;
    public Random r;

    public Solution() {

    }

    public Solution(Problem p, Random r) {
        this.problem = p;
        this.r = r;
    }

    public Solution(Operation[][] operationMatrix, Chromosome chromosome, Problem problem, double cost) {
        this.operationMatrix = new Operation[operationMatrix.length][];
        for (int i = 0; i < operationMatrix.length; i++) {
            this.operationMatrix[i] = new Operation[operationMatrix[i].length];
            for (int j = 0; j < operationMatrix[i].length; j++) {
                this.operationMatrix[i][j] = new Operation(operationMatrix[i][j]);
            }
        }

        this.problem = problem;
        this.chromosome = new Chromosome(chromosome);
        this.r = chromosome.r;
        this.cost = cost;
    }

    public void printToConsole() {
        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++) {
                // machineNo现在是1-based，直接输出
                System.out.print("Machine:" + operationMatrix[i][j].machineNo + "|Job:" + (i + 1) + "|Operation:"
                        + (j + 1));
                System.out.println("|time(" + operationMatrix[i][j].machineNo + "," + (i + 1) + ")="
                        + (operationMatrix[i][j].endTime - operationMatrix[i][j].startTime) + "|start time:"
                        + operationMatrix[i][j].startTime + "|end time:" + operationMatrix[i][j].endTime);
            }
        }
    }

    public void printToTxt() {
        StringBuilder jobNoBuilder = new StringBuilder();
        StringBuilder machineNoBuilder = new StringBuilder();
        StringBuilder operationNoBuilder = new StringBuilder();
        StringBuilder startTimeBuilder = new StringBuilder();
        StringBuilder endTimeBuilder = new StringBuilder();

        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++) {
                jobNoBuilder.append((i + 1) + " ");
                operationNoBuilder.append((j + 1) + " ");
                // machineNo现在是1-based，直接输出
                machineNoBuilder.append(operationMatrix[i][j].machineNo + " ");
                startTimeBuilder.append(operationMatrix[i][j].startTime + " ");
                endTimeBuilder.append(operationMatrix[i][j].endTime + " ");
            }
        }

        File file = new File("operationInfo.txt");// draw the picture which will
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write("工件号:" + jobNoBuilder.toString() + "\r\n");
            fw.write("工序号:" + operationNoBuilder.toString() + "\r\n");
            fw.write("机器号:" + machineNoBuilder.toString() + "\r\n");
            fw.write("开始时间:" + startTimeBuilder.toString() + "\r\n");
            fw.write("持续时间:" + endTimeBuilder.toString() + "\r\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 生成重复字符串（兼容Java 8）
     */
    private static String repeatStr(String s, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
    
    /**
     * 检查两个工件是否在XY平面上重叠
     * @param pi1 工件1
     * @param pi2 工件2
     * @return true表示重叠，false表示不重叠
     */
    private boolean isOverlap(ProgramEntity.PlaceItem pi1, ProgramEntity.PlaceItem pi2) {
        // 工件1的边界
        double left1 = pi1.x;
        double right1 = pi1.x + pi1.l;
        double bottom1 = pi1.y;
        double top1 = pi1.y + pi1.w;
        
        // 工件2的边界
        double left2 = pi2.x;
        double right2 = pi2.x + pi2.l;
        double bottom2 = pi2.y;
        double top2 = pi2.y + pi2.w;
        
        // 检查是否分离（不重叠的条件）
        // 如果一个矩形完全在另一个的左边、右边、上边或下边，则不重叠
        boolean separated = (right1 <= left2) ||  // pi1在pi2左边
                           (right2 <= left1) ||   // pi1在pi2右边
                           (top1 <= bottom2) ||   // pi1在pi2下边
                           (top2 <= bottom1);     // pi1在pi2上边
        
        return !separated;
    }
    
    /**
     * 打印详细的调度结果，包括打印阶段结果和甘特图数据
     */
    public void printDetailedSchedule() {
        System.out.println("\n" + repeatStr("=", 80));
        System.out.println("                         调度结果详细信息");
        System.out.println(repeatStr("=", 80));
        
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        int machineCount = problem.getMachineCount();
        int jobCount = problem.getJobCount();
        
        // ==================== 第一部分：打印阶段结果 ====================
        System.out.println("\n【第一阶段：3D打印结果】");
        System.out.println(repeatStr("-", 60));
        
        ProgramEntity.Machine.PrintMachine[] printMachines = new ProgramEntity.Machine.PrintMachine[printMachineCount];
        for (int i = 0; i < printMachineCount; i++) {
            printMachines[i] = (ProgramEntity.Machine.PrintMachine) problem.getMachines()[i];
        }
        
        boolean hasError = false;
        
        if (chromosome != null && chromosome.printSolution != null) {
            for (int m = 0; m < printMachineCount; m++) {
                ProgramEntity.Machine.PrintMachine pm = printMachines[m];
                System.out.println("\n▶ 打印机 " + (m + 1) + " (平台尺寸: " + 
                                 String.format("%.0f", pm.L) + " x " + 
                                 String.format("%.0f", pm.W) + ", 最大高度: " + 
                                 String.format("%.0f", pm.H) + "):");
                
                if (chromosome.printSolution[m] != null && !chromosome.printSolution[m].isEmpty()) {
                    int batchNo = 1;
                    for (Object obj : chromosome.printSolution[m]) {
                        if (obj instanceof ProgramEntity.Solution) {
                            ProgramEntity.Solution sol = (ProgramEntity.Solution) obj;
                            System.out.println("\n  批次 " + batchNo + ":");
                            System.out.println("    时间: [" + String.format("%.2f", sol.startTime) + 
                                             " ~ " + String.format("%.2f", sol.endTime) + "]");
                            System.out.println("    最大高度: " + String.format("%.2f", sol.maxG));
                            
                            // 输出每个工件的位置信息
                            if (sol.placeItemList != null && !sol.placeItemList.isEmpty()) {
                                System.out.println("    工件位置详情:");
                                System.out.println("    " + String.format("%-8s", "工件") + 
                                                 String.format("%-10s", "X坐标") + 
                                                 String.format("%-10s", "Y坐标") + 
                                                 String.format("%-8s", "长(L)") + 
                                                 String.format("%-8s", "宽(W)") + 
                                                 String.format("%-8s", "高(H)") + 
                                                 String.format("%-8s", "旋转"));
                                System.out.println("    " + repeatStr("-", 58));
                                
                                for (ProgramEntity.PlaceItem pi : sol.placeItemList) {
                                    System.out.println("    " + 
                                        String.format("%-8s", "J" + pi.name) + 
                                        String.format("%-10.2f", pi.x) + 
                                        String.format("%-10.2f", pi.y) + 
                                        String.format("%-8.2f", pi.l) + 
                                        String.format("%-8.2f", pi.w) + 
                                        String.format("%-8.2f", pi.h) + 
                                        String.format("%-8s", pi.isRotate ? "是" : "否"));
                                }
                                
                                // 验证是否超出打印机范围
                                System.out.println("\n    【位置验证】");
                                boolean batchValid = true;
                                for (ProgramEntity.PlaceItem pi : sol.placeItemList) {
                                    double endX = pi.x + pi.l;
                                    double endY = pi.y + pi.w;
                                    if (endX > pm.L || endY > pm.W) {
                                        System.out.println("    ❌ 工件J" + pi.name + " 超出打印机范围! " +
                                                         "位置[" + String.format("%.2f", pi.x) + "," + String.format("%.2f", pi.y) + "] " +
                                                         "尺寸[" + String.format("%.2f", pi.l) + "x" + String.format("%.2f", pi.w) + "] " +
                                                         "→ 终点[" + String.format("%.2f", endX) + "," + String.format("%.2f", endY) + "] " +
                                                         "超出平台[" + String.format("%.0f", pm.L) + "x" + String.format("%.0f", pm.W) + "]");
                                        batchValid = false;
                                        hasError = true;
                                    }
                                    if (pi.h > pm.H) {
                                        System.out.println("    ❌ 工件J" + pi.name + " 高度超出! " +
                                                         "工件高度=" + String.format("%.2f", pi.h) + 
                                                         " > 打印机最大高度=" + String.format("%.0f", pm.H));
                                        batchValid = false;
                                        hasError = true;
                                    }
                                }
                                
                                // 验证是否有重叠
                                for (int i = 0; i < sol.placeItemList.size(); i++) {
                                    ProgramEntity.PlaceItem pi1 = sol.placeItemList.get(i);
                                    for (int j = i + 1; j < sol.placeItemList.size(); j++) {
                                        ProgramEntity.PlaceItem pi2 = sol.placeItemList.get(j);
                                        if (isOverlap(pi1, pi2)) {
                                            System.out.println("    ❌ 工件J" + pi1.name + " 与 工件J" + pi2.name + " 重叠!");
                                            batchValid = false;
                                            hasError = true;
                                        }
                                    }
                                }
                                
                                if (batchValid) {
                                    System.out.println("    ✓ 批次" + batchNo + "位置验证通过");
                                }
                            }
                            batchNo++;
                        }
                    }
                } else {
                    System.out.println("  (无工件分配)");
                }
            }
            
            // 打印阶段总结
            System.out.println("\n" + repeatStr("-", 60));
            if (hasError) {
                System.out.println("【3D打印阶段验证结果】❌ 存在位置错误，请检查!");
            } else {
                System.out.println("【3D打印阶段验证结果】✓ 所有工件位置正确，无重叠，无越界");
            }
        } else {
            System.out.println("  打印解决方案未初始化");
        }
        
        // ==================== 第二部分：甘特图数据 ====================
        System.out.println("\n\n【甘特图数据】");
        System.out.println(repeatStr("-", 60));
        
        // 获取机器矩阵
        getMachineMatrix();
        
        String[] machineTypes = new String[machineCount];
        for (int i = 0; i < machineCount; i++) {
            if (i < printMachineCount) {
                machineTypes[i] = "打印机";
            } else if (i < printMachineCount + batchMachineCount) {
                machineTypes[i] = "批处理机";
            } else {
                machineTypes[i] = "离散加工机";
            }
        }
        
        for (int m = 0; m < machineCount; m++) {
            System.out.println("\n▶ 机器 " + (m + 1) + " (" + machineTypes[m] + "):");
            if (machineMatrix != null && machineMatrix[m] != null && machineMatrix[m].length > 0) {
                System.out.println("  " + String.format("%-8s", "工件") + 
                                 String.format("%-8s", "工序") + 
                                 String.format("%-12s", "开始时间") + 
                                 String.format("%-12s", "结束时间") + 
                                 String.format("%-10s", "持续时间"));
                System.out.println("  " + repeatStr("-", 50));
                for (Operation op : machineMatrix[m]) {
                    String opType;
                    if (op.task == 0) opType = "打印";
                    else if (op.task == 1) opType = "批处理";
                    else opType = "离散" + (op.task - 1);
                    
                    System.out.println("  " + 
                        String.format("%-8s", "J" + op.jobNo) + 
                        String.format("%-8s", opType) + 
                        String.format("%-12.2f", op.startTime) + 
                        String.format("%-12.2f", op.endTime) + 
                        String.format("%-10.2f", op.endTime - op.startTime));
                }
            } else {
                System.out.println("  (空闲)");
            }
        }
        
        // ==================== 第三部分：按工件视角的甘特图 ====================
        System.out.println("\n\n【按工件视角的调度结果】");
        System.out.println(repeatStr("-", 60));
        
        for (int j = 0; j < jobCount; j++) {
            System.out.println("\n▶ 工件 J" + j + " (共" + operationMatrix[j].length + "道工序):");
            System.out.println("  " + String.format("%-10s", "工序") + 
                             String.format("%-12s", "机器") + 
                             String.format("%-12s", "开始时间") + 
                             String.format("%-12s", "结束时间") + 
                             String.format("%-10s", "持续时间"));
            System.out.println("  " + repeatStr("-", 56));
            
            for (int op = 0; op < operationMatrix[j].length; op++) {
                Operation o = operationMatrix[j][op];
                String opType;
                String machType;
                if (op == 0) {
                    opType = "打印";
                    machType = "打印机" + o.machineNo;
                } else if (op == 1) {
                    opType = "批处理";
                    machType = "批处理机" + (o.machineNo - printMachineCount);
                } else {
                    opType = "离散" + (op - 1);
                    machType = "离散机" + (o.machineNo - printMachineCount - batchMachineCount);
                }
                
                System.out.println("  " + 
                    String.format("%-10s", opType) + 
                    String.format("%-12s", machType) + 
                    String.format("%-12.2f", o.startTime) + 
                    String.format("%-12.2f", o.endTime) + 
                    String.format("%-10.2f", o.endTime - o.startTime));
            }
        }
        
        // ==================== 第四部分：统计信息 ====================
        System.out.println("\n\n【统计信息】");
        System.out.println(repeatStr("-", 60));
        System.out.println("  最大完工时间 (Makespan): " + String.format("%.2f", cost));
        System.out.println("  工件数量: " + jobCount);
        System.out.println("  机器数量: " + machineCount + 
                         " (打印机:" + printMachineCount + 
                         ", 批处理机:" + batchMachineCount + 
                         ", 离散机:" + (machineCount - printMachineCount - batchMachineCount) + ")");
        
        // 计算机器利用率
        double totalTime = cost;
        System.out.println("\n  机器利用率:");
        for (int m = 0; m < machineCount; m++) {
            double busyTime = 0;
            if (machineMatrix != null && machineMatrix[m] != null) {
                for (Operation op : machineMatrix[m]) {
                    busyTime += (op.endTime - op.startTime);
                }
            }
            double utilization = (totalTime > 0) ? (busyTime / totalTime * 100) : 0;
            System.out.println("    机器" + (m + 1) + " (" + machineTypes[m] + "): " + 
                             String.format("%.1f%%", utilization) + 
                             " (工作时间: " + String.format("%.2f", busyTime) + ")");
        }
        
        System.out.println("\n" + repeatStr("=", 80));
    }

    public void getMachineMatrix() {
        int machineCount = problem.getMachineCount();
        ArrayList<Operation> matrix[] = new ArrayList[machineCount];
        for (int i = 0; i < machineCount; i++)
            matrix[i] = new ArrayList<Operation>();

        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++) {
                Operation tempOperation = operationMatrix[i][j];
                // machineNo是1-based，需要-1转为0-based索引
                int machIdx = tempOperation.machineNo - 1;
                if (machIdx < 0 || machIdx >= machineCount) continue; // 跳过无效机器
                ArrayList<Operation> machine = matrix[machIdx];
                int k;
                if (machine.size() == 0 || tempOperation.endTime <= machine.get(0).startTime)
                    machine.add(0, new Operation(tempOperation));
                else {
                    for (k = 0; k < machine.size() - 1; k++)
                        if (machine.get(k).endTime <= tempOperation.startTime && machine.get(k + 1).startTime >= tempOperation.endTime)
                            break;
                    machine.add(k + 1, new Operation(tempOperation));
                }
            }
        }

        this.machineMatrix = new Operation[machineCount][];
        for (int i = 0; i < machineMatrix.length; i++) {
//            machineMatrix[i] = (Operation[])matrix[i].toArray(new Operation[matrix[i].size()]);
            machineMatrix[i] = new Operation[matrix[i].size()];
            for (int j = 0; j < machineMatrix[i].length; j++) {
                machineMatrix[i][j] = matrix[i].get(j);
            }
        }
    }

//    public void printSchedPicInConsole() {
//        // 控制台输出图形
//        double start = 0, end = 0;
//        int machineNo = 0;
//        String flagString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
//        Operation tempOperation = null;
//        int j = 0, p = 0, q = 0, i = 0;
//        char ch = 'a';
//        int machineCount = problem.getMachineCount();
//        int colums = 2000;
//        char sheduleMatrix[][] = new char[machineCount][colums];
//        for (i = 0; i < machineCount; i++)
//            Arrays.fill(sheduleMatrix[i], ' ');
//        machineNo = 0;
//        int jobCount = problem.getJobCount();
//        int operCount = problem.getMaxOperationCount();
//
//        System.out.println();
//        System.out.println(" Gantt for this schedule:");
//        for (p = 0; p < jobCount; p++) {
//            ch = flagString.charAt(p);// 每一个工件对应一种字符
//            System.out.print(" job" + (p + 1) + ":" + ch + ";");
//            for (q = 0; q < operationMatrix[p].length; q++) {
//                tempOperation = operationMatrix[p][q];
//                start = tempOperation.startTime;
//                end = tempOperation.endTime;
//                machineNo = tempOperation.machineNo;
//                if (machineNo == -1)
//                    continue;
//                for (j = start; j < end; j++)
//                    sheduleMatrix[machineNo][j] = ch;
//            }
//        }
//
//        testSchedule(sheduleMatrix);
//
//        // 绘制甘特图
//        Formatter formatter = new Formatter(System.out);
//        System.out.println();
//        for (i = 0; i < machineCount; i++) {
//            formatter.format(" Machine " + (i + 1) + ":");
//            for (j = 0; j < colums; j++)
//                System.out.print(sheduleMatrix[i][j]);
//            System.out.println();
//        }
//        System.out.println();
//    }

//    private void testSchedule(char[][] sheduleMatrix) {
//        String flagString = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
//        boolean flag = true;
//        outer:
//        for (int i = 0; i < operationMatrix.length; i++) {
//            for (int j = 0; j < operationMatrix[i].length; j++) {
//                Operation o = this.operationMatrix[i][j];
//                if (o.jobNo == -1)
//                    continue;
//                char ch = flagString.charAt(o.jobNo);// 每一个工件对应一种字符
//                for (int k = o.startTime; k < o.endTime; k++) {
//                    if (sheduleMatrix[o.machineNo][k] != ch) {
//                        flag = false;
//                        break outer;
//                    }
//                }
//            }
//        }
//        if (!flag)
//            System.err.println("===========!规划错误！===========");
//    }

    public Chromosome toChromosome() {
        chromosome = new Chromosome(r);
        int jobCount = problem.getJobCount();
        
        // 新染色体结构：打印工序(jobCount) + 离散工序(totalOps - 2*jobCount)
        // 不包含批处理工序
        int chromosomeLen = 0;
        for (int i = 0; i < jobCount; i++) {
            chromosomeLen += (problem.getOperationCountArr()[i] - 1); // 不含批处理
        }
        
        chromosome.gene_MS = new int[chromosomeLen];
        chromosome.gene_OS = new int[chromosomeLen];

        // 分离打印工序和离散工序
        ArrayList<Operation> printOps = new ArrayList<>();
        ArrayList<Operation> discreteOps = new ArrayList<>();
        
        for (Operation[] ops : operationMatrix) {
            for (Operation o : ops) {
                if (o.task == 0) {
                    // 打印工序（工序0）
                    printOps.add(o);
                } else if (o.task >= 2) {
                    // 离散工序（工序2及以后）
                    discreteOps.add(o);
                }
                // 批处理工序（工序1）不加入染色体
            }
        }

        // 按开始时间排序
        printOps.sort((o1, o2) -> Double.compare(o1.startTime, o2.startTime));
        discreteOps.sort((o1, o2) -> Double.compare(o1.startTime, o2.startTime));

        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        int discreteMachineStart = printMachineCount + batchMachineCount + 1; // 离散机器起始编号(1-based)
        
        // 填充打印工序部分（前jobCount个基因）
        // 确保每个工件恰好出现一次（有效排列）
        boolean[] jobUsed = new boolean[jobCount];
        Arrays.fill(jobUsed, false);
        int fillIdx = 0;
        
        // 首先按时间顺序填充（去重）
        for (int i = 0; i < printOps.size() && fillIdx < jobCount; i++) {
            Operation o = printOps.get(i);
            if (o.jobNo >= 0 && o.jobNo < jobCount && !jobUsed[o.jobNo]) {
                chromosome.gene_OS[fillIdx] = o.jobNo;
                int machNo = o.machineNo;
                if (machNo < 1 || machNo > printMachineCount) {
                    machNo = 1;
                }
                chromosome.gene_MS[fillIdx] = machNo;
                jobUsed[o.jobNo] = true;
                fillIdx++;
            }
        }
        
        // 填充缺失的工件
        for (int j = 0; j < jobCount && fillIdx < jobCount; j++) {
            if (!jobUsed[j]) {
                chromosome.gene_OS[fillIdx] = j;
                chromosome.gene_MS[fillIdx] = 1; // 默认打印机1
                fillIdx++;
            }
        }

        // 填充离散工序部分（从jobCount开始）
        for (int i = 0; i < discreteOps.size() && (jobCount + i) < chromosomeLen; i++) {
            Operation o = discreteOps.get(i);
            chromosome.gene_OS[jobCount + i] = o.jobNo;
            // 确保离散工序的机器号在离散机器范围内
            int machNo = o.machineNo;
            if (machNo < discreteMachineStart) {
                machNo = discreteMachineStart; // 默认使用第一台离散机器
            }
            chromosome.gene_MS[jobCount + i] = machNo;
        }

        chromosome.fitness = 1.0 / cost;
        return chromosome;
    }

    public boolean checkSolution() {
        boolean[][] exist = new boolean[problem.getJobCount()][];

        for (int i = 0; i < operationMatrix.length; i++) {
            exist[i] = new boolean[problem.getOperationCountArr()[i]];
            Arrays.fill(exist[i], false);
            exist[operationMatrix[i][0].jobNo][operationMatrix[i][0].task] = true;

            // 打印工序和批处理工序的时间不从proDesMatrix获取，跳过检查
            // 只检查离散工序（task >= 2）
            for (int j = 1; j < problem.getOperationCountArr()[i]; j++) {
                Operation o = operationMatrix[i][j];
                Operation before = operationMatrix[i][j - 1];
                if (before.endTime > o.startTime) {
                    System.out.println("同一工件对应工序顺序错误！");
                    return false;
                }
                exist[o.jobNo][o.task] = true;
                
                // 只检查离散工序的加工时间
                if (o.task >= 2) {
                    int machIdx = o.machineNo - 1; // 转为0-based索引
                    double expectedTime = problem.getProDesMatrix()[problem.getOperationToIndex()[i][j]][machIdx];
                    if (Math.abs((o.endTime - o.startTime) - expectedTime) > 0.001) {
                        System.out.println("工序对应加工时间错误！:" + "job " + i + " task " + j);
                        System.out.println("实际时间:" + (o.endTime - o.startTime) + ", 期望时间:" + expectedTime);
                        return false;
                    }
                }
            }
        }

        this.getMachineMatrix();
        for (int i = 0; i < problem.getMachineCount(); i++) {
            for (int j = 1; j < machineMatrix[i].length; j++) {
                if (machineMatrix[i][j - 1].endTime > machineMatrix[i][j].startTime) {
                    System.out.println("同一机器工序加工时间错误！");
                    return false;
                }
            }
        }

        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 1; j < problem.getOperationCountArr()[i]; j++) {
                if (!exist[i][j]) {
                    System.out.println("存在未完成的工件！" + "job" + i + " task" +j + ";");
                    return false;
                }
            }
        }

        double longest = Double.MIN_VALUE;
        for (Operation[] o : operationMatrix)
            if (o[o.length - 1].endTime > longest) longest = o[o.length - 1].endTime;

        if (Math.abs(longest - cost) > 0.001) {
            System.out.println("解计算错误！longest=" + longest + ", cost=" + cost);
            return false;
        }

        System.out.println("该解正确且可行！");
        return true;
    }
}
