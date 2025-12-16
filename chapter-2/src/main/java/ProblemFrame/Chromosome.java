package ProblemFrame;

import ProgramEntity.Job;
import ProgramEntity.Solution;

import java.util.*;

public class Chromosome implements Comparable<Chromosome> {
    public int[] gene_OS;
    public int[] gene_MS;

    public Random r;
    public double fitness;
    public List<Solution>[] printSolution;

    public Chromosome(Random r) {
        this.r = r;
    }

    public Chromosome(Job[] entries, Random r) {
        this.r = r;
        int jobCount = entries.length;
        
        // 计算染色体中需要编码的工序数
        // 染色体只包含：打印工序（每个工件1次） + 离散工序（每个工件opsNr-2次）
        // 批处理工序（第2道）不在染色体中体现，由算法自动安排
        int totalGenesInChromosome = 0;
        for (Job e : entries) {
            // 打印工序1次 + 离散工序(opsNr-2)次 = opsNr-1次
            totalGenesInChromosome += (e.opsNr - 1);
        }
        
        // ============ 第一部分：打印工序（每个工件一次） ============
        // 前jobCount个基因专门用于打印阶段的机器分配
        ArrayList<Integer> printOs = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            printOs.add(i); // 每个工件只出现一次
        }
        Collections.shuffle(printOs, this.r); // shuffle决定打印顺序
        
