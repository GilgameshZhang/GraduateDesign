package AlgorthmFrame.pso;

import ProblemFrame.CaculateFitness;
import ProblemFrame.Particle;
import ProblemFrame.PSOParameters;
import ProgramEntity.Operation;
import ProgramEntity.Problem;

/**
 * PSO邻域改进集成示例
 * 
 * 展示如何在PSO算法中集成OS和MA邻域改进方法
 * 
 * 策略说明：
 * 1. 每次迭代后对gbest进行轻量级邻域改进
 * 2. 每N代对所有粒子的pbest进行中等强度改进
 * 3. 每M代对部分粒子进行深度搜索
 */
public class PSO_WithLocalSearch extends PSO {
    
    // 邻域改进参数
    private boolean enableLocalSearch = true;      // 是否启用邻域改进
    private double localSearchProb = 0.5;          // 局部搜索概率
    private int osAttempts = 15;                   // OS改进尝试次数
    private int maAttempts = 8;                    // MA改进尝试次数
    private int pbImprovementInterval = 5;         // pbest改进间隔（代）
    private int deepSearchInterval = 20;           // 深度搜索间隔（代）
    
    /**
     * 构造函数
     */
    public PSO_WithLocalSearch(Problem input, PSOParameters params) {
        super(input, params);
        
        // 可以从参数中读取邻域改进配置
        if (params.enableLocalSearch != null) {
            this.enableLocalSearch = params.enableLocalSearch;
        }
    }
    
    /**
     * 重写主循环，添加邻域改进
     */
    @Override
    public Solution solve() {
        // ... 初始化代码（与原PSO相同）...
        
        // 获取需要的成员变量
        Particle[] swarm = getSwarm();
        Particle gBest = getGBest();
        Problem input = getInput();
        CaculateFitness caculateFitness = getCaculateFitness();
        Operation[][] operationMatrix = getOperationMatrix();
        
        long startTime = System.currentTimeMillis();
        int noImprove = 0;
        int currentIteration = 0;
        
        while (noImprove < getMaxStagnantStep() && 
               System.currentTimeMillis() - startTime <= getMaxRunTime() * 60 * 1000) {
            
            currentIteration++;
            
            // ============= PSO标准更新 =============
            for (Particle particle : swarm) {
                updateVelocity(particle);
                updatePosition(particle);
                evaluateParticle(particle);
                particle.updatePBest();
            }
            
            // 更新全局最优
            Particle currentBest = findBestParticle(swarm);
            if (currentBest.fitness > gBest.fitness) {
                gBest = new Particle(currentBest);
                noImprove = 0;
            } else {
                noImprove++;
            }
            
            // ============= 邻域改进策略 =============
            if (enableLocalSearch) {
                
                // 策略1：每次迭代对gbest进行轻量级改进
                improveGBest(gBest, input, caculateFitness, operationMatrix);
                
                // 策略2：每N代对pbest进行中等强度改进
                if (currentIteration % pbImprovementInterval == 0) {
                    improvePBests(swarm, input, caculateFitness, operationMatrix);
                }
                
                // 策略3：每M代对部分粒子进行深度搜索
                if (currentIteration % deepSearchInterval == 0) {
                    deepSearch(swarm, input, caculateFitness, operationMatrix);
                }
            }
            
            // ... 其他代码（输出、记录等）...
        }
        
        // ... 返回结果 ...
        return null; // 省略
    }
    
    /**
     * 策略1：对gbest进行轻量级邻域改进
     * 
     * 每次迭代都执行，使用较低的概率和较少的尝试次数
     */
    private void improveGBest(Particle gBest, Problem input, 
                             CaculateFitness caculateFitness, 
                             Operation[][] operationMatrix) {
        
        double lightProb = localSearchProb * 0.6;  // 降低概率
        int lightOSAttempts = (int) (osAttempts * 0.5);  // 减少尝试次数
        int lightMAAttempts = (int) (maAttempts * 0.5);
        
        // OS改进
        if (gBest.localSearchOS(input, caculateFitness, operationMatrix, 
                                lightProb, lightOSAttempts)) {
            evaluateParticle(gBest);
            System.out.println("  [改进] gbest OS改进成功");
        }
        
        // MA改进
        if (gBest.localSearchMA(input, caculateFitness, operationMatrix, 
                                lightProb, lightMAAttempts)) {
            evaluateParticle(gBest);
            System.out.println("  [改进] gbest MA改进成功");
        }
    }
    
