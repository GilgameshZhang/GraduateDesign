package AlgorthmFrame.ga;

import ProblemFrame.CaculateFitness;
import ProblemFrame.Chromosome;
import ProblemFrame.GAParameters;
import ProgramEntity.Item;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import ProgramEntity.Solution;

import java.util.*;

public class ChromosomeOperation {
    Random r;
    Problem input;
    CaculateFitness caculateFitness;  // 用于实际fitness评估
    GAParameters params;  // 参数配置

    public ChromosomeOperation() {}

    public ChromosomeOperation(Random r, Problem input) {
        this.r = r;
        this.input = input;
    }
    
    public ChromosomeOperation(Random r, Problem input, CaculateFitness caculateFitness) {
        this.r = r;
        this.input = input;
        this.caculateFitness = caculateFitness;
    }
    
    public ChromosomeOperation(Random r, Problem input, CaculateFitness caculateFitness, GAParameters params) {
        this.r = r;
        this.input = input;
        this.caculateFitness = caculateFitness;
        this.params = params;
    }
    
    /**
     * 设置参数配置
     */
    public void setParameters(GAParameters params) {
        this.params = params;
    }

    public Chromosome[] Selection(Chromosome[] parents, double pr) {
        int popNr = parents.length;
        Chromosome[] children = new Chromosome[popNr];

        // 选择策略一：精英选择。
        int num = (int) (pr * popNr);
        ArrayList<Chromosome> p = new ArrayList<>();
        Collections.addAll(p, parents);
        Collections.sort(p);
        for (int i = 0; i < num; i++) {
            children[i] = p.get(i);
        }

        // 选择策略二：锦标赛选择。
        for (int i = num; i < popNr; i++) {
            int n1 = r.nextInt(popNr);
            int n2 = r.nextInt(popNr);
            if (parents[n1].fitness < parents[n2].fitness)
                children[i] = parents[n2];
            else
                children[i] = parents[n1];
        }

//        // 选择策略三：轮盘赌选择。
//        double[] prob = new double[popNr];
//        double sum = 0.0d;
//
//        for (Chromosome i : parents)
//            sum += i.fitness;
//
//        prob[0] = parents[0].fitness / sum;
//        for (int i = 1; i < parents.length; i++) {
//            prob[i] = prob[i - 1] + parents[i].fitness / sum;
//        }
//
//        for (int i = num; i < popNr; i++) {
//            double rand_num = r.nextDouble();
//            for (int j = 0; j < parents.length; j++) {
//                if (prob[j] > rand_num) {
//                    children[i] = parents[j];
//                    break;
//                }
//            }
//        }

        return children;
    }

    // 轮盘赌选择父代个体
    public Chromosome[] Selection2(Chromosome[] parents) {
        int len = parents.length;
        double[] prob = new double[len];
        double sum = 0.0d;
        Chromosome[] chromosomes = new Chromosome[len];

        for (Chromosome i : parents) {
            sum += i.fitness;
        }

        prob[0] = parents[0].fitness / sum;
        for (int i = 1; i < parents.length; i++) {
            prob[i] = prob[i - 1] + parents[i].fitness / sum;
        }

        for (int i = 0; i < len; i++) {
            double rand_num = r.nextDouble();
            for (int j = 0; j < parents.length; j++) {
                if (prob[j] > rand_num) {
                    chromosomes[i] = parents[j];
                    break;
                }
            }
        }

        return chromosomes;
    }


    public void Crossover(Chromosome c1, Chromosome c2) {
        // 交叉会修改gene_OS和gene_MS，旧的printSolution不再有效
        // 必须在交叉前清空，避免后续变异使用过时的装箱结果
        c1.printSolution = null;
        c2.printSolution = null;
        
        int jobCount = input.getJobCount();
        
        // ✅ 打印阶段（前jobCount个）：OS和MS同步进行POX或JBX交叉
        // ✅ 离散阶段（jobCount之后）：只对OS进行POX或JBX交叉，MS保持不变
        
        double crossoverType = r.nextDouble();
        if (crossoverType < 0.5) {
            // 50% 使用POX交叉
            // 打印段：OS和MS同步交叉
            operSeqCrossoverPOX_PrintSync(c1.gene_OS, c1.gene_MS, c2.gene_OS, c2.gene_MS);
            // 离散段：只交叉OS
            operSeqCrossoverPOX_DiscreteOnly(c1.gene_OS, c2.gene_OS, jobCount);
        } else {
            // 50% 使用JBX交叉
            // 打印段：OS和MS同步交叉
            operSeqCrossoverJBX_PrintSync(c1.gene_OS, c1.gene_MS, c2.gene_OS, c2.gene_MS);
            // 离散段：只交叉OS
            operSeqCrossoverJBX_DiscreteOnly(c1.gene_OS, c2.gene_OS, jobCount);
        }
        
        // ⚠️ 注意：离散段的MS不参与交叉，因为它是固定顺序的相对索引
        
        // 修复交叉后可能出现的无效机器分配
        fixInvalidMachineAssignments(c1);
        fixInvalidMachineAssignments(c2);
        
        // 验证交叉后的染色体是否有效
        validateChromosome(c1, "交叉后-c1");
        validateChromosome(c2, "交叉后-c2");
    }
    
    /**
     * 修复染色体中无效的机器分配
     * 新编码方式：离散工序的ms[i]存储的是可选机器集合中的相对索引（1-based）
     * 在交叉、变异或初始化后调用，确保每个工序分配的机器都能加工该工序
     */
    public void fixInvalidMachineAssignments(Chromosome chromosome) {
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        int jobCount = input.getJobCount();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int[] operationCountArr = input.getOperationCountArr();
        
        // ✅ 检查离散工序段：按固定顺序（工件编号顺序）验证，与OS解耦
        int msIndex = jobCount;  // 从离散段起点开始
        
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            // 获取该工件的离散工序数量
            int discreteOpsCount = operationCountArr[jobNo] - 2;  // 减去打印和批处理
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;  // 实际工序编号
            int operIdx = operationToIndex[jobNo][operNo];
            
                // 找到所有可用机器
                List<Integer> availableMachines = new ArrayList<>();
                for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                    if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                        availableMachines.add(k + 1);
                    }
                }
                
                if (availableMachines.isEmpty()) {
                    System.out.println("❌ 错误：工件" + jobNo + "的工序" + operNo + "没有可用机器！");
                    msIndex++;
                    continue;
                }
                
                // 检查当前相对索引是否有效
                int currentRelativeIndex = ms[msIndex];
                
                // 相对索引应该在 1 到 availableMachines.size() 之间
                if (currentRelativeIndex < 1 || currentRelativeIndex > availableMachines.size()) {
                    // 相对索引无效，随机选择一个有效的相对索引
                    int newRelativeIndex = 1 + r.nextInt(availableMachines.size());
                    
                    System.out.println("[修复] J" + jobNo + " 工序" + operNo + 
                        " 相对索引 " + currentRelativeIndex + " → " + newRelativeIndex);
                    System.out.println("  MS位置=" + msIndex + ", 可用机器数=" + availableMachines.size());
                    System.out.print("  可用机器: ");
                    for (int m : availableMachines) {
                        System.out.print("M" + m + " ");
                    }
                    System.out.println();
                    
                    ms[msIndex] = newRelativeIndex;
                }
                
                msIndex++;
            }
        }
        
        // 检查打印工序段（打印工序仍使用绝对机器编号）
        int printMachineCount = input.getPrintMachineCount();
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        
        for (int i = 0; i < jobCount; i++) {
            int jobNo = os[i];
            
            // ⚠️ 检查jobNo是否有效（防止交叉算子未正确填充）
            if (jobNo < 0 || jobNo >= jobCount) {
                System.out.println("❌ 错误：打印段位置" + i + "的工件编号无效: " + jobNo);
                // 随机分配一个有效的工件编号
                jobNo = r.nextInt(jobCount);
                os[i] = jobNo;
                System.out.println("  → 修复为工件" + jobNo);
            }
            
            int machineNo = ms[i];
            
            // 验证打印机编号是否在有效范围内
            if (machineNo < 1 || machineNo > printMachineCount) {
                System.out.println("[修复] J" + jobNo + " 打印工序机器编号无效: " + machineNo);
                
                // 找到能容纳该零件的打印机
                Item item = items[jobNo];
                List<Integer> suitablePrinters = new ArrayList<>();
                for (int m = 0; m < printMachineCount; m++) {
                    PrintMachine pm = (PrintMachine) machines[m];
                    boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                    boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                    if (fitsNormal || fitsRotated) {
                        suitablePrinters.add(m + 1);
                    }
                }
                
                if (!suitablePrinters.isEmpty()) {
                    int newMachine = suitablePrinters.get(r.nextInt(suitablePrinters.size()));
                    ms[i] = newMachine;
                    System.out.println("  → 修复为 M" + newMachine);
                } else {
                    System.out.println("  → 无可用打印机！");
                }
            }
        }
    }
    
    private void validateChromosome(Chromosome c, String tag) {
        boolean hasInvalid = false;
        StringBuilder errorMsg = new StringBuilder();
        
        for (int i = 0; i < c.gene_OS.length; i++) {
            if (c.gene_OS[i] < 0) {
                hasInvalid = true;
                errorMsg.append("  位置 ").append(i).append(": gene_OS = ").append(c.gene_OS[i]).append(" (应该>=0)\n");
            }
            if (c.gene_MS[i] <= 0) {
                hasInvalid = true;
                errorMsg.append("  位置 ").append(i).append(": gene_MS = ").append(c.gene_MS[i]).append(" (应该>=1)\n");
            }
        }
        
        if (hasInvalid) {
            System.out.println("\n===== 发现无效染色体：" + tag + " =====");
            System.out.println(errorMsg.toString());
            System.out.print("gene_OS: [");
            for (int i = 0; i < c.gene_OS.length; i++) {
                System.out.print(c.gene_OS[i]);
                if (i < c.gene_OS.length - 1) System.out.print(", ");
            }
            System.out.println("]");
            System.out.print("gene_MS: [");
            for (int i = 0; i < c.gene_MS.length; i++) {
                System.out.print(c.gene_MS[i]);
                if (i < c.gene_MS.length - 1) System.out.print(", ");
            }
            System.out.println("]");
            System.out.println("=========================================\n");
        }
    }

    public void operSeqCrossoverZLL(int o1[], int o2[]) {
        // ZLL自己尝试的cross方法
        int len = o1.length;
        int jobCount = input.getJobCount();
        int[] operationCountArr = input.getOperationCountArr();

        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);

        // 生成随机的交叉起点和终点
        int start = r.nextInt(len);
        int end = r.nextInt(len);
//		start = 3;
//		end = 4;
        int temp = 0;
        if (start > end) {
            temp = start;
            start = end;
            end = temp;
        }

        // 统计
        int[] operCount = new int[jobCount];// 在标记区域内每个工件已经执行的工序数
        Arrays.fill(operCount, 0);
        for (int i = start; i <= end; i++)
            operCount[p1[i]]++;

        // 赋值
        int index = 0;// p2的下标,同时作用于p1的赋值
        for (int i = 0; i < len; i++) {
            if (i >= start && i <= end) {
                o1[i] = p1[i];
            } else {
                while (true) {
                    if (operCount[p2[index]] < operationCountArr[p2[index]]) {
                        o1[i] = p2[index];
                        operCount[p2[index]]++;
                        break;
                    } else {
                        o2[index] = p2[index];
                        index++;
                    }
                }
                o2[index] = p1[i];
                index++;
            }
        }

    }

    /**
     * 打印段POX交叉：OS和MS同步交叉（只作用于前jobCount个元素）
     */
    private void operSeqCrossoverPOX_PrintSync(int[] o1, int[] m1, int[] o2, int[] m2) {
        int jobCount = input.getJobCount();
        
        if (jobCount < 2) {
            return;  // 工件数太少，无法交叉
        }
        
        // 随机分配工件集（至少1个，最多jobCount-1个，确保两个父代都有贡献）
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = 1 + r.nextInt(jobCount - 1);  // [1, jobCount-1]
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // ⚠️ 调试输出
//        System.out.println("  [POX打印段] jobSet1=" + jobSet1 + ", jobCount=" + jobCount);
//        System.out.println("    交叉前-父代1打印段OS: " + Arrays.toString(Arrays.copyOfRange(o1, 0, jobCount)));
//        System.out.println("    交叉前-父代2打印段OS: " + Arrays.toString(Arrays.copyOfRange(o2, 0, jobCount)));
        
        // 备份打印段
        int[] p1 = new int[jobCount];
        int[] p2 = new int[jobCount];
        int[] pm1 = new int[jobCount];
        int[] pm2 = new int[jobCount];
        System.arraycopy(o1, 0, p1, 0, jobCount);
        System.arraycopy(o2, 0, p2, 0, jobCount);
        System.arraycopy(m1, 0, pm1, 0, jobCount);
        System.arraycopy(m2, 0, pm2, 0, jobCount);
        
        // 清空打印段
        Arrays.fill(o1, 0, jobCount, -1);
        Arrays.fill(o2, 0, jobCount, -1);
        Arrays.fill(m1, 0, jobCount, -1);
        Arrays.fill(m2, 0, jobCount, -1);
        
        // 从p1中选jobSet1的工件放入o1（同时移动对应的机器）
            for (int i = 0; i < jobCount; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                m1[i] = pm1[i];
                    p1[i] = -1;
                pm1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                m2[i] = pm2[i];
                    p2[i] = -1;
                pm2[i] = -1;
                }
            }

        // 填充剩余位置
            int index1 = 0;
            int index2 = 0;
            for (int i = 0; i < jobCount; i++) {
                if (o2[i] == -1) {
                while (index1 < jobCount && p1[index1] == -1) {
                        index1++;
                    }
                if (index1 < jobCount) {
                    o2[i] = p1[index1];
                    m2[i] = pm1[index1];
                    index1++;
                } else {
                    // ⚠️ 无法填充，输出调试信息
                    System.out.println("❌ POX_PrintSync: 无法填充o2[" + i + "], jobSet1=" + jobSet1);
                }
                }
                if (o1[i] == -1) {
                while (index2 < jobCount && p2[index2] == -1) {
                        index2++;
                    }
                if (index2 < jobCount) {
                    o1[i] = p2[index2];
                    m1[i] = pm2[index2];
                    index2++;
                } else {
                    // ⚠️ 无法填充，输出调试信息
                    System.out.println("❌ POX_PrintSync: 无法填充o1[" + i + "], jobSet1=" + jobSet1);
                }
            }
        }
        
        // ⚠️ 调试输出
//        System.out.println("    交叉后-子代1打印段OS: " + Arrays.toString(Arrays.copyOfRange(o1, 0, jobCount)));
//        System.out.println("    交叉后-子代2打印段OS: " + Arrays.toString(Arrays.copyOfRange(o2, 0, jobCount)));
    }
    
    /**
     * 离散段POX交叉：只交叉OS（从jobCount开始到结束）
     */
    private void operSeqCrossoverPOX_DiscreteOnly(int[] o1, int[] o2, int jobCount) {
        int totalLen = o1.length;
        int discreteLen = totalLen - jobCount;
        
        if (discreteLen <= 0) {
            return;
        }
        
        int totalJobCount = input.getJobCount();
        if (totalJobCount < 2) {
            return;  // 工件数太少，无法交叉
        }
        
        // 随机分配工件集（至少1个，最多jobCount-1个，确保两个父代都有贡献）
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < totalJobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = 1 + r.nextInt(totalJobCount - 1);  // [1, jobCount-1]
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // 备份离散段
        int[] p1 = new int[discreteLen];
        int[] p2 = new int[discreteLen];
        System.arraycopy(o1, jobCount, p1, 0, discreteLen);
        System.arraycopy(o2, jobCount, p2, 0, discreteLen);
        
        // 清空离散段
        Arrays.fill(o1, jobCount, totalLen, -1);
        Arrays.fill(o2, jobCount, totalLen, -1);
        
        // 从p1中选jobSet1的工件放入o1
        for (int i = 0; i < discreteLen; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[jobCount + i] = p1[i];
                p1[i] = -1;
            }
            if (jobSet1.contains(p2[i])) {
                o2[jobCount + i] = p2[i];
                p2[i] = -1;
            }
        }
        
        // 填充剩余位置
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < discreteLen; i++) {
            if (o2[jobCount + i] == -1) {
                while (index1 < discreteLen && p1[index1] == -1) {
                    index1++;
                }
                if (index1 < discreteLen) {
                    o2[jobCount + i] = p1[index1];
                    index1++;
                }
            }
            if (o1[jobCount + i] == -1) {
                while (index2 < discreteLen && p2[index2] == -1) {
                    index2++;
                }
                if (index2 < discreteLen) {
                    o1[jobCount + i] = p2[index2];
                    index2++;
                }
            }
        }
    }
    
    /**
     * 打印段JBX交叉：OS和MS同步交叉（只作用于前jobCount个元素）
     */
    private void operSeqCrossoverJBX_PrintSync(int[] o1, int[] m1, int[] o2, int[] m2) {
        int jobCount = input.getJobCount();
        
        if (jobCount < 2) {
            return;  // 工件数太少，无法交叉
        }
        
        // 随机分配工件集（至少1个，最多jobCount-1个，确保两个父代都有贡献）
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = 1 + r.nextInt(jobCount - 1);  // [1, jobCount-1]
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // ⚠️ 调试输出
//        System.out.println("  [JBX打印段] jobSet1=" + jobSet1 + ", jobCount=" + jobCount);
//        System.out.println("    交叉前-父代1打印段OS: " + Arrays.toString(Arrays.copyOfRange(o1, 0, jobCount)));
//        System.out.println("    交叉前-父代2打印段OS: " + Arrays.toString(Arrays.copyOfRange(o2, 0, jobCount)));
        
        // 备份打印段
        int[] p1 = new int[jobCount];
        int[] p2 = new int[jobCount];
        int[] pm1 = new int[jobCount];
        int[] pm2 = new int[jobCount];
        System.arraycopy(o1, 0, p1, 0, jobCount);
        System.arraycopy(o2, 0, p2, 0, jobCount);
        System.arraycopy(m1, 0, pm1, 0, jobCount);
        System.arraycopy(m2, 0, pm2, 0, jobCount);
        
        // 清空打印段
        Arrays.fill(o1, 0, jobCount, -1);
        Arrays.fill(o2, 0, jobCount, -1);
        Arrays.fill(m1, 0, jobCount, -1);
        Arrays.fill(m2, 0, jobCount, -1);
        
        // JBX: 保留jobSet1的工件在原位置，其他位置用另一个父代的非jobSet1工件填充
        
        // 步骤1：保留jobSet1的工件在原位置
        for (int i = 0; i < jobCount; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
                m1[i] = pm1[i];
            }
            if (jobSet1.contains(p2[i])) {
                o2[i] = p2[i];
                m2[i] = pm2[i];
            }
        }
        
        // 步骤2：收集非jobSet1的工件（用于填充）
        List<Integer> nonJobSet1_p1 = new ArrayList<>();  // 父代1中非jobSet1的工件
        List<Integer> nonJobSet1_p1_machines = new ArrayList<>();
        List<Integer> nonJobSet1_p2 = new ArrayList<>();  // 父代2中非jobSet1的工件
        List<Integer> nonJobSet1_p2_machines = new ArrayList<>();
        
        for (int i = 0; i < jobCount; i++) {
            if (!jobSet1.contains(p1[i])) {
                nonJobSet1_p1.add(p1[i]);
                nonJobSet1_p1_machines.add(pm1[i]);
            }
            if (!jobSet1.contains(p2[i])) {
                nonJobSet1_p2.add(p2[i]);
                nonJobSet1_p2_machines.add(pm2[i]);
            }
        }
        
        // 步骤3：填充空位
        int index1 = 0;  // 用于填充o1的索引（从nonJobSet1_p2取）
        int index2 = 0;  // 用于填充o2的索引（从nonJobSet1_p1取）
        
        for (int i = 0; i < jobCount; i++) {
            // 填充o1：用父代2的非jobSet1工件
            if (o1[i] == -1) {
                if (index2 < nonJobSet1_p2.size()) {
                    o1[i] = nonJobSet1_p2.get(index2);
                    m1[i] = nonJobSet1_p2_machines.get(index2);
                    index2++;
                }
            }
            // 填充o2：用父代1的非jobSet1工件
            if (o2[i] == -1) {
                if (index1 < nonJobSet1_p1.size()) {
                    o2[i] = nonJobSet1_p1.get(index1);
                    m2[i] = nonJobSet1_p1_machines.get(index1);
                    index1++;
                }
            }
        }
        
        // ⚠️ 调试输出
