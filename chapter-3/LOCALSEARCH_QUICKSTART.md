# NSGA-II局部搜索快速入门

## 1分钟快速开始

### 启用局部搜索（3行代码）

```java
NSGAII nsgaii = new NSGAII(problem, objectives);
nsgaii.enableLocalSearch(true);  // 启用局部搜索
nsgaii.solve();
```

## 5分钟完整示例

```java
import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOEvaluator;
import ProgramEntity.Problem;
import util.ReadDataUtil;
import java.util.Arrays;

public class QuickStartExample {
    public static void main(String[] args) {
        // 1. 读取算例
        Problem problem = ReadDataUtil.getData("path/to/instance.txt");
        
        // 2. 创建NSGA-II（双目标：Cmax + 能耗）
        NSGAII nsgaii = new NSGAII(problem, Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        ));
        
        // 3. 配置基本参数
        nsgaii.setPopulationSize(100);
        nsgaii.setMaxGenerations(500);
        nsgaii.setMaxRunTimeMinutes(5.0);
        
        // 4. 启用局部搜索（每10代执行一次）
        nsgaii.enableLocalSearch(true, 10);
        
        // 5. 运行算法
        nsgaii.solve();
        
        // 6. 获取结果
        System.out.println("Pareto前沿大小: " + nsgaii.getParetoFront().size());
    }
}
```

## 高级配置（10分钟）

### 自定义局部搜索参数

```java
// 启用局部搜索并配置参数
nsgaii.enableLocalSearch(true, 5);  // 每5代执行一次

// 精细调参
nsgaii.setLocalSearchParameters(
    15,      // L: 每个精英15次尝试（默认10）
    0.15,    // eta: 选择15%精英（默认10%）
    0.08,    // epsE: T型允许能耗上升8%（默认5%）
    0.03,    // epsC: E型允许Cmax上升3%（默认2%）
    0.01,    // improvC: T型要求Cmax下降1%（默认0.5%）
    0.015    // improvE: E型要求能耗下降1.5%（默认1%）
);
```

### 参数说明

| 参数 | 含义 | 推荐范围 | 默认值 |
|------|------|----------|--------|
| interval | 局部搜索间隔（代数） | 5-20 | 10 |
| L | 每个精英的尝试次数 | 5-20 | 10 |
| eta | 精英选择比例 | 0.05-0.20 | 0.10 |
| epsE | T型允许能耗上升比例 | 0.03-0.10 | 0.05 |
| epsC | E型允许Cmax上升比例 | 0.01-0.05 | 0.02 |
| improvC | T型要求Cmax下降比例 | 0.003-0.02 | 0.005 |
| improvE | E型要求能耗下降比例 | 0.005-0.03 | 0.01 |

## 对比测试（15分钟）

```java
// 测试1: 不启用局部搜索
NSGAII nsgaii1 = new NSGAII(problem, objectives);
nsgaii1.setPopulationSize(100);
nsgaii1.setMaxGenerations(100);
long start1 = System.currentTimeMillis();
nsgaii1.solve();
long time1 = System.currentTimeMillis() - start1;

// 测试2: 启用局部搜索
NSGAII nsgaii2 = new NSGAII(problem, objectives);
nsgaii2.setPopulationSize(100);
nsgaii2.setMaxGenerations(100);
nsgaii2.enableLocalSearch(true);
long start2 = System.currentTimeMillis();
nsgaii2.solve();
long time2 = System.currentTimeMillis() - start2;

// 对比
System.out.println("不含LS - Pareto前沿: " + nsgaii1.getParetoFront().size() 
                 + ", 耗时: " + (time1/1000.0) + "秒");
System.out.println("含LS   - Pareto前沿: " + nsgaii2.getParetoFront().size() 
                 + ", 耗时: " + (time2/1000.0) + "秒");
```

## 常见问题

### Q1: 局部搜索会增加多少运行时间？
**A**: 通常增加20-50%，取决于L和eta参数。

### Q2: 什么时候应该启用局部搜索？
**A**: 
- ✅ 需要高质量Pareto前沿
- ✅ 有充足的运行时间预算
- ✅ 问题规模中等（20-100个工件）
- ❌ 快速原型测试
- ❌ 问题规模过大（>200个工件）

