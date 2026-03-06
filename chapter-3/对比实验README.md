# 算法对比实验框架

## 📦 项目概述

本框架用于在3D打印调度问题上对比四种多目标进化算法的性能：

| 算法 | 描述 |
|------|------|
| **NSGA-II** | 非支配排序遗传算法 II |
| **MOEA/D** | 基于分解的多目标进化算法 |
| **MOGWO** | 多目标灰狼优化算法 |
| **SPEA2** | 强度Pareto进化算法 2 |

## 🎯 核心功能

✅ **自动化对比实验**
- 四种算法同时运行
- 每个算例运行10次
- 统一参数配置

✅ **三大评价指标**
- **HV (Hypervolume)**：收敛性+多样性
- **IGD (Inverted Generational Distance)**：与真实前沿的距离
- **C-metric (Coverage)**：算法间支配关系

✅ **完整的输出结构**
- 每次运行的Pareto前沿
- 每个算法的汇总Pareto前沿
- 近似Pareto前沿（PF*）
- 详细对比报告

✅ **可视化工具**
- Pareto前沿对比图
- HV/IGD箱线图
- 统计显著性检验

## 📂 文件结构

```
chapter-3/
├── src/test/java/
│   └── AlgorithmComparisonExperimentRunner.java  # 主实验程序
├── output/comparison_experiment/                  # 输出目录
│   └── 算例名称_时间戳/
│       ├── NSGAII/                               # 各算法结果
│       ├── MOEAD/
│       ├── MOGWO/
│       ├── SPEA2/
│       ├── ApproximateParetoFront.txt            # PF*
│       └── ComparisonReport.txt                  # 对比报告
├── run_comparison.bat                             # 快速启动脚本
├── visualize_comparison.py                        # 可视化脚本
├── 算法对比实验使用说明.md                        # 详细文档
└── 对比实验快速指南.md                            # 快速指南
```

## 🚀 快速开始

### 1. 运行实验

**Windows**：
```bash
双击运行: run_comparison.bat
```

**其他系统**：
```bash
cd chapter-3
mvn test -Dtest=AlgorithmComparisonExperimentRunner
```

### 2. 选择模式

```
1. 单个算例对比     (约15-30分钟)
2. J20批量对比      (约1-2小时)
3. J50批量对比      (约2-4小时)
4. J100批量对比     (约4-8小时)
5. 全部算例对比     (约8-15小时)
```

### 3. 查看结果

- 对比报告：`output/comparison_experiment/.../ComparisonReport.txt`
- Pareto前沿：`output/comparison_experiment/.../算法名/AlgorithmParetoFront.txt`

### 4. 生成图表

```bash
pip install numpy matplotlib seaborn scipy pandas
python visualize_comparison.py
```

## 📊 输出说明

### 目录结构

```
J20P3B2D5_01_20260129_143025/
├── NSGAII/
│   ├── run_1/
│   │   ├── ParetoFront.txt          # 第1次运行的Pareto前沿
│   │   └── Statistics.txt           # 第1次运行的统计信息
│   ├── run_2/ ... run_10/
│   └── AlgorithmParetoFront.txt     # NSGA-II汇总Pareto前沿 ⭐
├── MOEAD/
│   └── AlgorithmParetoFront.txt     # MOEA/D汇总Pareto前沿 ⭐
├── MOGWO/
│   └── AlgorithmParetoFront.txt     # MOGWO汇总Pareto前沿 ⭐
├── SPEA2/
│   └── AlgorithmParetoFront.txt     # SPEA2汇总Pareto前沿 ⭐
├── ApproximateParetoFront.txt       # 近似Pareto前沿（PF*）
└── ComparisonReport.txt             # 对比报告 ⭐⭐⭐
```

### 关键文件

| 文件 | 用途 |
|------|------|
| `ComparisonReport.txt` | **论文数据来源**，包含所有指标 |
| `AlgorithmParetoFront.txt` | **画图用**，各算法的汇总前沿 |
| `ApproximateParetoFront.txt` | IGD计算的参考集 |

## 📈 性能指标

### Hypervolume (HV)

**定义**：Pareto前沿与参考点围成的超体积

**计算方法**：
1. 归一化目标值到 [0, 1]
2. 使用参考点 (1.1, 1.1)
3. 计算2D超体积

**解读**：
- **越大越好**
- 同时反映收敛性和多样性
- 最权威的多目标指标

### IGD (Inverted Generational Distance)

**定义**：获得前沿与参考前沿的平均距离

**计算方法**：
1. 构建近似Pareto前沿 PF*（所有算法的非支配解合集）
2. 对PF*中每个点，找到算法前沿中最近的点
3. 计算平均欧氏距离

**解读**：
- **越小越好**
- 反映逼近真实前沿的程度
- 与HV互补

### C-metric (Coverage)

**定义**：算法A支配算法B的比例

**公式**：
$$C(A, B) = \frac{|\{x \in B | \exists y \in A: y \prec x\}|}{|B|}$$

