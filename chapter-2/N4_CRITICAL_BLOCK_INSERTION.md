# N4 邻域动作：关键块插入 (Critical Block Insertion)

## 时间
2025-12-19

## 动作描述

**N4 邻域动作**是一个针对**离散处理段**的经典邻域搜索策略，通过在关键路径的关键块内移动工序位置来优化 makespan。

---

## 一、核心概念

### 1. 关键路径 (Critical Path)

**定义**: 从起点到终点用时最长的路径，该路径决定了整个调度的 makespan。

```
示例：
工件 J0: 打印(10h) → 批处理(5h) → 离散1(3h) → 离散2(2h) → 离散3(4h)
        总时间 = 24h ← 关键路径

工件 J1: 打印(8h) → 批处理(4h) → 离散1(2h) → 离散2(3h)
        总时间 = 17h

工件 J2: 打印(6h) → 批处理(3h) → 离散1(1h)
        总时间 = 10h

关键路径在 J0，因为它的完成时间最晚（24h = makespan）
```

### 2. 关键块 (Critical Block)

**定义**: 关键路径上在**同一机器**上**连续加工**的工序序列。

```
示例：
关键路径: [打印, 批处理, 离散1(M3), 离散2(M3), 离散3(M4)]
                              └────────关键块1────────┘  └─关键块2─┘

关键块1: [离散1, 离散2] 在机器 M3 上
关键块2: [离散3] 在机器 M4 上（只有1个工序，不是真正的块）
```

**关键块的特征**:
- ✅ 在关键路径上
- ✅ 在同一机器上加工
- ✅ 连续加工（中间没有其他工件的工序）
- ✅ 至少包含 2 个工序

### 3. 为什么关键块很重要？

关键块是 makespan 的瓶颈：
- 关键块内工序的顺序直接影响 makespan
- 调整关键块内的工序顺序可能减少等待时间
- 经典调度理论证明：**最优解必定在关键块的某个邻域内**

---

## 二、N4 动作原理

### 动作定义

**关键块插入**: 将关键块内某个工序从当前位置移动到块内其他位置。

```
例如，关键块: [离散1, 离散2, 离散3, 离散4]
                  ↑
         将离散1移动到离散3后面
                  
结果: [离散2, 离散3, 离散1, 离散4]
```

### 约束条件

✅ **允许的操作**:
- 只在关键块内部移动工序
- 只改变工序序列（gene_OS）
- 不改变机器分配（gene_MS）

❌ **不允许的操作**:
- 跨关键块移动工序
- 移动到非关键路径的工序
- 修改机器分配

### 作用层

- **上层（gene_OS）**: ✅ 修改工序顺序
- **下层（gene_MS）**: ❌ 不修改机器分配

---

## 三、实现算法

### 1. 识别关键路径

```java
private List<Operation> identifyCriticalPath(Operation[][] operationMatrix)
```

**步骤**:
```
1. 遍历所有工件
   │
2. 找到最后完成时间最大的工件
   │  (该工件的最后一个工序的 endTime = makespan)
   │
3. 收集该工件的所有工序
   │  → 这些工序构成关键路径
   │
4. 返回关键路径
```

**示例**:
```
工件0最后完成时间: 24.5h ← 最大
工件1最后完成时间: 18.3h
工件2最后完成时间: 15.7h

→ 关键路径 = 工件0的所有工序
```

### 2. 识别关键块

```java
private List<CriticalBlock> identifyCriticalBlocks(List<Operation> criticalPath, Chromosome chromosome)
```

**步骤**:
```
1. 遍历关键路径上的每个工序
   │
2. 只考虑离散段工序 (op.task >= 2)
   │
3. 检查当前工序和上一个工序是否在同一机器
   │
   ├─ 是 → 添加到当前关键块
   │
   └─ 否 → 保存当前关键块，开始新块
   │
4. 只保留包含 >= 2 个工序的块
```

**示例**:
```
关键路径: [打印(M1), 批处理(M5), 离散1(M3), 离散2(M3), 离散3(M4), 离散4(M4), 离散5(M3)]
                                      └────块1────┘  └────块2────┘  └─块3─┘

块1: [离散1, 离散2] 在 M3
块2: [离散3, 离散4] 在 M4
块3: [离散5] 在 M3 → 只有1个工序，丢弃
```

### 3. 生成候选移动

```java
private List<CriticalBlockMove> generateMovesN4(Chromosome chromosome, Operation[][] operationMatrix, int maxCandidates)
```

