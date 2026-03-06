package test;

import AlgorithmFrame.nsgaii.*;
import ProblemFrame.MOIndividual;
import org.junit.Test;
import java.util.*;

import static org.junit.Assert.*;

/**
 * NSGA-II 单元测试
 * 
 * 测试内容：
 * 1. 支配关系判断
 * 2. 非支配排序
 * 3. 拥挤距离计算
 * 4. 环境选择
 * 5. tie-breaker机制
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class NSGAIITest {
    
    private static final double EPSILON = 1e-9;
    
    /**
     * 测试1: 支配关系判断
     */
    @Test
    public void testDominance() {
        System.out.println("========================================");
        System.out.println("测试1: 支配关系判断");
        System.out.println("========================================");
        
        Random r = new Random(123);
        
        // 创建测试个体（2目标，最小化）
        MOIndividual ind1 = new MOIndividual(r);
        ind1.objectives = new double[]{1.0, 2.0};
        
        MOIndividual ind2 = new MOIndividual(r);
        ind2.objectives = new double[]{2.0, 3.0};
        
        MOIndividual ind3 = new MOIndividual(r);
        ind3.objectives = new double[]{1.5, 1.5};
        
        MOIndividual ind4 = new MOIndividual(r);
        ind4.objectives = new double[]{1.0, 2.0}; // 与ind1相同
        
        // ind1 应该支配 ind2 (1.0<2.0, 2.0<3.0)
        assertTrue("ind1应该支配ind2", ind1.dominates(ind2));
        assertFalse("ind2不应该支配ind1", ind2.dominates(ind1));
        
        // ind1 和 ind3 互不支配
        assertFalse("ind1不应该支配ind3", ind1.dominates(ind3));
        assertFalse("ind3不应该支配ind1", ind3.dominates(ind1));
        
        // ind1 和 ind4 相同，互不支配
        assertFalse("相同个体不应该互相支配", ind1.dominates(ind4));
        assertFalse("相同个体不应该互相支配", ind4.dominates(ind1));
        
        System.out.println("✓ 支配关系判断测试通过");
        System.out.println();
    }
    
    /**
     * 测试2: 快速非支配排序
     */
    @Test
    public void testFastNonDominatedSort() {
        System.out.println("========================================");
        System.out.println("测试2: 快速非支配排序");
        System.out.println("========================================");
        
        Random r = new Random(123);
        List<MOIndividual> population = new ArrayList<>();
        
        // 创建测试种群（2目标）
        // F1: (1,4), (2,3), (3,2), (4,1)
        population.add(createIndividual(r, 1.0, 4.0));
        population.add(createIndividual(r, 2.0, 3.0));
        population.add(createIndividual(r, 3.0, 2.0));
        population.add(createIndividual(r, 4.0, 1.0));
        
        // F2: (2,4), (3,3), (4,2)
        population.add(createIndividual(r, 2.0, 4.0));
        population.add(createIndividual(r, 3.0, 3.0));
        population.add(createIndividual(r, 4.0, 2.0));
        
        // F3: (5,5)
        population.add(createIndividual(r, 5.0, 5.0));
        
        // 执行非支配排序
        List<List<MOIndividual>> fronts = NSGAIIOperations.fastNonDominatedSort(population);
        
        // 验证前沿数量
        assertEquals("应该有3个前沿", 3, fronts.size());
        
        // 验证第一前沿大小
        assertEquals("第一前沿应该有4个个体", 4, fronts.get(0).size());
        
        // 验证第二前沿大小
        assertEquals("第二前沿应该有3个个体", 3, fronts.get(1).size());
        
        // 验证第三前沿大小
        assertEquals("第三前沿应该有1个个体", 1, fronts.get(2).size());
        
        // 验证rank赋值
        for (MOIndividual ind : fronts.get(0)) {
            assertEquals("第一前沿rank应该为1", 1, ind.rank);
        }
        for (MOIndividual ind : fronts.get(1)) {
            assertEquals("第二前沿rank应该为2", 2, ind.rank);
        }
        for (MOIndividual ind : fronts.get(2)) {
            assertEquals("第三前沿rank应该为3", 3, ind.rank);
        }
        
        System.out.println("前沿1大小: " + fronts.get(0).size());
        System.out.println("前沿2大小: " + fronts.get(1).size());
        System.out.println("前沿3大小: " + fronts.get(2).size());
        System.out.println("✓ 快速非支配排序测试通过");
        System.out.println();
    }
    
    /**
     * 测试3: 拥挤距离计算
     */
    @Test
    public void testCrowdingDistance() {
        System.out.println("========================================");
        System.out.println("测试3: 拥挤距离计算");
        System.out.println("========================================");
        
        Random r = new Random(123);
        
        // 测试3.1: 前沿大小<=2的情况
        List<MOIndividual> smallFront = new ArrayList<>();
        smallFront.add(createIndividual(r, 1.0, 2.0));
        smallFront.add(createIndividual(r, 2.0, 1.0));
        
        NSGAIIOperations.assignCrowdingDistance(smallFront);
        
        for (MOIndividual ind : smallFront) {
            assertEquals("前沿大小<=2时，所有个体crowding应该为无穷大",
                Double.POSITIVE_INFINITY, ind.crowdingDistance, EPSILON);
        }
        System.out.println("✓ 测试3.1: 小前沿crowding=INF 通过");
        
        // 测试3.2: 正常前沿的拥挤距离
        List<MOIndividual> front = new ArrayList<>();
        front.add(createIndividual(r, 1.0, 5.0));  // 边界
        front.add(createIndividual(r, 2.0, 4.0));  // 中间
        front.add(createIndividual(r, 3.0, 3.0));  // 中间
        front.add(createIndividual(r, 4.0, 2.0));  // 中间
        front.add(createIndividual(r, 5.0, 1.0));  // 边界
        
        NSGAIIOperations.assignCrowdingDistance(front);
        
        // 验证边界个体
        assertTrue("第一个个体应该是边界（crowding=INF）",
            Double.isInfinite(front.get(0).crowdingDistance));
        assertTrue("最后一个个体应该是边界（crowding=INF）",
            Double.isInfinite(front.get(4).crowdingDistance));
        
        // 验证中间个体的crowding > 0
        for (int i = 1; i < 4; i++) {
            assertTrue("中间个体的crowding应该>0",
                front.get(i).crowdingDistance > 0);
            System.out.printf("  个体%d crowding=%.4f\n", 
                i, front.get(i).crowdingDistance);
        }
        
        System.out.println("✓ 测试3.2: 正常前沿crowding计算 通过");
        System.out.println();
    }
    
    /**
     * 测试4: 环境选择
     */
    @Test
    public void testEnvironmentalSelection() {
        System.out.println("========================================");
        System.out.println("测试4: 环境选择");
        System.out.println("========================================");
        
        Random r = new Random(123);
        
        // 创建父代（5个）
        List<MOIndividual> parent = new ArrayList<>();
        parent.add(createIndividual(r, 1.0, 5.0));
        parent.add(createIndividual(r, 2.0, 4.0));
        parent.add(createIndividual(r, 3.0, 3.0));
        parent.add(createIndividual(r, 4.0, 2.0));
        parent.add(createIndividual(r, 5.0, 1.0));
        
        // 创建子代（5个，质量较差）
        List<MOIndividual> offspring = new ArrayList<>();
        offspring.add(createIndividual(r, 2.5, 4.5));
        offspring.add(createIndividual(r, 3.5, 3.5));
        offspring.add(createIndividual(r, 4.5, 2.5));
        offspring.add(createIndividual(r, 6.0, 6.0));
        offspring.add(createIndividual(r, 7.0, 7.0));
        
        // 环境选择（目标种群大小=5）
        int targetSize = 5;
        double delta = 1e-6;
        List<MOIndividual> nextPop = NSGAIIOperations.environmentalSelection(
            parent, offspring, targetSize, delta
        );
        
        // 验证结果大小
        assertEquals("环境选择应该返回恰好N个个体", 
            targetSize, nextPop.size());
        
        // 验证所有个体都有rank
        for (MOIndividual ind : nextPop) {
            assertTrue("选中的个体应该有有效的rank",
                ind.rank > 0 && ind.rank < Integer.MAX_VALUE);
        }
        
        System.out.println("选择后的种群:");
        for (int i = 0; i < nextPop.size(); i++) {
            MOIndividual ind = nextPop.get(i);
            System.out.printf("  个体%d: rank=%d, crowding=%.4f, obj=%s\n",
                i + 1, ind.rank, ind.crowdingDistance, ind.objectivesToString());
        }
        
        System.out.println("✓ 环境选择测试通过");
        System.out.println();
    }
    
    /**
     * 测试5: tie-breaker机制
     */
    @Test
    public void testTieBreaker() {
        System.out.println("========================================");
        System.out.println("测试5: tie-breaker机制");
        System.out.println("========================================");
        
        Random r = new Random(123);
        
        // 创建两个拥挤距离非常接近的个体
        MOIndividual ind1 = createIndividual(r, 1.0, 2.0);
        ind1.rank = 1;
        ind1.crowdingDistance = 0.500000001;
        ind1.packingQ = 0.8;  // 更好的装箱质量
        ind1.batchCount = 5;
        
        MOIndividual ind2 = createIndividual(r, 1.0, 2.0);
        ind2.rank = 1;
        ind2.crowdingDistance = 0.500000002;
        ind2.packingQ = 0.7;  // 较差的装箱质量
        ind2.batchCount = 6;
        
        // 使用tie-breaker比较（delta=1e-6）
        double delta = 1e-6;
        int result = ind1.compareForTruncation(ind2, delta);
        
        // ind1和ind2的crowding距离差异 < delta，应该比较packingQ
        // ind1.packingQ(0.8) > ind2.packingQ(0.7)，所以ind1更优（返回<0）
        assertTrue("crowding接近时，packingQ更大的个体应该更优",
            result < 0);
        
        System.out.println("个体1: crowding=" + ind1.crowdingDistance + 
            ", packingQ=" + ind1.packingQ);
        System.out.println("个体2: crowding=" + ind2.crowdingDistance + 
            ", packingQ=" + ind2.packingQ);
        System.out.println("比较结果: " + (result < 0 ? "个体1更优" : "个体2更优"));
        System.out.println("✓ tie-breaker测试通过");
        System.out.println();
    }
    
    /**
     * 测试6: 锦标赛选择
     */
    @Test
    public void testTournamentSelection() {
        System.out.println("========================================");
        System.out.println("测试6: 锦标赛选择");
        System.out.println("========================================");
        
        Random r = new Random(123);
        List<MOIndividual> population = new ArrayList<>();
        
        // 创建有明显优劣的种群
        MOIndividual best = createIndividual(r, 1.0, 1.0);
        best.rank = 1;
        best.crowdingDistance = Double.POSITIVE_INFINITY;
        population.add(best);
        
        for (int i = 0; i < 9; i++) {
            MOIndividual ind = createIndividual(r, 5.0 + i, 5.0 + i);
            ind.rank = 2;
            ind.crowdingDistance = 0.5;
            population.add(ind);
        }
        
        // 执行多次锦标赛选择
        int bestSelectedCount = 0;
        int trials = 100;
        
        for (int i = 0; i < trials; i++) {
            MOIndividual selected = NSGAIIOperations.tournamentSelection(
                population, 3, r
            );
            if (selected == best) {
                bestSelectedCount++;
            }
        }
        
        // 最优个体应该有较高的选中概率
        double selectionRate = (double) bestSelectedCount / trials;
        System.out.println("最优个体选中率: " + 
            String.format("%.1f%%", selectionRate * 100));
        
        assertTrue("最优个体的选中率应该>50%", 
            selectionRate > 0.5);
        
        System.out.println("✓ 锦标赛选择测试通过");
        System.out.println();
    }
    
    /**
     * 综合测试: 完整流程
     */
    @Test
    public void testCompleteWorkflow() {
        System.out.println("========================================");
        System.out.println("综合测试: 完整NSGA-II流程");
        System.out.println("========================================");
        
        Random r = new Random(123);
        
        // 1. 创建初始种群
        List<MOIndividual> population = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double obj1 = 1.0 + r.nextDouble() * 10.0;
            double obj2 = 1.0 + r.nextDouble() * 10.0;
            population.add(createIndividual(r, obj1, obj2));
        }
        
        // 2. 非支配排序
        List<List<MOIndividual>> fronts = NSGAIIOperations.fastNonDominatedSort(population);
        System.out.println("非支配排序: " + fronts.size() + " 个前沿");
        
        // 3. 拥挤距离计算
        for (List<MOIndividual> front : fronts) {
            NSGAIIOperations.assignCrowdingDistance(front);
        }
        System.out.println("拥挤距离计算完成");
        
        // 4. 环境选择
        List<MOIndividual> offspring = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            double obj1 = 1.0 + r.nextDouble() * 10.0;
            double obj2 = 1.0 + r.nextDouble() * 10.0;
            offspring.add(createIndividual(r, obj1, obj2));
        }
        
        List<MOIndividual> nextPop = NSGAIIOperations.environmentalSelection(
            population, offspring, 20, 1e-6
        );
        
        assertEquals("环境选择应该返回正确数量的个体", 
            20, nextPop.size());
        
        // 5. 获取Pareto前沿
        List<MOIndividual> paretoFront = NSGAIIOperations.getParetoFront(nextPop);
        System.out.println("Pareto前沿大小: " + paretoFront.size());
        
        assertTrue("Pareto前沿不应该为空", 
            !paretoFront.isEmpty());
        
        System.out.println("✓ 综合测试通过");
        System.out.println();
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 创建测试用的个体
     */
    private MOIndividual createIndividual(Random r, double obj1, double obj2) {
        MOIndividual ind = new MOIndividual(r);
        ind.objectives = new double[]{obj1, obj2};
        ind.gene_OS = new int[]{0, 1, 2};
        ind.gene_MS = new int[]{1, 1, 1};
        return ind;
    }
}

