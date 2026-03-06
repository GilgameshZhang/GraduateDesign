# N5 邻域动作：关键工序机器重分配 (Critical Operation Machine Reassignment)

## 时间
2025-12-19

## 动作描述

**N5 邻域动作**是一个**复合邻域**，针对离散处理段的关键路径工序，同时优化**机器分配**和**工序插入位置**。

---

## 一、核心概念

### 复合邻域 (Compound Neighborhood)

N5 是一个**复合邻域**，一次移动同时修改两层编码：
- **下层（gene_MS）**: 修改机器分配
- **上层（gene_OS）**: 调整工序插入位置

```
传统单层邻域：
  N1/N2: 只改 gene_MS（跨机移动）
  N3/N4: 只改 gene_OS（顺序调整）

复合邻域N5：
  同时改 gene_MS + gene_OS
  机器重分配 + 位置优化
```

### 约束条件

✅ **允许的操作**:
- 选取关键路径上的工序
- 在其可选机器集中重新选择机器
- 在新机器序列中找最优插入位置
- **一次只作用一个工序**

❌ **不允许的操作**:
- 同时移动多个工序
- 分配到不可行的机器
- 破坏工序依赖关系

---

## 二、动作原理

### 为什么需要 N5？

**场景问题**:
```
假设关键路径上有一个工序 Op1：
  当前: 在机器 M3 加工，排在位置 10
  
可能存在的问题：
  1. M3 是瓶颈机器（负载很重）
  2. Op1 在 M3 上等待时间长
  3. 但 Op1 也可以在 M4 上加工（更快或更空闲）

N4 的局限：
  - N4 只能调整 Op1 在 M3 上的位置
  - 无法将 Op1 移到 M4

N5 的能力：
  - 将 Op1 从 M3 移到 M4
  - 并在 M4 上找到最优插入位置
  - 可能大幅减少 makespan！
```

### 操作步骤

```
1. 识别关键路径
   │
2. 对关键路径上的每个离散工序：
   │
   ├─ 获取该工序的可选机器集
   │  (从 proDesMatrix 读取)
   │
   ├─ 对每个可选机器（除了当前机器）：
   │  │
   │  ├─ 找到新机器上合理的插入位置
   │  │  （考虑同一工件的其他工序位置）
   │  │
   │  └─ 生成候选: (工序, 新机器, 插入位置)
   │
3. 评估所有候选，接受第一个改进的
```

---

## 三、实现算法

### 1. 生成候选移动

```java
private List<MachineReassignmentMove> generateMovesN5(
    Chromosome chromosome, 
    Operation[][] operationMatrix, 
    int maxCandidates)
```

**步骤**:
```
1. 识别关键路径（复用 identifyCriticalPath）
   │
2. 遍历关键路径上的离散工序
   │
3. 对每个工序：
   │
   ├─ 从 proDesMatrix 获取可选机器集
   │  (加工时间 > 0 且 != MAX_VALUE)
   │
   ├─ 排除当前机器
   │
   ├─ 对每个可选机器：
   │  │
   │  ├─ 调用 generateInsertPositions()
   │  │  找到 3-5 个候选插入位置
   │  │
   │  └─ 创建 MachineReassignmentMove
   │
4. 返回候选列表（限制 maxCandidates）
```

### 2. 生成插入位置

```java
private List<Integer> generateInsertPositions(
    int[] os, int[] ms, 
    int operationPosition, 
    int newMachine, 
    List<Integer> sameJobPositions, 
    int jobCount)
```

**策略**:
```
如果新机器上没有其他工序：
  │
  ├─ 离散段开头
  ├─ 离散段中间
  └─ 离散段末尾

如果新机器上有其他工序：
  │
  ├─ 插入到第一个工序之前
  ├─ 插入到中间工序附近
  └─ 插入到最后一个工序之后

如果同一工件在新机器上有其他工序：
  │
  └─ 优先考虑插入到它们附近
     （保持工件内部工序的紧凑性）
```

