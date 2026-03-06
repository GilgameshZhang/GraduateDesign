# PSO集成N1-N5邻域搜索策略说明

## 已完成的修改

成功将GA算法中的N1-N5邻域搜索策略集成到PSO算法中，实现混合PSO算法。

---

## 修改内容

### 1. **Particle.java中添加的新方法**

添加了`localSearchWithN1N5()`方法，封装对GA局部搜索策略的调用：

```java
public boolean localSearchWithN1N5(Problem problem, 
                                   CaculateFitness caculateFitness,
                                   ProgramEntity.Operation[][] operationMatrix,
                                   AlgorthmFrame.ga.ChromosomeOperation chromOps,
                                   int maxIterations)
```

**功能**：
- 将粒子转换为染色体
- 调用GA的LocalSearch方法（包含N1-N5五个邻域动作）
- 如果找到更优解，更新粒子
- 返回是否改进

### 2. **PSO.java中的修改**

#### 2.1 添加成员变量

```java
private ChromosomeOperation chromOps;  // 染色体操作类
private GAParameters gaParams;         // GA参数配置
```

#### 2.2 在构造函数中初始化

```java
this.gaParams = GAParameters.getDefaultParameters();
this.chromOps = new ChromosomeOperation(input, r, caculateFitness, gaParams);
```

#### 2.3 修改主循环中的改进策略

原有的简单swap/insert操作已替换为GA的N1-N5邻域搜索：

---

## N1-N5邻域搜索策略详解

### N1: 基于打印时间的跨机移动
- **目标**: 打印段优化
- **策略**: 从打印完工时间最长的机器中，用轮盘赌选择零件（高度越高权重越大），移动到能满足要求且打印完工时间最短的机器
- **候选数量**: 默认10个

### N2: 基于面积占用率的跨机移动  
- **目标**: 打印段优化
- **策略**: 从面积占用率最低的机器中，用轮盘赌选择零件（面积越大权重越大），移动到面积占用率更高的机器
- **候选数量**: 默认10个

### N3: 相邻批次交换
- **目标**: 打印段优化
- **策略**: 交换同一台打印机上相邻批次的打印顺序
- **候选数量**: 默认5个

### N4: 关键块首尾相邻工序交换
- **目标**: 离散处理阶段优化
- **策略**: 识别关键路径上的关键块，交换关键块首尾位置的相邻工序
- **候选数量**: 默认8个

### N5: 关键工序机器重分配
- **目标**: 离散处理阶段优化  
- **策略**: 对关键路径上的工序，用轮盘赌选择并重新分配机器
- **候选数量**: 默认6个

---

## PSO中的三层搜索策略

### 策略1：轻量级搜索（每次迭代）
```java
// 对gbest进行N1-N5搜索，最大迭代5次
gBest.localSearchWithN1N5(input, caculateFitness, operationMatrix, chromOps, 5)
```

**说明**：
- 频率：每次PSO迭代都执行
- 强度：低（5次内部迭代）
- 目标：对全局最优持续微调

### 策略2：中等强度搜索（每5代）
```java
// 对所有粒子的pbest进行N1-N5搜索，最大迭代10次
for (Particle p : swarm) {
    // 临时替换为pbest
    // 执行N1-N5搜索
    // 如果改进，更新pbest
}
```

**说明**：
- 频率：每5代PSO迭代执行一次
- 强度：中（10次内部迭代）
- 目标：提升所有粒子的个体最优质量

### 策略3：深度搜索（每20代）
```java
// 对前20%的优秀粒子进行深度N1-N5搜索，最大迭代20次
int deepSearchCount = (int)(swarm.length * 0.2);
for (int i = 0; i < deepSearchCount; i++) {
    sortedSwarm[i].localSearchWithN1N5(input, caculateFitness, operationMatrix, chromOps, 20)
}
```

**说明**：
- 频率：每20代PSO迭代执行一次
- 强度：高（20次内部迭代）
- 目标：对优秀个体进行深度挖掘

---

## 算法流程图

