package AlgorthmFrame.ga;

import ProblemFrame.Chromosome;
import ProgramEntity.Item;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Problem;
import ProgramEntity.Solution;

import java.util.*;

public class ChromosomeOperation {
    Random r;
    Problem input;

    public ChromosomeOperation(Random r, Problem input) {
        this.r = r;
        this.input = input;
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
        // 分两阶段进行POX交叉：
        // 1. 打印阶段（前jobCount个基因）
        // 2. 离散工序阶段（后面的基因）
        crossoverTwoStage(c1, c2);
    }
    
    /**
     * 两阶段POX交叉：分别对打印阶段和离散工序阶段进行交叉
     */
    private void crossoverTwoStage(Chromosome c1, Chromosome c2) {
        int jobCount = input.getJobCount();
        int len = c1.gene_OS.length;
        
        // 备份原始基因
        int[] p1_OS = new int[len];
        int[] p2_OS = new int[len];
        int[] p1_MS = new int[len];
        int[] p2_MS = new int[len];
        System.arraycopy(c1.gene_OS, 0, p1_OS, 0, len);
        System.arraycopy(c2.gene_OS, 0, p2_OS, 0, len);
        System.arraycopy(c1.gene_MS, 0, p1_MS, 0, len);
        System.arraycopy(c2.gene_MS, 0, p2_MS, 0, len);
        
        // ===== 第一阶段：打印工序交叉（前jobCount个基因）=====
        // 随机选择一部分工件（确保不是全部，以产生实际交叉效果）
        ArrayList<Integer> allJobs = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            allJobs.add(i);
        }
        Collections.shuffle(allJobs, r);
        // splitPoint范围[1, jobCount-1]，确保两个集合都不为空
        int splitPoint = 1 + r.nextInt(Math.max(1, jobCount - 1));
        Set<Integer> jobSet1 = new HashSet<>(allJobs.subList(0, splitPoint));
        
        // 对打印阶段进行POX交叉
        int[] child1_print_OS = new int[jobCount];
        int[] child2_print_OS = new int[jobCount];
        int[] child1_print_MS = new int[jobCount];
        int[] child2_print_MS = new int[jobCount];
        Arrays.fill(child1_print_OS, -1);
        Arrays.fill(child2_print_OS, -1);
        Arrays.fill(child1_print_MS, -1);
        Arrays.fill(child2_print_MS, -1);
        
        // 保留jobSet1中的工件位置
        for (int i = 0; i < jobCount; i++) {
            if (jobSet1.contains(p1_OS[i])) {
                child1_print_OS[i] = p1_OS[i];
                child1_print_MS[i] = p1_MS[i];
            }
            if (jobSet1.contains(p2_OS[i])) {
                child2_print_OS[i] = p2_OS[i];
                child2_print_MS[i] = p2_MS[i];
            }
        }
        
        // 从另一个父代填充剩余位置
        int idx1 = 0, idx2 = 0;
        for (int i = 0; i < jobCount; i++) {
            // 填充child1
            if (child1_print_OS[i] == -1) {
                while (idx2 < jobCount && jobSet1.contains(p2_OS[idx2])) {
                    idx2++;
                }
                if (idx2 < jobCount) {
                    child1_print_OS[i] = p2_OS[idx2];
                    child1_print_MS[i] = p2_MS[idx2];
                    idx2++;
                }
            }
            // 填充child2
            if (child2_print_OS[i] == -1) {
                while (idx1 < jobCount && jobSet1.contains(p1_OS[idx1])) {
                    idx1++;
                }
                if (idx1 < jobCount) {
                    child2_print_OS[i] = p1_OS[idx1];
                    child2_print_MS[i] = p1_MS[idx1];
                    idx1++;
                }
            }
        }
        
        // 复制打印阶段结果
        System.arraycopy(child1_print_OS, 0, c1.gene_OS, 0, jobCount);
        System.arraycopy(child2_print_OS, 0, c2.gene_OS, 0, jobCount);
        System.arraycopy(child1_print_MS, 0, c1.gene_MS, 0, jobCount);
        System.arraycopy(child2_print_MS, 0, c2.gene_MS, 0, jobCount);
        
