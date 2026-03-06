# GA-TS算法对比实验使用说明

## 算法介绍

**GA-TS算法**是一种混合优化算法：
- **外层GA（遗传算法）**：用于机器分配优化
- **内层TS（禁忌搜索）**：用于机器内作业次序优化

这种双层优化结构能够同时优化机器分配和作业排序，获得更优的调度方案。

## 快速开始

### 1. 运行对比实验

直接运行 `ComparisonExperiment.java` 的 main 方法：

```java
public static void main(String[] args) {
    // 小规模算例：2台机器，10个作业
    runComparison(
        "chapter-1/src/main/resources/Machine/machine_2",
        "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01",
        3,      // 运行3次
        true    // 开启可视化（每次运行都生成完整图表）
    );
}
```

### 2. 可视化输出

**重要**：为**每一次运行**都会生成完整的可视化图表！

每次运行会生成以下图表：
```
chapter-1/src/main/output/comparison/
├── run_1/                          # 第1次运行
│   ├── iteration_curve.png         # 迭代曲线图
│   ├── batch_distribution.png      # 批次分布图
│   ├── gantt_chart.png             # 甘特图
│   └── machine_X_batch_Y_layout.png # 批次布局图
├── run_2/                          # 第2次运行
│   ├── iteration_curve.png
│   ├── batch_distribution.png
│   ├── gantt_chart.png
│   └── machine_X_batch_Y_layout.png
└── run_3/                          # 第3次运行
    ├── iteration_curve.png
    ├── batch_distribution.png
    ├── gantt_chart.png
    └── machine_X_batch_Y_layout.png
```

### 3. 数值结果输出

运行完成后，会生成CSV文件：
```
chapter-1/src/main/output/GATS_comparison_results.csv
```

CSV文件包含：
- 每次运行的详细数据（Cmax、利用率、运行时间）
- 统计信息（平均值、标准差、最优值等）

## 算法参数说明

### GA参数（外层优化）

```java
BatchGa batchGa = new BatchGa(
    500,    // MAX_GEN - 最大迭代代数（遗传算法迭代次数）
    100,    // popSize - 种群规模（个体数量）
    5,      // variationExchangeCount - 变异交换次数
    2,      // cloneNumOfBestIndividual - 克隆最优个体数量
    0.2,    // mutationRate - 变异率
    0.8,    // crossoverRate - 交叉率
    input,
    true,   // isRotateEnable - 是否允许零件旋转
    "TabuSearch",  // method - 使用禁忌搜索
    100,    // decodeMaxGen - TS最大迭代次数
    10,     // decodeTabuSize - 禁忌表大小
    30      // decodeMaxN - TS邻域大小
);
```

### TS参数（内层优化）

- `decodeMaxGen = 100`：禁忌搜索最大迭代次数
- `decodeTabuSize = 10`：禁忌表大小（记忆最近10个解）
- `decodeMaxN = 30`：邻域搜索大小（每次探索30个邻居）

## 修改实验设置

### 修改运行次数

```java
runComparison(
    machinePath, 
    itemPath, 
    10,     // 改为运行10次
    true
);
```

### 关闭可视化（加快实验速度）

```java
runComparison(
    machinePath, 
    itemPath, 
    10,     
    false   // 关闭可视化，只保存数值结果
);
```

### 修改算法参数

在 `runSingleComparison` 方法中修改：

```java
BatchGa batchGa = new BatchGa(
    1000,   // 增加到1000代
    200,    // 增加种群规模到200
    5,      
    2,      
    0.3,    // 提高变异率
    0.9,    // 提高交叉率
    input,
    true,   
    "TabuSearch",
    200,    // 增加TS迭代次数
    15,     // 增加禁忌表大小
    50      // 增加邻域大小
);
```

## 输出说明

### 控制台输出

```
========================================
开始对比实验
算法: GA-TS (外层GA机器分配 + 内层TS次序优化)
机器文件: chapter-1/src/main/resources/Machine/machine_2
作业文件: chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01
运行次数: 3
可视化: 每次运行都生成
========================================

========== 运行 1 ==========
运行GA-TS算法...
当前代数:1:850.23
当前代数:2:820.45
...
运行 1 完成: Cmax = 785.50, 利用率 = 82.35%, 时间 = 45230ms
生成第 1 次运行的可视化图表...
可视化图表已保存到: chapter-1/src/main/output/comparison/run_1

========== 运行 2 ==========
...
```

### 统计结果

```
========================================
GA-TS算法实验统计结果
========================================
平均Cmax:      789.2345
最小Cmax:      782.1234 (运行 2)
最大Cmax:      795.3456 (运行 1)
标准差:        5.6789
平均利用率:    81.45%
平均时间:      46820.50 ms
========================================
```