        // ============ 第二部分：离散工序（跳过批处理） ============
        // 每个工件的离散工序（工序2及之后，因为工序0是打印，工序1是批处理）
        ArrayList<Integer> discreteOs = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            for (int j = 2; j < entries[i].opsNr; j++) { // 从工序2开始（离散工序）
                discreteOs.add(i);
            }
        }
        Collections.shuffle(discreteOs, this.r);
        
        // ============ 合并gene_OS ============
        this.gene_OS = new int[totalGenesInChromosome];
        for (int i = 0; i < printOs.size(); i++) {
            this.gene_OS[i] = printOs.get(i);
        }
        for (int i = 0; i < discreteOs.size(); i++) {
            this.gene_OS[jobCount + i] = discreteOs.get(i);
        }
        
        // ============ 生成gene_MS（与gene_OS对应）============
        // gene_MS[i] 直接存储机器号（1-based），不是备选机器的索引
        this.gene_MS = new int[totalGenesInChromosome];
        
        // 打印工序的机器选择（前jobCount个基因）
        for (int i = 0; i < jobCount; i++) {
            int jobNo = this.gene_OS[i];
            // 从打印工序（工序0）的可用机器列表中随机选择一台
            List<Integer> availMachines = entries[jobNo].availableMachines[0];
            this.gene_MS[i] = availMachines.get(r.nextInt(availMachines.size()));
        }
        
        // 离散工序的机器选择（第jobCount个基因开始）
        int[] discreteOperCount = new int[jobCount]; // 追踪每个工件当前是第几道离散工序
        for (int i = jobCount; i < totalGenesInChromosome; i++) {
            int jobNo = this.gene_OS[i];
            int discreteIdx = discreteOperCount[jobNo]; // 当前是该工件的第几道离散工序
            int operNo = 2 + discreteIdx; // 实际工序号（工序2开始是离散工序）
            
            // 从该工序的可用机器列表中随机选择一台
            List<Integer> availMachines = entries[jobNo].availableMachines[operNo];
            this.gene_MS[i] = availMachines.get(r.nextInt(availMachines.size()));
            
            discreteOperCount[jobNo]++;
        }

        this.fitness = 0;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }

    public Chromosome(int[] OS, int[] MS, Random r) {
        this.gene_OS = OS;
        this.gene_MS = MS;
        this.r = r;
        this.fitness = -1;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }

    public Chromosome(Chromosome c) {
        this.gene_MS = new int[c.gene_MS.length];
        System.arraycopy(c.gene_MS, 0, this.gene_MS, 0, c.gene_MS.length);
        this.gene_OS = new int[c.gene_OS.length];
        System.arraycopy(c.gene_OS, 0, this.gene_OS, 0, c.gene_OS.length);
        this.r = c.r;
        this.fitness = c.fitness;
        // 复制printSolution
        if (c.printSolution != null) {
            this.printSolution = new List[c.printSolution.length];
            for (int i = 0; i < c.printSolution.length; i++) {
                if (c.printSolution[i] != null) {
                    this.printSolution[i] = new ArrayList<>(c.printSolution[i]);
                } else {
                    this.printSolution[i] = new ArrayList<>();
                }
            }
        } else {
            this.printSolution = null;
        }
    }

    @Override
    public int compareTo(Chromosome o) {
        Chromosome s = (Chromosome) o;
        if (s.fitness > this.fitness) {
            return 1;
        } else if (this.fitness == s.fitness) {
            return 0;
        } else {
            return -1;
        }
    }
    
    /**
     * 打印染色体的详细信息，用于调试
     * @param jobCount 工件数量，用于分析gene_OS
     */
    public void printChromosomeInfo(int jobCount) {
        System.out.println("\n" + "--------------------------");
        System.out.println("              染色体详细信息");
        System.out.println("--------------------------------");
        
        System.out.println("基因长度: OS=" + gene_OS.length + ", MS=" + gene_MS.length);
        System.out.println("染色体结构: [打印工序:" + jobCount + "个] + [离散工序:" + (gene_OS.length - jobCount) + "个]");
        System.out.println("注意: 批处理工序不在染色体中体现，由算法自动安排");
        System.out.println("适应度: " + fitness);
        
        // 打印gene_OS
        System.out.println("\n【工序序列 gene_OS】");
        System.out.print("  打印工序: ");
        for (int i = 0; i < jobCount && i < gene_OS.length; i++) {
            System.out.print(gene_OS[i] + " ");
        }
        System.out.println();
        System.out.print("  离散工序: ");
        for (int i = jobCount; i < gene_OS.length; i++) {
            System.out.print(gene_OS[i] + " ");
            if ((i - jobCount + 1) % 20 == 0) System.out.print("\n            ");
        }
        System.out.println();
        
        // 统计每个工件在gene_OS中出现的次数
        System.out.println("\n【工件出现次数统计】");
        int[] jobCountsInPrint = new int[jobCount];
        int[] jobCountsInDiscrete = new int[jobCount];
        for (int i = 0; i < jobCount && i < gene_OS.length; i++) {
            jobCountsInPrint[gene_OS[i]]++;
        }
        for (int i = jobCount; i < gene_OS.length; i++) {
            jobCountsInDiscrete[gene_OS[i]]++;
        }
        for (int i = 0; i < jobCount; i++) {
            System.out.println("  工件" + i + ": 打印" + jobCountsInPrint[i] + "次, 离散" + jobCountsInDiscrete[i] + "次");
        }
        
        // 分析打印阶段（前jobCount个基因）
        System.out.println("\n【打印阶段分析（前" + jobCount + "个基因）】");
        for (int i = 0; i < jobCount && i < gene_OS.length; i++) {
            int jobNo = gene_OS[i];
            int machineNo = gene_MS[i];
            System.out.println("  位置" + i + ": 工件" + jobNo + " → 打印机" + machineNo);
        }
        
        // 检查打印阶段是否有重复工件
        System.out.println("\n【打印阶段重复检查】");
        boolean[] printedJobs = new boolean[jobCount];
        boolean hasDuplicate = false;
        for (int i = 0; i < jobCount && i < gene_OS.length; i++) {
            int jobNo = gene_OS[i];
            if (printedJobs[jobNo]) {
                System.out.println("  警告: 工件" + jobNo + "在打印阶段被重复分配！(位置" + i + ")");
                hasDuplicate = true;
            }
            printedJobs[jobNo] = true;
        }
        if (!hasDuplicate) {
            System.out.println("  [OK] 打印阶段无重复工件");
        }
        
        // 检查是否所有工件都被分配了打印机
        System.out.println("\n【工件打印分配检查】");
        boolean allAssigned = true;
        for (int i = 0; i < jobCount; i++) {
            if (!printedJobs[i]) {
                System.out.println("  警告: 工件" + i + "未被分配打印任务！");
                allAssigned = false;
            }
        }
        if (allAssigned) {
            System.out.println("  [OK] 所有工件都已分配打印机");
        }
        
        // 打印gene_MS
        System.out.println("\n【机器选择 gene_MS】");
        System.out.print("  打印机选择: ");
        for (int i = 0; i < jobCount && i < gene_MS.length; i++) {
            System.out.print(gene_MS[i] + " ");
        }
        System.out.println();
        System.out.print("  离散机器选择: ");
        for (int i = jobCount; i < gene_MS.length; i++) {
            System.out.print(gene_MS[i] + " ");
            if ((i - jobCount + 1) % 20 == 0) System.out.print("\n                  ");
        }
        System.out.println();
        
        System.out.println("-----------------------------------------");
    }
}
