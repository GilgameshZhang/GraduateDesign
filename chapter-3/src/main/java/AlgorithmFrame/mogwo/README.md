# MOGWO - 多目标灰狼优化算法

## 算法简介

MOGWO (Multi-Objective Grey Wolf Optimizer) 是一种基于灰狼捕猎行为的多目标优化算法，由Mirjalili等人于2016年提出。

### 核心思想

灰狼的社会层级：
- **Alpha (α)**: 支配地位最高，决策者
- **Beta (β)**: 第二层级，辅助Alpha决策
- **Delta (δ)**: 第三层级
- **Omega (ω)**: 其他狼群成员

MOGWO模拟灰狼的捕猎过程，通过三个领导者引导狼群逐步接近猎物（最优解）。

### 算法特点

1. **外部存档机制**: 维护非支配解集
2. **网格划分策略**: 保持解的多样性
3. **自适应系数**: 平衡探索与开发能力
4. **多领导者引导**: Alpha、Beta、Delta协同指导种群进化

### 位置更新公式

```
D_α = |C₁ · X_α - X|
D_β = |C₂ · X_β - X|  
D_δ = |C₃ · X_δ - X|

X₁ = X_α - A₁ · D_α
X₂ = X_β - A₂ · D_β
X₃ = X_δ - A₃ · D_δ

X(t+1) = (X₁ + X₂ + X₃) / 3
```

其中：
- `A = 2a·r₁ - a`: 探索/开发系数，a从2线性递减到0
- `C = 2·r₂`: 随机权重系数
- `r₁, r₂`: [0,1]的随机数

## 快速开始

### 最简单的使用方式

```java
import AlgorithmFrame.mogwo.*;

public class Test {
    public static void main(String[] args) {
        // 问题文件
        String problemFile = "src/main/resources/instance/energy/J20P3B2D5_energy.txt";
        
        // 输出目录
        String outputDir = "output/mogwo";
        
        // 运行MOGWO（默认配置：双目标Cmax+能耗）
        MOGWORunner.quickRun(
            problemFile,    // 问题文件
            outputDir,      // 输出目录
            100,           // 种群大小
            500,           // 最大迭代次数
            12345L         // 随机种子
        );
    }
}
```

### 自定义配置

```java
import AlgorithmFrame.mogwo.*;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

public class CustomTest {
    public static void main(String[] args) throws Exception {
        // 1. 加载问题
        File instanceFile = new File("src/main/resources/instance/energy/J20P3B2D5_energy.txt");
        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        // 2. 定义优化目标
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),           // 最小化完工时间
            new MOEvaluator.TotalEnergyConsumption(problem)    // 最小化能耗
        );
        
        // 3. 创建MOGWO算法
        MOGWO mogwo = new MOGWO(problem, objectives);
        
        // 4. 设置参数
        mogwo.setPopulationSize(100);        // 种群大小
        mogwo.setArchiveSize(100);           // 存档大小
        mogwo.setMaxGenerations(500);        // 最大迭代次数
        mogwo.setGridDivisions(10);          // 网格划分数
        mogwo.setSeed(12345L);               // 随机种子
        
        // 5. 运行算法
        List<MOIndividual> paretoFront = mogwo.solve();
        
        // 6. 输出结果
        System.out.println("Pareto前沿大小: " + paretoFront.size());
        for (int i = 0; i < Math.min(5, paretoFront.size()); i++) {
            MOIndividual ind = paretoFront.get(i);
            System.out.printf("解%d: Cmax=%.2f, Energy=%.2f\n",
                i + 1, ind.objectives[0], ind.objectives[1]);
        }
    }
}
```

### 启用局部搜索

```java
// 创建MOGWO并启用局部搜索
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

## 参数说明

### 基础参数

| 参数 | 说明 | 推荐值 | 范围 |
|------|------|--------|------|
| `populationSize` | 种群大小（灰狼数量） | 100 | 50-200 |
| `archiveSize` | 外部存档大小 | 100 | 等于种群大小 |
| `maxGenerations` | 最大迭代次数 | 500 | 100-1000 |
| `maxRunTimeMinutes` | 最大运行时间（分钟） | 5.0 | 1-30 |
| `gridDivisions` | 网格划分数 | 10 | 5-20 |

### 参数调优建议

**小规模问题（J ≤ 20）**：
```java
populationSize = 50;
archiveSize = 50;
maxGenerations = 300;
gridDivisions = 10;
```

**中等规模问题（20 < J ≤ 50）**：
```java
populationSize = 100;
archiveSize = 100;
maxGenerations = 500;
gridDivisions = 10;
```

**大规模问题（J > 50）**：
```java
populationSize = 200;
archiveSize = 200;
maxGenerations = 1000;
gridDivisions = 15;
```

## 完整示例

### 示例1: 运行单个算例

```java
import AlgorithmFrame.mogwo.*;

