# MOGWO算法完整实现

## 📦 已完成的工作

已为你完整实现了**多目标灰狼优化算法（MOGWO）**，用作NSGA-II的对比算法。

## 📁 文件结构

```
chapter-3/
├── src/main/java/AlgorithmFrame/mogwo/
│   ├── MOGWO.java                              # ✅ 核心算法类
│   ├── MOGWOOperations.java                    # ✅ 算子操作类
│   ├── MOGWORunner.java                        # ✅ 算法运行器
│   ├── QuickStart.java                         # ✅ 快速启动示例
│   ├── ComparisonExperiment.java               # ✅ 对比实验框架
│   ├── README.md                               # ✅ 详细使用文档
│   ├── MOGWO_IMPLEMENTATION_SUMMARY.md         # ✅ 实现总结
│   └── MOGWO_USAGE_GUIDE.md                    # ✅ 使用指南
│
└── src/test/java/
    └── MOGWOSimpleTest.java                    # ✅ 简单测试
```

## 🚀 快速开始（3种方式）

### 方式1: 使用Runner（最简单）

```java
import AlgorithmFrame.mogwo.*;

public class Test {
    public static void main(String[] args) {
        MOGWORunner.quickRun(
            "src/main/resources/instance/energy/J20P3B2D5_energy.txt",
            "output/mogwo",
            100,    // 种群大小
            500,    // 最大代数
            12345L  // 随机种子
        );
    }
}
```

### 方式2: 运行QuickStart示例

直接运行 `AlgorithmFrame.mogwo.QuickStart` 类的main方法。

### 方式3: 运行测试

运行 `MOGWOSimpleTest.java` 验证实现。

## 🧪 与NSGA-II对比实验

### 快速对比（推荐）

```java
import AlgorithmFrame.mogwo.*;

public class Comparison {
    public static void main(String[] args) {
        ComparisonExperiment.ExperimentConfig config = 
            new ComparisonExperiment.ExperimentConfig();
        config.numRuns = 10;           // 每个算法运行10次
        config.populationSize = 100;
        config.maxGenerations = 500;
        
        ComparisonExperiment.runComparison(
            "src/main/resources/instance/energy/J20P3B2D5_energy.txt",
            "output/comparison",
            config
        );
        
        // 结果保存在: output/comparison/comparison_results.csv
    }
}
```

这会同时运行：
- ✅ MOGWO
- ✅ NSGA-II
- ✅ SPEA2
- ✅ MOEA/D

并生成对比结果CSV文件。

## 📊 实验设计建议

### 实验1: 基础性能测试
- 目的：验证MOGWO的有效性
- 算例：J20, J50, J100
- 运行次数：10次
- 使用：`MOGWORunner.quickRun()`

### 实验2: MOGWO vs NSGA-II
- 目的：对比两个算法的性能
- 评价指标：Hypervolume, Spacing, 运行时间
- 统计检验：Wilcoxon秩和检验
- 使用：`ComparisonExperiment.runComparison()`

### 实验3: 参数敏感性分析（可选）
- 关键参数：种群大小、网格划分数
- 使用：`MOGWORunner.compareConfigurations()`

## 📈 评价指标

实验会自动计算以下指标：

| 指标 | 说明 | 越大越好/越小越好 |
|------|------|------------------|
| ParetoFrontSize | Pareto前沿大小 | 越大越好 |
| Hypervolume | 超体积 | 越大越好 |
| Spacing | 间距指标 | 越小越好 |
| RunTime | 运行时间 | 越小越好 |
| MinCmax | 最小完工时间 | 越小越好 |
| MinEnergy | 最小能耗 | 越小越好 |

## 🔧 参数设置

### 推荐参数

| 问题规模 | populationSize | maxGenerations | gridDivisions |
|---------|---------------|----------------|---------------|
| 小（J≤20） | 50 | 300 | 10 |
| 中（20<J≤50） | 100 | 500 | 10 |
| 大（J>50） | 200 | 1000 | 15 |

### 启用局部搜索（可选）

```java
MOGWO mogwo = new MOGWO(problem, objectives);
mogwo.enableLocalSearch(true, 10);  // 每10代执行一次
mogwo.setLocalSearchParameters(10, 0.10, 0.05, 0.02, 0.005, 0.01);
```

## 📝 论文写作建议

### 算法介绍

> 本研究采用多目标灰狼优化算法（MOGWO）作为对比算法。MOGWO基于灰狼的社会层级和捕猎行为，
> 通过Alpha、Beta、Delta三个领导者引导狼群向最优解移动，并使用外部存档和网格机制维护
> Pareto前沿的收敛性和多样性。

### 实验设置

> 实验中，MOGWO和NSGA-II均设置种群大小为100，最大迭代次数为500代。MOGWO的网格划分数
> 设为10。每个算法在每个算例上独立运行10次，随机种子设为12345-12354。

### 结果分析

可以从以下角度：
1. **收敛性**：比较Hypervolume值
2. **多样性**：比较Spacing指标
3. **效率**：比较运行时间
4. **解的质量**：比较极端解的目标值

## 📖 详细文档

- **完整使用说明**：`AlgorithmFrame/mogwo/README.md`
- **实现细节**：`AlgorithmFrame/mogwo/MOGWO_IMPLEMENTATION_SUMMARY.md`
- **使用指南**：`AlgorithmFrame/mogwo/MOGWO_USAGE_GUIDE.md`

## ✅ 实现特点

1. **完全集成**：与NSGA-II、SPEA2、MOEA/D使用统一接口
2. **编码一致**：使用相同的OS/MS编码方案
3. **评价兼容**：复用现有的评价函数和局部搜索
4. **实验友好**：内置对比实验框架
5. **文档完善**：提供详细的使用文档和示例

## 🎯 核心算法流程

```
1. 初始化灰狼种群
2. 评价所有个体
3. 初始化外部存档
4. For each generation:
   4.1 计算自适应参数 a = 2 - 2*t/T
   4.2 从存档选择Alpha、Beta、Delta
   4.3 For each wolf:
       - 基于三个领导者更新位置
       - 修复并评价新解
   4.4 更新外部存档
   4.5 若存档超容量，使用网格截断
5. 返回Pareto前沿
```

## 🔬 算法特点

| 特性 | 说明 |
|------|------|
| 社会层级 | 三个领导者协同引导 |
| 自适应探索 | 参数a从2线性递减到0 |
| 外部存档 | 维护非支配解集 |
| 网格机制 | 保持解的多样性 |
| 贪婪选择 | 只保留更优的解 |

## 📞 使用帮助

如有问题，请：
1. 查看 `README.md` 详细文档
2. 运行 `MOGWOSimpleTest.java` 测试
3. 参考 `QuickStart.java` 示例
4. 检查代码中的注释

## 🎓 适用场景

- ✅ 毕业设计对比实验
- ✅ 多目标优化研究
- ✅ 调度问题求解
- ✅ 算法性能分析

## 📦 依赖关系

无需额外依赖，完全基于你现有的代码框架：
- ✅ 复用 `MOIndividual` 类
- ✅ 复用 `MOEvaluator` 类
- ✅ 复用 `Problem` 类
- ✅ 复用局部搜索模块

## 🚦 下一步

1. **验证实现**：运行 `MOGWOSimpleTest.java`
2. **测试算例**：运行 `QuickStart.java`
3. **对比实验**：运行 `ComparisonExperiment.java`
4. **分析结果**：查看生成的CSV文件
5. **撰写论文**：根据实验结果完成对比分析

---

**创建日期**: 2026-01-19  
**版本**: 1.0  
**状态**: ✅ 完整实现

祝你毕业设计顺利！🎉