        // ===== 第二阶段：离散工序交叉（从jobCount开始）=====
        int discreteLen = len - jobCount;
        if (discreteLen > 0) {
            // 重新随机选择工件集合（确保不是全部）
            Collections.shuffle(allJobs, r);
            int splitPoint2 = 1 + r.nextInt(Math.max(1, jobCount - 1));
            Set<Integer> jobSet2 = new HashSet<>(allJobs.subList(0, splitPoint2));
            
            int[] child1_discrete_OS = new int[discreteLen];
            int[] child2_discrete_OS = new int[discreteLen];
            int[] child1_discrete_MS = new int[discreteLen];
            int[] child2_discrete_MS = new int[discreteLen];
            Arrays.fill(child1_discrete_OS, -1);
            Arrays.fill(child2_discrete_OS, -1);
            Arrays.fill(child1_discrete_MS, -1);
            Arrays.fill(child2_discrete_MS, -1);
            
            // 保留jobSet2中的工件位置
            for (int i = 0; i < discreteLen; i++) {
                int p1Val = p1_OS[jobCount + i];
                int p2Val = p2_OS[jobCount + i];
                if (jobSet2.contains(p1Val)) {
                    child1_discrete_OS[i] = p1Val;
                    child1_discrete_MS[i] = p1_MS[jobCount + i];
                }
                if (jobSet2.contains(p2Val)) {
                    child2_discrete_OS[i] = p2Val;
                    child2_discrete_MS[i] = p2_MS[jobCount + i];
                }
            }
            
            // 从另一个父代填充剩余位置
            idx1 = 0;
            idx2 = 0;
            for (int i = 0; i < discreteLen; i++) {
                // 填充child1
                if (child1_discrete_OS[i] == -1) {
                    while (idx2 < discreteLen && jobSet2.contains(p2_OS[jobCount + idx2])) {
                        idx2++;
                    }
                    if (idx2 < discreteLen) {
                        child1_discrete_OS[i] = p2_OS[jobCount + idx2];
                        child1_discrete_MS[i] = p2_MS[jobCount + idx2];
                        idx2++;
                    }
                }
                // 填充child2
                if (child2_discrete_OS[i] == -1) {
                    while (idx1 < discreteLen && jobSet2.contains(p1_OS[jobCount + idx1])) {
                        idx1++;
                    }
                    if (idx1 < discreteLen) {
                        child2_discrete_OS[i] = p1_OS[jobCount + idx1];
                        child2_discrete_MS[i] = p1_MS[jobCount + idx1];
                        idx1++;
                    }
                }
            }
            
            // 复制离散阶段结果
            System.arraycopy(child1_discrete_OS, 0, c1.gene_OS, jobCount, discreteLen);
            System.arraycopy(child2_discrete_OS, 0, c2.gene_OS, jobCount, discreteLen);
            System.arraycopy(child1_discrete_MS, 0, c1.gene_MS, jobCount, discreteLen);
            System.arraycopy(child2_discrete_MS, 0, c2.gene_MS, jobCount, discreteLen);
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

    public void operSeqCrossoverPOX(int o1[], int o2[], int num) {
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
        if (num == 0) {
            for (int i = 0; i < jobCount; i++) {
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
            for (int i = 0; i < jobCount; i++) {
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
        } else {
            for (int i = jobCount; i < len; i++) {
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
            for (int i = jobCount; i < len; i++) {
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
        int[] pm1 = new int[len];
        int[] pm2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(m1, 0, pm1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        System.arraycopy(m2, 0, pm2, 0, len);
        Arrays.fill(o1, -1);
        Arrays.fill(m1, -1);
        Arrays.fill(o2, -1);
        Arrays.fill(m2, -1);
        if (num == 0) {
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
                    while (index1 < p1.length && p1[index1] == -1) {  // ✓ 添加边界检查
                        index1++;
                    }
                    if (index1 < p1.length) {  // ✓ 确保不越界
                        o2[i] = p1[index1];
                        m2[i] = pm1[index1];
                        index1++;
                    }
                }
                if (o1[i] == -1) {
                    while (index2 < p2.length && p2[index2] == -1) {  // ✓ 添加边界检查
                        index2++;
                    }
                    if (index2 < p2.length) {  // ✓ 确保不越界
                        o1[i] = p2[index2];
                        m1[i] = pm2[index2];
                        index2++;
                    }
                }
            }
        } else {
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

            int index1 = 0;
            int index2 = 0;
            for (int i = jobCount; i < len; i++) {
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

    public void Mutation(Chromosome chromosome) {
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            operPrintSeqMutationSwap(chromosome);
            chromosome.gene_MS = printMachineSeqMutation(chromosome);
        } else {
            operSeqMutationSwap(chromosome);
            chromosome.gene_MS = machineSeqMutation(chromosome);
            //operSeqMutationNeighbor(chromosome.gene_OS);
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
        
        // 检查是否有离散工序可以变异
        int discreteLen = len - jobCount;
        if (discreteLen < 2) {
            return; // 离散工序不足2个，无法交换
        }
        
        // 随机查找两点（只在离散阶段内交换）
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            int posa = jobCount + r.nextInt(discreteLen);
            int posb = jobCount + r.nextInt(discreteLen);
            // 修复：确保posb也在离散阶段内
            while (posa == posb) {
                posb = jobCount + r.nextInt(discreteLen);
            }
            int temp;
            int temp1;
            if (posa > posb) {
                temp = posa;
                posa = posb;
                posb = temp;
            }
            temp = os[posa];
            temp1 = ms[posa];
            ms[posa] = ms[posb];
            os[posa] = os[posb];
            os[posb] = temp;
            ms[posb] = temp1;
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

    /**
     * 打印机器变异：随机选择一个工件，将其分配到另一台打印机
     */
    public int[] printMachineSeqMutation(Chromosome chromosome) {
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int printCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        
        if (printMachineCount <= 1) {
            return ms; // 只有一台打印机，无法变异
        }
        
        // 随机选择一个工件位置（打印阶段）
        int pos = r.nextInt(printCount);
        int jobNo = os[pos];
        int currentMachine = ms[pos]; // 当前机器号（1-based）
        
        // 随机选择另一台打印机（机器号1到printMachineCount）
        int newMachine = currentMachine;
        int maxAttempts = 10;
        int attempt = 0;
        while (newMachine == currentMachine && attempt < maxAttempts) {
            newMachine = r.nextInt(printMachineCount) + 1; // 1-based机器号
            // 检查工件尺寸是否适合新打印机
            PrintMachine pm = (PrintMachine) machines[newMachine - 1]; // 0-based索引
            Item item = items[jobNo];
            if (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H) {
                break; // 尺寸合适
            }
            attempt++;
        }
        
        if (newMachine != currentMachine) {
            ms[pos] = newMachine;
        }
        return ms;
    }

    public int[] machineSeqMutation(Chromosome chromosome) {
        int printCount = input.getJobCount();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        
        if (os.length <= printCount) {
            return ms; // 没有离散工序
        }
        
        //随机生成一个点在printCount到最后一个点之间的点
        int pos = printCount + r.nextInt(os.length - printCount);
        //寻找这个点是该工件的第几道离散工序
        int discreteCount = 0;
        for (int i = printCount; i <= pos; i++) {
            if (os[i] == os[pos]) {
                discreteCount++;
            }
        }
        // 实际工序号 = 2 + (离散工序计数-1)，因为工序0是打印，工序1是批处理
        int operNo = 2 + (discreteCount - 1);
        
        //找到该点在proDesMatrix中的位置
        int index = operationToIndex[os[pos]][operNo];
        List<Integer> availableMachines = new ArrayList<>();
        // 离散工序只能选择离散加工机器（跳过打印机和批处理机）
        int printMachineCount = input.getPrintMachineCount();
        int batchMachineCount = input.getBatchMachineCount();
        int discreteMachineStart = printMachineCount + batchMachineCount; // 0-based起始索引
        for (int i = discreteMachineStart; i < proDesMatrix[index].length; i++) {
            if (proDesMatrix[index][i] > 0) {
                availableMachines.add(i + 1); // 转为1-based机器号
            }
        }
        
        if (!availableMachines.isEmpty() && availableMachines.size() > 1) {
            // 随机选择一个不同于当前的机器
            int currentMachine = ms[pos];
            int newMachine = currentMachine;
            int maxAttempts = 10;
            int attempt = 0;
            while (newMachine == currentMachine && attempt < maxAttempts) {
                newMachine = availableMachines.get(r.nextInt(availableMachines.size()));
                attempt++;
            }
            ms[pos] = newMachine;
        } else if (availableMachines.size() == 1) {
            ms[pos] = availableMachines.get(0);
        }
        return ms;
    }
}