    /**
     * 策略2：对所有粒子的pbest进行中等强度改进
     * 
     * 每N代执行一次，使用标准的概率和尝试次数
     */
    private void improvePBests(Particle[] swarm, Problem input, 
                              CaculateFitness caculateFitness, 
                              Operation[][] operationMatrix) {
        
        int improvedCount = 0;
        
        for (Particle p : swarm) {
            // 对每个粒子的pbest进行改进
            if (p.improvePBest(input, caculateFitness, operationMatrix, 
                              localSearchProb, osAttempts, maAttempts)) {
                improvedCount++;
            }
        }
        
        System.out.printf("  [改进] 共%d个pbest得到改进（共%d个粒子）\n", 
                         improvedCount, swarm.length);
    }
    
    /**
     * 策略3：对部分粒子进行深度搜索
     * 
     * 每M代执行一次，使用较高的概率和较多的尝试次数
     * 仅对适应度较高的前20%粒子进行深度搜索
     */
    private void deepSearch(Particle[] swarm, Problem input, 
                           CaculateFitness caculateFitness, 
                           Operation[][] operationMatrix) {
        
        // 1. 对粒子按适应度排序
        Particle[] sortedSwarm = swarm.clone();
        java.util.Arrays.sort(sortedSwarm, (p1, p2) -> Double.compare(p2.fitness, p1.fitness));
        
        // 2. 对前20%的粒子进行深度搜索
        int deepSearchCount = Math.max(1, (int) (swarm.length * 0.2));
        double deepProb = localSearchProb * 1.5;  // 提高概率
        int deepOSAttempts = osAttempts * 2;      // 增加尝试次数
        int deepMAAttempts = maAttempts * 2;
        
        int improvedCount = 0;
        
        for (int i = 0; i < deepSearchCount; i++) {
            Particle p = sortedSwarm[i];
            boolean improved = false;
            
            // OS深度搜索
            if (p.localSearchOS(input, caculateFitness, operationMatrix, 
                                deepProb, deepOSAttempts)) {
                evaluateParticle(p);
                improved = true;
            }
            
            // MA深度搜索
            if (p.localSearchMA(input, caculateFitness, operationMatrix, 
                                deepProb, deepMAAttempts)) {
                evaluateParticle(p);
                improved = true;
            }
            
            if (improved) {
                improvedCount++;
            }
        }
        
        System.out.printf("  [深度搜索] 对前%d个粒子进行深度搜索，%d个得到改进\n", 
                         deepSearchCount, improvedCount);
    }
    
    // ============= Getter方法（需要访问父类私有成员） =============
    // 注意：这些方法需要在父类PSO中添加，或将相应成员改为protected
    
    private Particle[] getSwarm() {
        // 返回粒子群
        return null; // 需要在父类中实现
    }
    
    private Particle getGBest() {
        // 返回全局最优
        return null; // 需要在父类中实现
    }
    
    private Problem getInput() {
        // 返回问题实例
        return null; // 需要在父类中实现
    }
    
    private CaculateFitness getCaculateFitness() {
        // 返回适应度计算器
        return null; // 需要在父类中实现
    }
    
    private Operation[][] getOperationMatrix() {
        // 返回工序矩阵
        return null; // 需要在父类中实现
    }
    
    private int getMaxStagnantStep() {
        return 30000; // 需要在父类中实现
    }
    
    private double getMaxRunTime() {
        return 3.0; // 需要在父类中实现
    }
}

/**
 * 使用示例
 */
class LocalSearchExample {
    
