# MOGWO两段式编码适配说明

## 问题分析

### 原始错误
```
java.lang.ArrayIndexOutOfBoundsException: 5
at ProblemFrame.CaculateFitness.evaluate(CaculateFitness.java:319)
```

### 根本原因

这个问题使用**两段式编码**：

1. **第一段：打印工序**（前`jobCount`个位置）
   - OS: 每个job恰好出现一次（0到jobCount-1的一个排列）
   - MS: 对应打印机编号（1到printMachineCount）
   - 特点：OS和MS必须同步

2. **第二段：离散工序**（`jobCount`之后的位置）
   - OS: 每个job可能出现多次（取决于该job有多少离散工序）
   - MS: 按job顺序固定排列（相对索引）
   - 特点：MS的顺序与OS无关，是按job编号顺序排列的

**MOGWO原始实现的问题**：
- `moveTowards()` 和 `combinePositions()` 方法直接对整个OS/MS数组操作
- 没有区分打印段和离散段
- 破坏了两段式编码的结构约束

## 修复方案

### 1. moveTowards() - 向领导者移动

**关键改进**：分两段处理

#### 打印段（OS和MS同步）
```java
// 1. 决定哪些job来自领导者
Set<Integer> leaderJobs = new HashSet<>();
for (int i = 0; i < jobCount; i++) {
    if (random.nextDouble() < leaderBias) {
        leaderJobs.add(i);
    }
}

// 2. 重建打印段（保持OS和MS同步）
// 先放置来自leader的job（保持leader中的顺序）
for (int i = 0; i < jobCount; i++) {
    int job = leader.gene_OS[i];
    if (leaderJobs.contains(job)) {
        printOS[fillIndex] = job;
        printMS[fillIndex] = leader.gene_MS[i];  // 同步！
        fillIndex++;
    }
}

// 再放置来自wolf的job（保持wolf中的顺序）
for (int i = 0; i < jobCount; i++) {
    int job = wolf.gene_OS[i];
    if (!leaderJobs.contains(job)) {
        printOS[fillIndex] = job;
        printMS[fillIndex] = wolf.gene_MS[i];  // 同步！
        fillIndex++;
    }
}
```

#### 离散段（OS混合，MS保持结构）
```java
// 1. OS进行混合
for (int i = 0; i < discreteLength; i++) {
    int idx = jobCount + i;
    if (random.nextDouble() < leaderBias) {
        newWolf.gene_OS[idx] = leader.gene_OS[idx];
    } else {
        newWolf.gene_OS[idx] = wolf.gene_OS[idx];
    }
}

// 2. MS保持原结构（不变）
System.arraycopy(wolf.gene_MS, jobCount, newWolf.gene_MS, jobCount, discreteLength);
```

### 2. combinePositions() - 组合三个位置

**关键改进**：分两段处理

#### 打印段（投票机制）
```java
// 1. 为每个job在三个位置中投票
Map<Integer, List<Integer>> jobVotes = new HashMap<>();

// 收集每个job在X1,X2,X3中的位置
for (int i = 0; i < jobCount; i++) {
    jobVotes.get(X1.gene_OS[i]).add(i);
    jobVotes.get(X2.gene_OS[i]).add(i);
    jobVotes.get(X3.gene_OS[i]).add(i);
}

// 2. 计算平均位置并排序
Map<Integer, Double> avgPositions = new HashMap<>();
for (int job = 0; job < jobCount; job++) {
    double avg = jobVotes.get(job).stream()
        .mapToInt(Integer::intValue).average().orElse(job);
    avgPositions.put(job, avg);
}

// 3. 按平均位置排序（投票结果）
List<Integer> sortedJobs = new ArrayList<>(avgPositions.keySet());
sortedJobs.sort(Comparator.comparingDouble(avgPositions::get));

// 4. MS对应选择
for (int i = 0; i < jobCount; i++) {
    int job = newWolf.gene_OS[i];
    // 从X1,X2,X3中随机选择一个对应的MS
}
```

#### 离散段（简单投票）
```java
// OS投票
for (int i = jobCount; i < length; i++) {
    double r = random.nextDouble();
    if (r < 0.33) newWolf.gene_OS[i] = X1.gene_OS[i];
    else if (r < 0.67) newWolf.gene_OS[i] = X2.gene_OS[i];
    else newWolf.gene_OS[i] = X3.gene_OS[i];
}

// MS继承base（不变）
```

### 3. repairOS() - 修复OS基因

**关键改进**：分两段修复

#### 打印段修复
```java
// 检查：应该恰好包含0到jobCount-1各一次
Set<Integer> printJobs = new HashSet<>();
for (int i = 0; i < jobCount; i++) {
    printJobs.add(os[i]);
}

// 修复：重新生成一个合法的排列
if (printJobs.size() != jobCount) {
    ArrayList<Integer> jobs = new ArrayList<>();
    for (int i = 0; i < jobCount; i++) jobs.add(i);
    Collections.shuffle(jobs, random);
    
    for (int i = 0; i < jobCount; i++) {
        individual.gene_OS[i] = jobs.get(i);
    }
}
```

