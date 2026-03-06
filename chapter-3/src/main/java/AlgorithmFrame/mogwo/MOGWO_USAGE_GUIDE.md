# MOGWO算法使用指南

## 快速开始（3步完成）

### 方法1：使用MOGWORunner（最简单）

```java
import AlgorithmFrame.mogwo.*;

public class MyTest {
    public static void main(String[] args) {
        MOGWORunner.quickRun(
            "src/main/resources/instance/energy/J20P3B2D5_energy.txt",  // 问题文件
            "output/mogwo",                                              // 输出目录
            100,                                                         // 种群大小
            500,                                                         // 最大代数
            12345L                                                       // 随机种子
        );
    }
}
```

### 方法2：直接使用MOGWO类

```java
import AlgorithmFrame.mogwo.*;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

public class MyTest {
    public static void main(String[] args) throws Exception {
        // 1. 加载问题
        File file = new File("src/main/resources/instance/energy/J20P3B2D5_energy.txt");
        EnergyAwareInput input = new EnergyAwareInput(file);
        Problem problem = input.getProblemDesFromFile();
        
        // 2. 定义目标
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        );
        
        // 3. 运行MOGWO
        MOGWO mogwo = new MOGWO(problem, objectives);
        mogwo.setPopulationSize(100);
        mogwo.setMaxGenerations(500);
        mogwo.setSeed(12345L);
        
        List<MOIndividual> paretoFront = mogwo.solve();
        
        // 4. 查看结果
        System.out.println("Pareto前沿大小: " + paretoFront.size());
    }
}
```

### 方法3：运行QuickStart示例

直接运行 `AlgorithmFrame.mogwo.QuickStart` 的main方法。

## 验证安装

运行测试文件验证MOGWO是否正确实现：

```bash
# 在IDE中运行
src/test/java/MOGWOSimpleTest.java
```

或者在命令行：

```bash
# 编译
javac -cp "target/classes:lib/*" src/test/java/MOGWOSimpleTest.java

# 运行
java -cp "target/classes:lib/*:src/test/java" MOGWOSimpleTest
```

## 与NSGA-II对比实验

### 方案1：快速对比（单次运行）

```java
import AlgorithmFrame.mogwo.*;

public class QuickComparison {
    public static void main(String[] args) {
        // 这个方法在QuickStart.java中已经实现
        QuickStart.comparisonExperiment();
    }
}
```

### 方案2：完整对比（多次运行 + 统计分析）

```java
import AlgorithmFrame.mogwo.*;

public class FullComparison {
    public static void main(String[] args) {
        String problemFile = "src/main/resources/instance/energy/J20P3B2D5_energy.txt";
        String outputDir = "output/comparison";
        
        // 配置实验参数
        ComparisonExperiment.ExperimentConfig config = 
            new ComparisonExperiment.ExperimentConfig();
        config.numRuns = 10;           // 每个算法运行10次
        config.populationSize = 100;   // 种群大小
        config.maxGenerations = 500;   // 最大代数
        config.baseSeed = 12345L;      // 基础随机种子
        
        // 运行对比实验（会同时运行MOGWO、NSGA-II、SPEA2、MOEA/D）
        ComparisonExperiment.runComparison(problemFile, outputDir, config);
        
        // 结果会保存在 output/comparison/comparison_results.csv
    }
}
```

输出的CSV格式：

```csv
Algorithm,Run,ParetoFrontSize,RunTime,Hypervolume,Spacing,MinCmax,MinEnergy
MOGWO,1,45,12.34,0.8523,0.0234,1198.45,5234.67
MOGWO,2,47,12.56,0.8612,0.0221,1203.12,5189.34
...
NSGA-II,1,48,14.23,0.8445,0.0198,1205.67,5167.89
...
```

## 参数调优建议

### 不同规模问题的推荐参数

```java
// 小规模问题（J ≤ 20）
mogwo.setPopulationSize(50);
mogwo.setArchiveSize(50);
mogwo.setMaxGenerations(300);
mogwo.setGridDivisions(10);

// 中等规模问题（20 < J ≤ 50）
mogwo.setPopulationSize(100);
mogwo.setArchiveSize(100);
mogwo.setMaxGenerations(500);
mogwo.setGridDivisions(10);

// 大规模问题（J > 50）
mogwo.setPopulationSize(200);
mogwo.setArchiveSize(200);
mogwo.setMaxGenerations(1000);
mogwo.setGridDivisions(15);
```

### 启用局部搜索（可选）

```java
MOGWO mogwo = new MOGWO(problem, objectives);
mogwo.setPopulationSize(100);
mogwo.setMaxGenerations(500);

// 启用局部搜索（每10代执行一次）
mogwo.enableLocalSearch(true, 10);

// 设置局部搜索参数
mogwo.setLocalSearchParameters(
    10,      // L: 每次局部搜索的最大迭代次数
    0.10,    // eta: 选择top 10%的解进行局部搜索
    0.05,    // epsE: 能耗改进阈值
    0.02,    // epsC: Cmax改进阈值
    0.005,   // improvC: Cmax显著改进阈值
    0.01     // improvE: 能耗显著改进阈值
);

List<MOIndividual> paretoFront = mogwo.solve();
```

## 批量运行多个算例