```
PSO主循环（每次迭代）
│
├─ 更新速度和位置
├─ 评估适应度
├─ 更新pbest和gbest
│
└─ 邻域搜索策略
    │
    ├─ 每次迭代：对gbest轻量级N1-N5搜索（5次）
    │   └─ 轮换使用N1→N2→N3→N4→N5
    │
    ├─ 每5代：对所有pbest中等强度N1-N5搜索（10次）
    │   └─ 轮换使用N1→N2→N3→N4→N5
    │
    └─ 每20代：对前20%粒子深度N1-N5搜索（20次）
        └─ 轮换使用N1→N2→N3→N4→N5
```

---

## 参数配置

### GA参数（影响N1-N5搜索）

```java
GAParameters params = GAParameters.getDefaultParameters();
```

关键参数：
- `localSearchMaxIter`: 局部搜索最大迭代次数
- `localSearchNoImprove`: 连续无改进停止阈值（默认3）
- `n1n2Candidates`: N1/N2候选数量（默认10）
- `n3Candidates`: N3候选数量（默认5）
- `n4Candidates`: N4候选数量（默认8）
- `n5Candidates`: N5候选数量（默认6）

### 搜索频率和强度

可根据问题规模调整：

**小规模问题（<20个工件）**：
- 策略1：每次迭代，3次内部迭代
- 策略2：每3代，8次内部迭代
- 策略3：每10代，15次内部迭代

**中规模问题（20-50个工件）**：
- 策略1：每次迭代，5次内部迭代（当前配置）
- 策略2：每5代，10次内部迭代（当前配置）
- 策略3：每20代，20次内部迭代（当前配置）

**大规模问题（>50个工件）**：
- 策略1：每2次迭代，5次内部迭代
- 策略2：每10代，12次内部迭代
- 策略3：每30代，25次内部迭代

---

## 优势分析

### 1. 多样化的邻域结构
- N1/N2/N3针对打印段优化
- N4/N5针对离散处理段优化
- 覆盖了问题的不同方面

### 2. 分级搜索策略
- 轻量级：保持高效率
- 中等强度：平衡探索与开发
- 深度搜索：充分挖掘优秀解

### 3. 针对性强
- N1: 针对打印时间瓶颈
- N2: 针对装载率优化
- N3: 针对批次顺序
- N4: 针对关键路径
- N5: 针对机器分配

### 4. 与PSO协同
- PSO负责全局搜索（探索）
- N1-N5负责局部搜索（开发）
- 两者互补，效果更好

---

## 预期效果

1. **收敛速度**：相比纯PSO更快
2. **解的质量**：makespan更小
3. **稳定性**：多次运行结果更稳定
4. **适用性**：对不同规模问题都有效

---

## 调试建议

可以在代码中添加更详细的日志输出，观察每种邻域动作的效果：

```java
// 在Particle.java的localSearchWithN1N5方法中
double beforeMakespan = caculateFitness.evaluate(c, problem, operationMatrix);
chromOps.LocalSearch(c, maxIterations);
double afterMakespan = 1000000.0 / c.fitness;

if (afterMakespan < beforeMakespan) {
    System.out.printf("  N1-N5搜索改进: %.2f → %.2f (改进%.2f)\n", 
                     beforeMakespan, afterMakespan, beforeMakespan - afterMakespan);
}
```

---

## 消融实验建议

可以通过GAParameters控制开关进行消融实验：

```java
// 实验1：不使用局部搜索
gaParams.enableLocalSearch = false;

// 实验2：只使用N1和N2（打印段优化）
gaParams.enableN1 = true;
gaParams.enableN2 = true;
gaParams.enableN3 = false;
gaParams.enableN4 = false;
gaParams.enableN5 = false;

// 实验3：只使用N4和N5（离散段优化）
gaParams.enableN1 = false;
gaParams.enableN2 = false;
gaParams.enableN3 = false;
gaParams.enableN4 = true;
gaParams.enableN5 = true;

// 实验4：使用所有邻域策略（完整版）
// 使用默认参数即可
```

---

## 总结

成功将GA中成熟的N1-N5邻域搜索策略集成到PSO算法中，形成混合PSO-LS（PSO with Local Search）算法。该算法结合了PSO的全局搜索能力和局部搜索的精细优化能力，预期能够获得更好的优化效果。

