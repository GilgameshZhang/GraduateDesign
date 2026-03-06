# NSGA-II 快速使用指南

## 1. 前置要求

由于 chapter-3 是独立模块，需要从 chapter-2 复制一些必要的类：

### 需要复制的文件

从 `chapter-2/src/main/java/` 复制到 `chapter-3/src/main/java/`：

```
ProgramEntity/
├── Job.java
├── Item.java
├── Machine/
│   ├── Machine.java
│   ├── PrintMachine.java
│   ├── BathchMachine.java
│   └── DiscreteProcessingMachine.java
├── Operation.java
├── PlaceItem.java
├── Problem.java
├── SkyLine.java
└── Solution.java

ProblemFrame/
├── Chromosome.java  (如果需要完整功能)
├── CaculateFitness.java
├── InitializationStrategy.java (如果使用启发式初始化)
└── SkyLinePacking.java

util/
└── Input.java (用于读取算例)
```

**简化方案**：如果你只想测试 NSGA-II 的核心功能（排序、拥挤距离等），可以先不复制这些文件，只运行单元测试。

---

## 2. 运行单元测试（无需chapter-2依赖）

```bash
cd chapter-3
mvn test
```

这会运行以下测试：
- ✅ 支配关系判断
- ✅ 快速非支配排序
- ✅ 拥挤距离计算
- ✅ 环境选择
- ✅ tie-breaker机制
- ✅ 锦标赛选择

---

## 3. 完整使用示例（需要chapter-2依赖）

### 示例1: 双目标优化（Cmax + 能耗）

```java
import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOIndividual;
import ProgramEntity.Problem;
import util.Input;
import java.io.File;
import java.util.List;

public class SimpleNSGAIIExample {
    public static void main(String[] args) {
        // 1. 读取算例
        String instancePath = "src/main/resources/instance/J20P3B2D5_01.txt";
        Input input = new Input(new File(instancePath));
        Problem problem = input.getProblemDesFromFile();
        
        // 2. 创建NSGA-II算法（默认双目标：Cmax + 能耗）
        NSGAII nsgaii = new NSGAII(problem);
        
        // 3. 设置参数
        nsgaii.setPopulationSize(100);
        nsgaii.setCrossoverRate(0.9);
        nsgaii.setMutationRate(0.1);
        nsgaii.setMaxGenerations(200);
        nsgaii.setMaxRunTimeMinutes(3.0);
        
        // 4. 运行算法
        List<MOIndividual> paretoFront = nsgaii.solve();
        
        // 5. 输出结果
        System.out.println("\n找到 " + paretoFront.size() + " 个Pareto最优解:");
        for (int i = 0; i < Math.min(10, paretoFront.size()); i++) {
            MOIndividual ind = paretoFront.get(i);
            System.out.printf("解%2d: %s | packingQ=%.4f | batches=%d\n",
                i + 1, ind.objectivesToString(), ind.packingQ, ind.batchCount);
        }
    }
}
```

### 示例2: 三目标优化（Cmax + 能耗 + 等待时间）

```java
import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOEvaluator;
import ProblemFrame.MOIndividual;
import ProgramEntity.Problem;
import util.Input;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class ThreeObjectiveExample {
    public static void main(String[] args) {
        // 1. 读取算例
        String instancePath = "src/main/resources/instance/J20P3B2D5_01.txt";
        Input input = new Input(new File(instancePath));
        Problem problem = input.getProblemDesFromFile();
        
        // 2. 定义三个目标
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),      // Cmax
            new MOEvaluator.TotalEnergyConsumption(),     // 能耗
            new MOEvaluator.TotalBatchWaitingTime()       // 等待时间
        );
        
        // 3. 创建NSGA-II实例
        NSGAII nsgaii = new NSGAII(problem, objectives);
        
        // 4. 配置packingQ计算模式
        nsgaii.setPackingQMode(MOEvaluator.PackingQMode.LAST_BATCH_AVG);
        
        // 5. 运行
        List<MOIndividual> paretoFront = nsgaii.solve();
        
        // 6. 分析结果
        System.out.println("\nPareto前沿统计:");
        System.out.println("----------------------------------------");
        
        // 找到每个目标的极值
        double minCmax = Double.POSITIVE_INFINITY;
        double minEnergy = Double.POSITIVE_INFINITY;
        double minWait = Double.POSITIVE_INFINITY;
        
        for (MOIndividual ind : paretoFront) {
            minCmax = Math.min(minCmax, ind.objectives[0]);
            minEnergy = Math.min(minEnergy, ind.objectives[1]);
            minWait = Math.min(minWait, ind.objectives[2]);
        }
        
        System.out.printf("最小Cmax: %.2f\n", minCmax);
        System.out.printf("最小能耗: %.2f\n", minEnergy);
        System.out.printf("最小等待时间: %.2f\n", minWait);
    }
}
```

### 示例3: 自定义能耗系数

