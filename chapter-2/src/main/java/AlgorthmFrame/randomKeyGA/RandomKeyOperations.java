package AlgorthmFrame.randomKeyGA;

import java.util.Random;

/**
 * 随机密钥遗传算法的交叉和变异算子
 * 
 * 交叉操作：参数化均匀交叉（Parameterized Uniform Crossover）
 * - 对每个基因位，通过"掷硬币"决定继承父代1或父代2
 * - 确保朝向、旋转、设备分配与零件序列同步
 * 
 * 变异操作：
 * - 零件序列：随机替换某个零件的随机密钥（仍在[0,1]区间）
 * - 朝向：随机替换为1-3
 * - 旋转：随机替换为1-8
 * - 设备分配：随机替换为1-m
 */
public class RandomKeyOperations {
    
    private Random random;
    
    /**
     * 构造函数
     */
    public RandomKeyOperations(Random random) {
        this.random = random;
    }
    
    /**
     * 参数化均匀交叉（Parameterized Uniform Crossover, PUX）
     * 对于每个基因位，以0.5的概率从父代1或父代2继承
     * 
     * @param parent1 父代1
     * @param parent2 父代2
     * @return 两个子代（offspring[0]和offspring[1]）
     */
    public RandomKeyChromosome[] crossover(RandomKeyChromosome parent1, RandomKeyChromosome parent2) {
        int partCount = parent1.getPartCount();
        int machineCount = parent1.getMachineCount();
        
        // 创建两个子代
        RandomKeyChromosome offspring1 = new RandomKeyChromosome(partCount, machineCount, random);
        RandomKeyChromosome offspring2 = new RandomKeyChromosome(partCount, machineCount, random);
        
        // 对每个零件基因进行交叉
        for (int i = 0; i < partCount; i++) {
            // 掷硬币：决定是否交换
            boolean swap = random.nextBoolean();
            
            if (!swap) {
                // 子代1继承父代1，子代2继承父代2
                offspring1.partSequenceKeys[i] = parent1.partSequenceKeys[i];
                offspring1.partOrientations[i] = parent1.partOrientations[i];
                offspring1.partRotations[i] = parent1.partRotations[i];
                offspring1.machineAssignments[i] = parent1.machineAssignments[i];
                
                offspring2.partSequenceKeys[i] = parent2.partSequenceKeys[i];
                offspring2.partOrientations[i] = parent2.partOrientations[i];
                offspring2.partRotations[i] = parent2.partRotations[i];
                offspring2.machineAssignments[i] = parent2.machineAssignments[i];
            } else {
                // 子代1继承父代2，子代2继承父代1
                offspring1.partSequenceKeys[i] = parent2.partSequenceKeys[i];
                offspring1.partOrientations[i] = parent2.partOrientations[i];
                offspring1.partRotations[i] = parent2.partRotations[i];
                offspring1.machineAssignments[i] = parent2.machineAssignments[i];
                
                offspring2.partSequenceKeys[i] = parent1.partSequenceKeys[i];
                offspring2.partOrientations[i] = parent1.partOrientations[i];
                offspring2.partRotations[i] = parent1.partRotations[i];
                offspring2.machineAssignments[i] = parent1.machineAssignments[i];
            }
        }
        
        return new RandomKeyChromosome[]{offspring1, offspring2};
    }
    
    /**
     * 变异操作
     * 对染色体的每个子串独立进行变异
     * 
     * @param chromosome 待变异的染色体
     * @param mutationRate 变异率
     */
    public void mutate(RandomKeyChromosome chromosome, double mutationRate) {
        int partCount = chromosome.getPartCount();
        int machineCount = chromosome.getMachineCount();
        
        // 1. 零件序列子串变异：随机替换某个零件的随机密钥
        for (int i = 0; i < partCount; i++) {
            if (random.nextDouble() < mutationRate) {
                chromosome.partSequenceKeys[i] = random.nextDouble();
            }
        }
        
        // 2. 朝向子串变异：随机替换为1-3
        for (int i = 0; i < partCount; i++) {
            if (random.nextDouble() < mutationRate) {
                chromosome.partOrientations[i] = random.nextInt(3) + 1;
            }
        }
        
        // 3. 旋转子串变异：随机替换为1-8
        for (int i = 0; i < partCount; i++) {
            if (random.nextDouble() < mutationRate) {
                chromosome.partRotations[i] = random.nextInt(8) + 1;
            }
        }
        
        // 4. 设备分配子串变异：随机替换为1-m
        for (int i = 0; i < partCount; i++) {
            if (random.nextDouble() < mutationRate) {
                chromosome.machineAssignments[i] = random.nextInt(machineCount) + 1;
            }
        }
    }
    