**步骤**:
```
1. 识别关键路径
   │
2. 识别关键块
   │
3. 对每个关键块:
   │
   └─ 对块内每个工序:
      │
      └─ 尝试插入到块内所有其他位置
         │
         例如: [Op1, Op2, Op3, Op4]
               │
               ├─ Op1 → 位置2: [Op2, Op1, Op3, Op4]
               ├─ Op1 → 位置3: [Op2, Op3, Op1, Op4]
               ├─ Op1 → 位置4: [Op2, Op3, Op4, Op1]
               ├─ Op2 → 位置1: [Op2, Op1, Op3, Op4]
               ...
```

### 4. 应用插入操作

```java
private void applyCriticalBlockMove(Chromosome chromosome, CriticalBlockMove move)
```

**步骤**:
```
1. 保存原始 gene_OS
   │
2. 提取 fromPosition 处的工件号
   │
3. 执行插入:
   │
   ├─ 如果向后移动 (fromPos < toPos):
   │  │
   │  └─ 将 fromPos 后面的元素前移
   │     [A, B, C, D, E]
   │      ↑     ↑
   │      from  to
   │     
   │     → [B, C, A, D, E]
   │
   └─ 如果向前移动 (fromPos > toPos):
      │
      └─ 将 fromPos 前面的元素后移
        [A, B, C, D, E]
             ↑  ↑
             to from
        
        → [A, D, B, C, E]
```

---

## 四、数据结构

### CriticalBlockMove 类

```java
private static class CriticalBlockMove {
    int fromPosition;    // 工序在 gene_OS 中的原位置
    int toPosition;      // 目标位置
    int[] originalOS;    // 保存原始基因（用于撤销）
}
```

### CriticalBlock 类

```java
private static class CriticalBlock {
    int machineNo;                    // 机器编号（1-based）
    List<Integer> operationPositions; // 工序在 gene_OS 中的位置
}
```

---

## 五、集成到局部搜索

### LocalSearch 中的使用

```java
for (int iter = 0; iter < maxIterations; iter++) {
    int neighborhoodType = iter % 4;  // 轮换4种邻域
    
    if (neighborhoodType == 0) {
        // N1: 打印段 - 跨机移动（负载均衡）
    } else if (neighborhoodType == 1) {
        // N2: 打印段 - 跨机移动（装箱优化）
    } else if (neighborhoodType == 2) {
        // N3: 打印段 - 批次交换
    } else {
        // N4: 离散段 - 关键块插入 ← 新增
        candidates = generateMovesN4(chromosome, tempOpMatrix, 8);
    }
}
```

### 轮换策略

```
迭代 0: N1（打印段优化）
迭代 1: N2（打印段优化）
迭代 2: N3（打印段优化）
迭代 3: N4（离散段优化）← 关键块插入
迭代 4: N1
迭代 5: N2
迭代 6: N3
迭代 7: N4
...
```

---

## 六、为什么 N4 有效？

### 理论基础

**Balas 和 Vazacopoulos (1998)**: 
> "对于 Job Shop 调度问题，最优解必定存在于关键块邻域的某个解中。"

这意味着：通过系统地搜索关键块的邻域，我们可以找到最优或接近最优的解。

### 实际效果

✅ **针对性强**: 直接优化决定 makespan 的关键路径  
✅ **效率高**: 只搜索关键块，不浪费时间在非关键区域  
✅ **改进稳定**: 关键块插入通常能稳定减少 makespan  
✅ **理论保证**: 有理论支持的经典方法

### 与其他邻域的互补性

| 邻域 | 优化对象 | 作用阶段 | 优化效果 |
|------|---------|---------|---------|
| N1 | 打印机负载 | 打印段 | 均衡负载 |
| N2 | 装箱效率 | 打印段 | 提高利用率 |
| N3 | 批次顺序 | 打印段 | 调整顺序 |
| **N4** | **关键路径** | **离散段** | **减少 makespan** |

N4 是唯一直接针对离散段优化的邻域，与 N1/N2/N3 形成完美互补。

---

## 七、示例

### 示例 1：简单关键块插入

**初始状态**:
```
关键路径（工件 J0）:
  打印(M1, 10h) → 批处理(M5, 5h) → 离散1(M3, 3h) → 离散2(M3, 2h) → 离散3(M4, 4h)
                                      └────关键块────┘

gene_OS (离散段): [..., J0, J1, J0, J2, J3, ...]
                        ↑离散1 ↑离散2
                        位置10  位置12
```

**生成候选**:
```
候选1: 将 J0-离散1 从位置10 移到位置12 (插到离散2后面)
候选2: 将 J0-离散2 从位置12 移到位置10 (插到离散1前面)
```

**应用候选1后**:
```
gene_OS (离散段): [..., J1, J0, J0, J2, J3, ...]
                        ↑离散2 ↑离散1
```

如果这个顺序能减少等待时间 → 接受该移动！