**解读**：
- **非对称**：C(A,B) ≠ C(B,A)
- 值域：[0, 1]
- 直观反映支配关系

**示例**：
```
C(NSGAII, MOEAD) = 0.45  →  NSGA-II支配MOEA/D的45%
C(MOEAD, NSGAII) = 0.12  →  MOEA/D只支配NSGA-II的12%
```

## 🔧 配置参数

在 `AlgorithmComparisonExperimentRunner.java` 中修改：

```java
// 基本配置
private static final int REPEAT_TIMES = 10;          // 重复次数
private static final double MAX_RUN_TIME = 5.0;      // 最大时间（分钟）
private static final int POPULATION_SIZE = 100;      // 种群大小
private static final double CROSSOVER_RATE = 0.9;    // 交叉率
private static final double MUTATION_RATE = 0.1;     // 变异率

// 局部搜索
private static final boolean ENABLE_LOCAL_SEARCH = true;
private static final int LOCAL_SEARCH_L = 10;

// 多线程
private static final boolean ENABLE_MULTI_THREAD = true;
```

### 推荐配置

| 场景 | 种群 | 时间 | 重复次数 |
|------|------|------|---------|
| 快速测试 | 50 | 3分钟 | 5次 |
| 正常实验 | 100 | 5分钟 | 10次 |
| 论文发表 | 100 | 5分钟 | 30次 |
| 高精度 | 150 | 10分钟 | 50次 |

## 📖 论文写作模板

### 实验设置

> 本文采用NSGA-II、MOEA/D、MOGWO和SPEA2四种经典多目标进化算法进行性能对比。所有算法采用统一参数：种群大小100，交叉率0.9，变异率0.1，最大运行时间5分钟。每个算例每个算法独立运行10次。采用Hypervolume（HV）、Inverted Generational Distance（IGD）和C-metric三个指标评价算法性能。

### 结果描述

> 表X展示了各算法在J20系列算例上的性能。NSGA-II的平均HV为0.856±0.012，显著优于MOEA/D（0.823±0.018，p<0.01）、MOGWO（0.835±0.016，p<0.01）和SPEA2（0.846±0.013，p<0.05）。IGD结果表明NSGA-II最接近真实Pareto前沿（0.0123±0.0018）。C-metric显示NSGA-II对MOEA/D的支配率为45.32%，而反向支配率仅为12.34%，充分验证了算法的优越性。

### 表格模板

**表：算法性能对比**

| 算法 | HV | IGD | 排名 |
|------|-----|-----|------|
| NSGA-II | 0.856±0.012 | 0.0123±0.0018 | 1 |
| SPEA2 | 0.846±0.013 | 0.0157±0.0015 | 2 |
| MOGWO | 0.835±0.016 | 0.0182±0.0018 | 3 |
| MOEA/D | 0.823±0.018 | 0.0235±0.0023 | 4 |

## 🎨 可视化

### 自动生成图表

```bash
python visualize_comparison.py
```

**生成**：
- `pareto_front_comparison.png` - Pareto前沿对比图
- `hv_comparison.png` - HV箱线图
- `igd_comparison.png` - IGD箱线图
- `statistical_test_report.txt` - 统计检验报告

### 手动绘图（Python）

```python
import numpy as np
import matplotlib.pyplot as plt

# 读取Pareto前沿
data = np.loadtxt('output/.../NSGAII/AlgorithmParetoFront.txt', skiprows=4)
plt.scatter(data[:, 0], data[:, 1], label='NSGA-II')

# 重复其他算法...

plt.xlabel('Cmax')
plt.ylabel('Energy')
plt.legend()
plt.savefig('comparison.png', dpi=300)
```

## 🔍 故障排除

### 编译失败

```bash
mvn clean install
```

### 内存不足

```bash
export MAVEN_OPTS="-Xmx8g"
```

### 运行太慢

1. 减少重复次数：`REPEAT_TIMES = 5`
2. 降低运行时间：`MAX_RUN_TIME = 3.0`
3. 减小种群：`POPULATION_SIZE = 50`

### Python画图失败

```bash
pip install numpy matplotlib seaborn scipy pandas
```

## 📚 文档索引

| 文档 | 用途 |
|------|------|
| `对比实验快速指南.md` | ⭐ 3步完成实验 |
| `算法对比实验使用说明.md` | 📖 详细使用文档 |
| `对比实验README.md` | 📦 本文件（项目概述） |

## 💡 提示

1. **首次运行**：选择模式1（单个算例），验证配置
2. **论文实验**：选择模式5（全部算例），增加重复次数到30
3. **结果分析**：重点关注 `ComparisonReport.txt`
4. **论文写作**：使用 `AlgorithmParetoFront.txt` 画图
5. **统计检验**：使用 `visualize_comparison.py` 自动生成

## 📮 技术支持

遇到问题？
1. 查看 `ComparisonReport.txt` 的详细数据
2. 检查控制台的运行日志
3. 参考 `算法对比实验使用说明.md`

---

**祝实验顺利！** 🎉

如有疑问，欢迎查阅详细文档或查看代码注释。
