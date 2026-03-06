# 随机密钥GA对比算法 - 快速使用指南

## 快速开始（3分钟）

### 步骤1：验证算法是否正常工作

运行简单测试（使用小算例J10）：

```bash
# 在IDE中运行或使用命令行
java SimpleRandomKeyGATest
```

**预期输出**：
```
========================================
    随机密钥GA算法 - 简单测试
========================================

正在读取算例: ...
算例信息:
  - 零件数量: 10
  - 打印机数量: 2
  ...
最优Makespan: xxxx.xx
✓ 算法运行正常！
```

### 步骤2：运行完整对比实验

运行所有算例的对比实验（需要较长时间）：

```bash
java RandomKeyGAExperimentRunner
```

**实验配置**：
- 算例规模：J10, J20, J50, J100
- 每个算例：运行10次
- 并行计算：自动使用多线程
- 输出位置：`src/main/resources/result/对比试验/随机密钥GA/`

### 步骤3：查看结果

实验完成后，查看汇总统计表：

```
src/main/resources/result/对比试验/随机密钥GA/实验汇总统计表.txt
```

对比混合GA的结果：

```
src/main/resources/result/对比试验/混合GA/实验汇总统计表.txt
```

---

## 详细使用说明

### 1. 单个算例测试

```java
import AlgorthmFrame.randomKeyGA.RandomKeyGA;
import ProblemFrame.GAParameters;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import java.io.File;

// 读取算例
Input input = new Input(new File("path/to/instance.txt"));
Problem problem = input.getProblemDesFromFile();

// 配置参数
GAParameters params = new GAParameters();
params.popSize = 100;         // 种群大小
params.pc = 0.8;              // 交叉率
params.pm = 0.1;              // 变异率
params.maxStagnantStep = 50;  // 最大停滞代数
params.maxRunTime = 5.0;      // 运行时间限制（分钟）

// 运行算法
RandomKeyGA ga = new RandomKeyGA(problem, params);
RandomKeyGA.RandomKeySolution solution = ga.solve();

// 查看结果
System.out.println("最优Makespan: " + solution.cost);
System.out.println("迭代次数: " + solution.makespanHistory.size());
```

### 2. 批量实验（推荐）

直接运行 `RandomKeyGAExperimentRunner.java`：

**优势**：
- ✅ 自动扫描所有算例
- ✅ 多线程并行加速
- ✅ 自动保存详细结果
- ✅ 生成汇总统计表

**配置修改**（可选）：

```java
// 在 RandomKeyGAExperimentRunner.java 中修改

// 修改算例目录
private static final String BASE_DIR = "your/path/to/instances/";

// 修改结果输出目录
private static final String RESULT_BASE_DIR = "your/output/path/";

// 修改每个算例运行次数（默认10次）
private static final int RUNS_PER_INSTANCE = 10;
```

### 3. 参数调优

在 `RandomKeyGAExperimentRunner.java` 的 `main` 方法中修改：

```java
GAParameters params = GAParameters.getDefaultParameters();

// 调整参数
params.popSize = 200;         // 增大种群 → 更多样，但更慢
params.pc = 0.9;              // 增大交叉率 → 更多探索
params.pm = 0.05;             // 减小变异率 → 更稳定
params.maxStagnantStep = 100; // 增大停滞步数 → 更长运行
params.maxRunTime = 10.0;     // 增大时间限制 → 更充分搜索
```

---

## 结果解读

### 单次运行结果

每次运行生成的文件夹包含：

```
J10P2B1D2_01_run1/
├── experiment_info.txt        # 实验配置信息
├── algorithm_output.txt       # 算法运行日志
└── iteration_curve.txt        # 迭代曲线数据
```

**迭代曲线数据格式**：
```
Gen 0: 5432.15
Gen 1: 5321.89
Gen 2: 5210.44
...
```

### 汇总统计表

```
================================================================================
                     随机密钥GA算法 - 综合实验汇总统计表
================================================================================
算例名称                  平均值        最优值        最差值      标准差      变异系数    平均耗时(s)
------------------------------------------------------------------------------------
J10P2B1D2_01             5123.45      5012.33      5234.56    78.90      1.54%      45.67
J10P2B1D2_02             5234.67      5123.45      5345.78    89.12      1.70%      46.89
...
------------------------------------------------------------------------------------
总平均                   5178.56      5067.89
```

