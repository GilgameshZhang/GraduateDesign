package AlgorithmFrame.machineChoice.ga;

import ProblemFrame.Input;
import java.util.*;

/**
 * 简化变异算子的BatchGa（用于消融实验）
 * 工件层：随机两点交换
 * 机器层：随机选择一个点位，重新分配给满足要求的任意机器
 */
public class BatchGaSimpleMutation extends BatchGa {
    
    public BatchGaSimpleMutation(int MAX_GEN, int popSize, double variationExchangeCount, 
                                 int cloneNumOfBestIndividual, double mutationRate, 
                                 double crossoverRate, Input input, boolean isRotateEnable, 
                                 String method, int decodeMaxGen, int decodeTabuSize, 
                                 int decodeMaxN, long timeLimitMs) {
        super(MAX_GEN, popSize, variationExchangeCount, cloneNumOfBestIndividual, 
              mutationRate, crossoverRate, input, isRotateEnable, method, 
              decodeMaxGen, decodeTabuSize, decodeMaxN, timeLimitMs);
    }
    
    /**
     * 简化的变异操作（覆盖父类方法）
     * 工件层：随机两点交换
     * 机器层：随机选择一个点位，重新分配给满足要求的任意机器
     * 
     * @param k 变异的基因idx
     */
    @Override
    protected void varation(int k) {
        // 小根堆用于保存多次变异后的最优个体
        PriorityQueue<BatchGenome> heap = new PriorityQueue<>(new Comparator<BatchGenome>() {
            @Override
            public int compare(BatchGenome o1, BatchGenome o2) {
                return util.compareUtil.compareDouble(o1.fitness, o2.fitness);
            }
        });
        
        BatchGenome genome = newPopulation.get(k);
        
        // 空值检查
        if (genome == null || genome.genomeItemArray == null || genome.genomeMachineArray == null) {
            return;
        }
        
        int[] machineArray = genome.genomeMachineArray;
        int[] itemArray = genome.genomeItemArray;
        
        // 执行多次简化变异，选择最优结果
        for (int i = 0; i < 2 * variationExchangeCount; i++) {
            BatchGenome genomeI = copyGenome(genome);
            int[] machineArrayI = genomeI.genomeMachineArray.clone();
            int[] itemArrayI = genomeI.genomeItemArray.clone();
            
            double r = random.nextDouble();
            
            if (r < 0.5) {
                // 策略1：工件层随机两点交换
                simpleTwoPointSwap(machineArrayI, itemArrayI);
            } else {
                // 策略2：机器层随机重新分配
                simpleRandomReassignment(machineArrayI, itemArrayI);
            }
            
            // 更新变异后的基因
            genomeI.genomeMachineArray = machineArrayI;
            genomeI.genomeItemArray = itemArrayI;
            genomeI.solutions.clear();
            
            // 更新适应度
            genomeI.updateFitnessAndSolution();
            heap.add(genomeI);
        }
        
        // 选择最优的变异结果
        BatchGenome bestVariationGenome = heap.poll();
        newPopulation.set(k, bestVariationGenome);
    }
    
    /**
     * 工件层简单两点交换
     * 随机选取两个点位，将其工件层和机器层的点位同时交换
     * 
     * @param machineArray 机器分配数组
     * @param itemArray 工件序列数组
     */
    private void simpleTwoPointSwap(int[] machineArray, int[] itemArray) {
        if (itemArray.length < 2) {
            return; // 至少需要2个元素才能交换
        }
        
        // 随机选择两个不同的位置
        int pos1 = random.nextInt(itemArray.length);
        int pos2 = random.nextInt(itemArray.length);
        
        // 确保两个位置不同
        int attempts = 0;
        while (pos1 == pos2 && attempts < 100) {
            pos2 = random.nextInt(itemArray.length);
            attempts++;
        }
        
        if (pos1 == pos2) {
            return; // 无法找到两个不同的位置
        }
        
        // 交换工件层
        int tempItem = itemArray[pos1];
        itemArray[pos1] = itemArray[pos2];
        itemArray[pos2] = tempItem;
        
        // 同时交换机器层
        int tempMachine = machineArray[pos1];
        machineArray[pos1] = machineArray[pos2];
        machineArray[pos2] = tempMachine;
    }
    
    /**
     * 机器层简单随机重新分配
     * 随机选择一个点位，将其重新分配给任意一个满足加工要求的机器
     * 
     * @param machineArray 机器分配数组
     * @param itemArray 工件序列数组
     */
    private void simpleRandomReassignment(int[] machineArray, int[] itemArray) {
        if (machineArray.length == 0 || machines.length <= 1) {
            return; // 没有可选的机器
        }
        
        // 随机选择一个工件位置
        int pos = random.nextInt(machineArray.length);
        int itemId = itemArray[pos];
        int currentMachine = machineArray[pos];
        
        // 找到所有满足加工要求的机器
        List<Integer> validMachines = new ArrayList<>();
        for (int m = 0; m < machines.length; m++) {
            // 检查机器是否满足尺寸要求
            if (items[itemId].l <= machines[m].L && 
                items[itemId].w <= machines[m].W && 
                items[itemId].h <= machines[m].H) {
                validMachines.add(m);
            }
        }
        
        // 如果没有找到满足要求的机器，保持原分配
        if (validMachines.isEmpty()) {
            return;
        }
        
        // 从满足要求的机器中随机选择一个（可能与当前机器相同）
        int newMachine = validMachines.get(random.nextInt(validMachines.size()));
        machineArray[pos] = newMachine;
    }
    
    /**
     * 获取算法名称标识
     */
    public String getAlgorithmName() {
        return "GA-SimpleMutation";
    }
}