//        System.out.println("    交叉后-子代1打印段OS: " + Arrays.toString(Arrays.copyOfRange(o1, 0, jobCount)));
//        System.out.println("    交叉后-子代2打印段OS: " + Arrays.toString(Arrays.copyOfRange(o2, 0, jobCount)));
    }
    
    /**
     * 离散段JBX交叉：只交叉OS（从jobCount开始到结束）
     */
    private void operSeqCrossoverJBX_DiscreteOnly(int[] o1, int[] o2, int jobCount) {
        int totalLen = o1.length;
        int discreteLen = totalLen - jobCount;
        
        if (discreteLen <= 0) {
            return;
        }
        
        int totalJobCount = input.getJobCount();
        if (totalJobCount < 2) {
            return;  // 工件数太少，无法交叉
        }
        
        // 随机分配工件集（至少1个，最多jobCount-1个，确保两个父代都有贡献）
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < totalJobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = 1 + r.nextInt(totalJobCount - 1);  // [1, jobCount-1]
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // 备份离散段
        int[] p1 = new int[discreteLen];
        int[] p2 = new int[discreteLen];
        System.arraycopy(o1, jobCount, p1, 0, discreteLen);
        System.arraycopy(o2, jobCount, p2, 0, discreteLen);
        
        // 清空离散段
        Arrays.fill(o1, jobCount, totalLen, -1);
        Arrays.fill(o2, jobCount, totalLen, -1);
        
        // JBX: 保留jobSet1的工件在原位置，其他位置用另一个父代的非jobSet1工件填充
        
        // 步骤1：保留jobSet1的工件在原位置
        for (int i = 0; i < discreteLen; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[jobCount + i] = p1[i];
            }
            if (jobSet1.contains(p2[i])) {
                o2[jobCount + i] = p2[i];
            }
        }
        
        // 步骤2：收集非jobSet1的工件（用于填充）
        List<Integer> nonJobSet1_p1 = new ArrayList<>();  // 父代1中非jobSet1的工件
        List<Integer> nonJobSet1_p2 = new ArrayList<>();  // 父代2中非jobSet1的工件
        
        for (int i = 0; i < discreteLen; i++) {
            if (!jobSet1.contains(p1[i])) {
                nonJobSet1_p1.add(p1[i]);
            }
            if (!jobSet1.contains(p2[i])) {
                nonJobSet1_p2.add(p2[i]);
            }
        }
        
        // 步骤3：填充空位
        int index1 = 0;  // 用于填充o1的索引（从nonJobSet1_p2取）
        int index2 = 0;  // 用于填充o2的索引（从nonJobSet1_p1取）
        
        for (int i = 0; i < discreteLen; i++) {
            // 填充o1：用父代2的非jobSet1工件
            if (o1[jobCount + i] == -1) {
                if (index2 < nonJobSet1_p2.size()) {
                    o1[jobCount + i] = nonJobSet1_p2.get(index2);
                    index2++;
                }
            }
            // 填充o2：用父代1的非jobSet1工件
            if (o2[jobCount + i] == -1) {
                if (index1 < nonJobSet1_p1.size()) {
                    o2[jobCount + i] = nonJobSet1_p1.get(index1);
                    index1++;
                }
            }
        }
    }
    
    public void operSeqCrossoverPOX(int o1[], int o2[]) {
        // 随机分配工件集
        int jobCount = input.getJobCount();
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = r.nextInt(jobCount);
        List<Integer> jobSet1 = temp.subList(0, len1);
        List<Integer> jobSet2 = temp.subList(len1, jobCount);

//		List<Integer> jobSet1 = new ArrayList<>();
//		jobSet1.add(1);
//		List<Integer> jobSet2 = new ArrayList<>();
//		jobSet2.add(0);
//		jobSet2.add(2);
        // 分配
        int len = o1.length;
        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        Arrays.fill(o1, -1);
        Arrays.fill(o2, -1);
            for (int i = 0; i < len; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    p1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    p2[i] = -1;
                }
            }

            int index1 = 0;
            int index2 = 0;
            for (int i = 0; i < len; i++) {
                if (o2[i] == -1) {
                    while (p1[index1] == -1) {
                        index1++;
                    }
                    o2[i] = p1[index1];
                    index1++;
                }
                if (o1[i] == -1) {
                    while (p2[index2] == -1) {
                        index2++;
                    }
                    o1[i] = p2[index2];
                    index2++;
            }
        }
    }

    public void operSeqCrossoverJBX(int o1[], int o2[]) {
        // 随机分配工件集
        int jobCount = input.getJobCount();
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);

        int len1 = r.nextInt(jobCount);
        List<Integer> jobSet1 = temp.subList(0, len1);
        List<Integer> jobSet2 = temp.subList(len1, jobCount);

//		List<Integer> jobSet1 = new ArrayList<>();
//		jobSet1.add(1);
//		List<Integer> jobSet2 = new ArrayList<>();
//		jobSet2.add(0);
//		jobSet2.add(2);

        // 分配
        int len = o1.length;

        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        Arrays.fill(o1, -1);
        Arrays.fill(o2, -1);
        for (int i = 0; i < len; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
            } else {
                p1[i] = -1;
            }
            if (jobSet2.contains(p2[i])) {
                o2[i] = p2[i];
            } else {
                p2[i] = -1;
            }
        }


        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < len; i++) {
            if (o2[i] == -1) {
                while (p1[index1] == -1) {
                    index1++;
                }
                o2[i] = p1[index1];
                index1++;
            }
            if (o1[i] == -1) {
                while (p2[index2] == -1) {
                    index2++;
                }
                o1[i] = p2[index2];
                index2++;
            }
        }

    }

    public void operSeqCrossoverPOX(int o1[], int[] m1, int[] m2, int o2[], int num) {
        // 随机分配工件集
        int jobCount = input.getJobCount();
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = r.nextInt(jobCount) + 1;
        List<Integer> jobSet1 = temp.subList(0, len1);
        List<Integer> jobSet2 = temp.subList(len1, jobCount);
        //System.out.println("jobSet1: " + jobSet1);
        //System.out.println("jobSet2: " + jobSet2);
//		List<Integer> jobSet1 = new ArrayList<>();
//		jobSet1.add(1);
//		List<Integer> jobSet2 = new ArrayList<>();
//		jobSet2.add(0);
//		jobSet2.add(2);
        // 分配
        int len = o1.length;
        int[] p1 = new int[len];
        int[] p2 = new int[len];
        int[] pm1 = new int[len];
        int[] pm2 = new int[len];
        // p1, pm1, p2, pm2 是父代的备份
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(m1, 0, pm1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        System.arraycopy(m2, 0, pm2, 0, len);
        
        if (num == 0) {
            // 打印工序段交叉：只交叉打印段，离散段保持原样
            // 先将打印段设为-1，离散段保持原值
            Arrays.fill(o1, 0, jobCount, -1);
            Arrays.fill(m1, 0, jobCount, -1);
            Arrays.fill(o2, 0, jobCount, -1);
            Arrays.fill(m2, 0, jobCount, -1);
            // 离散段保持原样（复制）
            System.arraycopy(p1, jobCount, o1, jobCount, len - jobCount);
            System.arraycopy(pm1, jobCount, m1, jobCount, len - jobCount);
            System.arraycopy(p2, jobCount, o2, jobCount, len - jobCount);
            System.arraycopy(pm2, jobCount, m2, jobCount, len - jobCount);
            
            // 从p1中选jobSet1的工件放入o1的打印段
            for (int i = 0; i < jobCount; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    m1[i] = pm1[i];
                    p1[i] = -1;
                    pm1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    m2[i] = pm2[i];
                    p2[i] = -1;
                    pm2[i] = -1;
                }
            }
            int index1 = 0;
            int index2 = 0;
            for (int i = 0; i < jobCount; i++) {
                if (o2[i] == -1) {
                    // 只从打印工序段（0到jobCount-1）搜索
                    while (index1 < jobCount && p1[index1] == -1) {
                        index1++;
                    }
                    if (index1 < jobCount) {
                        o2[i] = p1[index1];
                        m2[i] = pm1[index1];
                        index1++;
                    } else {
                        // 极端情况：无法从p1填充，说明所有p1打印段都被jobSet1选中了
                        System.out.println("WARNING: 打印工序段交叉 - o2位置" + i + "无法从p1填充，使用p2原值");
                        o2[i] = p2[i];
                        m2[i] = pm2[i];
                    }
                }
                if (o1[i] == -1) {
                    // 只从打印工序段（0到jobCount-1）搜索
                    while (index2 < jobCount && p2[index2] == -1) {
                        index2++;
                    }
                    if (index2 < jobCount) {
                        o1[i] = p2[index2];
                        m1[i] = pm2[index2];
                        index2++;
                    } else {
                        // 极端情况：无法从p2填充，说明所有p2打印段都被jobSet1选中了
                        System.out.println("WARNING: 打印工序段交叉 - o1位置" + i + "无法从p2填充，使用p1原值");
                        o1[i] = p1[i];
                        m1[i] = pm1[i];
                    }
                }
            }
        } else {
            // 离散工序段交叉：使用基于工件的交叉策略
            // 打印工序段交叉：只交叉打印段，离散段保持原样
            // 先将打印段设为-1，离散段保持原值
            Arrays.fill(o1, jobCount, len, -1);
            Arrays.fill(m1, jobCount, len, -1);
            Arrays.fill(o2, jobCount, len, -1);
            Arrays.fill(m2, jobCount, len, -1);
            // 离散段保持原样（复制）
            System.arraycopy(p1, 0, o1, 0, jobCount);
            System.arraycopy(pm1, 0, m1, 0, jobCount);
            System.arraycopy(p2, 0, o2, 0, jobCount);
            System.arraycopy(pm2, 0, m2, 0, jobCount);

            // 从p1中选jobSet1的工件放入o1的打印段
            for (int i = jobCount; i < len; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    m1[i] = pm1[i];
                    p1[i] = -1;
                    pm1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    m2[i] = pm2[i];
                    p2[i] = -1;
                    pm2[i] = -1;
                }
            }
            int index1 = jobCount;
            int index2 = jobCount;
            for (int i = jobCount; i < len; i++) {
                if (o2[i] == -1) {
                    // 只从打印工序段（0到jobCount-1）搜索
                    while (index1 >= jobCount && p1[index1] == -1) {
                        index1++;
                    }
                    if (index1 >= jobCount) {
                        o2[i] = p1[index1];
                        m2[i] = pm1[index1];
                        index1++;
                    } else {
                        // 极端情况：无法从p1填充，说明所有p1打印段都被jobSet1选中了
                        System.out.println("WARNING: 打印工序段交叉 - o2位置" + i + "无法从p1填充，使用p2原值");
                        o2[i] = p2[i];
                        m2[i] = pm2[i];
                    }
                }
                if (o1[i] == -1) {
                    // 只从打印工序段（0到jobCount-1）搜索
                    while (index2 >= jobCount && p2[index2] == -1) {
                        index2++;
                    }
                    if (index2 >= jobCount) {
                        o1[i] = p2[index2];
                        m1[i] = pm2[index2];
                        index2++;
                    } else {
                        // 极端情况：无法从p2填充，说明所有p2打印段都被jobSet1选中了
                        System.out.println("WARNING: 打印工序段交叉 - o1位置" + i + "无法从p2填充，使用p1原值");
                        o1[i] = p1[i];
                        m1[i] = pm1[i];
                    }
                }
            }
        }
        
