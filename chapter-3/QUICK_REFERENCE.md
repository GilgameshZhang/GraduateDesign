# 能耗优化 - 快速参考

## 🔧 核心公式

### 关机判定
```
gap >= T_warmup + T_breakEven
其中 T_breakEven = E_switch / P_idle
```

### 能耗计算
```
E_run = P_run * duration
E_gap = E_switch (关机) 或 P_idle * gap (待机)
E_total = sum(E_run + E_gap)
```

## 💻 代码速查

### 1. 基本使用
```java
// 创建能耗目标
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 添加到目标列表
objectives.add(energyObj);

// 评价
evaluator.evaluate(individual, problem, operationMatrix, objectives);

// 获取能耗
double energy = individual.objectives[1];
```

### 2. 自定义参数
```java
PowerParameters params = new PowerParameters(
    5.0,    // P_run
    0.5,    // P_idle
    2.0,    // E_switch
    5.0,    // T_warmup
    true    // allowSwitch
);
energyObj.setMachineParameters(machineId, params);
```

### 3. 对比实验
```java
// 策略1: 开关机
MOEvaluator.TotalEnergyConsumption e1 = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 策略2: 纯待机
MOEvaluator.TotalEnergyConsumption e2 = 
    new MOEvaluator.TotalEnergyConsumption(problem, false, false);
```

## 📊 默认参数

| 设备 | P_run | P_idle | E_switch | T_warmup | 允许关机 |
|------|-------|--------|----------|----------|----------|
| 打印机 | 5.0 | 0.5 | 2.0 | 5.0 | ✓ |
| 批处理 | 3.0 | 0.8 | 5.0 | 20.0 | ✗ |
| 离散机 | 2.0 | 0.3 | 1.0 | 3.0 | ✓ |

## ✅ 验证清单

- [ ] 能耗 >= 0
- [ ] 长间隔关机（gap大 → E≈E_switch）
- [ ] 短间隔待机（gap小 → E≈P_idle*gap）
- [ ] 不允许关机设备始终待机
- [ ] Pareto前沿合理（能耗-makespan权衡）

## 🐛 常见问题

**Q: 总能耗为0？**
A: 检查`operationMatrix`是否正确填充

**Q: 没有关机？**
A: 检查`gap >= T_warmup + T_breakEven`

**Q: 编译错误？**
A: 确保复制了`Chromosome.java`, `CaculateFitness.java`

## 📁 文件清单

```
chapter-3/
├── src/main/java/ProblemFrame/
│   ├── PowerParameters.java       ✓ 新增
│   ├── EnergyCalculator.java      ✓ 新增
│   ├── MOEvaluator.java           ✓ 已更新
│   ├── MOIndividual.java          ✓ 已有
│   ├── Chromosome.java            ⚠️ 需从chapter-2复制
│   ├── CaculateFitness.java       ⚠️ 需从chapter-2复制
│   └── SkyLinePacking.java        ⚠️ 需从chapter-2复制
├── src/test/java/
│   └── EnergyCalculatorTest.java  ✓ 新增
├── ENERGY_OPTIMIZATION_README.md  ✓ 新增
└── QUICK_REFERENCE.md             ✓ 本文件
```

## 🚀 快速开始

```bash
# 1. 运行测试
mvn test -Dtest=EnergyCalculatorTest

# 2. 运行示例
mvn exec:java -Dexec.mainClass="AlgorithmFrame.nsgaii.EnergyOptimizationExample"

# 3. 运行NSGA-II（需先复制缺失文件）
mvn exec:java -Dexec.mainClass="AlgorithmFrame.nsgaii.NSGAII"
```

## 📞 帮助

详细文档：`ENERGY_OPTIMIZATION_README.md`