### 3. 应用机器重分配

```java
private void applyMachineReassignment(Chromosome chromosome, MachineReassignmentMove move)
```

**步骤**:
```
1. 保存原始 gene_OS 和 gene_MS
   │
2. 修改机器分配
   ms[operationPosition] = newMachine
   │
3. 调整工序位置（如果需要）
   │
   如果 opPos < insertPos (向后移动):
   │
   └─ 将 opPos 到 insertPos 之间的元素前移
      [A, B, C, D, E]
       ↑     ↑
       op    insert
      
      → [B, C, A, D, E]
      
   如果 opPos > insertPos (向前移动):
   │
   └─ 将 insertPos 到 opPos 之间的元素后移
      [A, B, C, D, E]
          ↑  ↑
          insert op
      
      → [A, D, B, C, E]
```

---

## 四、数据结构

### MachineReassignmentMove 类

```java
private static class MachineReassignmentMove {
    int operationPosition;    // 工序在 gene_OS 中的位置
    int oldMachine;           // 原机器（1-based）
    int newMachine;           // 新机器（1-based）
    int newInsertPosition;    // 在新机器序列中的插入位置
    int[] originalOS;         // 保存原始基因（用于撤销）
    int[] originalMS;         // 保存原始基因（用于撤销）
}
```

**关键字段**:
- `oldMachine` / `newMachine`: 机器分配的变化
- `newInsertPosition`: 在新机器序列中的最优位置
- 保存两层基因用于撤销

---

## 五、集成到局部搜索

### LocalSearch 中的使用

```java
for (int iter = 0; iter < maxIterations; iter++) {
    int neighborhoodType = iter % 5;  // 轮换5种邻域
    
    if (neighborhoodType == 0) {
        // N1: 打印段 - 负载均衡
    } else if (neighborhoodType == 1) {
        // N2: 打印段 - 装箱优化
    } else if (neighborhoodType == 2) {
        // N3: 打印段 - 批次交换
    } else if (neighborhoodType == 3) {
        // N4: 离散段 - 关键块插入
    } else {
        // N5: 离散段 - 机器重分配 ← 复合邻域
        candidates = generateMovesN5(chromosome, tempOpMatrix, 6);
    }
}
```

### 轮换策略

```
迭代 0: N1（打印段）
迭代 1: N2（打印段）
迭代 2: N3（打印段）
迭代 3: N4（离散段 - 关键块）
迭代 4: N5（离散段 - 机器重分配）← 复合邻域
迭代 5: N1
...
```

---

## 六、为什么 N5 有效？

### 理论基础

**灵活性强**: 
- N4 只能在同一机器上调整工序顺序
- N5 可以跨机器移动 + 调整顺序
- 搜索空间更大，可能找到更优解

**针对性强**:
- 只作用于关键路径上的工序
- 避免浪费时间在非关键区域
- 直接优化决定 makespan 的瓶颈

**实际效果**:
- 某些工序在当前机器上是瓶颈
- 移到其他机器可能大幅减少等待
- 尤其当机器负载不均衡时效果显著

### 与其他邻域的互补性

| 邻域 | 优化对象 | 作用层 | 灵活性 |
|------|---------|--------|--------|
| N1/N2 | 打印机分配 | gene_MS | 低 |
| N3 | 批次顺序 | gene_OS + gene_MS | 中 |
| N4 | 关键块顺序 | gene_OS | 中 |
| **N5** | **关键工序位置** | **gene_MS + gene_OS** | **高** |

N5 是最灵活的邻域，可以在机器间移动并同时优化位置。

---

## 七、示例

### 示例 1：简单机器重分配

