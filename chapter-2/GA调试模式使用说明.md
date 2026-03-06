# 遗传算法调试模式使用说明

## 概述

为了方便观察和测试遗传算法中的**交叉（Crossover）**和**变异（Mutation）**操作，我们在 `GA.java` 中添加了调试打印功能。

## 调试参数说明

在 `GA.java` 类中，有以下可配置的静态参数：

```java
GA.DEBUG_MODE = true/false;              // 总调试开关
GA.PRINT_CROSSOVER = true/false;         // 是否打印交叉操作
GA.PRINT_MUTATION = true/false;          // 是否打印变异操作
GA.PRINT_INTERVAL = N;                   // 打印间隔（每N代打印一次）
GA.PRINT_CHROMOSOME_LENGTH = M;          // 打印染色体的前M个基因
```

### 参数详解

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `DEBUG_MODE` | boolean | false | 总调试开关，必须开启才能打印调试信息 |
| `PRINT_CROSSOVER` | boolean | true | 是否打印交叉操作前后的染色体变化 |
| `PRINT_MUTATION` | boolean | true | 是否打印变异操作前后的染色体变化 |
| `PRINT_INTERVAL` | int | 50 | 打印间隔，每N代打印一次（避免输出过多） |
| `PRINT_CHROMOSOME_LENGTH` | int | 10 | 显示染色体的前M个基因（避免输出过长） |

## 使用方法

### 方法1：在测试程序中配置

```java
// 在运行GA之前设置调试参数
GA.DEBUG_MODE = true;
GA.PRINT_CROSSOVER = true;
GA.PRINT_MUTATION = true;
GA.PRINT_INTERVAL = 10;  // 每10代打印一次
GA.PRINT_CHROMOSOME_LENGTH = 15;

// 然后运行算法
GA ga = new GA(problem);
Solution solution = ga.solve();
```

### 方法2：使用提供的测试程序

直接运行 `TestGAWithDebug.java`：

```bash
cd chapter-2
javac -cp "src/main/java" src/test/java/TestGAWithDebug.java
java -cp "src/main/java;src/test/java" test.TestGAWithDebug
```

## 输出示例

### 交叉操作输出示例

```
========== 代数 0: 交叉操作示例 ==========
  交叉前-父代0: OS=[0,1,2,3,4,0,1,2,3,4,...], MS=[1,2,1,2,1,3,4,3,4,3,...], fitness=0.0012
  交叉前-母代1: OS=[2,3,4,0,1,2,3,4,0,1,...], MS=[2,1,2,1,2,4,3,4,3,4,...], fitness=0.0011
  交叉后-父代0: OS=[0,3,2,3,1,0,1,2,3,4,...], MS=[1,1,1,2,2,3,4,3,4,3,...], fitness=0.0012
  交叉后-母代1: OS=[2,1,4,0,4,2,3,4,0,1,...], MS=[2,2,2,1,1,4,3,4,3,4,...], fitness=0.0011
================================================
```

### 变异操作输出示例

```
========== 代数 0: 变异操作示例 ==========
  变异前-个体5: OS=[1,2,0,3,4,1,2,0,3,4,...], MS=[2,1,1,2,1,4,3,3,4,3,...], fitness=0.0010
  变异后-个体5: OS=[1,2,4,3,0,1,2,0,3,4,...], MS=[2,1,1,2,1,4,3,5,4,3,...], fitness=0.0010
================================================

  本代共发生 5 次变异
```

## 染色体信息解读

- **gene_OS（操作序列）**: 表示工序的执行顺序
  - 数字表示工件编号
  - 例如 `[0,1,2,3,4]` 表示依次执行工件0、1、2、3、4的工序

- **gene_MS（机器选择）**: 表示每个工序选择的机器
  - 数字表示机器编号（1-based）
  - 例如 `[1,2,1,2,1]` 表示前5个工序分别选择机器1、2、1、2、1

- **fitness（适应度）**: 染色体的适应度值
  - 值越大表示解越好
  - fitness = 1 / makespan

## 调试建议

### 1. 观察前几代的变化
```java
GA.PRINT_INTERVAL = 1;  // 每代都打印
```
适合观察算法初期的种群演化过程。

### 2. 只关注交叉或变异
```java
GA.PRINT_CROSSOVER = true;
GA.PRINT_MUTATION = false;  // 只看交叉，不看变异
```

### 3. 显示完整染色体
```java
GA.PRINT_CHROMOSOME_LENGTH = 100;  // 显示更多基因
```
适合小规模问题的详细分析。

### 4. 定期抽样检查
```java
GA.PRINT_INTERVAL = 50;  // 每50代打印一次
```
适合长时间运行的算法，定期检查演化情况。

## 注意事项

1. **输出量控制**: 如果 `PRINT_INTERVAL` 设置过小（如1），会产生大量输出，影响性能
2. **大规模问题**: 对于大规模问题，建议适当增大 `PRINT_INTERVAL` 和减小 `PRINT_CHROMOSOME_LENGTH`
3. **性能影响**: 调试模式会略微影响算法性能，生产环境建议关闭 `DEBUG_MODE`

## 进阶用法

### 添加自定义打印函数

如果需要更详细的染色体信息，可以使用 `printChromosomeDetailed` 方法：

```java
// 在GA.java中的适当位置添加
if (DEBUG_MODE && gen == 0) {
    printChromosomeDetailed("初始最优染色体:", best);
}
```

这会输出完整的染色体信息，包括所有基因位的值。

## 常见问题

**Q: 为什么没有看到任何调试输出？**
A: 确保 `GA.DEBUG_MODE = true` 已设置，且当前代数满足 `PRINT_INTERVAL` 的条件。

**Q: 如何只打印特定代数的信息？**
A: 可以设置 `PRINT_INTERVAL` 为该代数，或在代码中添加条件判断。

**Q: 染色体长度太长，无法完整显示？**
A: 增大 `PRINT_CHROMOSOME_LENGTH` 的值，或使用 `printChromosomeDetailed` 方法。

## 示例代码

完整的测试示例请参考：
- `src/test/java/TestGAWithDebug.java`

## 总结

通过调试模式，您可以：
- ✅ 观察交叉操作如何交换父母代的基因
- ✅ 观察变异操作如何改变染色体
- ✅ 追踪种群演化过程
- ✅ 验证遗传算法的正确性
- ✅ 分析算法性能和收敛特性