public class Example1 {
    public static void main(String[] args) {
        MOGWORunner.quickRun(
            "src/main/resources/instance/energy/J20P3B2D5_energy.txt",
            "output/mogwo/example1",
            100, 500, 12345L
        );
    }
}
```

### 示例2: 批量运行多个算例

```java
import AlgorithmFrame.mogwo.*;

public class Example2 {
    public static void main(String[] args) {
        String[] instances = {
            "J20P3B2D5_energy.txt",
            "J50P5B4D10_energy.txt",
            "J100P4B2D10_energy.txt"
        };
        
        for (String instanceName : instances) {
            String problemFile = "src/main/resources/instance/energy/" + instanceName;
            String outputDir = "output/mogwo/" + instanceName.replace(".txt", "");
            
            System.out.println("\n运行算例: " + instanceName);
            MOGWORunner.quickRun(problemFile, outputDir, 100, 500, 12345L);
        }
    }
}
```

### 示例3: MOGWO与NSGA-II对比

```java
import AlgorithmFrame.mogwo.*;
import AlgorithmFrame.nsgaii.*;
import ProblemFrame.*;
import ProgramEntity.*;
import java.io.File;
import java.util.*;

public class Example3 {
    public static void main(String[] args) throws Exception {
        // 加载问题
        File instanceFile = new File("src/main/resources/instance/energy/J20P3B2D5_energy.txt");
        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        // 定义目标
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        );
        
        // 统一参数
        int popSize = 100;
        int maxGen = 500;
        long seed = 12345L;
        
        // 运行MOGWO
        System.out.println("运行 MOGWO...");
        MOGWO mogwo = new MOGWO(problem, objectives);
        mogwo.setPopulationSize(popSize);
        mogwo.setArchiveSize(popSize);
        mogwo.setMaxGenerations(maxGen);
        mogwo.setSeed(seed);
        
        long startTime = System.currentTimeMillis();
        List<MOIndividual> mogwoPF = mogwo.solve();
        long mogwoTime = System.currentTimeMillis() - startTime;
        
        // 运行NSGA-II
        System.out.println("\n运行 NSGA-II...");
        NSGAII nsgaii = new NSGAII(problem, objectives);
        nsgaii.setPopulationSize(popSize);
        nsgaii.setMaxGenerations(maxGen);
        nsgaii.setSeed(seed);
        
        startTime = System.currentTimeMillis();
        List<MOIndividual> nsgaiiPF = nsgaii.solve();
        long nsgaiiTime = System.currentTimeMillis() - startTime;
        
        // 对比结果
        System.out.println("\n========== 对比结果 ==========");
        System.out.printf("MOGWO:   PF大小=%d, 时间=%.2fs\n", 
            mogwoPF.size(), mogwoTime / 1000.0);
        System.out.printf("NSGA-II: PF大小=%d, 时间=%.2fs\n", 
            nsgaiiPF.size(), nsgaiiTime / 1000.0);
    }
}
```

### 示例4: 使用配置类运行

```java
import AlgorithmFrame.mogwo.*;