**初始状态**:
```
关键路径（工件 J0）:
  ...  → 离散2(M3, 位置12) → 离散3(M4, 位置18) → ...
            ↑
            当前makespan瓶颈

gene_OS 离散段: [..., J1, J0, J2, J0, J3, J0, ...]
                          ↑12      ↑18
                      离散2(M3)  离散3(M4)

gene_MS 离散段: [...,  2,  3,  2,  4,  2,  4, ...]
```

**可选机器**:
```
离散2 可以在: M3(当前), M4, M5
```

**生成候选**:
```
候选1: 离散2 从 M3 移到 M4, 插入位置 18（在离散3之前）
候选2: 离散2 从 M3 移到 M5, 插入位置 15
...
```

**应用候选1后**:
```
gene_OS: [..., J1, J2, J0, J0, J3, J0, ...]
                     ↑离散2改到这里
                     
gene_MS: [...,  2,  2,  4,  4,  2,  4, ...]
                     ↑M4
```

如果这能减少等待时间 → makespan 下降！

### 示例 2：复杂场景

**关键路径分析**:
```
工件 J0 的关键路径:
  打印(M1, 10h) 
  → 批处理(M5, 5h) 
  → 离散1(M3, 3h, 开始时间15h, 等待0h)
  → 离散2(M3, 2h, 开始时间25h, 等待7h) ← 瓶颈！
  → 离散3(M4, 4h, 开始时间27h, 等待0h)
  
makespan = 31h

问题：离散2 在 M3 上等待 7h（M3 很忙）
```

**N5 的解决方案**:
```
离散2 也可以在 M4 上加工（用时 2.5h）

将离散2 从 M3 移到 M4：
  打印(M1, 10h) 
  → 批处理(M5, 5h) 
  → 离散1(M3, 3h, 开始时间15h)
  → 离散2(M4, 2.5h, 开始时间18h, 等待0h) ← 移到M4
  → 离散3(M4, 4h, 开始时间20.5h)
  
新makespan = 24.5h  （减少了 6.5h！）
```

---

## 八、性能分析

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| 识别关键路径 | O(n) | n = 工件数 |
| 遍历关键工序 | O(k) | k = 关键路径长度 |
| 获取可选机器 | O(m) | m = 机器总数 |
| 生成插入位置 | O(p) | p = 新机器上工序数 |
| 应用/撤销移动 | O(n) | 需要移动数组元素 |

**总体**: O(k × m × p + n) ≈ O(k × m × p)

通常 k、m、p 都不大（< 20），所以较快。

### 候选数量控制

- **maxCandidates = 6**: 每次生成最多 6 个候选
- **原因**: 
  - 复合邻域评估开销较大
  - 每个候选需要重新 evaluate（装箱 + 调度）
  - 限制候选数平衡"质量 vs 速度"

### 与其他邻域的对比

| 邻域 | 候选数 | 计算开销 | 改进潜力 |
|------|--------|---------|---------|
| N1 | 10 | 中 | 中 |
| N2 | 10 | 中 | 中 |
| N3 | 5 | 低 | 小 |
| N4 | 8 | 低 | 中 |
| **N5** | **6** | **高** | **大** |

N5 计算开销最高，但改进潜力也最大。

---

## 九、参数配置

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| **候选数量** | 6 | 平衡质量与速度 |
| **插入位置数** | 3 | 每个机器尝试3个位置 |
| **轮换周期** | 5 | N1→N2→N3→N4→N5 |

### 动态调整建议

**如果运行太慢**:
```java
// 减少候选数
generateMovesN5(chromosome, tempOpMatrix, 3);  // 6 → 3
```

**如果想要更好的解**:
```java
// 增加候选数
generateMovesN5(chromosome, tempOpMatrix, 10);  // 6 → 10

// 或增加插入位置数
// 在 generateInsertPositions() 中返回更多位置
```

---

## 十、调试与验证

### 调试日志

在 `generateMovesN5` 中添加：