#### 离散段修复
```java
// 统计每个job的离散工序数
int[] discreteJobCounts = new int[jobCount];
for (int i = jobCount; i < os.length; i++) {
    discreteJobCounts[os[i]]++;
}

// 检查是否匹配预期数量
for (int i = 0; i < jobCount; i++) {
    int expected = opsCount[i] - 2;  // 减去打印和批处理
    if (discreteJobCounts[i] != expected) {
        needRepair = true;
        break;
    }
}

// 修复：重新生成离散段
if (needRepair) {
    ArrayList<Integer> discreteOS = new ArrayList<>();
    for (int i = 0; i < jobCount; i++) {
        for (int j = 0; j < opsCount[i] - 2; j++) {
            discreteOS.add(i);
        }
    }
    Collections.shuffle(discreteOS, random);
    
    for (int i = 0; i < discreteOS.size(); i++) {
        individual.gene_OS[jobCount + i] = discreteOS.get(i);
    }
}
```

### 4. repairMS() - 修复MS基因

**关键改进**：分两段修复

#### 打印段MS
```java
// 注意：打印段MS需要根据OS中的job来确定
for (int i = 0; i < jobCount; i++) {
    int jobNo = os[i];  // 第i位置的工件编号
    int machineId = ms[i];
    
    // 检查机器是否合法（能否容纳该工件）
    if (!isValidPrintMachine(jobNo, machineId)) {
        ms[i] = selectValidPrintMachine(jobNo);
    }
}
```

#### 离散段MS
```java
// 注意：离散段MS是按job编号顺序排列的（与OS无关）
int msIndex = jobCount;
for (int jobNo = 0; jobNo < jobCount; jobNo++) {
    int discreteOpsCount = opsCount[jobNo] - 2;
    
    for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
        int operNo = 2 + localOperNo;
        int operIdx = operationToIndex[jobNo][operNo];
        
        // 检查MS是否在合法范围内
        ArrayList<Integer> availableMachines = getAvailableMachines(operIdx);
        if (ms[msIndex] < 1 || ms[msIndex] > availableMachines.size()) {
            ms[msIndex] = random.nextInt(availableMachines.size()) + 1;
        }
        
        msIndex++;
    }
}
```

## 关键要点

### ✅ 必须遵守的规则

1. **打印段**：
   - OS和MS必须同步操作
   - OS是0到jobCount-1的一个排列（无重复）
   - MS对应每个job的打印机分配

2. **离散段**：
   - OS可以有重复（每个job可能有多个离散工序）
   - MS按job编号顺序固定排列（与OS中的顺序无关）
   - MS是相对索引（1到availableMachines.size()）

3. **操作原则**：
   - 任何修改打印段OS的操作，必须同步修改打印段MS
   - 离散段OS可以自由混合，但离散段MS必须保持结构
   - 修复时分两段独立处理

### ❌ 常见错误

1. **错误**：直接对整个OS数组进行操作
   ```java
   // 错误示例
   for (int i = 0; i < os.length; i++) {
       newOS[i] = mix(os1[i], os2[i]);
   }
   ```
   
2. **错误**：打印段OS和MS不同步
   ```java
   // 错误示例
   printOS = shuffle(printOS);  // 只改OS
   // printMS没有同步改变！
   ```

3. **错误**：修改离散段MS的顺序
   ```java
   // 错误示例
   discreteMS = shuffle(discreteMS);  // 破坏了MS的固定结构！
   ```

## 测试验证

运行前确保：

1. **检查OS结构**：
   ```java
   // 打印段：每个job恰好一次
   Set<Integer> printJobs = new HashSet<>();
   for (int i = 0; i < jobCount; i++) {
       printJobs.add(os[i]);
   }
   assert printJobs.size() == jobCount;
   
   // 离散段：每个job的数量正确
   int[] counts = new int[jobCount];
   for (int i = jobCount; i < os.length; i++) {
       counts[os[i]]++;
   }
   for (int i = 0; i < jobCount; i++) {
       assert counts[i] == opsCount[i] - 2;
   }
   ```

2. **检查MS合法性**：
   ```java
   // 打印段MS：每个机器能容纳对应job
   for (int i = 0; i < jobCount; i++) {
       int job = os[i];
       int machine = ms[i];
       assert canFitInMachine(job, machine);
   }
   ```

## 参考

- **NSGA-II实现**: `NSGAIIOperations.java`
  - `crossover()` 方法（第376行）
  - `operSeqCrossoverPOX_PrintSync()` 方法（第477行）
  - `operSeqCrossoverPOX_DiscreteOnly()` 方法

---

**修复日期**: 2026-01-19  
**状态**: ✅ 已完成两段式编码适配