public class Example4 {
    public static void main(String[] args) {
        // 创建配置
        MOGWORunner.MOGWOConfig config = new MOGWORunner.MOGWOConfig();
        
        // 基础参数
        config.populationSize = 100;
        config.archiveSize = 100;
        config.maxGenerations = 500;
        config.seed = 12345L;
        
        // 目标选择
        config.optimizeCmax = true;
        config.optimizeEnergy = true;
        config.optimizeTardiness = false;
        
        // MOGWO特定参数
        config.gridDivisions = 10;
        config.packingQMode = MOEvaluator.PackingQMode.LAST_BATCH_MIN;
        
        // 局部搜索
        config.enableLocalSearch = true;
        config.localSearchInterval = 10;
        config.localSearchL = 10;
        config.localSearchEta = 0.10;
        
        // 运行
        MOGWORunner.fullRun(
            "src/main/resources/instance/energy/J20P3B2D5_energy.txt",
            "output/mogwo/example4",
            config
        );
    }
}
```

## 输出结果

运行后会生成以下文件：

```
output/mogwo/
├── MOGWO_pareto_front.csv    # Pareto前沿数据
└── ...
```

**CSV文件格式**：
```csv
Solution,Cmax,Energy,PackingQ,BatchCount
1,1234.56,5678.90,0.8523,12
2,1198.45,5812.34,0.8301,13
...
```

## 算法流程

```
1. 初始化灰狼种群（使用多样化策略）
2. 评价所有个体
3. 初始化外部存档（非支配解集）
4. For each generation:
   4.1 计算自适应参数 a = 2 - 2*t/T
   4.2 从存档中选择Alpha、Beta、Delta
   4.3 For each wolf:
       - 基于三个领导者更新位置
       - 修复并评价新解
       - 贪婪选择保留
   4.4 更新外部存档（维护非支配性）
   4.5 若存档超容量，使用网格截断
   4.6 若启用局部搜索，执行局部搜索
5. 返回Pareto前沿
```

## 与其他算法的比较

| 特性 | MOGWO | NSGA-II | SPEA2 | MOEA/D |
|------|-------|---------|-------|--------|
| 选择机制 | 领导者引导 | 拥挤距离排序 | 适应度+密度 | 邻域协作 |
| 多样性维护 | 网格机制 | 拥挤距离 | k近邻密度 | 权重向量 |
| 存档策略 | 外部存档 | 精英保留 | 外部存档 | 无存档 |
| 参数复杂度 | 低 | 中 | 中 | 高 |
| 收敛速度 | 快 | 中 | 中 | 快 |
| 适用场景 | 通用 | 通用 | 多目标3+ | 规则前沿 |

### MOGWO的优势

1. **简单高效**: 参数少，易于调试
2. **收敛快速**: 三个领导者加速收敛
3. **多样性好**: 网格机制有效维持分布
4. **鲁棒性强**: 对参数不敏感

### MOGWO的劣势

1. 对不规则Pareto前沿的处理不如SPEA2
2. 网格划分在高维目标空间效率降低
3. 领导者选择可能陷入局部最优

## 算法改进建议

### 1. 自适应网格划分

```java
// 根据目标数量自适应调整网格数
int adaptiveGridDivisions = Math.max(5, 20 - objectives.size() * 3);
mogwo.setGridDivisions(adaptiveGridDivisions);
```

### 2. 混合局部搜索

```java
// 每隔一定代数执行局部搜索
mogwo.enableLocalSearch(true, 10);
```

### 3. 多策略种群初始化

算法已实现多样化初始化策略：
- RANDOM_RANDOM_RANDOM
- AREA_DESC_SPT_RANDOM
- HEIGHT_DESC_ROULETTE_LOADBALANCE
- RANDOM_ROULETTE_ROULETTE

## 常见问题

### Q1: MOGWO和NSGA-II哪个更好？

A: 取决于具体问题：
- **收敛速度**: MOGWO通常更快
- **多样性**: NSGA-II在复杂前沿上更均匀
- **参数调试**: MOGWO更简单
- **建议**: 对于本问题，两者都适用，可以都尝试

### Q2: 如何选择网格划分数？

A: 推荐值：
- 双目标: gridDivisions = 10
- 三目标: gridDivisions = 7-8
- 四目标及以上: gridDivisions = 5-6

### Q3: 种群大小如何设置？

A: 根据问题规模：
- J ≤ 20: popSize = 50
- 20 < J ≤ 50: popSize = 100
- J > 50: popSize = 150-200

### Q4: 是否需要启用局部搜索？

A: 
- **不启用**: 算法快速，适合初步探索
- **启用**: 解质量更高，但耗时更长
- **建议**: 对比实验时可以不启用，最终求解时启用

## 参考文献

```
Mirjalili, S., Saremi, S., Mirjalili, S. M., & Coelho, L. D. S. (2016). 
Multi-objective grey wolf optimizer: A novel algorithm for multi-criterion optimization. 
Expert Systems with Applications, 47, 106-119.
```

## 联系方式

如有问题，请联系作者或查阅源代码注释。
