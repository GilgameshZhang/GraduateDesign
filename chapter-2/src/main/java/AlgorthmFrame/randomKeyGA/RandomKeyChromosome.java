package AlgorthmFrame.randomKeyGA;

import ProgramEntity.Solution;
import ProgramEntity.Operation;

import java.util.*;

/**
 * 随机密钥编码染色体
 * 包含4个子串：
 * 1. 零件序列（随机密钥，[0,1]区间随机数）
 * 2. 零件朝向（1-3，代表3种朝向）
 * 3. 旋转角度（1-8，代表8种旋转角度，45°增量）
 * 4. 设备分配（1-m，m为打印机数量）
 */
public class RandomKeyChromosome implements Comparable<RandomKeyChromosome> {
    
    // 染色体子串
    public double[] partSequenceKeys;    // 零件序列随机密钥 [0,1]
    public int[] partOrientations;        // 零件朝向 1-3
    public int[] partRotations;           // 旋转角度 1-8
    public int[] machineAssignments;      // 设备分配 1-m
    
    // 适应度值（越大越好，fitness = FITNESS_SCALE / makespan）
    public double fitness;
    
    // 实际makespan值（越小越好）
    public double makespan;
    
    // 打印批次信息（用于可视化）
    public List<Solution>[] printSolution;  // 每台打印机的批次列表
    
    // 工序调度信息（用于甘特图）
    public Operation[][] operationMatrix;  // 每个零件的所有工序信息
    
    // 零件数量
    private int partCount;
    
    // 打印机数量
    private int machineCount;
    
    // 随机数生成器
    private Random random;
    
    /**
     * 构造函数：随机初始化染色体
     * @param partCount 零件数量
     * @param machineCount 打印机数量
     * @param random 随机数生成器
     */
    public RandomKeyChromosome(int partCount, int machineCount, Random random) {
        this.partCount = partCount;
        this.machineCount = machineCount;
        this.random = random;
        
        // 初始化4个子串
        this.partSequenceKeys = new double[partCount];
        this.partOrientations = new int[partCount];
        this.partRotations = new int[partCount];
        this.machineAssignments = new int[partCount];
        
        // 随机初始化
        randomInitialize();
    }
    