    public static void main(String[] args) {
        // 1. 创建问题实例
        Problem problem = loadProblem("data/instance.txt");
        
        // 2. 配置PSO参数
        PSOParameters params = new PSOParameters();
        params.swarmSize = 50;
        params.w = 0.7;
        params.c1 = 1.5;
        params.c2 = 1.5;
        params.maxStagnantStep = 1000;
        params.maxRunTime = 5.0;  // 5分钟
        
        // 邻域改进参数
        params.enableLocalSearch = true;
        params.localSearchProb = 0.5;
        params.osAttempts = 15;
        params.maAttempts = 8;
        
        // 3. 创建并运行PSO
        PSO_WithLocalSearch pso = new PSO_WithLocalSearch(problem, params);
        Solution solution = pso.solve();
        
        // 4. 输出结果
        System.out.println("最优makespan: " + solution.cost);
    }
    
    private static Problem loadProblem(String filename) {
        // 加载问题实例
        return null; // 省略实现
    }
}

/**
 * 简化版：直接在原PSO的主循环中添加邻域改进
 * 
 * 如果不想创建新的子类，可以直接在原PSO的solve()方法中添加以下代码：
 */
class InlineIntegrationExample {
    
    public void modifyPSOMainLoop() {
        /*
        // 在PSO主循环中的位置：更新全局最优之后
        
        // 更新全局最优
        Particle currentBest = findBestParticle(swarm);
        if (currentBest.fitness > gBest.fitness) {
            gBest = new Particle(currentBest);
            noImprove = 0;
        } else {
            noImprove++;
        }
        
        // ========== 添加邻域改进代码 ==========
        
        // 1. 每次迭代改进gbest（轻量级）
        if (gBest.localSearchOS(input, caculateFitness, operationMatrix, 0.3, 10)) {
            evaluateParticle(gBest);
        }
        if (gBest.localSearchMA(input, caculateFitness, operationMatrix, 0.3, 5)) {
            evaluateParticle(gBest);
        }
        
        // 2. 每5代改进pbest（中等强度）
        if (currentIteration % 5 == 0) {
            for (Particle p : swarm) {
                p.improvePBest(input, caculateFitness, operationMatrix, 0.5, 15, 8);
            }
        }
        
        // 3. 每20代深度搜索（高强度）
        if (currentIteration % 20 == 0) {
            // 对前20%粒子进行深度搜索
            Particle[] sorted = swarm.clone();
            Arrays.sort(sorted, (p1, p2) -> Double.compare(p2.fitness, p1.fitness));
            int count = Math.max(1, (int)(swarm.length * 0.2));
            
            for (int i = 0; i < count; i++) {
                sorted[i].localSearchOS(input, caculateFitness, operationMatrix, 0.8, 30);
                sorted[i].localSearchMA(input, caculateFitness, operationMatrix, 0.8, 15);
                evaluateParticle(sorted[i]);
            }
        }
        
        // ========================================
        
        // 继续原有的代码...
        */
    }
}

/**
 * 参数配置建议
 * 
 * 根据问题规模选择合适的参数：
 */
class ParameterRecommendations {
    
    // 小规模问题（<20个工件）
    static class SmallScale {
        static final double LOCAL_SEARCH_PROB = 0.6;
        static final int OS_ATTEMPTS = 10;
        static final int MA_ATTEMPTS = 5;
        static final int PB_IMPROVEMENT_INTERVAL = 3;
        static final int DEEP_SEARCH_INTERVAL = 10;
    }
    
    // 中规模问题（20-50个工件）
    static class MediumScale {
        static final double LOCAL_SEARCH_PROB = 0.5;
        static final int OS_ATTEMPTS = 15;
        static final int MA_ATTEMPTS = 8;
        static final int PB_IMPROVEMENT_INTERVAL = 5;
        static final int DEEP_SEARCH_INTERVAL = 20;
    }
    
    // 大规模问题（>50个工件）
    static class LargeScale {
        static final double LOCAL_SEARCH_PROB = 0.4;
        static final int OS_ATTEMPTS = 20;
        static final int MA_ATTEMPTS = 10;
        static final int PB_IMPROVEMENT_INTERVAL = 10;
        static final int DEEP_SEARCH_INTERVAL = 30;
    }
}