```java
if (DEBUG_MODE) {
    System.out.println("N5: 关键路径长度 = " + criticalPath.size());
    System.out.println("    工序: J" + jobNo + "-Op" + op.task);
    System.out.println("    当前机器: M" + currentMachine);
    System.out.println("    可选机器: " + availableMachines);
    System.out.println("    生成候选数: " + moves.size());
}
```

### 验证正确性

检查点：
1. ✅ 新机器在可选机器集中
2. ✅ 插入位置在合理范围内（jobCount 到 os.length）
3. ✅ 插入后 gene_OS 长度不变
4. ✅ 插入后 gene_MS 对应关系正确
5. ✅ 撤销后完全恢复原状态

---

## 十一、常见问题

### Q1: 为什么只作用一个工序？
**A**: 
- 复合邻域评估开销大
- 同时移动多个工序，候选数爆炸
- 单个工序足以带来显著改进
- 多次迭代可以逐步优化多个工序

### Q2: 如何选择插入位置？
**A**: 
三种策略组合：
1. 新机器上的关键位置（开头、中间、末尾）
2. 同一工件其他工序附近（保持紧凑）
3. 新机器现有工序之间（减少等待）

### Q3: N5 会增加多少计算时间？
**A**: 
- 候选数少（6个）
- 每 5 次迭代才用一次 N5
- 预计增加总时间 15-25%
- 但换来的解质量提升值得

### Q4: N5 和 N4 有什么区别？
**A**: 

| 特性 | N4 关键块插入 | N5 机器重分配 |
|------|-------------|--------------|
| 机器 | 不变（同机器内） | 改变（跨机器） |
| 位置 | 改变（块内移动） | 改变（最优插入） |
| 灵活性 | 中 | 高 |
| 搜索空间 | 小 | 大 |

N5 更灵活，但开销也更大。

---

## 十二、扩展方向

### 短期优化:
1. ✅ 实现基本的机器重分配（已完成）
2. ⏳ 测试效果，收集数据
3. ⏳ 调整候选数量（6 → 4 或 8）

### 中期扩展:
1. **启发式排序**: 优先尝试负载更轻的机器
2. **增量评估**: 快速估算机器变更的影响
3. **自适应插入**: 根据机器状态动态选择插入位置

### 长期研究:
1. **批量重分配**: 同时重分配关键块内的所有工序
2. **多目标优化**: 同时考虑 makespan 和负载均衡
3. **机器学习**: 学习哪些工序适合重分配

---

## 十三、参考文献

1. **Zhang et al. (2007)**: 
   *"An effective hybrid particle swarm optimization algorithm for multi-objective flexible job-shop scheduling problem"*

2. **Gao et al. (2008)**: 
   *"A hybrid genetic and variable neighborhood descent algorithm for flexible job shop scheduling problems"*

3. **Li & Gao (2016)**: 
   *"An effective hybrid genetic algorithm and tabu search for flexible job shop scheduling problem"*

---

## 十四、总结

### N5 的优势

✅ **最灵活的邻域**: 同时优化机器分配和工序顺序  
✅ **针对关键路径**: 只优化影响 makespan 的工序  
✅ **改进潜力大**: 可以突破单层邻域的局限  
✅ **理论支持**: 复合邻域是求解 FJSP 的有效方法

### 邻域体系完整性

```
打印段优化:
  N1 ─┐
  N2 ─┼─→ 跨机移动 + 批次优化
  N3 ─┘

离散段优化:
  N4 ─┐
  N5 ─┴─→ 关键路径优化（顺序 + 机器）
  
完整覆盖: 打印段 + 离散段 + 单层 + 复合
```

### 使用建议

- **小规模问题**: 可以用 N5，效果好
- **大规模问题**: 建议限制候选数（≤ 4）
- **时间紧张**: 可以关闭 N5（只用 N1-N4）
- **追求质量**: 增加 N5 候选数和迭代次数

---

## 作者
AI Assistant (Claude Sonnet 4.5)

## 版本
v1.0 - 2025-12-19