### Q3: 如何选择interval参数？
**A**: 
- 小问题（<30工件）：5-10代
- 中等问题（30-100工件）：10-15代
- 大问题（>100工件）：15-20代

### Q4: 局部搜索没有改善效果怎么办？
**A**: 尝试：
1. 增加L（尝试次数）
2. 调整epsilon参数（放宽接受条件）
3. 检查算子是否适合问题特点
4. 增加局部搜索频率（减小interval）

### Q5: 如何添加自定义算子？
**A**: 
```java
// 1. 实现算子
public class MyOperator extends AbstractOperator {
    public MyOperator(Random random) {
        super("MyOp", IndividualType.TIME_DEFICIENT, random);
    }
    
    @Override
    public Candidate tryApply(MOIndividual ind, Problem prob) {
        // 实现你的邻域操作
        Delta delta = new Delta();
        // ... 修改个体并记录delta
        return new Candidate(ind, delta, getName());
    }
}

// 2. 注册算子（需要修改NSGAII.initializeLocalSearch()）
operatorSelector.registerTimeOperator(new MyOperator(random));
```

## 预期效果

### 小规模问题（10-30工件）
- Pareto前沿质量提升：5-10%
- 运行时间增加：20-30%

### 中等规模问题（30-100工件）
- Pareto前沿质量提升：10-15%
- 运行时间增加：30-50%

### 大规模问题（>100工件）
- Pareto前沿质量提升：5-8%
- 运行时间增加：40-60%

## 性能优化建议

### 1. 调整精英比例（eta）
```java
// 小问题：选更多精英（15-20%）
nsgaii.setLocalSearchParameters(10, 0.15, ...);

// 大问题：选更少精英（5-8%）
nsgaii.setLocalSearchParameters(10, 0.05, ...);
```

### 2. 调整尝试次数（L）
```java
// 初期：更多尝试（15-20次）
// 后期：可以减少尝试（5-10次）

// 建议：保持默认10次
```

### 3. 调整局部搜索频率
```java
// 频繁LS（适合小问题）
nsgaii.enableLocalSearch(true, 5);

// 稀疏LS（适合大问题）
nsgaii.enableLocalSearch(true, 20);
```

## 输出解读

```
代数   50 | Pareto前沿大小:  15 | 耗时: 12.34秒
  局部搜索: 精英分类: T=8 (53.3%), E=7 (46.7%)
         ↑                  ↑              ↑
      执行LS            T型个体数      E型个体数
```

最终输出：
```
========================================
           算法运行完成
========================================
总代数: 500
总耗时: 123.45 秒
Pareto前沿大小: 25
----------------------------------------
局部搜索统计: 尝试=500, 接受=125 (25.0%), Pareto=80, ε-容忍=45
                 ↑        ↑        ↑         ↑          ↑
             总尝试    接受数   接受率   Pareto支配  ε-容忍接受
```

算子统计：
```
=== 算子统计 ===
时间向算子:
  T1-SimpleSwap[T] - 成功率: 28.50% (57/200)
                       ↑              ↑
                    成功率      (成功次数/总次数)
```

## 调试技巧

### 1. 查看分类结果
分类输出会显示精英个体的类型分布，理想情况是T和E大致均衡（40-60%）。

### 2. 监控成功率
- **>30%**: 算子效果好，可以考虑增加L
- **20-30%**: 正常范围
- **<20%**: 算子效果一般，可能需要调整参数或换算子

### 3. 检查接受率
- **>30%**: 可以考虑收紧epsilon参数
- **15-30%**: 正常范围
- **<15%**: 太严格，放宽epsilon参数

## 进阶使用

查看详细文档：
- [LOCALSEARCH_README.md](LOCALSEARCH_README.md) - 完整使用说明
- [LOCALSEARCH_IMPLEMENTATION_SUMMARY.md](LOCALSEARCH_IMPLEMENTATION_SUMMARY.md) - 实现总结

## 快速测试

```bash
# 编译
cd chapter-3
mvn clean compile

# 运行测试
mvn test -Dtest=LocalSearchNSGAIITest

# 或直接运行
java -cp target/classes:target/test-classes LocalSearchNSGAIITest
```

## 祝你使用愉快！

有问题？查看 [LOCALSEARCH_README.md](LOCALSEARCH_README.md) 获取更多帮助。