```java
import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOEvaluator;
import ProblemFrame.MOIndividual;
import ProgramEntity.Problem;
import util.Input;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CustomEnergyExample {
    public static void main(String[] args) {
        // 读取算例
        String instancePath = "src/main/resources/instance/J20P3B2D5_01.txt";
        Input input = new Input(new File(instancePath));
        Problem problem = input.getProblemDesFromFile();
        
        // 自定义能耗目标（不同的功率系数）
        MOEvaluator.TotalEnergyConsumption energyObj = 
            new MOEvaluator.TotalEnergyConsumption(
                3.0,   // 打印功率系数（较高）
                1.0,   // 批处理功率系数
                0.5    // 离散加工功率系数（较低）
            );
        
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            energyObj
        );
        
        NSGAII nsgaii = new NSGAII(problem, objectives);
        List<MOIndividual> paretoFront = nsgaii.solve();
        
        System.out.println("使用自定义能耗模型，找到 " + 
            paretoFront.size() + " 个Pareto解");
    }
}
```

---

## 4. 验证 tie-breaker 是否工作

```java
import AlgorithmFrame.nsgaii.NSGAIIOperations;
import ProblemFrame.MOIndividual;
import java.util.*;

public class TieBreakerDemo {
    public static void main(String[] args) {
        Random r = new Random(123);
        
        // 创建一个包含拥挤距离相近个体的前沿
        List<MOIndividual> front = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            MOIndividual ind = new MOIndividual(r);
            ind.objectives = new double[]{
                1.0 + i * 0.5,
                10.0 - i * 0.5
            };
            ind.gene_OS = new int[]{0, 1, 2};
            ind.gene_MS = new int[]{1, 1, 1};
            
            // 设置不同的packingQ值
            ind.packingQ = 0.5 + r.nextDouble() * 0.3;
            ind.batchCount = 5 + r.nextInt(5);
            
            front.add(ind);
        }
        
        // 计算拥挤距离
        NSGAIIOperations.assignCrowdingDistance(front);
        
        System.out.println("拥挤距离计算结果:");
        for (int i = 0; i < front.size(); i++) {
            MOIndividual ind = front.get(i);
            System.out.printf("个体%2d: crowding=%8.4f, packingQ=%.4f, batches=%d\n",
                i, ind.crowdingDistance, ind.packingQ, ind.batchCount);
        }
        
        // 模拟截断场景（只保留5个）
        System.out.println("\n模拟截断（保留5个）:");
        
        // 按截断规则排序
        front.sort((a, b) -> a.compareForTruncation(b, 1e-6));
        
        System.out.println("截断后保留的个体:");
        for (int i = 0; i < Math.min(5, front.size()); i++) {
            MOIndividual ind = front.get(i);
            System.out.printf("保留%2d: crowding=%8.4f, packingQ=%.4f\n",
                i + 1, ind.crowdingDistance, ind.packingQ);
        }
    }
}
```

---

## 5. 查看算法统计数据

```java
import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOIndividual;
import ProgramEntity.Problem;
import util.Input;
import java.io.File;
import java.util.List;

public class StatisticsExample {
    public static void main(String[] args) {
        String instancePath = "src/main/resources/instance/J20P3B2D5_01.txt";
        Input input = new Input(new File(instancePath));
        Problem problem = input.getProblemDesFromFile();
        
        NSGAII nsgaii = new NSGAII(problem);
        nsgaii.setMaxGenerations(100);
        
        // 运行
        List<MOIndividual> paretoFront = nsgaii.solve();
        
        // 获取统计数据
        List<Integer> paretoSizeHistory = nsgaii.getParetoSizeHistory();
        List<Double> hypervolumeHistory = nsgaii.getHypervolomeHistory();
        
        // 输出演化过程
        System.out.println("\nPareto前沿大小演化:");
        for (int i = 0; i < Math.min(10, paretoSizeHistory.size()); i++) {
            System.out.printf("代数%3d: Pareto大小=%d", 
                (i + 1) * 10, paretoSizeHistory.get(i));
            
            if (i < hypervolumeHistory.size()) {
                System.out.printf(", 超体积=%.2f", hypervolumeHistory.get(i));
            }
            System.out.println();
        }
    }
}
```

---

## 6. 常见问题

### Q1: 编译错误 - 找不到 Problem 类

**原因**: 未复制 chapter-2 的依赖类。

**解决**: 
1. 复制上述"需要复制的文件"
2. 或者先运行单元测试（不需要依赖）

### Q2: packingQ 总是 0.0

**原因**: `Solution.rate` 字段未计算。

**解决**: 确保 `CaculateFitness.evaluate()` 中计算了批次的面积占用率。

### Q3: tie-breaker 没有生效

**检查**:
- delta 是否设置合理（推荐 1e-6 ~ 1e-4）
- packingQ 是否正确计算（不全为0）
- crowdingDistance 是否确实接近

### Q4: Pareto前沿大小一直是1

**可能原因**:
- 所有个体的目标值完全相同
- 目标函数计算有误

**调试**:
```java
for (MOIndividual ind : population) {
    System.out.println(ind.objectivesToString());
}
```

---

## 7. 下一步

1. ✅ 运行单元测试验证核心功能
2. ✅ 复制 chapter-2 的依赖类
3. ✅ 运行简单示例
4. ✅ 调整参数进行实验
5. ✅ 扩展自定义目标函数
6. ✅ 可视化Pareto前沿（TODO）

---

**祝使用愉快！** 🚀