    /**
     * 锦标赛选择（Tournament Selection）
     * 从种群中随机选择tournamentSize个个体，返回其中最优的
     * 
     * @param population 种群
     * @param tournamentSize 锦标赛规模
     * @return 选中的个体
     */
    public RandomKeyChromosome tournamentSelection(RandomKeyChromosome[] population, int tournamentSize) {
        RandomKeyChromosome best = population[random.nextInt(population.length)];
        
        for (int i = 1; i < tournamentSize; i++) {
            RandomKeyChromosome candidate = population[random.nextInt(population.length)];
            if (candidate.fitness > best.fitness) {
                best = candidate;
            }
        }
        
        return best;
    }
    
    /**
     * 轮盘赌选择（Roulette Wheel Selection）
     * 根据适应度比例选择个体
     * 
     * @param population 种群
     * @return 选中的个体
     */
    public RandomKeyChromosome rouletteWheelSelection(RandomKeyChromosome[] population) {
        // 计算总适应度
        double totalFitness = 0.0;
        for (RandomKeyChromosome chromosome : population) {
            totalFitness += chromosome.fitness;
        }
        
        // 如果总适应度为0或负数，随机选择
        if (totalFitness <= 0) {
            return population[random.nextInt(population.length)];
        }
        
        // 生成随机数
        double randomValue = random.nextDouble() * totalFitness;
        
        // 累积适应度，找到选中的个体
        double cumulativeFitness = 0.0;
        for (RandomKeyChromosome chromosome : population) {
            cumulativeFitness += chromosome.fitness;
            if (cumulativeFitness >= randomValue) {
                return chromosome;
            }
        }
        
        // 理论上不应该到这里，但为了安全返回最后一个
        return population[population.length - 1];
    }
    
    /**
     * 精英保留：从父代和子代中选择最优的个体进入下一代
     * 
     * @param population 当前种群
     * @param eliteSize 精英个体数量（前10%）
     * @return 精英个体数组
     */
    public RandomKeyChromosome[] selectElites(RandomKeyChromosome[] population, int eliteSize) {
        // 按适应度降序排序
        RandomKeyChromosome[] sorted = population.clone();
        java.util.Arrays.sort(sorted);  // 利用Comparable接口
        
        // 选择前eliteSize个个体
        RandomKeyChromosome[] elites = new RandomKeyChromosome[eliteSize];
        for (int i = 0; i < eliteSize; i++) {
            elites[i] = new RandomKeyChromosome(sorted[i]);
        }
        
        return elites;
    }
    
    /**
     * 修复染色体中可能的无效值
     * （虽然随机密钥编码本身应该避免这种问题，但作为保险）
     */
    public void repairChromosome(RandomKeyChromosome chromosome) {
        int partCount = chromosome.getPartCount();
        int machineCount = chromosome.getMachineCount();
        
        // 修复随机密钥：确保在[0,1]区间
        for (int i = 0; i < partCount; i++) {
            if (chromosome.partSequenceKeys[i] < 0.0 || chromosome.partSequenceKeys[i] > 1.0) {
                chromosome.partSequenceKeys[i] = random.nextDouble();
            }
        }
        
        // 修复朝向：确保在1-3范围
        for (int i = 0; i < partCount; i++) {
            if (chromosome.partOrientations[i] < 1 || chromosome.partOrientations[i] > 3) {
                chromosome.partOrientations[i] = random.nextInt(3) + 1;
            }
        }
        
        // 修复旋转：确保在1-8范围
        for (int i = 0; i < partCount; i++) {
            if (chromosome.partRotations[i] < 1 || chromosome.partRotations[i] > 8) {
                chromosome.partRotations[i] = random.nextInt(8) + 1;
            }
        }
        
        // 修复机器分配：确保在1-m范围
        for (int i = 0; i < partCount; i++) {
            if (chromosome.machineAssignments[i] < 1 || chromosome.machineAssignments[i] > machineCount) {
                chromosome.machineAssignments[i] = random.nextInt(machineCount) + 1;
            }
        }
    }
}

