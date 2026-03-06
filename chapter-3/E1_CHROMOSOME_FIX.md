# E1算子染色体修改修复

## 修复时间
2026-01-15

## 问题描述

E1算子（打印任务集中化）在迁移零件时存在严重问题：
- **只记录了Delta快照**，但没有修改染色体
- **步骤8的关键代码被注释掉了**
- 没有修改`gene_MS`来反映零件的机器分配变化
- 导致下次评估时，染色体重新解码会覆盖E1的修改

## 问题根源

### 原代码（错误）

```java
// 步骤8: 执行迁移
//        Solution sourceBatch = individual.printSolution[sourcePrinter.printerNo]
//            .get(transferItem.batchIndex);
//        sourceBatch.placeItemList.remove(transferItem.itemIndex);
//
//        // 步骤9: 标记affectedJobs
//        int jobNo = parseJobNumber(transferItem.item.name);
//        if (jobNo >= 0) {
//            delta.markJobAffected(jobNo);
//        }

return new Candidate(individual, delta, getName());
```

**问题**：
1. 步骤8被完全注释掉
2. 没有修改染色体（`gene_MS`）
3. 只修改了`printSolution`（但会在下次评估时被覆盖）

## 解决方案

### 修复后的代码

```java
// 步骤8: 执行迁移 - 修改染色体的gene_MS
int jobNo = parseJobNumber(transferItem.item.name);
if (jobNo < 0) {
    return null;  // 无法解析工件编号
}

// 在gene_OS中找到该工件的位置（打印段：0 ~ jobCount-1）
int jobCount = problem.getJobCount();
int jobPositionInOS = -1;
for (int i = 0; i < jobCount; i++) {
    if (individual.gene_OS[i] == jobNo) {
        jobPositionInOS = i;
        break;
    }
}

if (jobPositionInOS == -1) {
    return null;  // 未找到工件位置
}

// 修改gene_MS：从源打印机改为目标打印机（1-based）
individual.gene_MS[jobPositionInOS] = targetPrinter.printerNo + 1;

// 清空printSolution，下次评估会重新解码
individual.printSolution = null;

// 步骤9: 标记affectedJobs
delta.markJobAffected(jobNo);

return new Candidate(individual, delta, getName());
```

## 修复要点

### 1. 找到工件在染色体中的位置

```java
// E1是打印段算子，零件在gene_OS的打印段（0 ~ jobCount-1）
int jobPositionInOS = -1;
for (int i = 0; i < jobCount; i++) {
    if (individual.gene_OS[i] == jobNo) {
        jobPositionInOS = i;
        break;
    }
}
```

### 2. 修改gene_MS

```java
// 从源打印机改为目标打印机（1-based编号）
individual.gene_MS[jobPositionInOS] = targetPrinter.printerNo + 1;
```

**重要**：
- 打印段的`gene_MS`存储的是实际打印机编号（1-based）
- 不是相对索引（离散段才用相对索引）

### 3. 清空printSolution

```java
// 清空printSolution，确保下次评估重新解码染色体
individual.printSolution = null;
```

这确保了：
- 下次`evaluate`时会根据修改后的染色体重新解码
- E1的修改会生效并保留

## 为什么其他算子是正确的？

### T1-T3（打印段算子）

**T1**：
```java
// 修改gene_MS
individual.gene_MS[highestJobIdx] = toMachine;
individual.printSolution = null;
```

**T2**：
```java
// 修改gene_MS
individual.gene_MS[highestJobIdx] = minRateMachineIdx + 1;
individual.printSolution = null;
```

**T3**：
```java
// 批次交换：同时修改gene_OS和gene_MS
os[pos] = batch2Jobs.get(writeIdx);
ms[pos] = batch2Machines.get(writeIdx);
individual.printSolution = null;
```

### T4-T5（离散段算子）

**T4**：
```java
// 只交换gene_OS（工件编号）
int tempJob = os[swapPos1];
os[swapPos1] = os[swapPos2];
os[swapPos2] = tempJob;
// MS不变（固定顺序编码）
```

**T5**：
```java
// 修改gene_MS（相对索引）
individual.gene_MS[msIndex] = selectedRelativeIndex;
```

### E2（离散段算子）

**E2**：
```java
// 修改gene_MS（相对索引）
individual.gene_MS[pos] = newRelativeIndex;
delta.markJobAffected(jobNo);
```

## 编码方式回顾

### 打印段（0 ~ jobCount-1）

- `gene_OS[i]`：第i个打印的工件编号
- `gene_MS[i]`：该工件使用的打印机编号（**1-based实际编号**）

### 离散段（jobCount ~ totalOper-1）

- `gene_OS[i]`：第i个离散工序对应的工件编号
- `gene_MS[i]`：该工序使用的机器**相对索引**（1-based，固定顺序存储）

## 测试验证

### 修复前

```
E1算子执行 → 修改printSolution → 下次evaluate
→ 染色体未变 → 重新解码覆盖E1修改 → E1失效 ❌
```

### 修复后

```
E1算子执行 → 修改gene_MS → 清空printSolution → 下次evaluate
→ 根据修改后染色体解码 → E1修改生效 ✅
```

## 调试输出

为了便于追踪，所有算子都添加了调试输出：

```java
System.out.println("T1-PrintTimeBasedMove");
System.out.println("T2-AreaBasedMove");
System.out.println("T3-AdjacentBatchSwap");
System.out.println("T4-CriticalBlockSwap");
System.out.println("T5-MachineReassignment");
System.out.println("E1-PrintConsolidationMove");
System.out.println("E2-DiscreteEnergyOptimalReassign");
```

运行时可以看到哪些算子被调用。

## 总结

### 修复内容

1. ✅ 解注释并重写步骤8（执行迁移）
2. ✅ 在`gene_OS`中找到工件位置
3. ✅ 修改`gene_MS`（打印机编号）
4. ✅ 清空`printSolution`
5. ✅ 标记受影响的作业
6. ✅ 添加调试输出

### 核心原则

**局部搜索算子必须修改染色体，而不是只修改解码结果！**

因为：
- 每次`evaluate`都会重新解码染色体
- 如果不修改染色体，修改会被覆盖
- 必须修改`gene_OS`或`gene_MS`以确保修改持久化

### 文件修改

1. `E1_PrintConsolidationMove.java` - 修复染色体修改逻辑
2. 所有算子 - 添加调试输出

---

**版本**: v3.2 - E1染色体修复版  
**日期**: 2026-01-15  
**状态**: ✅ **已修复并验证**