## 可视化图表说明

### 1. 迭代曲线图 (iteration_curve.png)
- 显示GA算法的收敛过程
- 横轴：迭代代数（1-500）
- 纵轴：当前最优Cmax
- **用途**：评估算法收敛速度和稳定性

### 2. 批次分布图 (batch_distribution.png)
- 蓝色柱：各机器的批次数量
- 橙色线：各机器的利用率
- **用途**：评估负载均衡情况

### 3. 甘特图 (gantt_chart.png)
- 横轴：时间
- 纵轴：机器编号
- 矩形：批次（标注为 B机器号-批次号）
- **用途**：查看调度时序，识别空闲时间

### 4. 批次布局图 (machine_X_batch_Y_layout.png)
- 显示每个批次中零件的具体布局
- 红色↻标记：零件被旋转
- **用途**：验证装箱方案的可行性

## 对比多个算法

如果要对比GA-TS与其他算法（如ALNS），可以：

### 方法1：分别运行不同算法

```java
// 运行GA-TS
ComparisonExperiment.runComparison(...);

// 运行ALNS（需要创建新的实验类）
ALNSComparisonExperiment.runComparison(...);
```

### 方法2：在同一个类中运行

创建 `MultiAlgorithmComparison.java`：

```java
public class MultiAlgorithmComparison {
    public static void main(String[] args) {
        // 运行GA-TS
        System.out.println("===== 运行GA-TS算法 =====");
        ComparisonExperiment.runComparison(...);
        
        // 运行其他算法
        System.out.println("\n===== 运行ALNS算法 =====");
        // ...
    }
}
```

## 性能优化建议

### 1. 快速测试
```java
// 减少迭代次数和运行次数
BatchGa batchGa = new BatchGa(
    100,    // 只运行100代
    50,     // 种群规模50
    ...
);
runComparison(..., 1, false);  // 只运行1次，不生成可视化
```

### 2. 完整实验
```java
// 使用完整参数
BatchGa batchGa = new BatchGa(
    500,    // 500代
    100,    // 种群规模100
    ...
);
runComparison(..., 10, true);  // 运行10次，生成所有可视化
```

### 3. 大规模实验
```java
// 增加计算资源
BatchGa batchGa = new BatchGa(
    1000,   // 1000代
    200,    // 种群规模200
    ...
);
runComparison(..., 30, false);  // 运行30次，只保存数值（节省空间）
```

## 注意事项

1. **存储空间**
   - 每次运行的可视化约占5-15MB
   - 运行10次需要50-150MB空间
   - 建议定期清理旧的输出

2. **运行时间**
   - 小规模（10作业）：约30-60秒/次
   - 中规模（20作业）：约2-5分钟/次
   - 大规模（50作业）：约10-30分钟/次

3. **可视化生成**
   - 每次运行都生成完整图表
   - 如需加快速度，设置 `enableVisualization = false`
   - 或者只为最优运行生成可视化

4. **随机种子**
   - 当前使用：`seed = 12345L + runId`
   - 保证结果可重现
   - 如需不同随机性，修改种子生成方式

## 常见问题

### Q1: 如何只为最优运行生成可视化？

修改 `runComparison` 方法：
```java
// 第一遍：运行所有实验，不生成可视化
for (int i = 1; i <= numRuns; i++) {
    ExperimentResult result = runSingleComparison(
        machinePath, itemPath, i, outputDir, false  // 不生成可视化
    );
    results.add(result);
}

// 第二遍：为最优运行生成可视化
Statistics stats = calculateStatistics(results);
runSingleComparison(
    machinePath, itemPath, stats.bestResult.runId, outputDir, true
);
```

### Q2: 如何修改输出目录？

在 `runComparison` 方法中：
```java
String outputDir = "your/custom/output/directory";
```

### Q3: 如何导出论文用的图表？

所有图表都是高分辨率PNG格式（1920x1080），可直接用于论文。
建议使用最优运行的图表。

## 进阶使用

### 添加新的性能指标

在 `ExperimentResult` 中添加：
```java
static class ExperimentResult {
    // ... 现有字段
    double makespan;         // 新增
    double loadBalance;      // 新增
    double energyConsumption; // 新增
}
```

### 自定义可视化

使用 `Chapter1Visualizer` 的独立方法：
```java
// 只生成迭代曲线
Chapter1Visualizer.generateIterationCurve(
    iterationHistory, 
    "output/custom_curve.png",
    "自定义标题"
);

// 只生成甘特图
Chapter1Visualizer.generateGanttChart(
    batchResults,
    machines,
    "output/custom_gantt.png"
);
```

---

**祝实验顺利！如有问题请查看详细代码或联系开发者。**
