# SPEA2算法快速上手指南

## 一、验证安装

首先验证SPEA2是否正确安装。运行测试类：

```
位置: src/test/java/SimpleSPEA2Test.java
```

**预期输出：**
```
========================================
      SPEA2 简单测试
========================================

✅ 成功加载问题实例
   工件数: 20
   打印机数: 3
   批处理机数: 2

优化目标:
   1. 最小化最大完工时间 (Cmax)
   2. 最小化总能耗 (Energy)

>>> 运行 SPEA2 <<<
...
>>> 运行 NSGA-II (对比) <<<
...

========================================
           测试结果
========================================

✅ SPEA2 测试通过
✅ NSGA-II 测试通过
✅ 所有测试通过！SPEA2算法运行正常
========================================
```

## 二、基本使用示例

### 示例1：单次运行SPEA2

```java
import AlgorithmFrame.spea2.*;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

public class MyExperiment {
    public static void main(String[] args) throws Exception {
        // 1. 加载问题
        File instanceFile = new File("src/main/resources/instance/J20/J20P3B2D5_01.txt");
        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        // 2. 定义优化目标
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        );
        
        // 3. 创建并配置SPEA2
        SPEA2 spea2 = new SPEA2(problem, objectives);
        spea2.setPopulationSize(100);      // 种群大小
        spea2.setArchiveSize(100);         // 存档大小
        spea2.setMaxGenerations(200);      // 最大代数
        spea2.setCrossoverRate(0.9);       // 交叉率
        spea2.setMutationRate(0.1);        // 变异率
        spea2.setSeed(12345L);             // 随机种子（可重现）
        
        // 4. 运行算法
        List<MOIndividual> paretoFront = spea2.solve();
        
        // 5. 输出结果
        System.out.println("获得 " + paretoFront.size() + " 个Pareto最优解");
        for (int i = 0; i < Math.min(5, paretoFront.size()); i++) {
            MOIndividual ind = paretoFront.get(i);
            System.out.printf("解%d: Cmax=%.2f, Energy=%.2f\n",
                i + 1, ind.objectives[0], ind.objectives[1]);
        }
    }
}
```

### 示例2：使用Runner快速运行

```java
import AlgorithmFrame.spea2.SPEA2Runner;

public class QuickExperiment {
    public static void main(String[] args) {
        SPEA2Runner.quickRun(
            "src/main/resources/instance/J20/J20P3B2D5_01.txt",  // 问题文件
            "output/spea2_results",                              // 输出目录
            100,                                                 // 种群大小
            200,                                                 // 最大代数
            12345L                                               // 随机种子
        );
        
        // 结果会自动保存到 output/spea2_results/SPEA2_pareto_front.csv
    }
}
```

### 示例3：SPEA2 vs NSGAII 对比

```java
import AlgorithmFrame.spea2.SPEA2ComparisonExperiment;

public class ComparisonExperiment {
    public static void main(String[] args) {
        // 单次对比
        SPEA2ComparisonExperiment.runComparison(
            "src/main/resources/instance/J20/J20P3B2D5_01.txt",
            100,     // 种群大小
            200,     // 最大代数
            12345L   // 随机种子
        );
        
        // 输出会包含：
        // - Pareto前沿大小对比
        // - 运行时间对比
        // - 超体积指标对比
        // - C指标（覆盖度）对比
    }
}
```

### 示例4：批量对比实验

```java
import AlgorithmFrame.spea2.SPEA2ComparisonExperiment;

public class BatchComparison {
    public static void main(String[] args) {
        // 运行5次，计算平均值和标准差
        SPEA2ComparisonExperiment.runMultipleComparisons(
            "src/main/resources/instance/J20/J20P3B2D5_01.txt",
            100,     // 种群大小
            200,     // 最大代数
            5        // 运行次数
        );
        
        // 输出会包含：
        // - 超体积的平均值和标准差
        // - Pareto前沿大小的平均值和标准差
        // - 运行时间的平均值和标准差
    }
}
```

## 三、高级功能

### 功能1：启用局部搜索