```java
import AlgorithmFrame.mogwo.*;

public class BatchRun {
    public static void main(String[] args) {
        // 定义要测试的算例
        String[] instances = {
            "J20P3B2D5_energy.txt",
            "J50P5B4D10_energy.txt",
            "J100P4B2D10_energy.txt"
        };
        
        // 对每个算例运行MOGWO
        for (String instanceName : instances) {
            String problemFile = "src/main/resources/instance/energy/" + instanceName;
            String outputDir = "output/mogwo/" + instanceName.replace(".txt", "");
            
            System.out.println("\n运行算例: " + instanceName);
            
            MOGWORunner.quickRun(
                problemFile,
                outputDir,
                100,    // 种群大小
                500,    // 最大代数
                12345L  // 随机种子
            );
        }
        
        System.out.println("\n所有算例运行完成！");
    }
}
```

## 毕业论文实验建议

### 实验1: MOGWO基础性能测试

**目的**: 验证MOGWO在不同规模问题上的性能

**步骤**:
1. 选择3-5个不同规模的算例（J20, J50, J100）
2. 每个算例运行10次
3. 记录：Pareto前沿大小、运行时间、最优目标值

```java
// 使用MOGWORunner.quickRun()即可
```

### 实验2: MOGWO vs NSGA-II对比

**目的**: 证明MOGWO的有效性

**步骤**:
1. 选择相同的测试算例
2. 使用相同的参数配置
3. 每个算法运行10次
4. 使用Wilcoxon秩和检验进行统计分析

```java
// 使用ComparisonExperiment.runComparison()
```

**评价指标**:
- Hypervolume（超体积）
- Spacing（间距指标）
- 运行时间
- Pareto前沿大小

### 实验3: 参数敏感性分析（可选）

**目的**: 分析关键参数对算法性能的影响

**关键参数**:
- 种群大小：50, 100, 150, 200
- 网格划分数：5, 10, 15, 20

```java
int[] popSizes = {50, 100, 150, 200};
for (int popSize : popSizes) {
    mogwo.setPopulationSize(popSize);
    mogwo.setArchiveSize(popSize);
    // 运行并记录结果
}
```

### 实验4: 局部搜索效果分析（可选）

**目的**: 验证局部搜索的作用

**对比方案**:
- MOGWO（无局部搜索）
- MOGWO（有局部搜索）

```java
// 无局部搜索
MOGWO mogwo1 = new MOGWO(problem, objectives);
mogwo1.enableLocalSearch(false, 0);

// 有局部搜索
MOGWO mogwo2 = new MOGWO(problem, objectives);
mogwo2.enableLocalSearch(true, 10);
```

## 论文写作建议

### 算法描述

可以这样描述MOGWO：

> 本文采用多目标灰狼优化算法（MOGWO）作为对比算法。MOGWO由Mirjalili等人于2016年提出，
> 模拟灰狼的社会层级和捕猎行为。算法维护一个外部存档保存非支配解，并通过网格机制保持
> 解的多样性。在每次迭代中，从存档中选择三个领导者（Alpha、Beta、Delta）引导狼群
> 向最优解方向移动。

### 实验设置

> 实验中，MOGWO的参数设置如下：种群大小N=100，存档大小N̄=100，最大迭代次数T=500，
> 网格划分数D=10。为保证对比的公平性，NSGA-II与MOGWO采用相同的种群大小和迭代次数。
> 每个算法在每个算例上独立运行10次，随机种子分别设为12345-12354。

### 结果分析

可以从以下角度分析：

1. **收敛性**: 比较两个算法的Hypervolume值
2. **多样性**: 比较Spacing指标
3. **效率**: 比较运行时间
4. **解的质量**: 比较最优目标值

## 常见问题

### Q1: 算例文件在哪里？

A: 需要能耗相关的算例文件，路径通常是：
- `src/main/resources/instance/energy/J20P3B2D5_energy.txt`
- 或者使用现有的测试算例：`src/main/resources/test_instance_*.txt`

### Q2: 如何查看运行进度？

A: MOGWO会自动输出进度信息：
```
代数:   50 | 存档大小:   45 | 耗时: 5.23s
代数:  100 | 存档大小:   48 | 耗时: 10.45s
...
```

### Q3: 结果保存在哪里？

A: 结果CSV文件保存在你指定的输出目录中：
- `output/mogwo/MOGWO_pareto_front.csv`

### Q4: 如何可视化Pareto前沿？

A: 可以使用Python或Excel绘制散点图：

```python
import pandas as pd
import matplotlib.pyplot as plt

# 读取数据
df = pd.read_csv('output/mogwo/MOGWO_pareto_front.csv')

# 绘制Pareto前沿
plt.figure(figsize=(8, 6))
plt.scatter(df['Cmax'], df['Energy'], alpha=0.6)
plt.xlabel('Cmax')
plt.ylabel('Energy')
plt.title('MOGWO Pareto Front')
plt.grid(True)
plt.savefig('pareto_front.png', dpi=300)
plt.show()
```

## 文件清单

已创建的文件：

1. **核心算法**:
   - `MOGWO.java` - 主算法类
   - `MOGWOOperations.java` - 算子操作类

2. **运行器**:
   - `MOGWORunner.java` - 算法运行器
   - `QuickStart.java` - 快速启动示例
   - `ComparisonExperiment.java` - 对比实验

3. **测试**:
   - `MOGWOSimpleTest.java` - 简单测试

4. **文档**:
   - `README.md` - 使用说明
   - `MOGWO_IMPLEMENTATION_SUMMARY.md` - 实现总结
   - `MOGWO_USAGE_GUIDE.md` - 本文档

## 下一步

1. **验证实现**: 运行 `MOGWOSimpleTest.java`
2. **测试算例**: 运行 `QuickStart.java`
3. **对比实验**: 运行 `ComparisonExperiment.java`
4. **撰写论文**: 根据实验结果撰写论文相关章节

祝你毕业论文顺利！
