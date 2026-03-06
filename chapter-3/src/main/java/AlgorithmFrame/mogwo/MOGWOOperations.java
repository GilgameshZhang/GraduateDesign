package AlgorithmFrame.mogwo;

import ProblemFrame.*;
import ProgramEntity.*;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;

import java.util.*;

/**
 * MOGWO算法的基因操作类
 * 
 * 提供MOGWO算法所需的交叉、变异、修复等操作
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOGWOOperations {
    
    private Problem problem;
    private Random random;
    
    // 问题相关信息（缓存）
    private int jobCount;
    private int totalPrintMachineCount;
    private int totalDiscreteMachineCount;
    private double[][] proDesMatrix;
    private int[][] operationToIndex;
    private Machine[] machines;
    private Item[] items;
    private int[] opsCount;
    
    /**
     * 构造函数
     */
    public MOGWOOperations(Problem problem, Random random) {
        this.problem = problem;
        this.random = random;
        
        // 缓存问题信息
        this.jobCount = problem.getJobCount();
        this.totalPrintMachineCount = problem.getPrintMachineCount();
        this.totalDiscreteMachineCount = problem.getMachineCount() - problem.getPrintMachineCount();
        this.proDesMatrix = problem.getProDesMatrix();
        this.operationToIndex = problem.getOperationToIndex();
        this.machines = problem.getMachines();
        this.items = problem.getItems();
        this.opsCount = problem.getOperationCountArr();
    }
    
    // ==================== 交叉操作 ====================
    
    /**
     * OS基因的POX交叉（Precedence Preserving Order-based Crossover）
     * 
     * @param parent1 父代1的OS基因
     * @param parent2 父代2的OS基因
     * @param inheritProb 从parent1继承的概率
     * @return 子代的OS基因
     */
    public int[] crossoverOS(int[] parent1, int[] parent2, double inheritProb) {
        int length = parent1.length;
        int[] offspring = new int[length];
        boolean[] visited = new boolean[length];
        
        // 从parent1继承一部分job
        Set<Integer> inheritedJobs = new HashSet<>();
        for (int i = 0; i < jobCount; i++) {
            if (random.nextDouble() < inheritProb) {
                inheritedJobs.add(i);
            }
        }
        
        // 从parent1复制被选中的job的工序
        int index = 0;
        for (int i = 0; i < length; i++) {
            if (inheritedJobs.contains(parent1[i])) {
                offspring[index] = parent1[i];
                visited[i] = true;
                index++;
            }
        }
        
        // 从parent2复制剩余的工序（保持顺序）
        for (int i = 0; i < length; i++) {
            if (!inheritedJobs.contains(parent2[i])) {
                offspring[index] = parent2[i];
                index++;
            }
        }
        
        return offspring;
    }
    
    /**
     * MS基因的单点交叉
     * 
     * @param parent1 父代1的MS基因
     * @param parent2 父代2的MS基因
     * @return 子代的MS基因
     */
    public int[] crossoverMS(int[] parent1, int[] parent2) {
        int length = parent1.length;
        int[] offspring = new int[length];
        
        // 随机选择交叉点
        int crossPoint = random.nextInt(length);
        
        // 交叉
        for (int i = 0; i < length; i++) {
            if (i < crossPoint) {
                offspring[i] = parent1[i];
            } else {
                offspring[i] = parent2[i];
            }
        }
        
        return offspring;
    }
    
    // ==================== 变异操作 ====================
    
    /**
     * OS基因的交换变异
     * 
     * @param os OS基因
     * @param mutationRate 变异率
     * @return 变异后的OS基因
     */
    public int[] mutateOS(int[] os, double mutationRate) {
        int[] mutated = Arrays.copyOf(os, os.length);
        
        if (random.nextDouble() < mutationRate) {
            // 随机选择两个位置进行交换
            int pos1 = random.nextInt(os.length);
            int pos2 = random.nextInt(os.length);
            
            int temp = mutated[pos1];
            mutated[pos1] = mutated[pos2];
            mutated[pos2] = temp;
        }
        
        return mutated;
    }
    
    /**
     * MS基因的变异（随机改变机器选择）
     * 
     * @param ms MS基因
     * @param mutationRate 变异率
     * @return 变异后的MS基因
     */
    public int[] mutateMS(int[] ms, double mutationRate) {
        int[] mutated = Arrays.copyOf(ms, ms.length);
        
        // 打印工序的MS变异
        int printOpsCount = jobCount;
        for (int i = 0; i < printOpsCount; i++) {
            if (random.nextDouble() < mutationRate) {
                int jobNo = i;  // 假设前jobCount个是打印工序
                
                // 找出所有能容纳该零件的打印机
                ArrayList<Integer> suitableMachines = new ArrayList<>();
                Item item = items[jobNo];
                
                for (int m = 0; m < totalPrintMachineCount; m++) {
                    PrintMachine pm = (PrintMachine) machines[m];
                    boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                    boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                    if (fitsNormal || fitsRotated) {
                        suitableMachines.add(m + 1);  // 1-based
                    }
                }
                
                if (!suitableMachines.isEmpty()) {
                    mutated[i] = suitableMachines.get(random.nextInt(suitableMachines.size()));
                }
            }
        }
        
        // 离散工序的MS变异
        int msIndex = printOpsCount;
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            int discreteOpsCount = opsCount[jobNo] - 2;
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                if (random.nextDouble() < mutationRate) {
                    int operNo = 2 + localOperNo;
                    int operIdx = operationToIndex[jobNo][operNo];
                    
                    // 找出可用机器
                    ArrayList<Integer> availableMachines = new ArrayList<>();
                    for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                        if (proDesMatrix[operIdx][k] != 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                            availableMachines.add(k + 1);
                        }
                    }
                    
                    if (!availableMachines.isEmpty()) {
                        int selectedMachineIndex = random.nextInt(availableMachines.size());
                        mutated[msIndex] = selectedMachineIndex + 1;  // 相对索引
                    }
                }
                msIndex++;
            }
        }
        
        return mutated;
    }
    
    // ==================== 修复操作 ====================
    
    /**
     * 修复个体（确保基因合法性）
     * 
     * @param individual 待修复的个体
     */
    public void repair(MOIndividual individual) {
        // 1. 修复OS基因（确保每个工件的工序数正确）
        repairOS(individual);
        
        // 2. 修复MS基因（确保机器选择合法）
        repairMS(individual);
    }
    
    /**
     * 修复OS基因（两段式编码）
     */
    private void repairOS(MOIndividual individual) {
        int[] os = individual.gene_OS;
        
        // ==================== 第一段：打印工序（前jobCount个）====================
        // 检查打印段：应该恰好包含0到jobCount-1各一次
        Set<Integer> printJobs = new HashSet<>();
        boolean printNeedRepair = false;
        
        for (int i = 0; i < jobCount; i++) {
            int job = os[i];
            if (job < 0 || job >= jobCount || printJobs.contains(job)) {
                printNeedRepair = true;
                break;
            }
            printJobs.add(job);
        }
        
        // 修复打印段
        if (printNeedRepair || printJobs.size() != jobCount) {
            ArrayList<Integer> jobs = new ArrayList<>();
            for (int i = 0; i < jobCount; i++) {
                jobs.add(i);
            }
            Collections.shuffle(jobs, random);
            
            for (int i = 0; i < jobCount; i++) {
                individual.gene_OS[i] = jobs.get(i);
            }
        }
        
        // ==================== 第二段：离散工序（jobCount之后）====================
        // 检查离散段：统计每个job的离散工序数
        int[] discreteJobCounts = new int[jobCount];
        for (int i = jobCount; i < os.length; i++) {
            int job = os[i];
            if (job >= 0 && job < jobCount) {
                discreteJobCounts[job]++;
            }
        }
        
        // 检查是否需要修复离散段
        boolean discreteNeedRepair = false;
        for (int i = 0; i < jobCount; i++) {
            int expectedDiscreteOps = opsCount[i] - 2;  // 减去打印和批处理工序
            if (discreteJobCounts[i] != expectedDiscreteOps) {
                discreteNeedRepair = true;
                break;
            }
        }
        
        // 修复离散段
        if (discreteNeedRepair) {
            ArrayList<Integer> discreteOS = new ArrayList<>();
            for (int i = 0; i < jobCount; i++) {
                int discreteOpsCount = opsCount[i] - 2;
                for (int j = 0; j < discreteOpsCount; j++) {
                    discreteOS.add(i);
                }
            }
            Collections.shuffle(discreteOS, random);
            
            for (int i = 0; i < discreteOS.size(); i++) {
                individual.gene_OS[jobCount + i] = discreteOS.get(i);
            }
        }
    }
    
    /**
     * 修复MS基因（两段式编码）
     */
    private void repairMS(MOIndividual individual) {
        int[] os = individual.gene_OS;
        int[] ms = individual.gene_MS;
        
        // ==================== 第一段：打印工序的MS（前jobCount个）====================
        // 打印段的MS需要根据打印段的OS来确定对应的job
        for (int i = 0; i < jobCount; i++) {
            int jobNo = os[i];  // 获取第i个位置的工件编号
            int machineId = ms[i];
            
            // 检查机器是否合法
            if (machineId < 1 || machineId > totalPrintMachineCount) {
                // 随机选择一个合法的打印机
                ms[i] = selectValidPrintMachine(jobNo);
            } else {
                // 检查机器能否容纳该零件
                if (!canFitInMachine(jobNo, machineId - 1)) {
                    ms[i] = selectValidPrintMachine(jobNo);
                }
            }
        }
        
        // ==================== 第二段：离散工序的MS（jobCount之后）====================
        // 离散段的MS是按job顺序排列的（与OS的实际值无关）
        int msIndex = jobCount;
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            int discreteOpsCount = opsCount[jobNo] - 2;
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;
                int operIdx = operationToIndex[jobNo][operNo];
                
                if (msIndex >= ms.length) {
                    break;  // 防止越界
                }
                
                int relativeIndex = ms[msIndex];
                
                // 获取可用机器列表
                ArrayList<Integer> availableMachines = new ArrayList<>();
                for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                    if (proDesMatrix[operIdx][k] != 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                        availableMachines.add(k + 1);
                    }
                }
                
                // 检查当前选择是否合法
                if (availableMachines.isEmpty()) {
                    ms[msIndex] = 1;  // 默认值（虽然不合法，但避免崩溃）
                } else if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
                    // 随机选择一个合法的机器
                    ms[msIndex] = random.nextInt(availableMachines.size()) + 1;
                }
                
                msIndex++;
            }
        }
    }
    
    /**
     * 选择一个合法的打印机
     */
    private int selectValidPrintMachine(int jobNo) {
        Item item = items[jobNo];
        ArrayList<Integer> suitableMachines = new ArrayList<>();
        
        for (int m = 0; m < totalPrintMachineCount; m++) {
            PrintMachine pm = (PrintMachine) machines[m];
            boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
            boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
            if (fitsNormal || fitsRotated) {
                suitableMachines.add(m + 1);  // 1-based
            }
        }
        
        if (suitableMachines.isEmpty()) {
            return 1;  // 默认第一台机器
        }
        
        return suitableMachines.get(random.nextInt(suitableMachines.size()));
    }
    
    /**
     * 检查零件能否放入机器
     */
    private boolean canFitInMachine(int jobNo, int machineIdx) {
        if (machineIdx < 0 || machineIdx >= totalPrintMachineCount) {
            return false;
        }
        
        Item item = items[jobNo];
        PrintMachine pm = (PrintMachine) machines[machineIdx];
        
        boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
        boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
        
        return fitsNormal || fitsRotated;
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 计算两个个体的汉明距离（OS基因）
     */
    public int hammingDistanceOS(int[] os1, int[] os2) {
        int distance = 0;
        for (int i = 0; i < os1.length; i++) {
            if (os1[i] != os2[i]) {
                distance++;
            }
        }
        return distance;
    }
    
    /**
     * 计算两个个体的汉明距离（MS基因）
     */
    public int hammingDistanceMS(int[] ms1, int[] ms2) {
        int distance = 0;
        for (int i = 0; i < ms1.length; i++) {
            if (ms1[i] != ms2[i]) {
                distance++;
            }
        }
        return distance;
    }
}