//        System.out.println("\n----- operSeqCrossoverPOX结束 -----");
//        System.out.print("输出 o1前5: [");
//        for (int i = 0; i < Math.min(5, o1.length); i++) {
//            System.out.print(o1[i] + ", ");
//        }
//        System.out.println("...]");
//        System.out.print("输出 m1前5: [");
//        for (int i = 0; i < Math.min(5, m1.length); i++) {
//            System.out.print(m1[i] + ", ");
//        }
//        System.out.println("...]");
//        System.out.print("输出 m2前5: [");
//        for (int i = 0; i < Math.min(5, m2.length); i++) {
//            System.out.print(m2[i] + ", ");
//        }
//        System.out.println("...]");
//        System.out.println("-------------------------------\n");
    }


    public void machineSeqCrossover(int m1[], int m2[]) {
        int len = m1.length;

        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(m1, 0, p1, 0, len);
        System.arraycopy(m2, 0, p2, 0, len);

        // 生成随机的交叉起点和终点
        int start = r.nextInt(len);
        int end = r.nextInt(len);

//		start = 2;
//		end = 5;

        int temp = 0;
        if (start > end) {
            temp = start;
            start = end;
            end = temp;
        }

        for (int i = start; i < end; i++) {
            m1[i] = p2[i];
            m2[i] = p1[i];
        }
    }

    /**
     * 改进的打印阶段POX交叉（只对打印段进行交叉）
     * 同时交换工序序列和对应的机器分配
     */
    public void operSeqCrossoverPOX_Print(int[] o1, int[] m1, int[] o2, int[] m2) {
        int jobCount = input.getJobCount();
        
        // 随机分配工件集
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = r.nextInt(jobCount) + 1;
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // 备份打印段
        int[] p1 = new int[jobCount];
        int[] p2 = new int[jobCount];
        int[] pm1 = new int[jobCount];
        int[] pm2 = new int[jobCount];
        System.arraycopy(o1, 0, p1, 0, jobCount);
        System.arraycopy(o2, 0, p2, 0, jobCount);
        System.arraycopy(m1, 0, pm1, 0, jobCount);
        System.arraycopy(m2, 0, pm2, 0, jobCount);
        
        // 清空打印段
        Arrays.fill(o1, 0, jobCount, -1);
        Arrays.fill(o2, 0, jobCount, -1);
        Arrays.fill(m1, 0, jobCount, -1);
        Arrays.fill(m2, 0, jobCount, -1);
        
        // 从p1中选jobSet1的工件放入o1的打印段
        for (int i = 0; i < jobCount; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
                m1[i] = pm1[i];
                p1[i] = -1;
                pm1[i] = -1;
            }
            if (jobSet1.contains(p2[i])) {
                o2[i] = p2[i];
                m2[i] = pm2[i];
                p2[i] = -1;
                pm2[i] = -1;
            }
        }
        
        // 填充剩余位置
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < jobCount; i++) {
            if (o2[i] == -1) {
                while (p1[index1] == -1) {
                    index1++;
                }
                o2[i] = p1[index1];
                m2[i] = pm1[index1];
                index1++;
            }
            if (o1[i] == -1) {
                while (p2[index2] == -1) {
                    index2++;
                }
                o1[i] = p2[index2];
                m1[i] = pm2[index2];
                index2++;
            }
        }
    }

    /**
     * 改进的打印阶段JBX交叉（只对打印段进行交叉）
     * 同时交换工序序列和对应的机器分配
     */
    public void operSeqCrossoverJBX_Print(int[] o1, int[] m1, int[] o2, int[] m2) {
        int jobCount = input.getJobCount();
        
        // 随机分配工件集
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = r.nextInt(jobCount) + 1;
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // 备份打印段
        int[] p1 = new int[jobCount];
        int[] p2 = new int[jobCount];
        int[] pm1 = new int[jobCount];
        int[] pm2 = new int[jobCount];
        System.arraycopy(o1, 0, p1, 0, jobCount);
        System.arraycopy(o2, 0, p2, 0, jobCount);
        System.arraycopy(m1, 0, pm1, 0, jobCount);
        System.arraycopy(m2, 0, pm2, 0, jobCount);
        
        // 清空打印段
        Arrays.fill(o1, 0, jobCount, -1);
        Arrays.fill(o2, 0, jobCount, -1);
        Arrays.fill(m1, 0, jobCount, -1);
        Arrays.fill(m2, 0, jobCount, -1);
        
        // JBX: 保留jobSet1的工件在原位置，其他工件位置打乱
        for (int i = 0; i < jobCount; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
                m1[i] = pm1[i];
            } else {
                p1[i] = -1;
                pm1[i] = -1;
            }
            if (jobSet1.contains(p2[i])) {
                o2[i] = p2[i];
                m2[i] = pm2[i];
            } else {
                p2[i] = -1;
                pm2[i] = -1;
            }
        }
        
        // 填充剩余位置
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < jobCount; i++) {
            if (o2[i] == -1) {
                while (p1[index1] == -1) {
                    index1++;
                }
                o2[i] = p1[index1];
                m2[i] = pm1[index1];
                index1++;
            }
            if (o1[i] == -1) {
                while (p2[index2] == -1) {
                    index2++;
                }
                o1[i] = p2[index2];
                m1[i] = pm2[index2];
                index2++;
            }
        }
    }

    /**
     * 机器层交叉：选取任意两点交换其中的序列
     */
    public void machineSeqCrossover_TwoPointSwap(int[] m1, int[] m2) {
        int len = m1.length;
        if (len <= 1) return;
        
        // 随机选择两个交叉点
        int point1 = r.nextInt(len);
        int point2 = r.nextInt(len);
        
        if (point1 > point2) {
            int temp = point1;
            point1 = point2;
            point2 = temp;
        }
        
        // 交换两点之间的序列
        for (int i = point1; i <= point2; i++) {
            int temp = m1[i];
            m1[i] = m2[i];
            m2[i] = temp;
        }
    }

    /**
     * 变异操作 - 多试探选择最优策略
     * 
     * 新的变异策略：
     * 1. 对一个染色体生成多个变异候选（3-5个）
     * 2. 每个候选随机选择一种变异类型：
     *    - OS层变异（打印段）：随机交换两点，OS和MS同步
     *    - OS层变异（离散段）：随机交换两点，只交换OS
     *    - MS层变异：随机选择点位重分配机器
     * 3. 评估所有候选的fitness
     * 4. 选择最优的候选；如果都不如原染色体，则不变异
     */
    public void Mutation(Chromosome chromosome) {
        chromosome.printSolution = null;
        
        // 如果没有fitness计算器，退回到简单变异
        if (caculateFitness == null) {
            mutateSimple(chromosome);
            return;
        }
        
        // 生成多个变异候选（3-5个）
        int numCandidates = 3 + r.nextInt(3);
        
        // 计算原染色体的fitness（如果还没有）
        if (chromosome.fitness == 0) {
            Operation[][] opMatrix = createOperationMatrix();
            initOperationMatrix(opMatrix);
            caculateFitness.evaluate(chromosome, input, opMatrix);
        }
        
        // 创建操作矩阵用于评估
        Operation[][] opMatrix = createOperationMatrix();
        initOperationMatrix(opMatrix);
        
        // 初始化最优候选（只从变异候选中选择）
        Chromosome bestCandidate = null;
        double bestFitness = Double.MAX_VALUE;
        
        // 生成并评估多个变异候选
        for (int i = 0; i < numCandidates; i++) {
            // 复制染色体
            Chromosome candidate = new Chromosome(chromosome);
            
            // 随机选择变异类型：
            // 0=打印段OS交换, 1=离散段OS交换, 2=MS机器重分配, 3=同机器打印件优化
            int mutationType = r.nextInt(4);
            
            try {
                switch (mutationType) {
                    case 0:
                        // 打印段OS交换（OS和MS同步）
                        mutatePrintSequenceSwap(candidate);
                        break;
                    case 1:
                        // 离散段OS交换（只交换OS）
                        mutateDiscreteSequenceSwap(candidate);
                        break;
                    case 2:
                        // MS机器重分配（原有逻辑）
                        mutateMachineAssignment(candidate);
                        break;
                    case 3:
                        // 同机器打印件优化（交换/2-opt/插入）
                        mutatePrintSameMachineOptimization(candidate);
                        break;
                }
                
                // 修复可能的无效机器分配
                fixInvalidMachineAssignments(candidate);
                
                // 评估候选的fitness
                caculateFitness.evaluate(candidate, input, opMatrix);
                double candidateFitness = candidate.fitness;
                
                // 更新最优候选（包括原染色体本身）
                if (candidateFitness < bestFitness) {
                    bestFitness = candidateFitness;
                    bestCandidate = candidate;
                }
            } catch (Exception e) {
                // 如果变异失败，跳过这个候选
                System.err.println("⚠️ 变异候选" + i + "生成失败: " + e.getMessage());
            }
        }
        
        // 选择最优的候选（即使不如原染色体，也接受最好的候选）
        if (bestCandidate != null) {
            chromosome.gene_OS = bestCandidate.gene_OS;
            chromosome.gene_MS = bestCandidate.gene_MS;
            chromosome.fitness = bestFitness;
            chromosome.printSolution = null;
        }
        // 如果所有候选都生成失败，保持原染色体不变
    }
    
    /**
     * 简单变异（当没有fitness计算器时使用）
     */
    private void mutateSimple(Chromosome chromosome) {
        // 随机选择一种变异类型
        int mutationType = r.nextInt(4);
        
        switch (mutationType) {
            case 0:
                mutatePrintSequenceSwap(chromosome);
                break;
            case 1:
                mutateDiscreteSequenceSwap(chromosome);
                break;
            case 2:
                mutateMachineAssignment(chromosome);
                break;
            case 3:
                mutatePrintSameMachineOptimization(chromosome);
                break;
        }
        
        fixInvalidMachineAssignments(chromosome);
    }

    /**
     * 打印段OS交换变异：随机选择两个位置，同时交换OS和MS
     */
    private void mutatePrintSequenceSwap(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        
        if (jobCount < 2) {
            return;  // 工件数太少，无法交换
        }
        
        // 随机选择两个不同的位置
        int pos1 = r.nextInt(jobCount);
        int pos2 = r.nextInt(jobCount);
        
        // 确保两个位置不同
        while (pos2 == pos1) {
            pos2 = r.nextInt(jobCount);
        }
        
        // 同时交换OS和MS
        int tempOS = chromosome.gene_OS[pos1];
        chromosome.gene_OS[pos1] = chromosome.gene_OS[pos2];
        chromosome.gene_OS[pos2] = tempOS;
        
        int tempMS = chromosome.gene_MS[pos1];
        chromosome.gene_MS[pos1] = chromosome.gene_MS[pos2];
        chromosome.gene_MS[pos2] = tempMS;
    }
    
    /**
     * 打印段同机器优化变异：针对同一台打印机上的打印件进行局部优化
     * 随机选择一种操作：
     * 1. 任意交换两个打印件
     * 2. 2-opt交换（反转子序列）
     * 3. 插入操作（移除一个，插入到另一位置）
     */
    private void mutatePrintSameMachineOptimization(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        
        if (jobCount < 2) {
            return;
        }
        
        // 找出每台打印机上的打印件位置
        Map<Integer, List<Integer>> machineToPositions = groupPrintJobsByMachine(chromosome, jobCount);
        
        // 过滤出至少有2个打印件的打印机
        List<Integer> validMachines = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : machineToPositions.entrySet()) {
            if (entry.getValue().size() >= 2) {
                validMachines.add(entry.getKey());
            }
        }
        
        if (validMachines.isEmpty()) {
            return;  // 没有打印机有2个及以上的打印件
        }
        
        // 随机选择一台打印机
        int selectedMachine = validMachines.get(r.nextInt(validMachines.size()));
        List<Integer> positions = machineToPositions.get(selectedMachine);
        
        // 随机选择一种操作类型
        int operationType = r.nextInt(3);
        
        switch (operationType) {
            case 0:
                // 任意交换两个打印件
                sameMachineSwap(chromosome, positions);
                break;
            case 1:
                // 2-opt交换（反转子序列）
                sameMachine2Opt(chromosome, positions);
                break;
            case 2:
                // 插入操作
                sameMachineInsert(chromosome, positions);
                break;
        }
    }
    
    /**
     * 识别每台打印机上的打印件位置
     * @return Map<打印机编号, 位置列表>
     */
    private Map<Integer, List<Integer>> groupPrintJobsByMachine(Chromosome chromosome, int jobCount) {
        Map<Integer, List<Integer>> machineToPositions = new HashMap<>();
        
        for (int i = 0; i < jobCount; i++) {
            int machineNo = chromosome.gene_MS[i];
            machineToPositions.computeIfAbsent(machineNo, k -> new ArrayList<>()).add(i);
        }
        
        return machineToPositions;
    }
    
    /**
     * 同一台打印机上任意交换两个打印件
     */
    private void sameMachineSwap(Chromosome chromosome, List<Integer> positions) {
        if (positions.size() < 2) {
            return;
        }
        
        // 随机选择两个不同的位置
        int idx1 = r.nextInt(positions.size());
        int idx2 = r.nextInt(positions.size());
        
        while (idx2 == idx1) {
            idx2 = r.nextInt(positions.size());
        }
        
        int pos1 = positions.get(idx1);
        int pos2 = positions.get(idx2);
        
        // 交换OS（MS保持不变，因为它们在同一台机器上）
        int tempOS = chromosome.gene_OS[pos1];
        chromosome.gene_OS[pos1] = chromosome.gene_OS[pos2];
        chromosome.gene_OS[pos2] = tempOS;
    }
    
    /**
     * 同一台打印机上进行2-opt操作（反转子序列）
     * 2-opt: 选择两个位置，反转它们之间的子序列
     */
    private void sameMachine2Opt(Chromosome chromosome, List<Integer> positions) {
        if (positions.size() < 2) {
            return;
        }
        
        // 随机选择两个不同的位置索引
        int idx1 = r.nextInt(positions.size());
        int idx2 = r.nextInt(positions.size());
        
        while (idx2 == idx1) {
            idx2 = r.nextInt(positions.size());
        }
        
        // 确保idx1 < idx2
        if (idx1 > idx2) {
            int temp = idx1;
            idx1 = idx2;
            idx2 = temp;
        }
        
        // 反转positions[idx1...idx2]之间的OS
        while (idx1 < idx2) {
            int pos1 = positions.get(idx1);
            int pos2 = positions.get(idx2);
            
            int tempOS = chromosome.gene_OS[pos1];
            chromosome.gene_OS[pos1] = chromosome.gene_OS[pos2];
            chromosome.gene_OS[pos2] = tempOS;
            
            idx1++;
            idx2--;
        }
    }
    
    /**
     * 同一台打印机上进行插入操作
     * 插入: 移除一个打印件，插入到另一个位置
     */
    private void sameMachineInsert(Chromosome chromosome, List<Integer> positions) {
        if (positions.size() < 2) {
            return;
        }
        
        // 随机选择移除位置和插入位置
        int removeIdx = r.nextInt(positions.size());
        int insertIdx = r.nextInt(positions.size());
        
        while (insertIdx == removeIdx) {
            insertIdx = r.nextInt(positions.size());
        }
        
        int removePos = positions.get(removeIdx);
        int insertPos = positions.get(insertIdx);
        
        // 保存要移除的工件
        int removedJob = chromosome.gene_OS[removePos];
        
        // 根据方向进行移动
        if (removePos < insertPos) {
            // 向右插入：将[removePos+1, insertPos]的元素左移
            for (int i = removePos; i < insertPos; i++) {
                chromosome.gene_OS[i] = chromosome.gene_OS[i + 1];
            }
            chromosome.gene_OS[insertPos] = removedJob;
        } else {
            // 向左插入：将[insertPos, removePos-1]的元素右移
            for (int i = removePos; i > insertPos; i--) {
                chromosome.gene_OS[i] = chromosome.gene_OS[i - 1];
            }
            chromosome.gene_OS[insertPos] = removedJob;
        }
    }
    
    /**
     * 离散段OS交换变异：随机选择两个位置，只交换OS（MS固定不变）
     */
    private void mutateDiscreteSequenceSwap(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        int totalLen = chromosome.gene_OS.length;
        int discreteLen = totalLen - jobCount;
        
        if (discreteLen < 2) {
            return;  // 离散工序太少，无法交换
        }
        
        // 随机选择离散段的两个不同位置
        int pos1 = jobCount + r.nextInt(discreteLen);
        int pos2 = jobCount + r.nextInt(discreteLen);
        
        // 确保两个位置不同
        while (pos2 == pos1) {
            pos2 = jobCount + r.nextInt(discreteLen);
        }
        
        // 只交换OS，MS保持不变（因为MS是固定顺序）
        int tempOS = chromosome.gene_OS[pos1];
        chromosome.gene_OS[pos1] = chromosome.gene_OS[pos2];
        chromosome.gene_OS[pos2] = tempOS;
    }
    
    /**
     * MS机器重分配变异：随机选择若干点位重新分配机器
     * （保留原有的MS变异逻辑）
     */
    private void mutateMachineAssignment(Chromosome chromosome) {
        // 随机选择变异点的数量（1到3个点位）
        int mutationPoints = 1 + r.nextInt(3);
        
        int jobCount = input.getJobCount();
        int totalMSLength = chromosome.gene_MS.length;
        
        Set<Integer> selectedMSPositions = new HashSet<>();
        
        // 随机选择MS变异位置（打印段或离散段）
        while (selectedMSPositions.size() < mutationPoints && selectedMSPositions.size() < totalMSLength) {
            selectedMSPositions.add(r.nextInt(totalMSLength));
        }
        
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int[] operationCountArr = input.getOperationCountArr();
        
        // 对每个选中的MS位置进行变异
        for (int msPos : selectedMSPositions) {
            if (msPos < jobCount) {
                // 打印工序段：根据OS[msPos]确定工件
                int jobNo = chromosome.gene_OS[msPos];
                mutatePrintOperation(chromosome, msPos, jobNo);
            } else {
                // 离散工序段：根据msPos计算工件和工序编号（固定顺序）
                int offset = msPos - jobCount;
                int jobNo = 0;
                int localOperNo = offset;
                
                // 确定是哪个工件的哪道离散工序
                for (int j = 0; j < jobCount; j++) {
                    int discreteOpsCount = operationCountArr[j] - 2;
                    if (localOperNo < discreteOpsCount) {
                        jobNo = j;
                        break;
                    }
                    localOperNo -= discreteOpsCount;
                }
                
                int operNo = 2 + localOperNo;
                mutateDiscreteOperationByMS(chromosome, msPos, jobNo, operNo, proDesMatrix, operationToIndex);
            }
        }
    }

    /**
     * 打印工序的变异
     * 50%选择打印层高最高的打印机，50%随机选择打印机
     */
    private void mutatePrintOperation(Chromosome chromosome, int pos, int jobNo) {
        Item[] items = input.getItems();
        int printMachineCount = input.getPrintMachineCount();
        Machine[] machines = input.getMachines();
        
        // 获取零件尺寸
        Item item = items[jobNo];
        
        // 找到所有可以容纳该零件的打印机
        List<Integer> availablePrinters = new ArrayList<>();
        List<Double> printerHeights = new ArrayList<>();
        
        for (int i = 0; i < printMachineCount; i++) {
            PrintMachine pm = (PrintMachine) machines[i];
            // 检查零件尺寸是否能放入打印机（考虑旋转）
            boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
            boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
            if (fitsNormal || fitsRotated) {
                availablePrinters.add(i + 1);  // 1-based
                printerHeights.add(pm.printH);  // 打印层高
            }
        }
        
        if (availablePrinters.isEmpty()) {
            return;  // 没有可用打印机，不变异
        }
        
        // 50%选择打印层高最高的，50%随机选择
        if (r.nextDouble() < 0.5) {
            // 选择打印层高最高的打印机
            int maxHeightIndex = 0;
            double maxHeight = printerHeights.get(0);
            for (int i = 1; i < printerHeights.size(); i++) {
                if (printerHeights.get(i) > maxHeight) {
                    maxHeight = printerHeights.get(i);
                    maxHeightIndex = i;
                }
            }
            chromosome.gene_MS[pos] = availablePrinters.get(maxHeightIndex);
        } else {
            // 随机选择一个打印机
            chromosome.gene_MS[pos] = availablePrinters.get(r.nextInt(availablePrinters.size()));
        }
    }

    /**
     * 离散工序的变异（旧方法，保留以防需要）
     * 新编码方式：生成可选机器集合中的相对索引（1-based）
     * 50%选择加工时间最短的机器，50%随机选择机器
     */
    private void mutateDiscreteOperation(Chromosome chromosome, int pos, int jobNo, 
                                         double[][] proDesMatrix, int[][] operationToIndex) {
        int jobCount = input.getJobCount();
        
        // 计算这是该工件的第几次出现（离散工序编号）
        int operNo = 2;  // 从工序2开始（工序0=打印，工序1=批处理）
        for (int j = jobCount; j < pos; j++) {
            if (chromosome.gene_OS[j] == jobNo) {
                operNo++;
            }
        }
        
        // 获取工序在proDesMatrix中的索引
        if (operNo >= operationToIndex[jobNo].length) {
            return;  // 工序编号越界，不变异
        }
        int operIdx = operationToIndex[jobNo][operNo];
        
        // 找到所有可用机器及其加工时间
        List<Integer> availableMachines = new ArrayList<>();
        List<Double> processingTimes = new ArrayList<>();
        
        for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
            if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                availableMachines.add(k + 1);  // 存储实际机器编号（1-based）
                processingTimes.add(proDesMatrix[operIdx][k]);
            }
        }
        
        if (availableMachines.isEmpty()) {
            return;  // 没有可用机器，不变异
        }
        
        // 50%选择加工时间最短的，50%随机选择
        int selectedRelativeIndex;
        if (r.nextDouble() < 0.5) {
            // 选择加工时间最短的机器
            int minTimeIndex = 0;
            double minTime = processingTimes.get(0);
            for (int i = 1; i < processingTimes.size(); i++) {
                if (processingTimes.get(i) < minTime) {
                    minTime = processingTimes.get(i);
                    minTimeIndex = i;
                }
            }
            selectedRelativeIndex = minTimeIndex + 1;  // 转换为1-based相对索引
        } else {
            // 随机选择一个机器
            int randomIndex = r.nextInt(availableMachines.size());
            selectedRelativeIndex = randomIndex + 1;  // 转换为1-based相对索引
        }
        
        // 存储相对索引而不是绝对机器编号
        chromosome.gene_MS[pos] = selectedRelativeIndex;
    }
    
    /**
     * ✅ 离散工序的变异（新方法，基于MS的固定位置）
     * 直接根据工件编号和工序编号操作MS，与OS解耦
     * 50%选择加工时间最短的机器，50%随机选择机器
     */
    private void mutateDiscreteOperationByMS(Chromosome chromosome, int msPos, int jobNo, int operNo,
                                             double[][] proDesMatrix, int[][] operationToIndex) {
        // 获取工序在proDesMatrix中的索引
        if (operNo >= operationToIndex[jobNo].length) {
            return;  // 工序编号越界，不变异
        }
        int operIdx = operationToIndex[jobNo][operNo];
        
        // 找到所有可用机器及其加工时间
        List<Integer> availableMachines = new ArrayList<>();
        List<Double> processingTimes = new ArrayList<>();
        
        for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
            if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                availableMachines.add(k + 1);  // 存储实际机器编号（1-based）
                processingTimes.add(proDesMatrix[operIdx][k]);
            }
        }
        
        if (availableMachines.isEmpty()) {
            return;  // 没有可用机器，不变异
        }
        
        // 50%选择加工时间最短的，50%随机选择
        int selectedRelativeIndex;
        if (r.nextDouble() < 0.5) {
            // 选择加工时间最短的机器
            int minTimeIndex = 0;
            double minTime = processingTimes.get(0);
            for (int i = 1; i < processingTimes.size(); i++) {
                if (processingTimes.get(i) < minTime) {
                    minTime = processingTimes.get(i);
                    minTimeIndex = i;
                }
            }
            selectedRelativeIndex = minTimeIndex + 1;  // 转换为1-based相对索引
        } else {
            // 随机选择一个机器
            int randomIndex = r.nextInt(availableMachines.size());
            selectedRelativeIndex = randomIndex + 1;  // 转换为1-based相对索引
        }
        
        // 直接修改MS的固定位置
        chromosome.gene_MS[msPos] = selectedRelativeIndex;
    }
    
    /**
     * 局部搜索：对染色体进行局部改进
     * 使用 N1、N2、N3、N4 和 N5 五个邻域动作
       * N1: 基于打印时间的跨机移动（轮盘赌选择零件，高度越高权重越大）
       * N2: 基于面积占用率的跨机移动（轮盘赌选择零件，面积越大权重越大）
     * N3: 相邻批次交换
       * N4: 关键块首尾相邻工序交换（离散处理阶段优化）
       * N5: 关键工序机器重分配（轮盘赌选择，离散处理阶段优化）
     * 
     * @param chromosome 要改进的染色体
     * @param maxIterations 最大迭代次数
     */
    public void LocalSearch(Chromosome chromosome, int maxIterations) {
        if (caculateFitness == null) {
            return;  // 没有评估器，跳过局部搜索
        }
        
        // 创建临时的operationMatrix用于evaluate
        Operation[][] tempOpMatrix = createOperationMatrix();
        
        // 评估当前解的makespan
        double currentMakespan = caculateFitness.evaluate(chromosome, input, tempOpMatrix);
        chromosome.fitness = GA.FITNESS_SCALE / currentMakespan;
        
        int noImprovementCount = 0;
        // 使用参数化的无改进停止阈值，如果params为null则使用默认值
        int maxNoImprovement = (params != null) ? params.localSearchNoImprove : 3;
        
        for (int iter = 0; iter < maxIterations; iter++) {
            // 轮换使用 N1、N2、N3、N4 和 N5
            int neighborhoodType = iter % 5;
            
            boolean improved = false;
            
            if (neighborhoodType == 0) {
                // N1: 基于打印时间的跨机移动（打印段优化）
                // 检查N1是否启用
                boolean enableN1 = (params != null) ? params.enableN1 : true;
                if (!enableN1) {
                    continue;
                }
                
                List<Move> candidates = new ArrayList<>();
                // 使用参数化的候选数量，如果params为null则使用默认值
                int n1Cand = (params != null) ? params.n1n2Candidates : 10;
                candidates = generateMovesN1(chromosome, n1Cand);
                
                if (!candidates.isEmpty()) {
                    // 打乱候选顺序（首次改进策略）
                    Collections.shuffle(candidates);
                    
                    // 逐个评估候选移动，找到第一个改进就接受
                    for (Move move : candidates) {
                        // 应用移动
                        applyMove(chromosome, move);
                        
                        // 重置operationMatrix
                        initOperationMatrix(tempOpMatrix);
                        
                        // 评估新的makespan
                        double newMakespan = caculateFitness.evaluate(chromosome, input, tempOpMatrix);
                        
                        if (newMakespan < currentMakespan) {
                            // 找到改进，接受该移动
                            //System.out.println("N1局部搜索有效");
                            currentMakespan = newMakespan;
                            chromosome.fitness = GA.FITNESS_SCALE / newMakespan;
                            improved = true;
                            noImprovementCount = 0;
                            break;
                        } else {
                            // 没有改进，撤销移动
                            undoMove(chromosome, move);
                        }
                    }
                }
            } else if (neighborhoodType == 1) {
                // N2: 基于面积占用率的跨机移动（打印段优化）
                // 检查N2是否启用
                boolean enableN2 = (params != null) ? params.enableN2 : true;
                if (!enableN2) {
                    continue;
                }
                
                List<Move> candidates = new ArrayList<>();
                // 使用参数化的候选数量，如果params为null则使用默认值
                int n2Cand = (params != null) ? params.n1n2Candidates : 10;
                candidates = generateMovesN2(chromosome, n2Cand);
                
                if (!candidates.isEmpty()) {
                    // 打乱候选顺序（首次改进策略）
                    Collections.shuffle(candidates);
                    
                    // 逐个评估候选移动，找到第一个改进就接受
                    for (Move move : candidates) {
                        // 应用移动
                        applyMove(chromosome, move);
                        
                        // 重置operationMatrix
                        initOperationMatrix(tempOpMatrix);
                        
                        // 评估新的makespan
                        double newMakespan = caculateFitness.evaluate(chromosome, input, tempOpMatrix);
                        
                        if (newMakespan < currentMakespan) {
                            // 找到改进，接受该移动
                            //System.out.println("N2局部搜索有效");
                            currentMakespan = newMakespan;
                            chromosome.fitness = GA.FITNESS_SCALE / newMakespan;
                            improved = true;
                            noImprovementCount = 0;
                            break;
                        } else {
                            // 没有改进，撤销移动
                            undoMove(chromosome, move);
                        }
                    }
                }
            } else if (neighborhoodType == 2) {
                // 检查N3是否启用
                boolean enableN3 = (params != null) ? params.enableN3 : true;
                if (!enableN3) {
                    continue;
                }
                
                // N3: 相邻批次交换（打印段优化）
                int n3Cand = (params != null) ? params.n3Candidates : 5;
                List<BatchSwapMove> batchCandidates = generateMovesN3(chromosome, n3Cand);
                
                if (!batchCandidates.isEmpty()) {
                    // 打乱候选顺序
                    Collections.shuffle(batchCandidates);
                    
                    // 逐个评估候选批次交换
                    for (BatchSwapMove move : batchCandidates) {
                        // 应用批次交换
                        applyBatchSwap(chromosome, move);
                        
                        // 重置operationMatrix
                        initOperationMatrix(tempOpMatrix);
                        
                        // 评估新的makespan
                        double newMakespan = caculateFitness.evaluate(chromosome, input, tempOpMatrix);
                        
                        if (newMakespan < currentMakespan) {
                            // 找到改进，接受该移动
                            //System.out.println("局部搜索2有效");
                            currentMakespan = newMakespan;
                            chromosome.fitness = GA.FITNESS_SCALE / newMakespan;
                            improved = true;
                            noImprovementCount = 0;
                            break;
                        } else {
                            // 没有改进，撤销移动
                            undoBatchSwap(chromosome, move);
                        }
                    }
                }
            } else if (neighborhoodType == 3) {
                 // 检查N4是否启用
                boolean enableN4 = (params != null) ? params.enableN4 : true;
                if (!enableN4) {
                    continue;
                }
                
                 // N4: 关键块首尾相邻工序交换（离散段优化）
                int n4Cand = (params != null) ? params.n4Candidates : 8;
                List<CriticalBlockMove> criticalCandidates = generateMovesN4(chromosome, tempOpMatrix, n4Cand);

                if (!criticalCandidates.isEmpty()) {
                    // 打乱候选顺序
                    Collections.shuffle(criticalCandidates);

                    // 逐个评估候选关键块移动
                    for (CriticalBlockMove move : criticalCandidates) {
                        // 应用关键块移动
                        //System.out.println("执行N4");
                        applyCriticalBlockMove(chromosome, move);

                        // 重置operationMatrix
                        initOperationMatrix(tempOpMatrix);

                        // 评估新的makespan
                        double newMakespan = caculateFitness.evaluate(chromosome, input, tempOpMatrix);

                        if (newMakespan < currentMakespan) {
                            // 找到改进，接受该移动
                            //System.out.println("局部搜索3有效");
                            currentMakespan = newMakespan;
                            chromosome.fitness = GA.FITNESS_SCALE / newMakespan;
                            improved = true;
                            noImprovementCount = 0;
                            break;
                        } else {
                            // 没有改进，撤销移动
                            undoCriticalBlockMove(chromosome, move);
                        }
                    }
                }
            } else {
                 // 检查N5是否启用
                boolean enableN5 = (params != null) ? params.enableN5 : true;
                if (!enableN5) {
                    continue;
                }
                
                 // N5: 关键工序机器重分配（离散段优化 - 轮盘赌选择）
                int n5Cand = (params != null) ? params.n5Candidates : 6;
                List<MachineReassignmentMove> reassignCandidates = generateMovesN5(chromosome, tempOpMatrix, n5Cand);

                if (!reassignCandidates.isEmpty()) {
                    // 打乱候选顺序
                    Collections.shuffle(reassignCandidates);

                    // 逐个评估候选机器重分配移动
                    for (MachineReassignmentMove move : reassignCandidates) {
                        // 应用机器重分配
                        applyMachineReassignment(chromosome, move);

                        // 重置operationMatrix
                        initOperationMatrix(tempOpMatrix);

                        // 评估新的makespan
                        double newMakespan = caculateFitness.evaluate(chromosome, input, tempOpMatrix);

                        if (newMakespan < currentMakespan) {
                            // 找到改进，接受该移动
                            //System.out.println("离散段机器有效");
                            currentMakespan = newMakespan;
                            chromosome.fitness = GA.FITNESS_SCALE / newMakespan;
                            improved = true;
                            noImprovementCount = 0;
                            break;
                        } else {
                            // 没有改进，撤销移动
                            undoMachineReassignment(chromosome, move);
                        }
                    }
                }
            }
            
            if (!improved) {
                noImprovementCount++;
                if (noImprovementCount >= maxNoImprovement) {
                    break;  // 连续多次无改进，停止搜索
                }
            }
        }
    }
    
    /**
     * N1邻域：生成基于打印时间的跨机移动候选
     * 策略：从打印完工时间最长的机器中用轮盘赌选择零件（高度越高权重越大），
     *      移动到能够满足要求且打印完工时间最短的机器
     */
    /**
     * N1邻域：将加工时间最长机器上最高的零件分配给加工时间最短的打印机
     */
    private List<Move> generateMovesN1(Chromosome chromosome, int maxCandidates) {
        List<Move> moves = new ArrayList<>();
        
        // 必须先evaluate才有printSolution
        if (chromosome.printSolution == null) {
            return moves;
        }
        
        List<Solution>[] batchSolution = chromosome.printSolution;
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int printCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        
        // 步骤1：找到打印完工时间（加工时间）最长的机器
        int maxTimeMachineIdx = -1;
        double maxTime = Double.NEGATIVE_INFINITY;
        
        // ✅ 只遍历打印机范围
        int searchLimit = Math.min(printMachineCount, batchSolution.length);
        
        for (int i = 0; i < searchLimit; i++) {
            List<Solution> machineSolution = batchSolution[i];
            if (machineSolution == null || machineSolution.isEmpty()) {
                continue;
            }
            
            double endTime = machineSolution.get(machineSolution.size() - 1).endTime;
            if (endTime > maxTime) {
                maxTime = endTime;
                maxTimeMachineIdx = i;
            }
        }
        
        if (maxTimeMachineIdx == -1) {
            return moves;
        }
        
        int maxTimeMachineNo = maxTimeMachineIdx + 1;  // 1-based
        
        // 步骤2：在该机器上找到最高的零件
        int highestJobIdx = -1;
        double maxHeight = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < printCount; i++) {
            if (ms[i] == maxTimeMachineNo) {
                int jobNo = os[i];
                Item item = items[jobNo];
                
                if (item.h > maxHeight) {
                    maxHeight = item.h;
                    highestJobIdx = i;
                }
            }
        }
        
        if (highestJobIdx == -1) {
            return moves;
        }
        
        int jobNo = os[highestJobIdx];
            Item item = items[jobNo];
            
        // 步骤3：找到加工时间（完工时间）最短的打印机
        int minTimeMachineIdx = -1;
        double minTime = Double.POSITIVE_INFINITY;
            
            for (int i = 0; i < printMachineCount; i++) {
            if (i == maxTimeMachineIdx) continue;  // 跳过源机器
            
            // ✅ 安全地获取打印机
            if (i >= machines.length || !(machines[i] instanceof PrintMachine)) {
                continue;
            }
                
                PrintMachine targetMachine = (PrintMachine) machines[i];
                boolean fitsNormal = (item.l <= targetMachine.L && item.w <= targetMachine.W && item.h <= targetMachine.H);
                boolean fitsRotated = (item.w <= targetMachine.L && item.l <= targetMachine.W && item.h <= targetMachine.H);
                
                if (fitsNormal || fitsRotated) {
                    // 检查该机器的打印完工时间
                List<Solution> machineSolution = (i < batchSolution.length) ? batchSolution[i] : null;
                    double endTime = 0.0;
                    if (machineSolution != null && !machineSolution.isEmpty()) {
                        endTime = machineSolution.get(machineSolution.size() - 1).endTime;
                    }
                    
                if (endTime < minTime) {
                    minTime = endTime;
                    minTimeMachineIdx = i;
                    }
                }
            }
            
            // 如果找到了合适的目标机器，创建移动
        if (minTimeMachineIdx != -1) {
            Move move = new Move(highestJobIdx, maxTimeMachineNo, minTimeMachineIdx + 1);
                moves.add(move);
        }
        
        return moves;
    }
    
    /**
     * N2邻域：将最后一批中高度最高的零件转移到最后一批中面积利用率最小的机器
     */
    private List<Move> generateMovesN2(Chromosome chromosome, int maxCandidates) {
        List<Move> moves = new ArrayList<>();
        
        if (chromosome.printSolution == null) {
            return moves;
        }
        
        List<Solution>[] batchSolution = chromosome.printSolution;
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int printCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        
        // 步骤1：在所有打印机的最后一批中，找到高度最高的零件
        int highestJobIdx = -1;
        int highestJobMachineIdx = -1;
        double maxHeight = Double.NEGATIVE_INFINITY;
        
        // ✅ 只遍历打印机范围
        int searchLimit = Math.min(printMachineCount, batchSolution.length);
        
        for (int i = 0; i < searchLimit; i++) {
            List<Solution> machineSolution = batchSolution[i];
            if (machineSolution == null || machineSolution.isEmpty()) continue;
            
            Solution lastBatch = machineSolution.get(machineSolution.size() - 1);
            
            // 遍历该批次中的所有零件
            for (ProgramEntity.PlaceItem placeItem : lastBatch.placeItemList) {
                int jobNo = Integer.parseInt(placeItem.name);
                Item item = items[jobNo];
                
                if (item.h > maxHeight) {
                    maxHeight = item.h;
                    highestJobMachineIdx = i;
                    
                    // 在gene_OS中找到该零件的位置
                    for (int j = 0; j < printCount; j++) {
                        if (os[j] == jobNo && ms[j] == (i + 1)) {
                            highestJobIdx = j;
                            break;
                        }
                    }
                }
            }
        }
        
        if (highestJobIdx == -1) {
            return moves;
        }
        
        int jobNo = os[highestJobIdx];
            Item item = items[jobNo];
            
        // 步骤2：找到最后一批中面积利用率最小的机器（只遍历打印机）
        int minRateMachineIdx = -1;
        double minRate = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < printMachineCount; i++) {  // ✅ 只遍历打印机
            if (i == highestJobMachineIdx) continue;  // 跳过源机器
            
            // ✅ 安全地获取打印机
            if (i >= machines.length || !(machines[i] instanceof PrintMachine)) {
                continue;
            }
            
            PrintMachine targetMachine = (PrintMachine) machines[i];
            List<Solution> machineSolution = (i < batchSolution.length) ? batchSolution[i] : null;
            
            if (machineSolution == null || machineSolution.isEmpty()) {
                // 空机器，利用率为0（最小）
                        boolean fitsNormal = (item.l <= targetMachine.L && item.w <= targetMachine.W && item.h <= targetMachine.H);
                        boolean fitsRotated = (item.w <= targetMachine.L && item.l <= targetMachine.W && item.h <= targetMachine.H);
                        
                        if (fitsNormal || fitsRotated) {
                    minRate = 0.0;
                    minRateMachineIdx = i;
                }
                continue;
            }
            
            Solution lastBatch = machineSolution.get(machineSolution.size() - 1);
            double rate = lastBatch.rate;
            
            // 检查该机器能否容纳要移动的零件
            boolean fitsNormal = (item.l <= targetMachine.L && item.w <= targetMachine.W && item.h <= targetMachine.H);
            boolean fitsRotated = (item.w <= targetMachine.L && item.l <= targetMachine.W && item.h <= targetMachine.H);
            
            if ((fitsNormal || fitsRotated) && rate < minRate) {
                minRate = rate;
                minRateMachineIdx = i;
            }
        }
        
        // 如果找到了合适的目标机器，创建移动
        if (minRateMachineIdx != -1) {
            Move move = new Move(highestJobIdx, highestJobMachineIdx + 1, minRateMachineIdx + 1);
                            moves.add(move);
        }
                            
                                return moves;
                            }
    
    /**
     * 基于高度的轮盘赌选择零件（高度越高权重越大）
     * 
     * @param candidateIndices 候选零件的索引列表
     * @param heights 对应的高度列表
     * @return 选中的零件索引，如果失败返回-1
     */
    private int selectPartByRouletteForHeight(List<Integer> candidateIndices, List<Double> heights) {
        if (candidateIndices.isEmpty() || heights.isEmpty()) {
            return -1;
        }
        
        if (candidateIndices.size() == 1) {
            return candidateIndices.get(0);
        }
        
        // 计算权重（高度越高权重越大）
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0;
        
        for (double height : heights) {
            double weight = height;  // 直接使用高度作为权重
            weights.add(weight);
            totalWeight += weight;
        }
        
        if (totalWeight <= 0) {
            // 如果总权重为0，随机选择一个
            return candidateIndices.get(r.nextInt(candidateIndices.size()));
        }
        
        // 轮盘赌选择
        double randomValue = r.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return candidateIndices.get(i);
            }
        }
        
        // 默认返回最后一个
        return candidateIndices.get(candidateIndices.size() - 1);
    }
    
    /**
     * 基于面积的轮盘赌选择零件（面积越大权重越大）
     * 
     * @param candidateIndices 候选零件的索引列表
     * @param areas 对应的面积列表
     * @return 选中的零件索引，如果失败返回-1
     */
    private int selectPartByRouletteForArea(List<Integer> candidateIndices, List<Double> areas) {
        if (candidateIndices.isEmpty() || areas.isEmpty()) {
            return -1;
        }
        
        if (candidateIndices.size() == 1) {
            return candidateIndices.get(0);
        }
        
        // 计算权重（面积越大权重越大）
        List<Double> weights = new ArrayList<>();
        double totalWeight = 0;
        
        for (double area : areas) {
            double weight = area;  // 直接使用面积作为权重
            weights.add(weight);
            totalWeight += weight;
        }
        
        if (totalWeight <= 0) {
            // 如果总权重为0，随机选择一个
            return candidateIndices.get(r.nextInt(candidateIndices.size()));
        }
        
        // 轮盘赌选择
        double randomValue = r.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return candidateIndices.get(i);
            }
        }
        
        // 默认返回最后一个
        return candidateIndices.get(candidateIndices.size() - 1);
    }
    
    /**
     * N3邻域：生成相邻批次交换的候选
     * 策略：在同一台打印机上，交换相邻两个批次的执行顺序
     */
    private List<BatchSwapMove> generateMovesN3(Chromosome chromosome, int maxCandidates) {
        List<BatchSwapMove> moves = new ArrayList<>();
        
        if (chromosome.printSolution == null) {
            return moves;
        }


        List<Solution>[] batchSolution = chromosome.printSolution;
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        int printCount = input.getJobCount();

        // 找到打印完工时间最长的机器
        int maxMachineIdx = -1;
        double maxTime = Double.NEGATIVE_INFINITY;

        for (int i = 0; i < batchSolution.length; i++) {
            List<Solution> machineSolution = batchSolution[i];
            if (machineSolution == null || machineSolution.isEmpty()) {
                continue;
            }

            double endTime = machineSolution.get(machineSolution.size() - 1).endTime;
            if (endTime > maxTime) {
                maxTime = endTime;
                maxMachineIdx = i;
            }
        }

        if (maxMachineIdx == -1) {
            return moves;
        }
        // 遍历每台打印机
            List<Solution> machineBatches = batchSolution[maxMachineIdx];

            if (machineBatches == null || machineBatches.size() < 2) {
                return moves;  // 少于2个批次，无法交换
            }
            
            // 遍历相邻批次对
            for (int batchIdx = 0; batchIdx < machineBatches.size() - 1; batchIdx++) {
                Solution batch1 = machineBatches.get(batchIdx);
                Solution batch2 = machineBatches.get(batchIdx + 1);
                
                // 收集批次1的零件在gene_OS中的位置
                List<Integer> batch1Positions = new ArrayList<>();
                for (ProgramEntity.PlaceItem item : batch1.placeItemList) {
                    int jobNo = Integer.parseInt(item.name);
                    // 在打印段找到该零件的位置
                    for (int i = 0; i < printCount; i++) {
                        if (os[i] == jobNo && ms[i] == (maxMachineIdx + 1)) {
                            batch1Positions.add(i);
                            break;
                        }
                    }
                }
                
                // 收集批次2的零件在gene_OS中的位置
                List<Integer> batch2Positions = new ArrayList<>();
                for (ProgramEntity.PlaceItem item : batch2.placeItemList) {
                    int jobNo = Integer.parseInt(item.name);
                    // 在打印段找到该零件的位置
                    for (int i = 0; i < printCount; i++) {
                        if (os[i] == jobNo && ms[i] == (maxMachineIdx + 1)) {
                            // 确保不重复添加（一个零件只能在一个批次中）
                            if (!batch1Positions.contains(i)) {
                                batch2Positions.add(i);
                                break;
                            }
                        }
                    }
                }
                
                if (!batch1Positions.isEmpty() && !batch2Positions.isEmpty()) {
                    BatchSwapMove move = new BatchSwapMove(batch1Positions, batch2Positions);
                    moves.add(move);
                    
                    if (moves.size() >= maxCandidates) {
                        return moves;
                    }
                }
            }
        return moves;
    }
    
    /**
     * 应用移动：修改染色体的机器分配
     */
    private void applyMove(Chromosome chromosome, Move move) {
        chromosome.gene_MS[move.position] = move.toMachine;
        chromosome.printSolution = null;  // 清空装箱结果
    }
    
    /**
     * 撤销移动：恢复原机器分配
     */
    private void undoMove(Chromosome chromosome, Move move) {
        chromosome.gene_MS[move.position] = move.fromMachine;
        chromosome.printSolution = null;  // 清空装箱结果
    }
    
    /**
     * 应用批次交换：交换gene_OS中两个批次的零件顺序
     */
    private void applyBatchSwap(Chromosome chromosome, BatchSwapMove move) {
        // 保存原始状态
        move.saveOriginal(chromosome.gene_OS, chromosome.gene_MS);
        
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        
        // 提取批次1和批次2的零件和机器信息
        List<Integer> batch1Jobs = new ArrayList<>();
        List<Integer> batch1Machines = new ArrayList<>();
        for (int pos : move.batch1Positions) {
            batch1Jobs.add(os[pos]);
            batch1Machines.add(ms[pos]);
        }
        
        List<Integer> batch2Jobs = new ArrayList<>();
        List<Integer> batch2Machines = new ArrayList<>();
        for (int pos : move.batch2Positions) {
            batch2Jobs.add(os[pos]);
            batch2Machines.add(ms[pos]);
        }
        
        // 合并所有位置并排序（确保按原顺序处理）
        List<Integer> allPositions = new ArrayList<>();
        allPositions.addAll(move.batch1Positions);
        allPositions.addAll(move.batch2Positions);
        Collections.sort(allPositions);
        
        // 重新排列：先放batch2，再放batch1（实现交换）
        int writeIdx = 0;
        for (int pos : allPositions) {
            if (writeIdx < batch2Jobs.size()) {
                os[pos] = batch2Jobs.get(writeIdx);
                ms[pos] = batch2Machines.get(writeIdx);
            } else {
                int batch1Idx = writeIdx - batch2Jobs.size();
                os[pos] = batch1Jobs.get(batch1Idx);
                ms[pos] = batch1Machines.get(batch1Idx);
            }
            writeIdx++;
        }
        
        chromosome.printSolution = null;  // 清空装箱结果
    }
    
    /**
     * 撤销批次交换：恢复原始基因序列
     */
    private void undoBatchSwap(Chromosome chromosome, BatchSwapMove move) {
        chromosome.gene_OS = move.originalOS.clone();
        chromosome.gene_MS = move.originalMS.clone();
        chromosome.printSolution = null;  // 清空装箱结果
    }
    
    /**
     * N4邻域：生成关键块首尾相邻工序交换的候选（操作前约束筛选）
     * 
     * 策略：识别关键路径和关键块，交换块首和块尾两相邻工序
     * - 块首交换：交换块的第一个工序和第二个工序
     * - 块尾交换：交换块的倒数第二个工序和最后一个工序
     * 
     * 操作前约束筛选（在生成候选时执行）：
     * 1. 直接优先级约束：确保交换不违反工序的直接前驱后继关系
     * 2. 间接优先级约束：确保交换后工序仍在其所有前驱之后、所有后继之前
     * 3. 工序顺序约束：只在关键块内部交换，不跨块操作
     * 
     * 通过操作前筛选，从源头避免违反优先级约束的交换
     */
    private List<CriticalBlockMove> generateMovesN4(Chromosome chromosome, Operation[][] operationMatrix, int maxCandidates) {
        List<CriticalBlockMove> moves = new ArrayList<>();
        
        // 1. 识别关键路径
        List<Operation> criticalPath = identifyCriticalPath(operationMatrix);
        if (criticalPath.size() < 2) {
            return moves;  // 无关键路径或路径太短
        }
        
        // 2. 在关键路径上识别关键块（同一机器上连续的工序）
        List<CriticalBlock> criticalBlocks = identifyCriticalBlocks(criticalPath, chromosome);
        if (criticalBlocks.isEmpty()) {
            return moves;
        }
        
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        int jobCount = input.getJobCount();
        
        // 3. 对每个关键块，生成块首和块尾的交换候选
        for (CriticalBlock block : criticalBlocks) {
            if (block.operationPositions.size() < 2) {
                continue;  // 块太小，无法交换
            }
            Random r = new Random();
            double random = r.nextDouble();
            if (random < 0.5) {
                // 候选1：交换块首的两相邻工序（第一个和第二个）
                int headPos1 = block.operationPositions.get(0);  // 块首第一个工序
                int headPos2 = block.operationPositions.get(1);  // 块首第二个工序
                int jobNo1 = os[headPos1];
                int jobNo2 = os[headPos2];

                // 检查交换是否满足优先级约束
                //if (isSwapFeasible(headPos1, headPos2, jobNo1, jobNo2, os, ms, jobCount)) {
                CriticalBlockMove move = new CriticalBlockMove(headPos1, headPos2);
                moves.add(move);

                if (moves.size() >= maxCandidates) {
                    return moves;
                }
                }
                else {
                    // 候选2：交换块尾的两相邻工序（倒数第二个和最后一个）
                    //if (block.operationPositions.size() >= 2) {
                    int tailPos1 = block.operationPositions.get(block.operationPositions.size() - 2);  // 块尾倒数第二个
                    int tailPos2 = block.operationPositions.get(block.operationPositions.size() - 1);  // 块尾最后一个
                    int jobNo3 = os[tailPos1];
                    int jobNo4 = os[tailPos2];

                    // 检查交换是否满足优先级约束
                    //if (isSwapFeasible(tailPos1, tailPos2, jobNo3, jobNo4, os, ms, jobCount)) {
                    CriticalBlockMove move = new CriticalBlockMove(tailPos1, tailPos2);
                    moves.add(move);
                    
                    if (moves.size() >= maxCandidates) {
                        return moves;
                    }
                    //}
                    //}
                }
            }
        return moves;
    }
    
    /**
     * 识别关键路径：使用反向追溯法（CPM）找到离散处理阶段的关键工序序列
     * 
     * 注意：只考虑离散处理阶段（task >= 2），忽略打印和批处理阶段
     * 
     * 算法思路：
     * 1. 找到离散阶段完成时间最晚的工序（makespan工序）
     * 2. 从该工序反向追溯，找到所有离散工序的紧前工序
     * 3. 选择那些"时间紧张"的紧前工序（endTime无缝衔接的）
     * 4. 重复步骤2-3，直到追溯到离散阶段的起点
     */
    private List<Operation> identifyCriticalPath(Operation[][] operationMatrix) {
        List<Operation> criticalPath = new ArrayList<>();
        
        // 1. 找到离散阶段makespan工序（完成时间最晚的离散工序）
        Operation lastOp = null;
        double maxEndTime = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < operationMatrix.length; i++) {
            if (operationMatrix[i] == null || operationMatrix[i].length == 0) {
                continue;
            }
            
            for (Operation op : operationMatrix[i]) {
                // 只考虑离散工序（task >= 2）
                if (op != null && op.task >= 2 && op.endTime > maxEndTime) {
                    maxEndTime = op.endTime;
                    lastOp = op;
                }
            }
        }
        
        if (lastOp == null) {
            return criticalPath;
        }
        
        // 2. 使用栈进行反向追溯（从后往前）
        List<Operation> reversePath = new ArrayList<>();
        Set<String> visited = new HashSet<>();  // 防止循环
        Operation currentOp = lastOp;
        
        while (currentOp != null) {
            // 只添加离散工序到反向路径
            if (currentOp.task >= 2) {
                reversePath.add(currentOp);
            }
            
            String opKey = currentOp.jobNo + "_" + currentOp.task;
            visited.add(opKey);
            
            // 查找关键紧前工序（只考虑离散工序）
            Operation criticalPredecessor = findCriticalPredecessor(currentOp, operationMatrix, visited);
            currentOp = criticalPredecessor;
        }
        
        // 3. 反转路径（变成从前往后的顺序）
        for (int i = reversePath.size() - 1; i >= 0; i--) {
            criticalPath.add(reversePath.get(i));
        }
        
        return criticalPath;
    }
    
    /**
     * 找到指定工序的关键紧前工序（只考虑离散处理阶段）
     * 
     * 紧前工序有两类：
     * 1. 工艺紧前：同一工件的前一道离散工序
     * 2. 机器紧前：同一机器上紧挨着的前一道离散工序
     * 
     * 选择标准：紧前工序的endTime == 当前工序的startTime（无空闲时间）
     */
    private Operation findCriticalPredecessor(Operation currentOp, Operation[][] operationMatrix, Set<String> visited) {
        List<Operation> candidates = new ArrayList<>();
        
        // 候选1：工艺紧前工序（同一工件的前一道离散工序）
        if (currentOp.task > 2) {  // 不是第一道离散工序（task=2是第一道离散）
            Operation[] jobOps = operationMatrix[currentOp.jobNo];
            if (jobOps != null) {
                // 找到当前工序在工件中的位置，然后找前一道离散工序
                for (int i = 0; i < jobOps.length; i++) {
                    if (jobOps[i] != null && jobOps[i].task == currentOp.task && i > 0) {
                        // 向前找最近的一道离散工序
                        for (int j = i - 1; j >= 0; j--) {
                            Operation prevOp = jobOps[j];
                            if (prevOp != null && prevOp.task >= 2) {  // 只考虑离散工序
                                String key = prevOp.jobNo + "_" + prevOp.task;
                                if (!visited.contains(key)) {
                                    candidates.add(prevOp);
                                }
                                break;  // 找到就停止
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        // 候选2：机器紧前工序（同一机器上紧挨着的前一道离散工序）
        // 遍历所有离散工序，找到同一机器且endTime最接近当前startTime的工序
        Operation machinePredecessor = null;
        double maxEndTime = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < operationMatrix.length; i++) {
            if (operationMatrix[i] == null) continue;
            
            for (Operation op : operationMatrix[i]) {
                if (op == null || op.task < 2) continue;  // 只考虑离散工序
                
                String key = op.jobNo + "_" + op.task;
                if (visited.contains(key)) continue;
                
                // 同一机器 且 该工序在当前工序之前完成
                if (op.machineNo == currentOp.machineNo && op.endTime <= currentOp.startTime) {
                    if (op.endTime > maxEndTime) {
                        maxEndTime = op.endTime;
                        machinePredecessor = op;
                    }
                }
            }
        }
        
        if (machinePredecessor != null) {
            candidates.add(machinePredecessor);
        }
        
        // 从候选中选择关键紧前工序（endTime最接近currentOp.startTime的）
        Operation criticalPred = null;
        double minGap = Double.POSITIVE_INFINITY;
        final double EPSILON = 1e-6;  // 浮点数比较容忍度
        
        for (Operation candidate : candidates) {
            double gap = currentOp.startTime - candidate.endTime;
            
            // 优先选择无空闲的紧前工序（gap接近0）
            if (gap < minGap && gap >= -EPSILON) {  // gap >= 0 (允许小的浮点误差)
                minGap = gap;
                criticalPred = candidate;
            }
        }
        
        return criticalPred;
    }
    
    /**
     * 在关键路径上识别关键块
     * 关键块 = 关键路径上在同一机器上连续加工的工序序列
     */
    private List<CriticalBlock> identifyCriticalBlocks(List<Operation> criticalPath, Chromosome chromosome) {
        List<CriticalBlock> blocks = new ArrayList<>();
        
        if (criticalPath.isEmpty()) {
            return blocks;
        }
        
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        int jobCount = input.getJobCount();
        
        // 只考虑离散段的工序
        CriticalBlock currentBlock = null;
        int lastMachine = -1;
        
        for (Operation op : criticalPath) {
            // 跳过打印和批处理工序，只处理离散工序
            if (op.task < 2) {
                continue;
            }
            
            int currentMachine = op.machineNo;
            
            // 找到该工序在gene_OS中的位置
            int positionInOS = findOperationPosition(op, os, ms, jobCount);
            if (positionInOS == -1) {
                continue;  // 未找到，跳过
            }
            
            if (currentMachine == lastMachine && currentBlock != null) {
                // 同一机器，添加到当前块
                currentBlock.operationPositions.add(positionInOS);
            } else {
                // 不同机器，创建新块
                if (currentBlock != null && currentBlock.operationPositions.size() > 1) {
                    blocks.add(currentBlock);  // 保存上一个块（如果有多个工序）
                }
                currentBlock = new CriticalBlock(currentMachine);
                currentBlock.operationPositions.add(positionInOS);
                lastMachine = currentMachine;
            }
        }
        
        // 保存最后一个块
        if (currentBlock != null && currentBlock.operationPositions.size() > 1) {
            blocks.add(currentBlock);
        }
        
        return blocks;
    }
    
    /**
     * 在gene_OS中找到指定工序的位置（适配新编码方式）
     * 
     * 新编码方式：
     * - OS：离散工序随机排列
     * - MS：离散工序固定顺序，存储相对索引
     * 
     * 查找策略：
     * 1. 在OS中找到该工件的第N个离散工序
     * 2. 通过MS固定位置解码实际机器编号
     * 3. 验证机器编号是否匹配
     * 
     * 重要说明：
     * - operationMatrix中的machineNo是0-based（存储时machineNo-1）
     * - availableMachines中是1-based（加入时k+1）
     * - 比较时需要统一基数
     */
    private int findOperationPosition(Operation op, int[] os, int[] ms, int jobCount) {
        int jobNo = op.jobNo;
        int targetMachineNo = op.machineNo;  // 0-based（来自operationMatrix）
        int operNo = op.task;  // 工序编号
        
        if (operNo < 2) {
            return -1;  // 只处理离散工序
        }
        
        // 计算该工序是该工件的第几个离散工序（0-based）
        int discreteOperIndex = operNo - 2;
        
        // 在OS中找到该工件的第discreteOperIndex次出现（在离散段）
        int count = 0;
        int positionInOS = -1;
        for (int i = jobCount; i < os.length; i++) {
            if (os[i] == jobNo) {
                if (count == discreteOperIndex) {
                    positionInOS = i;
                    break;
                }
                count++;
            }
        }
        
        if (positionInOS == -1) {
        return -1;  // 未找到
        }
        
        // ✅ 验证机器编号：需要从MS的相对索引解码出实际机器编号
        // 1. 计算MS中的固定位置
        int msIndex = jobCount;
        for (int j = 0; j < jobNo; j++) {
            msIndex += (input.getOperationCountArr()[j] - 2);
        }
        msIndex += discreteOperIndex;
        
        // 2. 获取相对索引
        int relativeIndex = ms[msIndex];
        
        // 3. 根据相对索引获取实际机器编号
        int[][] operationToIndex = input.getOperationToIndex();
        double[][] proDesMatrix = input.getProDesMatrix();
        int operIdx = operationToIndex[jobNo][operNo];
        
        // 4. 找到可选机器列表（1-based）
        List<Integer> availableMachines = new ArrayList<>();
        for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
            if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                availableMachines.add(k + 1);  // 1-based
            }
        }
        
        // 5. 验证相对索引是否有效
        if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
            return -1;  // 相对索引无效
        }
        
        // 6. 获取实际机器编号（1-based）
        int actualMachineNo = availableMachines.get(relativeIndex - 1);
        
        // 7. 验证机器编号是否匹配
        // ⚠️ 关键：targetMachineNo是0-based，actualMachineNo是1-based
        // 需要统一到相同基数进行比较
        if (actualMachineNo - 1 == targetMachineNo) {  // 将actualMachineNo转为0-based
            return positionInOS;
        }
        
        return -1;  // 机器编号不匹配
    }
    
    /**
     * 操作前约束筛选：检查工序插入的可行性
     * 
     * 检查维度：
     * 1. 直接优先级约束：插入位置不能跨越前驱或后继工序
     * 2. 间接优先级约束：确保工序仍在其所有前驱之后、所有后继之前
     * 
     * @param fromPos 要移动的工序当前位置（在gene_OS中的索引）
     * @param toPos 目标插入位置（在gene_OS中的索引）
     * @param jobNo 工件编号
     * @param os gene_OS数组
     * @param ms gene_MS数组
     * @param jobCount 工件数量（离散段起始位置）
     * @return true表示可行，false表示不可行
     */
    /**
     * 操作前约束筛选：检查工序交换的可行性
     * 
     * 检查维度：
     * 1. 直接优先级约束：交换后两个工序不能违反各自的前驱后继关系
     * 2. 间接优先级约束：确保两个工序交换后仍在其所有前驱之后、所有后继之前
     * 
     * @param pos1 第一个工序位置（在gene_OS中的索引）
     * @param pos2 第二个工序位置（在gene_OS中的索引）
     * @param jobNo1 第一个工序的工件编号
     * @param jobNo2 第二个工序的工件编号
     * @param os gene_OS数组
     * @param ms gene_MS数组
     * @param jobCount 工件数量（离散段起始位置）
     * @return true表示可行，false表示不可行
     */
    private boolean isSwapFeasible(int pos1, int pos2, int jobNo1, int jobNo2, int[] os, int[] ms, int jobCount) {
        // 如果两个工序属于同一工件，需要检查它们的顺序关系
        if (jobNo1 == jobNo2) {
            // 找到该工件在离散段的所有工序位置（按出现顺序）
            List<Integer> jobOperationPositions = new ArrayList<>();
            for (int i = jobCount; i < os.length; i++) {
                if (os[i] == jobNo1) {
                    jobOperationPositions.add(i);
                }
            }
            
            int index1 = jobOperationPositions.indexOf(pos1);
            int index2 = jobOperationPositions.indexOf(pos2);
            
            // 如果不是相邻工序，不能交换（会违反工序顺序）
            if (Math.abs(index1 - index2) != 1) {
                return false;
            }
        }
        
        // 不同工件的工序可以自由交换（无工序顺序约束）
        return true;
    }
    
    private boolean isInsertionFeasible(int fromPos, int toPos, int jobNo, int[] os, int[] ms, int jobCount) {
        // 找到该工件在离散段的所有工序位置（按出现顺序）
        List<Integer> jobOperationPositions = new ArrayList<>();
        for (int i = jobCount; i < os.length; i++) {
            if (os[i] == jobNo) {
                jobOperationPositions.add(i);
            }
        }
        
        if (jobOperationPositions.size() <= 1) {
            return true;  // 只有一道工序，无约束
        }
        
        // 找到fromPos在该工件工序序列中的索引
        int operationIndex = jobOperationPositions.indexOf(fromPos);
        if (operationIndex == -1) {
            return false;  // 未找到，不应该发生
        }
        
        // 约束1：直接前驱后继检查
        // 获取直接前驱和后继工序的位置
        Integer directPredecessorPos = (operationIndex > 0) ? jobOperationPositions.get(operationIndex - 1) : null;
        Integer directSuccessorPos = (operationIndex < jobOperationPositions.size() - 1) ? jobOperationPositions.get(operationIndex + 1) : null;
        
        if (directPredecessorPos != null) {
            // toPos不能在直接前驱之前
            if (toPos <= directPredecessorPos) {
                return false;
            }
        }
        
        if (directSuccessorPos != null) {
            // toPos不能在直接后继之后
            if (toPos >= directSuccessorPos) {
                return false;
            }
        }
        
        // 约束2：间接优先级约束检查
        // 确保插入位置在所有前驱之后、所有后继之前
        for (int i = 0; i < operationIndex; i++) {
            // 所有前驱工序都必须在toPos之前
            int predPos = jobOperationPositions.get(i);
            if (toPos <= predPos) {
                return false;
            }
        }
        
        for (int i = operationIndex + 1; i < jobOperationPositions.size(); i++) {
            // 所有后继工序都必须在toPos之后
            int succPos = jobOperationPositions.get(i);
            if (toPos >= succPos) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 操作前约束筛选：检查两个工序是否可以交换
     * 
     * 可交换条件：
     * 1. 两个工序不属于同一工件（避免违反工艺顺序）
     * 2. 如果属于同一工件，它们之间无直接前序-后序关系
     * 
     * @param pos1 工序1在gene_OS中的位置
     * @param pos2 工序2在gene_OS中的位置
     * @param os gene_OS数组
     * @param jobCount 工件数量
     * @return true表示可交换，false表示不可交换
     */
    private boolean canSwapOperations(int pos1, int pos2, int[] os, int jobCount) {
        int job1 = os[pos1];
        int job2 = os[pos2];
        
        // 不同工件的工序可以交换
        if (job1 != job2) {
            return true;
        }
        
        // 同一工件的工序：检查是否有直接前序-后序关系
        // 找到该工件的所有工序位置
        List<Integer> jobPositions = new ArrayList<>();
        for (int i = jobCount; i < os.length; i++) {
            if (os[i] == job1) {
                jobPositions.add(i);
            }
        }
        
        int idx1 = jobPositions.indexOf(pos1);
        int idx2 = jobPositions.indexOf(pos2);
        
        // 如果是直接相邻的工序，不能交换（有直接前序-后序关系）
        if (Math.abs(idx1 - idx2) == 1) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 应用关键块内工序插入
     * 
     * 注意：已在操作前进行约束筛选，此处只执行插入操作
     */
    /**
     * 应用关键块移动：交换两个相邻工序（适配新编码方式）
     * 
     * 新编码方式下：
     * - 关键块只存在于离散段（离散处理工序）
     * - 离散段的MS是固定顺序的相对索引，不随OS改变
     * - 交换OS时，只交换工件编号，MS保持不变
     * 
     * 重要说明：
     * - pos1和pos2都在离散段（jobCount之后）
     * - 交换OS[pos1]和OS[pos2]（工件编号）
     * - MS不交换，因为MS是按固定顺序存储的相对索引
     * 
     * @param chromosome 染色体
     * @param move 移动操作（包含两个要交换的位置）
     */
    private void applyCriticalBlockMove(Chromosome chromosome, CriticalBlockMove move) {
        move.saveOriginal(chromosome.gene_OS, chromosome.gene_MS);
        int[] os = chromosome.gene_OS;
        int pos1 = move.fromPosition;
        int pos2 = move.toPosition;
        
        if (pos1 == pos2) {
            return;  // 无需交换
        }
        
        // ✅ 只交换gene_OS中的工件编号
        // 在新编码方式下，离散段的MS是固定顺序的，不应该交换
        int tempJob = os[pos1];
        os[pos1] = os[pos2];
        os[pos2] = tempJob;
        
        // ⚠️ 注意：gene_MS不交换！
        // 原因：离散段的MS按固定顺序存储相对索引
        // MS[jobCount] = 工件0工序2的相对索引
        // MS[jobCount+1] = 工件0工序3的相对索引
        // ...
        // 无论OS如何排列，MS的顺序都是固定的
        
        // 无需清空装箱结果
    }
    
    /**
     * 撤销关键块内工序交换
     */
    private void undoCriticalBlockMove(Chromosome chromosome, CriticalBlockMove move) {
        chromosome.gene_OS = move.originalOS.clone();
        chromosome.gene_MS = move.originalMS.clone();
        chromosome.printSolution = null;
    }
    
    /**
     * N5邻域：生成关键工序机器重分配的候选（基于轮盘赌选择）
     * 
     * 策略：识别所有关键路径上的离散处理工序，对可选机器根据加工时间进行轮盘赌重新选择
     * - 加工时间短的机器被选中概率高
     * - 只修改机器分配（gene_MS中的相对索引），不移动工序顺序（gene_OS）
     * 
     * 操作前约束筛选：
     * 1. 机器可行性约束：只考虑该工序的可选机器集合（proDesMatrix）
     * 2. 轮盘赌选择：加工时间短的机器权重大
     * 
     * 注意：由于离散工序的MS使用相对索引编码（固定顺序），此处直接修改MS中的相对索引
     */
    private List<MachineReassignmentMove> generateMovesN5(Chromosome chromosome, Operation[][] operationMatrix, int maxCandidates) {
        List<MachineReassignmentMove> moves = new ArrayList<>();
        
        // 1. 识别关键路径
        List<Operation> criticalPath = identifyCriticalPath(operationMatrix);
        if (criticalPath == null || criticalPath.isEmpty()) {
            return moves;
        }
        
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        int jobCount = input.getJobCount();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        
        // 2. 对关键路径上的每个离散工序
        for (Operation op : criticalPath) {
            // 只处理离散工序
            if (op.task < 2) {
                continue;
            }
            
            int jobNo = op.jobNo;
            int currentMachine = op.machineNo;  // 0-based（从operationMatrix读取）
            int taskNo = op.task;
            
            // 检查工序编号是否有效
            if (taskNo >= operationToIndex[jobNo].length) {
                continue;
            }
            
            // 获取该工序在proDesMatrix中的索引
            int operIdx = operationToIndex[jobNo][taskNo];
            
            // 计算该工序在gene_MS中的固定索引（离散工序的MS是固定顺序的）
            int msIndex = jobCount;  // 离散段起点
            for (int j = 0; j < jobNo; j++) {
                msIndex += (input.getOperationCountArr()[j] - 2);  // 加上前面所有工件的离散工序数
            }
            msIndex += (taskNo - 2);  // 加上当前工件的工序偏移
            
            // ✅ 约束1：机器可行性筛选 - 获取该工序的所有可选机器集及其加工时间
            List<Integer> allAvailableMachines = new ArrayList<>();  // 所有可选机器（0-based）
            List<Double> allProcessingTimes = new ArrayList<>();
            
            for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                    allAvailableMachines.add(k);  // 0-based机器索引
                    allProcessingTimes.add(proDesMatrix[operIdx][k]);
                }
            }
            
            if (allAvailableMachines.isEmpty()) {
                continue;  // 没有可选机器
            }
            
            // 排除当前机器，构建候选机器列表
            List<Integer> candidateMachinesIdx = new ArrayList<>();  // 候选机器在allAvailableMachines中的索引
            List<Double> candidateProcessingTimes = new ArrayList<>();
            
            for (int i = 0; i < allAvailableMachines.size(); i++) {
                if (allAvailableMachines.get(i) != currentMachine) {
                    candidateMachinesIdx.add(i);  // 记录在allAvailableMachines中的索引
                    candidateProcessingTimes.add(allProcessingTimes.get(i));
                }
            }
            
            if (candidateMachinesIdx.isEmpty()) {
                continue;  // 没有其他可选机器
            }
            
            // ✅ 轮盘赌选择：根据加工时间选择新机器（时间短权重大）
            int selectedIdx = selectMachineIndexByRoulette(candidateProcessingTimes);
            int selectedMachineInAllList = candidateMachinesIdx.get(selectedIdx);
            int selectedRelativeIndex = selectedMachineInAllList + 1;  // 转换为1-based相对索引
            
            // 生成候选移动（只修改MS中的相对索引，不移动OS位置）
                    MachineReassignmentMove move = new MachineReassignmentMove(
                msIndex,           // MS中的固定位置
                ms[msIndex],       // 当前相对索引
                selectedRelativeIndex,  // 新的相对索引
                msIndex            // 不移动位置，保持原位（使用msIndex占位）
                    );
                    moves.add(move);
                    
                    if (moves.size() >= maxCandidates) {
                        return moves;
                    }
                }
        
        return moves;
    }
    
    /**
     * 轮盘赌选择机器（基于加工时间，时间短权重大）
     * 
     * @param processingTimes 候选机器的加工时间列表
     * @return 选中的机器在候选列表中的索引（0-based）
     */
    private int selectMachineIndexByRoulette(List<Double> processingTimes) {
        if (processingTimes.size() == 1) {
            return 0;  // 只有一个候选，直接返回
        }
        
        // 计算权重：使用倒数，使得加工时间短的机器权重大
        List<Double> weights = new ArrayList<>();
        double maxTime = Double.NEGATIVE_INFINITY;
        for (double time : processingTimes) {
            if (time > maxTime) {
                maxTime = time;
            }
        }
        
        // 权重 = (maxTime - time + 1)，使得时间短的机器权重大
        double totalWeight = 0;
        for (double time : processingTimes) {
            double weight = (maxTime - time + 1);
            weights.add(weight);
            totalWeight += weight;
        }
        
        // 轮盘赌选择
        double randomValue = r.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return i;  // 返回在候选列表中的索引
            }
        }
        
        // 默认返回最后一个（不应该到达这里）
        return weights.size() - 1;
    }
//
//        return moves;
//    }
    
    /**
     * 找到同一工件在指定机器上的其他工序位置
     */
    private List<Integer> findSameJobOperationsOnMachine(int[] os, int[] ms, int jobNo, int machineNo, int jobCount) {
        List<Integer> positions = new ArrayList<>();
        
        for (int i = jobCount; i < os.length; i++) {
            if (os[i] == jobNo && ms[i] == machineNo) {
                positions.add(i);
            }
        }
        
        return positions;
    }
    
    /**
     * 生成插入位置候选
     * 策略：在新机器的离散段工序序列中，尝试几个关键位置
     */
    private List<Integer> generateInsertPositions(int[] os, int[] ms, int operationPosition, int newMachine, 
                                                   List<Integer> sameJobPositions, int jobCount) {
        List<Integer> positions = new ArrayList<>();
        
        // 找到新机器上所有工序的位置
        List<Integer> machinePositions = new ArrayList<>();
        for (int i = jobCount; i < os.length; i++) {
            if (ms[i] == newMachine && i != operationPosition) {
                machinePositions.add(i);
            }
        }
        
        if (machinePositions.isEmpty()) {
            // 新机器上没有其他工序，可以插入到离散段的任意位置
            // 尝试几个代表性位置
            positions.add(jobCount);  // 离散段开头
            if (os.length > jobCount + 1) {
                positions.add((jobCount + os.length) / 2);  // 中间
                positions.add(os.length - 1);  // 末尾
            }
        } else {
            // 新机器上有其他工序，尝试在这些工序之间插入
            Collections.sort(machinePositions);
            
            // 插入到第一个位置之前
            positions.add(Math.max(jobCount, machinePositions.get(0)));
            
            // 插入到中间位置
            if (machinePositions.size() > 1) {
                int mid = machinePositions.size() / 2;
                positions.add(machinePositions.get(mid));
            }
            
            // 插入到最后一个位置之后
            positions.add(Math.min(os.length - 1, machinePositions.get(machinePositions.size() - 1) + 1));
        }
        
        // 如果有同一工件的工序，优先考虑插入到它们附近
        if (!sameJobPositions.isEmpty()) {
            for (int pos : sameJobPositions) {
                if (!positions.contains(pos)) {
                    positions.add(pos);
                    if (positions.size() >= 3) {
                        break;  // 限制候选数量
                    }
                }
            }
        }
        
        return positions;
    }
    
    /**
     * 应用关键工序机器重分配
     * 这是一个复合操作：修改机器分配 + 调整工序位置
     * 
     * 注意：已在操作前进行约束筛选，此处只执行重分配操作
     */
    /**
     * 应用机器重分配：只修改gene_MS中的相对索引，不移动工序位置（适配新编码方式）
     * 
     * 新编码方式下：
     * - MS（离散段）是固定顺序的相对索引，与OS解耦
     * - 只需要修改MS中指定位置的相对索引
     * - OS保持不变
     * 
     * @param chromosome 染色体
     * @param move 机器重分配移动（包含MS位置和新的相对索引）
     */
    private void applyMachineReassignment(Chromosome chromosome, MachineReassignmentMove move) {
        move.saveOriginal(chromosome.gene_OS, chromosome.gene_MS);
        
        int[] ms = chromosome.gene_MS;
        int msIndex = move.operationPosition;  // MS中的固定位置
        int newRelativeIndex = move.newMachine;  // 新的相对索引
        
        // ✅ 直接修改MS中的相对索引
        ms[msIndex] = newRelativeIndex;
        
        // ⚠️ OS不变！
        // N5只修改机器分配，不改变工序执行顺序
        
 // 清空装箱结果
    }
    
    /**
     * 撤销关键工序机器重分配
     */
    private void undoMachineReassignment(Chromosome chromosome, MachineReassignmentMove move) {
        chromosome.gene_OS = move.originalOS.clone();
        chromosome.gene_MS = move.originalMS.clone();
        chromosome.printSolution = null;
    }
    
    /**
     * Move类：表示一个邻域移动
     */
    private static class Move {
        int position;      // gene_MS中的位置
        int fromMachine;   // 原机器（1-based）
        int toMachine;     // 目标机器（1-based）
        
        Move(int position, int fromMachine, int toMachine) {
            this.position = position;
            this.fromMachine = fromMachine;
            this.toMachine = toMachine;
        }
    }
    
    /**
     * BatchSwapMove类：表示相邻批次交换操作
     */
    private static class BatchSwapMove {
        List<Integer> batch1Positions;  // 第一个批次的零件在gene_OS中的位置
        List<Integer> batch2Positions;  // 第二个批次的零件在gene_OS中的位置
        int[] originalOS;               // 原始的gene_OS（用于撤销）
        int[] originalMS;               // 原始的gene_MS（用于撤销）
        
        BatchSwapMove(List<Integer> batch1Positions, List<Integer> batch2Positions) {
            this.batch1Positions = batch1Positions;
            this.batch2Positions = batch2Positions;
        }
        
        void saveOriginal(int[] os, int[] ms) {
            this.originalOS = os.clone();
            this.originalMS = ms.clone();
        }
    }
    
    /**
     * CriticalBlockMove类：表示关键块内工序插入操作
     */
    private static class CriticalBlockMove {
        int fromPosition;    // 要移动的工序在gene_OS中的位置
        int toPosition;      // 目标位置
        int[] originalOS;    // 原始gene_OS（用于撤销）
        int[] originalMS;    // 原始gene_MS（用于撤销）
        
        CriticalBlockMove(int fromPosition, int toPosition) {
            this.fromPosition = fromPosition;
            this.toPosition = toPosition;
        }
        
        void saveOriginal(int[] os, int[] ms) {
            this.originalOS = os.clone();
            this.originalMS = ms.clone();
        }
    }
    
    /**
     * MachineReassignmentMove类：表示关键工序机器重分配操作
     * 这是一个复合邻域：同时修改机器分配和工序插入位置
     */
    private static class MachineReassignmentMove {
        int operationPosition;    // 工序在gene_OS中的位置
        int oldMachine;           // 原机器（1-based）
        int newMachine;           // 新机器（1-based）
        int newInsertPosition;    // 在新机器序列中的插入位置
        int[] originalOS;         // 原始gene_OS（用于撤销）
        int[] originalMS;         // 原始gene_MS（用于撤销）
        
        MachineReassignmentMove(int operationPosition, int oldMachine, int newMachine, int newInsertPosition) {
            this.operationPosition = operationPosition;
            this.oldMachine = oldMachine;
            this.newMachine = newMachine;
            this.newInsertPosition = newInsertPosition;
        }
        
        void saveOriginal(int[] os, int[] ms) {
            this.originalOS = os.clone();
            this.originalMS = ms.clone();
        }
    }
    
    /**
     * 关键块：关键路径上在同一机器上连续加工的工序序列
     */
    private static class CriticalBlock {
        int machineNo;                    // 机器编号（1-based）
        List<Integer> operationPositions; // 工序在gene_OS中的位置
        
        CriticalBlock(int machineNo) {
            this.machineNo = machineNo;
            this.operationPositions = new ArrayList<>();
        }
    }
    
    /**
     * 使用实际fitness评估选择最优邻域解（保留用于其他地方可能调用）
     * 现在evaluate是幂等的，可以直接评估每个邻域解的真实makespan
     */
    private Chromosome selectBestNeighborByFitness(List<Chromosome> neighbors) {
        if (neighbors.isEmpty()) {
            return null;
        }
        
        if (neighbors.size() == 1) {
            return neighbors.get(0);
        }
        
        // 如果没有CaculateFitness实例，回退到启发式评估
        if (caculateFitness == null) {
            return selectBestNeighborHeuristic(neighbors);
        }
        
        // 创建临时的operationMatrix用于evaluate
        Operation[][] tempOpMatrix = createOperationMatrix();
        
        // 评估所有邻域解的实际fitness
        Chromosome bestNeighbor = neighbors.get(0);
        double bestMakespan = caculateFitness.evaluate(bestNeighbor, input, tempOpMatrix);
        bestNeighbor.fitness = GA.FITNESS_SCALE / bestMakespan;
        
        for (int i = 1; i < neighbors.size(); i++) {
            // 重置operationMatrix
            initOperationMatrix(tempOpMatrix);
            
            Chromosome neighbor = neighbors.get(i);
            double makespan = caculateFitness.evaluate(neighbor, input, tempOpMatrix);
            neighbor.fitness = GA.FITNESS_SCALE / makespan;
            
            // makespan越小越好
            if (makespan < bestMakespan) {
                bestMakespan = makespan;
                bestNeighbor = neighbor;
            }
        }
        
        return bestNeighbor;
    }
    
    /**
     * 使用启发式方法选择最有希望的邻域解（回退方案）
     * 当无法使用实际fitness评估时使用
     */
    private Chromosome selectBestNeighborHeuristic(List<Chromosome> neighbors) {
        if (neighbors.isEmpty()) {
            return null;
        }
        
        if (neighbors.size() == 1) {
            return neighbors.get(0);
        }
        
        // 启发式评估：计算打印段机器分配的均衡度
        Chromosome bestNeighbor = neighbors.get(0);
        double bestScore = evaluateBalanceScore(bestNeighbor);
        
        for (int i = 1; i < neighbors.size(); i++) {
            double score = evaluateBalanceScore(neighbors.get(i));
            if (score > bestScore) {
                bestScore = score;
                bestNeighbor = neighbors.get(i);
            }
        }
        
        return bestNeighbor;
    }
    
    /**
     * 创建一个新的operationMatrix用于evaluate
     */
    private Operation[][] createOperationMatrix() {
        Operation[][] matrix = new Operation[input.getJobCount()][];
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = new Operation[input.getOperationCountArr()[i]];
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = new Operation();
            }
        }
        return matrix;
    }
    
    /**
     * 初始化operationMatrix
     */
    private void initOperationMatrix(Operation[][] operationMatrix) {
        for (int i = 0; i < operationMatrix.length; i++)
            for (int j = 0; j < operationMatrix[i].length; j++)
                operationMatrix[i][j].initOperation();
    }
    
    /**
     * 评估染色体的均衡度分数（启发式）
     * 分数越高表示负载越均衡（越好）
     */
    private double evaluateBalanceScore(Chromosome chromosome) {
        int printCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        Item[] items = input.getItems();
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        
        // 统计每台打印机分配的零件数量和总高度
        int[] machineJobCount = new int[printMachineCount];
        double[] machineTotalHeight = new double[printMachineCount];
        
        for (int i = 0; i < printCount; i++) {
            int machineNo = ms[i];  // 1-based
            int machineIdx = machineNo - 1;
            if (machineIdx >= 0 && machineIdx < printMachineCount) {
                machineJobCount[machineIdx]++;
                int jobNo = os[i];
                machineTotalHeight[machineIdx] += items[jobNo].h;
            }
        }
        
        // 计算负载均衡度：使用方差的倒数（方差越小越均衡）
        // 同时考虑零件数量和总高度
        double avgJobCount = printCount / (double) printMachineCount;
        double avgHeight = 0;
        for (double h : machineTotalHeight) {
            avgHeight += h;
        }
        avgHeight /= printMachineCount;
        
        double varianceJobCount = 0;
        double varianceHeight = 0;
        for (int i = 0; i < printMachineCount; i++) {
            varianceJobCount += Math.pow(machineJobCount[i] - avgJobCount, 2);
            varianceHeight += Math.pow(machineTotalHeight[i] - avgHeight, 2);
        }
        varianceJobCount /= printMachineCount;
        varianceHeight /= printMachineCount;
        
        // 分数：方差越小，分数越高（加1避免除零）
        double score = 1.0 / (1.0 + varianceJobCount + varianceHeight / 100.0);
        
        return score;
    }
    
    /**
     * 验证染色体是否有效
     */
    private boolean isValidChromosome(Chromosome chromosome) {
        if (chromosome.gene_OS == null || chromosome.gene_MS == null) {
            return false;
        }
        
        if (chromosome.gene_OS.length != chromosome.gene_MS.length) {
            return false;
        }
        
        // 检查是否有无效值
        for (int i = 0; i < chromosome.gene_OS.length; i++) {
            if (chromosome.gene_OS[i] < 0 || chromosome.gene_MS[i] <= 0) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 打印扰动1：基于打印时间的零件重分配
     * 找到打印时间最长和最短的机器，将随机机器上的零件根据高度进行轮盘赌分给最短机器
     */
    private void printPerturbationByTime(Chromosome chromosome) {
        List<Solution>[] batchSolution = chromosome.printSolution;
        
        // printSolution只有在evaluate后才会初始化
        if (batchSolution == null) {
            return;
        }
        
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        int printCount = input.getJobCount();
        int printMachine = input.getPrintMachineCount();
        while(true) {
            Random r = new Random();
            int pos1 = r.nextInt(printCount);
            int p1 = os[pos1];
            int m = r.nextInt(printMachine);
            Random r1 = new Random(printMachine);
            PrintMachine m1 = (PrintMachine) input.getMachines()[m];
            if(ms[pos1] == m + 1 || m1.W < items[p1].w || m1.L < items[p1].l || m1.H < items[p1].h) {
                continue;
            }
            ms[pos1] = m + 1;
                break;
        }


        // 找到打印时间最长和最短的机器
//        int maxMachineIndex = -1;
//        int minMachineIndex = -1;
//        double maxTime = Double.NEGATIVE_INFINITY;
//        double minTime = Double.POSITIVE_INFINITY;
//
//        for (int i = 0; i < batchSolution.length; i++) {
//            List<Solution> machineSolution = batchSolution[i];
//            if (machineSolution == null || machineSolution.isEmpty()) {
//                minMachineIndex = i;
//                minTime = 0.0;
//                continue;
//            }
//
//            double endTime = machineSolution.get(machineSolution.size() - 1).endTime;
//            if (endTime > maxTime) {
//                maxTime = endTime;
//                maxMachineIndex = i;
//            }
//            if (endTime < minTime) {
//                minTime = endTime;
//                minMachineIndex = i;
//            }
//        }
//
//        if (maxMachineIndex == -1 || minMachineIndex == -1 || maxMachineIndex == minMachineIndex) {
//            return;
//        }
//
//        // 找到分配给最长时间机器的所有零件
//        Map<Integer, List<Integer>> machineMap = new HashMap<>();
//        for (int i = 0; i < printCount; i++) {
//            int machineNo = ms[i];  // 1-based
//            if (!machineMap.containsKey(machineNo)) {
//                machineMap.put(machineNo, new ArrayList<>());
//            }
//            machineMap.get(machineNo).add(i);
//        }
//
//        List<Integer> maxMachineJobs = machineMap.get(maxMachineIndex + 1);  // 1-based
//        if (maxMachineJobs == null || maxMachineJobs.isEmpty()) {
//            return;
//        }
//
//        // 使用轮盘赌选择一个零件（基于高度，高度越大被选中概率越大）
//        double totalHeight = 0.0;
//        for (int jobIdx : maxMachineJobs) {
//            int jobNo = os[jobIdx];
//            totalHeight += items[jobNo].h;
//        }
//
//        double rand = r.nextDouble() * totalHeight;
//        double sum = 0.0;
//        int selectedJobIdx = -1;
//        for (int jobIdx : maxMachineJobs) {
//            int jobNo = os[jobIdx];
//            sum += items[jobNo].h;
//            if (sum >= rand) {
//                selectedJobIdx = jobIdx;
//                break;
//            }
//        }
//
//        if (selectedJobIdx != -1) {
//            // 将选中的零件分配给合适的打印机
//            // 不直接分配给minMachineIndex，而是检查尺寸是否符合
//            int jobNo = os[selectedJobIdx];
//            Item item = items[jobNo];
//            Machine[] machines = input.getMachines();
//            int printMachineCount = input.getPrintMachineCount();
//
//            // 找出所有能容纳该零件的打印机
//            List<Integer> suitableMachines = new ArrayList<>();
//            for (int m = 0; m < printMachineCount; m++) {
//                PrintMachine pm = (PrintMachine) machines[m];
//                boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.printH);
//                boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.printH);
//                if (fitsNormal || fitsRotated) {
//                    suitableMachines.add(m + 1);  // 1-based
//                }
//            }
//
//            if (!suitableMachines.isEmpty()) {
//                // 优先选择打印时间最短的机器（如果尺寸符合）
//                int targetMachine = minMachineIndex + 1;  // 1-based
//                if (suitableMachines.contains(targetMachine)) {
//                    ms[selectedJobIdx] = targetMachine;
//                } else {
//                    // 如果最短时间的机器不符合尺寸，从符合尺寸的机器中随机选择
//                    ms[selectedJobIdx] = suitableMachines.get(r.nextInt(suitableMachines.size()));
//                }
//            }
//        }
    }
    
    /**
     * 打印扰动2：基于面积占用率的零件重分配
     * 找各打印机的最后一批次，将面积占用率最大的批次中最高的零件分配给面积占用率最小的机器
     */
    private void printPerturbationByArea(Chromosome chromosome) {
        List<Solution>[] batchSolution = chromosome.printSolution;
        
        if (batchSolution == null) {
            return;
        }
        
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int printCount = input.getJobCount();
        
        // 找各打印机的最后一批次及其面积占用率
        int maxRateMachineIdx = -1;
        int minRateMachineIdx = -1;
        double maxRate = Double.NEGATIVE_INFINITY;
        double minRate = Double.POSITIVE_INFINITY;
        int maxRateBatchIdx = -1;
        
        for (int i = 0; i < batchSolution.length; i++) {
            List<Solution> machineSolution = batchSolution[i];
            
            if (machineSolution == null || machineSolution.isEmpty()) {
                // 没有批次的机器，面积占用率视为0（最小）
                if (0 < minRate) {
                    minRate = 0;
                    minRateMachineIdx = i;
                }
                continue;
            }
            
            // 获取最后一个批次的面积占用率
            Solution lastBatch = machineSolution.get(machineSolution.size() - 1);
            double rate = lastBatch.rate;  // 利用率
            
            if (rate > maxRate) {
                maxRate = rate;
                maxRateMachineIdx = i;
                maxRateBatchIdx = machineSolution.size() - 1;
            }
            if (rate < minRate) {
                minRate = rate;
                minRateMachineIdx = i;
            }
        }
        
        if (maxRateMachineIdx == -1 || minRateMachineIdx == -1 || maxRateMachineIdx == minRateMachineIdx) {
            return;
        }
        
        // 找到面积占用率最大的批次中最高的零件
        List<Solution> maxRateMachineSolution = batchSolution[maxRateMachineIdx];
        if (maxRateBatchIdx >= maxRateMachineSolution.size()) {
            return;
        }
        
        Solution maxRateBatch = maxRateMachineSolution.get(maxRateBatchIdx);
        if (maxRateBatch.placeItemList.isEmpty()) {
            return;
        }
        
        // 找到该批次中高度最大的零件
        int highestJobNo = -1;
        double maxHeight = Double.NEGATIVE_INFINITY;
        for (ProgramEntity.PlaceItem item : maxRateBatch.placeItemList) {
            int jobNo = Integer.parseInt(item.name);
            if (items[jobNo].h > maxHeight) {
                maxHeight = items[jobNo].h;
                highestJobNo = jobNo;
            }
        }
        
        if (highestJobNo == -1) {
            return;
        }
        
        // 找到该零件在gene_MS中的位置并修改
        // 不直接分配给minRateMachineIdx，而是检查尺寸是否符合
        for (int i = 0; i < printCount; i++) {
            if (os[i] == highestJobNo) {
                Item item = items[highestJobNo];
                int printMachineCount = input.getPrintMachineCount();
                
                // 找出所有能容纳该零件的打印机
                List<Integer> suitableMachines = new ArrayList<>();
                for (int m = 0; m < printMachineCount; m++) {
                    PrintMachine pm = (PrintMachine) machines[m];
                    boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.printH);
                    boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.printH);
                    if (fitsNormal || fitsRotated) {
                        suitableMachines.add(m + 1);  // 1-based
                    }
                }
                
                if (!suitableMachines.isEmpty()) {
                    // 优先选择面积占用率最小的机器（如果尺寸符合）
                    int targetMachine = minRateMachineIdx + 1;  // 1-based
                    if (suitableMachines.contains(targetMachine)) {
                        ms[i] = targetMachine;
                    } else {
                        // 如果最小占用率的机器不符合尺寸，从符合尺寸的机器中随机选择
                        ms[i] = suitableMachines.get(r.nextInt(suitableMachines.size()));
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 打印机序列优化：对同一台打印机内的零件序列进行优化
     * 策略：选择一台打印机，对其内部的零件顺序进行重排或局部调整
     */
    private void printSequenceOptimization(Chromosome chromosome) {
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        int printCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        
        // 统计每台打印机分配的零件
        Map<Integer, List<Integer>> machineMap = new HashMap<>();
        for (int i = 0; i < printCount; i++) {
            int machineNo = ms[i];  // 1-based
            if (!machineMap.containsKey(machineNo)) {
                machineMap.put(machineNo, new ArrayList<>());
            }
            machineMap.get(machineNo).add(i);  // 存储位置索引
        }
        
        // 找到有多个零件的打印机
        List<Integer> candidateMachines = new ArrayList<>();
        for (Map.Entry<Integer, List<Integer>> entry : machineMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                candidateMachines.add(entry.getKey());
            }
        }
        
        if (candidateMachines.isEmpty()) {
            return;  // 没有可优化的机器
        }
        
        // 随机选择一台打印机
        int selectedMachine = candidateMachines.get(r.nextInt(candidateMachines.size()));
        List<Integer> positions = machineMap.get(selectedMachine);
        
        // 根据随机选择的策略进行序列优化
        int strategy = r.nextInt(3);
        
        switch (strategy) {
            case 0:
                // 策略3：2-opt局部优化
                twoOptSwap(os, positions);
                break;
            case 1:
                // 策略4：Insert操作（移动一个零件到另一个位置）
                insertMove(os, positions);
                break;
            case 2:
                // 策略5：随机Swap（交换两个零件）
                randomSwap(os, positions);
                break;
        }
    }
    
    /**
     * 按面积排序零件序列
     */
    private void sortByArea(int[] os, List<Integer> positions, Item[] items, boolean ascending) {
        if (positions.size() <= 1) return;
        
        // 创建(位置, 工件号, 面积)的列表
        List<PosJobArea> list = new ArrayList<>();
        for (int pos : positions) {
            int jobNo = os[pos];
            double area = items[jobNo].l * items[jobNo].w;
            list.add(new PosJobArea(pos, jobNo, area));
        }
        
        // 按面积排序
        if (ascending) {
            list.sort((a, b) -> Double.compare(a.area, b.area));
        } else {
            list.sort((a, b) -> Double.compare(b.area, a.area));  // 降序
        }
        
        // 将排序后的工件号写回gene_OS
        for (int i = 0; i < list.size(); i++) {
            os[positions.get(i)] = list.get(i).jobNo;
        }
    }
    
    /**
     * 按高度排序零件序列
     */
    private void sortByHeight(int[] os, List<Integer> positions, Item[] items, boolean ascending) {
        if (positions.size() <= 1) return;
        
        // 创建(位置, 工件号, 高度)的列表
        List<PosJobHeight> list = new ArrayList<>();
        for (int pos : positions) {
            int jobNo = os[pos];
            list.add(new PosJobHeight(pos, jobNo, items[jobNo].h));
        }
        
        // 按高度排序
        if (ascending) {
            list.sort((a, b) -> Double.compare(a.height, b.height));
        } else {
            list.sort((a, b) -> Double.compare(b.height, a.height));  // 降序
        }
        
        // 将排序后的工件号写回gene_OS
        for (int i = 0; i < list.size(); i++) {
            os[positions.get(i)] = list.get(i).jobNo;
        }
    }
    
    /**
     * 2-opt操作：反转一个子序列
     */
    private void twoOptSwap(int[] os, List<Integer> positions) {
        if (positions.size() <= 2) return;
        
        // 随机选择两个位置
        int i = r.nextInt(positions.size());
        int j = r.nextInt(positions.size());
        
        if (i > j) {
            int temp = i;
            i = j;
            j = temp;
        }
        
        if (i == j) return;
        
        // 反转positions[i]到positions[j]之间的工件顺序
        while (i < j) {
            int pos1 = positions.get(i);
            int pos2 = positions.get(j);
            int temp = os[pos1];
            os[pos1] = os[pos2];
            os[pos2] = temp;
            i++;
            j--;
        }
    }
    
    /**
     * Insert操作：将一个零件移动到另一个位置
     */
    private void insertMove(int[] os, List<Integer> positions) {
        if (positions.size() <= 1) return;
        
        // 随机选择源位置和目标位置
        int from = r.nextInt(positions.size());
        int to = r.nextInt(positions.size());
        
        if (from == to) return;
        
        // 提取要移动的工件
        int fromPos = positions.get(from);
        int movingJob = os[fromPos];
        
        // 移动工件：先删除，再插入
        if (from < to) {
            // 向后移动
            for (int i = from; i < to; i++) {
                int currentPos = positions.get(i);
                int nextPos = positions.get(i + 1);
                os[currentPos] = os[nextPos];
            }
        } else {
            // 向前移动
            for (int i = from; i > to; i--) {
                int currentPos = positions.get(i);
                int prevPos = positions.get(i - 1);
                os[currentPos] = os[prevPos];
            }
        }
        
        // 插入到目标位置
        int toPos = positions.get(to);
        os[toPos] = movingJob;
    }
    
    /**
     * 随机交换两个零件
     */
    private void randomSwap(int[] os, List<Integer> positions) {
        if (positions.size() <= 1) return;
        
        // 随机选择两个位置
        int i = r.nextInt(positions.size());
        int j = r.nextInt(positions.size());
        
        int attempts = 0;
        while (i == j && attempts < 10) {
            j = r.nextInt(positions.size());
            attempts++;
        }
        
        if (i == j) return;
        
        // 交换两个位置的工件
        int pos1 = positions.get(i);
        int pos2 = positions.get(j);
        int temp = os[pos1];
        os[pos1] = os[pos2];
        os[pos2] = temp;
    }
    
    // 辅助类：存储位置、工件号和面积
    private static class PosJobArea {
        int pos;
        int jobNo;
        double area;
        
        PosJobArea(int pos, int jobNo, double area) {
            this.pos = pos;
            this.jobNo = jobNo;
            this.area = area;
        }
    }
    
    // 辅助类：存储位置、工件号和高度
    private static class PosJobHeight {
        int pos;
        int jobNo;
        double height;
        
        PosJobHeight(int pos, int jobNo, double height) {
            this.pos = pos;
            this.jobNo = jobNo;
            this.height = height;
        }
    }
    
    /**
     * 离散工序序列扰动：随机两点交换
     */
    private void discreteOperationSwap(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        int[] os = chromosome.gene_OS;
        int len = os.length;
        
        if (len <= jobCount + 1) {
            return;  // 离散段太短，无法交换
        }
        
        // 在离散段随机选择两个位置
        int pos1 = jobCount + r.nextInt(len - jobCount);
        int pos2 = jobCount + r.nextInt(len - jobCount);
        
        int attempts = 0;
        while (pos1 == pos2 && attempts < 100) {
            pos2 = jobCount + r.nextInt(len - jobCount);
            attempts++;
        }
        
        if (pos1 != pos2) {
            // 交换两个位置的工件
            int temp = os[pos1];
            os[pos1] = os[pos2];
            os[pos2] = temp;
        }
    }
    
    /**
     * 离散机器修复：优先选择本工序中加工时间最小的机器
     * 如果已经选择了则选择第二小的，依此类推
     */
    private void discreteMachineRepair(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        
        // 对离散段的每个位置进行机器修复
        for (int pos = jobCount; pos < os.length; pos++) {
            int jobNo = os[pos];
            
            // 计算这是该工件的第几个离散工序
            int operNo = 2;  // 从工序2开始
            for (int i = jobCount; i < pos; i++) {
                if (os[i] == jobNo) {
                    operNo++;
                }
            }
            
            // 检查工序编号是否有效
            if (operNo >= operationToIndex[jobNo].length) {
                continue;
            }
            
            int operIdx = operationToIndex[jobNo][operNo];
            
            // 收集所有可用机器及其加工时间
            List<MachineTime> availableMachines = new ArrayList<>();
            for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                double time = proDesMatrix[operIdx][k];
                if (time != 0 && time != Double.MAX_VALUE) {
                    availableMachines.add(new MachineTime(k + 1, time));  // 1-based
                }
            }
            
            if (availableMachines.isEmpty()) {
                continue;
            }
            
            // 按加工时间升序排序
            availableMachines.sort(Comparator.comparingDouble(m -> m.time));
            
            // 统计当前染色体中已经使用的机器
            Map<Integer, Integer> machineUsage = new HashMap<>();
            for (int i = jobCount; i < os.length; i++) {
                int m = ms[i];
                machineUsage.put(m, machineUsage.getOrDefault(m, 0) + 1);
            }
            
            // 优先选择加工时间最小且使用次数最少的机器
            int bestMachine = availableMachines.get(0).machineNo;  // 默认选择时间最短的
            int minUsage = Integer.MAX_VALUE;
            
            for (MachineTime mt : availableMachines) {
                int usage = machineUsage.getOrDefault(mt.machineNo, 0);
                if (usage < minUsage) {
                    minUsage = usage;
                    bestMachine = mt.machineNo;
                }
            }
            
            ms[pos] = bestMachine;
        }
    }
    
    /**
     * 辅助类：机器及其加工时间
     */
    private static class MachineTime {
        int machineNo;  // 1-based
        double time;
        
        MachineTime(int machineNo, double time) {
            this.machineNo = machineNo;
            this.time = time;
        }
    }

    private void operPrintSeqMutationSwap(Chromosome chromosome) {
        int[] geneOs = chromosome.gene_OS;
        int[] geneMs = chromosome.gene_MS;
        int printCount = input.getJobCount();
        // 随机查找两点
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            int posa = r.nextInt(printCount);
            int posb = r.nextInt(printCount);
            while (posa == posb)
                posb = r.nextInt(printCount);
            int temp;
            int temp1;
            if (posa > posb) {
                temp = posa;
                posa = posb;
                posb = temp;
            }
            temp = geneOs[posa];
            temp1 = geneMs[posa];
            geneOs[posa] = geneOs[posb];
            geneMs[posa] = geneMs[posb];
            geneOs[posb] = temp;
            geneMs[posb] = temp1;
        }
    }

    public void operSeqMutationSwap(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        int len = os.length;
        
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            // 基于工件的交换策略：交换两个工件的所有离散工序（作为整体块）
            // 这样保持每个工件内部工序-机器的对应关系
            
            // 第1步：找出所有出现在离散段的工件及其位置
            Map<Integer, List<Integer>> jobPositions = new HashMap<>();
            for (int i = jobCount; i < len; i++) {
                int job = os[i];
                if (!jobPositions.containsKey(job)) {
                    jobPositions.put(job, new ArrayList<>());
                }
                jobPositions.get(job).add(i);
            }
            
            List<Integer> jobList = new ArrayList<>(jobPositions.keySet());
            if (jobList.size() < 2) {
                return;  // 少于2个工件，无法交换
            }
            
            // 第2步：随机选择两个不同的工件
            int job1 = jobList.get(r.nextInt(jobList.size()));
            int job2 = jobList.get(r.nextInt(jobList.size()));
            int attempts = 0;
            while (job1 == job2 && attempts < 100) {
                job2 = jobList.get(r.nextInt(jobList.size()));
                attempts++;
            }
            if (job1 == job2) {
                return;
            }
            
            // 第3步：交换两个工件的所有工序
            List<Integer> pos1 = jobPositions.get(job1);
            List<Integer> pos2 = jobPositions.get(job2);
            
            // 保存两个工件的所有(工序,机器)对
            int[] job1Os = new int[pos1.size()];
            int[] job1Ms = new int[pos1.size()];
            for (int i = 0; i < pos1.size(); i++) {
                job1Os[i] = os[pos1.get(i)];
                job1Ms[i] = ms[pos1.get(i)];
            }
            
            int[] job2Os = new int[pos2.size()];
            int[] job2Ms = new int[pos2.size()];
            for (int i = 0; i < pos2.size(); i++) {
                job2Os[i] = os[pos2.get(i)];
                job2Ms[i] = ms[pos2.get(i)];
            }
            
            // 第4步：重新构建离散段，交换两个工件的所有工序块
            // 简单策略：遍历原离散段，遇到job1就写入job2的工序，遇到job2就写入job1的工序
            int[] tempOs = new int[len - jobCount];
            int[] tempMs = new int[len - jobCount];
            int writePos = 0;
            int job1Index = 0;  // job1工序的写入索引
            int job2Index = 0;  // job2工序的写入索引
            
            for (int i = jobCount; i < len; i++) {
                int currentJob = os[i];
                if (currentJob == job1) {
                    // 遇到job1，写入job2的下一个工序
                    if (job2Index < job2Os.length) {
                        tempOs[writePos] = job2Os[job2Index];
                        tempMs[writePos] = job2Ms[job2Index];
                        job2Index++;
                        writePos++;
                    }
                } else if (currentJob == job2) {
                    // 遇到job2，写入job1的下一个工序
                    if (job1Index < job1Os.length) {
                        tempOs[writePos] = job1Os[job1Index];
                        tempMs[writePos] = job1Ms[job1Index];
                        job1Index++;
                        writePos++;
                    }
                } else {
                    // 其他工件，保持不变
                    tempOs[writePos] = os[i];
                    tempMs[writePos] = ms[i];
                    writePos++;
                }
            }
            
            // 如果job1和job2的工序数量不同，可能还有剩余
            // 追加到末尾
            while (job1Index < job1Os.length) {
                tempOs[writePos] = job1Os[job1Index];
                tempMs[writePos] = job1Ms[job1Index];
                job1Index++;
                writePos++;
            }
            while (job2Index < job2Os.length) {
                tempOs[writePos] = job2Os[job2Index];
                tempMs[writePos] = job2Ms[job2Index];
                job2Index++;
                writePos++;
            }
            
            // 复制回原数组
            System.arraycopy(tempOs, 0, os, jobCount, tempOs.length);
            System.arraycopy(tempMs, 0, ms, jobCount, tempMs.length);
        }
    }

    public void operSeqMutationNeighbor(int[] os) {
        int len = os.length;
        int pos1 = r.nextInt(len);
        int pos2 = r.nextInt(len);
        while (os[pos1] == os[pos2])
            pos2 = r.nextInt(len);
        int pos3 = r.nextInt(len);
        while (os[pos3] == os[pos2] || os[pos3] == os[pos1])
            pos3 = r.nextInt(len);
        ArrayList<Integer> li = new ArrayList<>();
        li.add(os[pos1]);
        li.add(os[pos2]);
        li.add(os[pos3]);
        Collections.shuffle(li);
        os[pos1] = li.get(0);
        os[pos2] = li.get(1);
        os[pos3] = li.get(2);
    }

    //todo 修改打印机器变异的方法
    public int[] printMachineSeqMutation(Chromosome chromosome) {
        List<Solution>[] batchSolution = chromosome.printSolution;
        
        // printSolution只有在evaluate后才会初始化
        // 交叉或变异后的新染色体还未evaluate，printSolution为null
        if (batchSolution == null) {
            return chromosome.gene_MS;  // 直接返回原机器选择基因
        }
        
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int printCount = input.getJobCount();
        //找到加工时间最短的机器和加工时间最长的机器
        int maxMachineIndex = -1;
        int minMachineIndex = -1;
        for (int i = 0; i < batchSolution.length; i++) {
            List<Solution> machineISolution = batchSolution[i];
            // 跳过空的机器（没有分配任何工件）
            if (machineISolution == null || machineISolution.isEmpty()) {
                continue;
            }
            
            if (maxMachineIndex == -1) {
                maxMachineIndex = i;
                minMachineIndex = i;
            } else {
                List<Solution> machineMaxSolution = batchSolution[maxMachineIndex];
                List<Solution> machineMinSolution = batchSolution[minMachineIndex];
                if (machineISolution.get(machineISolution.size() - 1).endTime > machineMaxSolution.get(machineMaxSolution.size() - 1).endTime) {
                    maxMachineIndex = i;
                }
                if (machineISolution.get(machineISolution.size() - 1).endTime < machineMinSolution.get(machineMinSolution.size() - 1).endTime) {
                    minMachineIndex = i;
                }
            }
        }
        
        // 如果所有机器都为空，直接返回原基因
        if (maxMachineIndex == -1 || minMachineIndex == -1) {
            return ms;
        }
        // maxMachine暂时不需要使用
        PrintMachine minMachine = (PrintMachine) machines[minMachineIndex];
        //将加工时间最长的机器上的零件分给加工时间最短的机器
        //1. 找到机器对应零件的位置映射map（key是机器编号1-based，value是工件索引列表）
        Map<Integer, List<Integer>> machineMap = new HashMap<>();
        for (int i = 0; i < printCount; i++) {
            if (!machineMap.containsKey(ms[i])) {
                machineMap.put(ms[i], new ArrayList<>());
            }
            machineMap.get(ms[i]).add(i);
        }
        
        // maxMachineIndex和minMachineIndex是machines数组索引（0-based）
        // 需要转换为机器编号（1-based）来访问machineMap
        int maxMachineNo = maxMachineIndex + 1;
        int minMachineNo = minMachineIndex + 1;
        
        // 检查maxMachineNo对应的机器是否有分配的工件
        if (!machineMap.containsKey(maxMachineNo) || machineMap.get(maxMachineNo).isEmpty()) {
            return ms;  // 没有工件可以移动
        }
        
        //2. 随机一个加工时间最长机器上的零件
        // 先检查是否有可以移动的工件（尺寸符合要求）
        List<Integer> movableJobs = new ArrayList<>();
        for (int jobIndex : machineMap.get(maxMachineNo)) {
            if (items[jobIndex].l <= minMachine.L && items[jobIndex].w <= minMachine.W && items[jobIndex].h <= minMachine.H) {
                movableJobs.add(jobIndex);
            }
        }
        
        if (movableJobs.isEmpty()) {
            return ms;  // 没有可移动的工件（尺寸都太大）
        }
        
        // 随机选择一个可移动的工件
        int jobIndex = movableJobs.get(r.nextInt(movableJobs.size()));
        ms[jobIndex] = minMachineNo;
        return ms;
    }

    public int[] machineSeqMutation(Chromosome chromosome) {
        int printCount = input.getJobCount();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        //随机生成一个点在printCount到最后一个点之间的点
        int pos = printCount + r.nextInt(os.length - printCount);
        //寻找这个点是第几次碰到的（离散工序段，从工序2开始）
        int count = 2;  // 从工序2开始（工序0=打印，工序1=批处理）
        for (int i = printCount; i < pos; i++) {
            if (os[i] == os[pos]) {
                count++;
            }
        }
        //找到该点在proDesMatrix中的位置
        int jobNo = os[pos];
        // 检查工序编号是否有效
        if (count >= operationToIndex[jobNo].length) {
            return ms; // 工序编号越界，返回原基因
        }
        int index = operationToIndex[jobNo][count];
        List<Integer> list = new ArrayList<>();
        //遍历该点所在列，收集可用的实际机器编号（1-based）
        // gene_MS直接存储实际机器编号
        for (int i = 0; i < proDesMatrix[index].length; i++) {
            //不为0且不为Double.MAX_VALUE的点均可以为加工机器
            if (proDesMatrix[index][i] != 0 && proDesMatrix[index][i] != Double.MAX_VALUE) {
                list.add(i + 1);  // 存储实际机器编号（1-based）
            }
        }
        if (!list.isEmpty()) {
            while (true) {
                int random = r.nextInt(list.size());
                int newMachineNo = list.get(random);  // 实际机器编号
                if (ms[pos] == newMachineNo) {
                    continue;
                }
                ms[pos] = newMachineNo;
                break;
            }
        } else {
            throw new RuntimeException("没有找到可以的加工机器");
        }
        return ms;
    }
}