**指标说明**：
- **平均值**：10次运行的平均makespan
- **最优值**：10次运行中的最小makespan（越小越好）
- **最差值**：10次运行中的最大makespan
- **标准差**：衡量稳定性（越小越稳定）
- **变异系数**：标准差/平均值，标准化的稳定性指标（越小越好）
- **平均耗时**：单次运行的平均时间

---

## 与混合GA对比

### 对比步骤

1. **运行两个实验**：
   ```bash
   java ComprehensiveExperimentRunner      # 混合GA
   java RandomKeyGAExperimentRunner        # 随机密钥GA
   ```

2. **提取关键数据**：
   - 从两个汇总统计表中提取数据
   - 建议使用Excel或Python进行数据处理

3. **对比维度**：
   - **求解质量**：比较平均Makespan和最优Makespan
   - **稳定性**：比较标准差和变异系数
   - **效率**：比较平均运行时间
   - **规模适应性**：分别对比J10, J20, J50, J100的表现

### 对比示例

假设得到以下数据：

| 算法 | J10平均 | J20平均 | J50平均 | J100平均 | 总平均 |
|------|---------|---------|---------|----------|--------|
| 混合GA | 5123.45 | 6234.56 | 8345.67 | 12456.78 | 8040.12 |
| 随机密钥GA | 5234.67 | 6345.78 | 8456.89 | 12567.89 | 8151.31 |
| 差异(%) | +2.17% | +1.78% | +1.33% | +0.89% | +1.38% |

**结论示例**：
> 混合GA在所有规模上都优于随机密钥GA，平均改进1.38%。
> 但随机密钥GA在大规模问题(J100)上差距较小，显示出更好的规模适应性。

---

## 常见问题

### Q1: 算法运行很慢怎么办？

**解决方案**：
```java
// 减小种群大小
params.popSize = 50;  // 默认100

// 减小时间限制
params.maxRunTime = 2.0;  // 默认5分钟

// 减小停滞步数
params.maxStagnantStep = 20;  // 默认50
```

### Q2: 内存不足怎么办？

**增加JVM堆内存**：
```bash
java -Xmx4G RandomKeyGAExperimentRunner  # 4GB堆内存
```

### Q3: 如何只运行特定算例？

**修改 `RandomKeyGAExperimentRunner.java`**：
```java
// 在main方法中修改扫描的算例类别
for (String category : Arrays.asList("J10")) {  // 只运行J10
    // ...
}
```

### Q4: 如何保存更多信息？

**使用ExperimentResultWriter**：
```java
RandomKeyGA ga = new RandomKeyGA(problem, params);
ExperimentResultWriter writer = new ExperimentResultWriter(...);
ga.setResultWriter(writer);

// writer会自动记录算法运行日志
```

---

## 性能优化建议

### 小规模问题（J10, J20）

```java
params.popSize = 50;          // 较小种群足够
params.maxRunTime = 2.0;      // 短时间即可收敛
params.maxStagnantStep = 30;  // 较小停滞步数
```

### 中等规模问题（J50）

```java
params.popSize = 100;         // 默认配置
params.maxRunTime = 5.0;      
params.maxStagnantStep = 50;  
```

### 大规模问题（J100）

```java
params.popSize = 200;         // 增大种群
params.maxRunTime = 10.0;     // 更长时间
params.maxStagnantStep = 100; // 更多迭代
```

---

## 技术支持

### 调试模式

在 `RandomKeyGA.java` 中修改：

```java
// 增加输出频率
if (generation % 10 == 0) {  // 默认50
    // 输出进度
}
```

### 可视化迭代曲线

使用Python绘制：

```python
import matplotlib.pyplot as plt

# 读取迭代曲线数据
with open('iteration_curve.txt') as f:
    data = [float(line.split(':')[1]) for line in f]

plt.plot(data)
plt.xlabel('Generation')
plt.ylabel('Best Makespan')
plt.title('Convergence Curve')
plt.show()
```

---

## 下一步

1. ✅ 运行 `SimpleRandomKeyGATest` 验证算法
2. ✅ 运行 `RandomKeyGAExperimentRunner` 获取完整结果
3. ✅ 对比 `混合GA` 和 `随机密钥GA` 的结果
4. ✅ 根据结果撰写对比分析报告

祝实验顺利！🎉