    /**
     * 拷贝构造函数
     */
    public RandomKeyChromosome(RandomKeyChromosome other) {
        this.partCount = other.partCount;
        this.machineCount = other.machineCount;
        this.random = other.random;
        this.fitness = other.fitness;
        this.makespan = other.makespan;
        
        this.partSequenceKeys = Arrays.copyOf(other.partSequenceKeys, other.partSequenceKeys.length);
        this.partOrientations = Arrays.copyOf(other.partOrientations, other.partOrientations.length);
        this.partRotations = Arrays.copyOf(other.partRotations, other.partRotations.length);
        this.machineAssignments = Arrays.copyOf(other.machineAssignments, other.machineAssignments.length);
        
        // 拷贝打印批次信息
        if (other.printSolution != null) {
            this.printSolution = new List[other.printSolution.length];
            for (int i = 0; i < other.printSolution.length; i++) {
                if (other.printSolution[i] != null) {
                    this.printSolution[i] = new ArrayList<>(other.printSolution[i]);
                }
            }
        }
        
        // 拷贝工序矩阵
        if (other.operationMatrix != null) {
            this.operationMatrix = new Operation[other.operationMatrix.length][];
            for (int i = 0; i < other.operationMatrix.length; i++) {
                if (other.operationMatrix[i] != null) {
                    this.operationMatrix[i] = new Operation[other.operationMatrix[i].length];
                    for (int j = 0; j < other.operationMatrix[i].length; j++) {
                        if (other.operationMatrix[i][j] != null) {
                            this.operationMatrix[i][j] = new Operation(other.operationMatrix[i][j]);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 随机初始化染色体的所有子串
     */
    private void randomInitialize() {
        // 1. 零件序列：生成[0,1]区间的随机密钥
        for (int i = 0; i < partCount; i++) {
            partSequenceKeys[i] = random.nextDouble();
        }
        
        // 2. 零件朝向：随机选择1-3
        for (int i = 0; i < partCount; i++) {
            partOrientations[i] = random.nextInt(3) + 1;  // 1, 2, 3
        }
        
        // 3. 旋转角度：随机选择1-8（对应0°, 45°, 90°, ..., 315°）
        for (int i = 0; i < partCount; i++) {
            partRotations[i] = random.nextInt(8) + 1;  // 1-8
        }
        
        // 4. 设备分配：随机分配到1-m号机器
        for (int i = 0; i < partCount; i++) {
            machineAssignments[i] = random.nextInt(machineCount) + 1;  // 1-m
        }
    }
    
    /**
     * 解码零件序列：将随机密钥转换为零件加工顺序
     * @return 零件索引数组（按嵌套优先级排序，索引从0开始）
     */
    public int[] decodePartSequence() {
        // 创建索引数组
        Integer[] indices = new Integer[partCount];
        for (int i = 0; i < partCount; i++) {
            indices[i] = i;
        }
        
        // 按随机密钥排序：随机密钥越小，嵌套优先级越高（越先嵌套）
        Arrays.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                return Double.compare(partSequenceKeys[i1], partSequenceKeys[i2]);
            }
        });
        
        // 转换为int数组
        int[] sequence = new int[partCount];
        for (int i = 0; i < partCount; i++) {
            sequence[i] = indices[i];
        }
        
        return sequence;
    }
    
    /**
     * 获取零件i的朝向（1-3）
     */
    public int getOrientation(int partIndex) {
        return partOrientations[partIndex];
    }
    
    /**
     * 获取零件i的旋转角度（度数）
     */
    public double getRotationAngle(int partIndex) {
        return (partRotations[partIndex] - 1) * 45.0;  // 1->0°, 2->45°, ..., 8->315°
    }
    
    /**
     * 获取零件i分配的机器编号（1-m）
     */
    public int getMachineAssignment(int partIndex) {
        return machineAssignments[partIndex];
    }
    
    /**
     * 设置适应度值
     */
    public void setFitness(double fitness) {
        this.fitness = fitness;
    }
    
    /**
     * 设置makespan值
     */
    public void setMakespan(double makespan) {
        this.makespan = makespan;
    }
    
    /**
     * 获取零件数量
     */
    public int getPartCount() {
        return partCount;
    }
    
    /**
     * 获取机器数量
     */
    public int getMachineCount() {
        return machineCount;
    }
    
    /**
     * 比较器：按适应度降序排序（适应度越大越好）
     */
    @Override
    public int compareTo(RandomKeyChromosome other) {
        return Double.compare(other.fitness, this.fitness);
    }
    
    /**
     * 打印染色体信息（用于调试）
     */
    public void printChromosome() {
        System.out.println("=== 随机密钥染色体 ===");
        System.out.println("Fitness: " + fitness + ", Makespan: " + makespan);
        
        System.out.print("零件序列随机密钥: [");
        for (int i = 0; i < Math.min(10, partCount); i++) {
            System.out.printf("%.3f", partSequenceKeys[i]);
            if (i < Math.min(10, partCount) - 1) System.out.print(", ");
        }
        if (partCount > 10) System.out.print(", ...");
        System.out.println("]");
        
        // 解码后的序列
        int[] sequence = decodePartSequence();
        System.out.print("解码后零件序列: [");
        for (int i = 0; i < Math.min(10, partCount); i++) {
            System.out.print(sequence[i]);
            if (i < Math.min(10, partCount) - 1) System.out.print(", ");
        }
        if (partCount > 10) System.out.print(", ...");
        System.out.println("]");
        
        System.out.print("零件朝向: [");
        for (int i = 0; i < Math.min(10, partCount); i++) {
            System.out.print(partOrientations[i]);
            if (i < Math.min(10, partCount) - 1) System.out.print(", ");
        }
        if (partCount > 10) System.out.print(", ...");
        System.out.println("]");
        
        System.out.print("旋转角度: [");
        for (int i = 0; i < Math.min(10, partCount); i++) {
            System.out.print(partRotations[i]);
            if (i < Math.min(10, partCount) - 1) System.out.print(", ");
        }
        if (partCount > 10) System.out.print(", ...");
        System.out.println("]");
        
        System.out.print("设备分配: [");
        for (int i = 0; i < Math.min(10, partCount); i++) {
            System.out.print(machineAssignments[i]);
            if (i < Math.min(10, partCount) - 1) System.out.print(", ");
        }
        if (partCount > 10) System.out.print(", ...");
        System.out.println("]");
    }
    
    /**
     * 验证染色体合法性
     */
    public boolean isValid() {
        // 检查随机密钥是否在[0,1]区间
        for (double key : partSequenceKeys) {
            if (key < 0.0 || key > 1.0) {
                return false;
            }
        }
        
        // 检查朝向是否在1-3范围
        for (int orientation : partOrientations) {
            if (orientation < 1 || orientation > 3) {
                return false;
            }
        }
        
        // 检查旋转是否在1-8范围
        for (int rotation : partRotations) {
            if (rotation < 1 || rotation > 8) {
                return false;
            }
        }
        
        // 检查机器分配是否在1-m范围
        for (int machine : machineAssignments) {
            if (machine < 1 || machine > machineCount) {
                return false;
            }
        }
        
        return true;
    }
}