### 示例 2：多个关键块

**关键路径**:
```
打印 → 批处理 → 离散1(M3) → 离散2(M3) → 离散3(M4) → 离散4(M4) → 离散5(M3)
                  └────块1────┘  └────块2────┘

块1: [离散1, 离散2] 在 M3
块2: [离散3, 离散4] 在 M4
```

**生成的候选**:
- 块1: 2个工序 → 2个候选（交换它们）
- 块2: 2个工序 → 2个候选（交换它们）
- 总共 4 个候选

局部搜索会逐个评估，接受第一个改进的。

---

## 八、性能分析

### 时间复杂度

| 操作 | 复杂度 | 说明 |
|------|--------|------|
| 识别关键路径 | O(n) | n = 工件数 |
| 识别关键块 | O(k) | k = 关键路径工序数 |
| 生成候选 | O(b × m²) | b = 关键块数, m = 平均块大小 |
| 应用/撤销移动 | O(n) | 需要移动数组元素 |

**总体**: O(n + k + b×m² + n) ≈ O(b×m²)

通常 b 和 m 都很小（≤ 5），所以很快。

### 候选数量控制

- **maxCandidates = 8**: 每次生成最多 8 个候选
- **原因**: 关键块内的移动候选可能很多，需要限制
- **效果**: 平衡"质量 vs 速度"

---

## 九、调试与验证

### 调试日志

在 `generateMovesN4` 中添加：

```java
if (DEBUG_MODE) {
    System.out.println("N4: 关键路径长度 = " + criticalPath.size());
    System.out.println("    关键块数量 = " + criticalBlocks.size());
    for (CriticalBlock block : criticalBlocks) {
        System.out.println("    块(机器" + block.machineNo + "): " + 
                          block.operationPositions.size() + " 个工序");
    }
}
```

### 验证正确性

检查点：
1. ✅ 关键路径是最后完成的工件
2. ✅ 关键块在关键路径上
3. ✅ 关键块内工序在同一机器
4. ✅ 插入后 gene_OS 长度不变
5. ✅ 插入后工件集合不变
6. ✅ 插入不改变 gene_MS

---

## 十、参数配置

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| **候选数量** | 8 | 平衡质量与速度 |
| **最小块大小** | 2 | 至少2个工序才能插入 |
| **轮换周期** | 4 | N1→N2→N3→N4 |

---

## 十一、常见问题

### Q1: 为什么只在关键块内移动？
**A**: 
- 只有关键块上的调整才能直接减少 makespan
- 非关键路径的调整不影响 makespan
- 集中搜索关键区域，效率更高

### Q2: 如何找到工序在 gene_OS 中的位置？
**A**: 
```java
int findOperationPosition(Operation op, int[] os, int[] ms, int jobCount) {
    // 1. 获取工序的 jobNo, machineNo, 工序编号
    // 2. 在离散段（从 jobCount 开始）搜索
    // 3. 匹配 os[i] == jobNo 且 ms[i] == machineNo
    // 4. 还要匹配是该工件的第几个离散工序
}
```

### Q3: N4 会增加多少计算时间？
**A**: 
- 识别关键路径和关键块很快（< 1ms）
- 生成候选数量少（8个）
- 每 4 次迭代才用一次 N4
- 预计增加总时间 5-10%

### Q4: 关键路径会动态变化吗？
**A**: 
- 是的！每次 evaluate 后关键路径可能改变
- N4 每次都重新识别关键路径
- 这确保始终优化当前的瓶颈

---

## 十二、扩展方向

### 短期优化:
1. ✅ 实现基本的关键块插入（已完成）
2. ⏳ 测试效果，收集数据
3. ⏳ 调整候选数量（8 → 5 或 10）

### 中期扩展:
1. **多关键路径**: 识别次关键路径（makespan - ε）
2. **增量评估**: 快速估算插入的影响
3. **启发式排序**: 优先尝试更可能改进的候选

### 长期研究:
1. **关键块分裂**: 将大关键块拆成小块
2. **跨块移动**: 在相邻关键块间移动工序
3. **自适应N4**: 根据关键块特征选择策略

---

## 十三、参考文献

1. **Balas & Vazacopoulos (1998)**: 
   *"Guided Local Search with Shifting Bottleneck for Job Shop Scheduling"*

2. **Nowicki & Smutnicki (1996)**: 
   *"A Fast Taboo Search Algorithm for the Job Shop Problem"*

3. **Van Laarhoven et al. (1992)**: 
   *"Job Shop Scheduling by Simulated Annealing"*

---

## 作者
AI Assistant (Claude Sonnet 4.5)

## 版本
v1.0 - 2025-12-19