```java
SPEA2 spea2 = new SPEA2(problem, objectives);
spea2.setPopulationSize(100);
spea2.setMaxGenerations(200);

// 启用局部搜索（每10代执行一次）
spea2.enableLocalSearch(true, 10);

// 配置局部搜索参数
spea2.setLocalSearchParameters(
    10,     // L: 每个精英尝试次数
    0.10,   // eta: 选择10%精英
    0.05,   // epsE: T型允许能耗上升5%
    0.02,   // epsC: E型允许Cmax上升2%
    0.005,  // improvC: T型要求Cmax下降0.5%
    0.01    // improvE: E型要求能耗下降1%
);

List<MOIndividual> paretoFront = spea2.solve();
```

### 功能2：自定义配置

```java
import AlgorithmFrame.spea2.SPEA2Runner;

SPEA2Runner.SPEA2Config config = new SPEA2Runner.SPEA2Config();

// 基础参数
config.populationSize = 100;
config.archiveSize = 100;
config.maxGenerations = 200;
config.crossoverRate = 0.9;
config.mutationRate = 0.1;
config.maxRunTimeMinutes = 10.0;
config.seed = 12345L;
config.kNearest = 14;  // √(100+100) ≈ 14

// 优化目标
config.optimizeCmax = true;
config.optimizeEnergy = true;
config.optimizeTardiness = false;

// 局部搜索
config.enableLocalSearch = true;
config.localSearchInterval = 10;

// 运行
SPEA2Runner.fullRun(
    "src/main/resources/instance/J20/J20P3B2D5_01.txt",
    "output/spea2_full",
    config
);
```

### 功能3：多目标优化（3个目标）

```java
// 定义三个优化目标
List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
    new MOEvaluator.MaximumCompletionTime(),     // 目标1：最小化Cmax
    new MOEvaluator.TotalEnergyConsumption(problem),  // 目标2：最小化能耗
    new MOEvaluator.TotalTardiness()             // 目标3：最小化拖期
);

SPEA2 spea2 = new SPEA2(problem, objectives);
// ... 其他配置
List<MOIndividual> paretoFront = spea2.solve();
```

## 四、参数调优指南

### 4.1 种群大小 (populationSize)

| 问题规模 | 推荐值 | 说明 |
|---------|--------|------|
| J10-J20 | 50-100 | 小规模问题，种群不宜过大 |
| J20-J50 | 100-150 | 中等规模，平衡质量和时间 |
| J50-J100 | 150-200 | 大规模问题，需要更多探索 |

### 4.2 存档大小 (archiveSize)

**推荐设置：** archiveSize = populationSize

**特殊情况：**
- 需要更多Pareto解：archiveSize = 1.5 × populationSize
- 计算资源有限：archiveSize = 0.5 × populationSize

### 4.3 k近邻参数 (kNearest)

**推荐公式：** k = √(populationSize + archiveSize)

**示例：**
- populationSize=100, archiveSize=100 → k=14
- populationSize=50, archiveSize=50 → k=10
- populationSize=200, archiveSize=200 → k=20

### 4.4 最大代数 (maxGenerations)

| 问题规模 | 推荐值 | 说明 |
|---------|--------|------|
| J10-J20 | 100-200 | 小规模快速收敛 |
| J20-J50 | 200-300 | 中等规模适中 |
| J50-J100 | 300-500 | 大规模需更多代数 |

### 4.5 交叉率和变异率

**标准设置：**
- crossoverRate = 0.9
- mutationRate = 0.1

**调优建议：**
- 提高探索能力：增加mutationRate到0.15-0.2
- 提高收敛速度：降低mutationRate到0.05-0.08

## 五、常见问题

### Q1: SPEA2和NSGAII哪个更好？

**答：** 各有优劣，取决于问题：

**SPEA2优势：**
- 密度估计更精确（k近邻）
- Pareto前沿分布更均匀
- 对复杂问题可能性能更好

**NSGAII优势：**
- 计算速度更快
- 参数设置更简单
- 收敛速度通常更快

**建议：** 两个都运行，对比结果选择更好的。

### Q2: 如何判断算法是否收敛？

观察以下指标：
1. **存档大小变化**：趋于稳定
2. **超体积指标**：不再显著增长
3. **Pareto前沿分布**：解分布均匀

### Q3: 运行时间太长怎么办？

优化策略：
1. 减小种群大小（如从100降到50）
2. 减少最大代数（如从200降到100）
3. 关闭局部搜索（如果启用了）
4. 设置最大运行时间限制

```java
spea2.setMaxRunTimeMinutes(5.0);  // 最多运行5分钟
```

### Q4: 如何获取更多Pareto解？

方法：
1. 增加存档大小
2. 增加种群大小
3. 增加最大代数
4. 调整k近邻参数

```java
spea2.setPopulationSize(150);
spea2.setArchiveSize(150);
spea2.setMaxGenerations(300);
```

### Q5: CSV输出文件在哪里？

使用Runner时，结果保存在：
```
output/[outputDir]/SPEA2_pareto_front.csv
```

格式：
```csv
Solution,Cmax,Energy,PackingQ,BatchCount
1,1050.20,39800.00,0.8750,12
2,1065.30,38900.00,0.8650,13
...
```

## 六、论文实验建议

### 实验设置

```java
// 标准实验设置
int[] populationSizes = {50, 100, 150};
int[] maxGenerations = {100, 200, 300};
String[] instances = {
    "J20P3B2D5_01.txt",
    "J50P5B4D10_01.txt",
    "J100P4B2D10_01.txt"
};

// 对每个设置运行20-30次
for (String instance : instances) {
    for (int popSize : populationSizes) {
        for (int gen : maxGenerations) {
            // 运行20次
            for (int run = 0; run < 20; run++) {
                long seed = 12345L + run * 1000;
                // 运行SPEA2和NSGAII
                // 记录结果
            }
        }
    }
}
```

### 评价指标

1. **超体积 (Hypervolume)**：最重要
2. **C指标 (Coverage)**：判断支配关系
3. **IGD指标**：如果有真实Pareto前沿
4. **运行时间**：实用性考虑

### 统计检验

使用Wilcoxon秩和检验：
- 原假设：两个算法性能无差异
- p < 0.05：拒绝原假设，有显著差异

## 七、技巧和最佳实践

### 技巧1：固定随机种子确保可重现

```java
spea2.setSeed(12345L);  // 固定种子
```

### 技巧2：先小规模测试再大规模运行

```java
// 快速测试
spea2.setPopulationSize(30);
spea2.setMaxGenerations(50);

// 确认正常后，正式运行
spea2.setPopulationSize(100);
spea2.setMaxGenerations(200);
```

### 技巧3：使用多个实例验证算法稳健性

```java
String[] instances = {
    "J20P3B2D5_01.txt",
    "J20P3B2D5_02.txt",
    "J20P3B2D10_01.txt"
};

for (String instance : instances) {
    // 运行算法
}
```

### 技巧4：保存结果用于后续分析

```java
// 使用Runner自动保存
SPEA2Runner.quickRun(problemFile, "output/results", 100, 200, 12345L);

// 结果在: output/results/SPEA2_pareto_front.csv
```

### 技巧5：对比多个算法

```java
// 同时运行SPEA2, NSGAII, MOEAD
List<MOIndividual> spea2Result = runSPEA2();
List<MOIndividual> nsgaiiResult = runNSGAII();
List<MOIndividual> moeadResult = runMOEAD();

// 对比三者的超体积
```

## 八、下一步

1. ✅ 运行 `SimpleSPEA2Test` 验证安装
2. ✅ 尝试 `QuickStart.java` 示例
3. ✅ 运行对比实验
4. ✅ 调整参数优化性能
5. ✅ 准备论文数据和图表

## 九、帮助和支持

如有问题，请查阅：
- `README.md` - 详细文档
- `SPEA2_IMPLEMENTATION_SUMMARY.md` - 实现总结

---

**祝实验顺利！** 🎉
