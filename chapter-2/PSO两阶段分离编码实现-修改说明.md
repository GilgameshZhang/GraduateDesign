# PSO两阶段分离编码实现 - 修改说明

> **修改日期**: 2025年12月25日  
> **核心目标**: 正确实现PSO的两阶段分离编码，打印阶段和离散处理阶段独立处理

---

## 🎯 核心问题

原PSO实现未正确处理**打印阶段**和**离散处理阶段**的编码差异：

1. **打印阶段MS**：与OS对应，存储**绝对打印机编号**（1, 2, 3, ...）
2. **离散阶段MS**：**固定结构**（按工件编号顺序），存储**相对机器索引**（1表示第1个可用机器）

---

## 📊 两阶段编码差异对比

| 维度 | 打印阶段（Print Stage） | 离散处理阶段（Discrete Stage） |
|------|----------------------|----------------------------|
| **OS结构** | 工件打印顺序 | 工件离散工序顺序 |
| **OS长度** | jobCount | 总长度 - jobCount |
| **OS编码** | 可任意排列 | 可任意排列 |
| **MS结构** | 与OS位置对应 | ⚠️ **固定结构**（按工件0, 1, 2...顺序） |
| **MS长度** | jobCount | 所有工件的离散工序总数 |
| **MS存储内容** | **绝对打印机编号** (1=打印机1) | ⚠️ **相对机器索引** (1=第1个可用机器) |
| **MS与OS关系** | ✅ 强耦合（同步变化） | ❌ **解耦**（MS固定，OS可变） |

---

## 🔧 修改的文件

### 1. `ProblemFrame/Particle.java`

#### 1.1 连续编码字段（修改前）

```java
// 旧实现：单一连续向量
public double[] position_OS_continuous;     // OS连续向量
public double[] position_MS_continuous;     // MS连续向量
public double[] velocity_OS_continuous;     // OS速度
public double[] velocity_MS_continuous;     // MS速度
```

**问题**：未区分打印段和离散段，无法处理MS离散段的固定结构。

#### 1.2 连续编码字段（修改后）⭐

```java
// 新实现：两阶段分离
//===== 打印阶段：OS和MS对应 =====
public double[] position_OS_print_continuous;       // [0,1]^jobCount
public double[] position_MS_print_continuous;       // [0,1]^jobCount
public double[] velocity_OS_print_continuous;
public double[] velocity_MS_print_continuous;

//===== 离散处理阶段：OS可变，MS固定结构 =====
public double[] position_OS_discrete_continuous;    // [0,1]^discreteOpsCount
public double[] position_MS_discrete_continuous;    // [0,1]^discreteOpsCount（固定结构）
public double[] velocity_OS_discrete_continuous;
public double[] velocity_MS_discrete_continuous;

//===== 个体最优（也分阶段） =====
public double[] pBest_OS_print_continuous;
public double[] pBest_MS_print_continuous;
public double[] pBest_OS_discrete_continuous;
public double[] pBest_MS_discrete_continuous;
```

---

#### 1.3 构造函数修改

**旧实现**：
```java
public Particle(Chromosome chromosome, Random r, boolean useContinuous) {
    // 单一连续向量初始化
    this.position_OS_continuous = discreteToContinuous_OS(chromosome.gene_OS);
    this.position_MS_continuous = discreteToContinuous_MS(chromosome.gene_MS);
}
```

**新实现**：
```java
public Particle(Chromosome chromosome, Random r, boolean useContinuous, Problem problem) {
    if (useContinuous) {
        int jobCount = problem.getJobCount();
        int discreteLength = totalLength - jobCount;
        
        // 打印段：初始化连续位置
        this.position_OS_print_continuous = new double[jobCount];
        this.position_MS_print_continuous = new double[jobCount];
        for (int i = 0; i < jobCount; i++) {
            this.position_OS_print_continuous[i] = (i + r.nextDouble()) / jobCount;
            this.position_MS_print_continuous[i] = (i + r.nextDouble()) / jobCount;
        }
        
        // 离散段：初始化连续位置
        this.position_OS_discrete_continuous = new double[discreteLength];
        this.position_MS_discrete_continuous = new double[discreteLength];
        for (int i = 0; i < discreteLength; i++) {
            this.position_OS_discrete_continuous[i] = (i + r.nextDouble()) / discreteLength;
            this.position_MS_discrete_continuous[i] = r.nextDouble();
        }
        
        // 初始化速度和个体最优...
    }
}
```

**关键改变**：
- ✅ 需要传入`Problem`对象以获取jobCount
- ✅ 分别初始化打印段和离散段的连续向量
- ✅ 打印段和离散段长度不同

---

#### 1.4 映射方法：`continuousToDiscrete_OS()`

**旧实现**（单一排序，有bug）：
```java
public void continuousToDiscrete_OS() {
    // 对整个position_OS_continuous排序
    // 问题：打印段和离散段混在一起
}
```

**新实现**（两阶段分别排序）：
```java
public void continuousToDiscrete_OS(Problem problem) {
    int jobCount = problem.getJobCount();
    
    //===== 打印段：Random Key排序 =====
    // 对position_OS_print_continuous排序 → gene_OS[0:jobCount]
    List<IndexPriority> printPairs = new ArrayList<>();
    int[] originalPrintOS = new int[jobCount];
    for (int i = 0; i < jobCount; i++) {
        originalPrintOS[i] = gene_OS[i];
        printPairs.add(new IndexPriority(i, position_OS_print_continuous[i]));
    }
    printPairs.sort((a, b) -> Double.compare(a.priority, b.priority));
    for (int i = 0; i < jobCount; i++) {
        gene_OS[i] = originalPrintOS[printPairs.get(i).index];
    }
    
    //===== 离散段：Random Key排序 =====
    // 对position_OS_discrete_continuous排序 → gene_OS[jobCount:end]
    int discreteLength = totalLength - jobCount;
    List<IndexPriority> discretePairs = new ArrayList<>();
    int[] originalDiscreteOS = new int[discreteLength];
    for (int i = 0; i < discreteLength; i++) {
        originalDiscreteOS[i] = gene_OS[jobCount + i];
        discretePairs.add(new IndexPriority(i, position_OS_discrete_continuous[i]));
    }
    discretePairs.sort((a, b) -> Double.compare(a.priority, b.priority));
    for (int i = 0; i < discreteLength; i++) {
        gene_OS[jobCount + i] = originalDiscreteOS[discretePairs.get(i).index];
    }
}
```

**关键改变**：
- ✅ 打印段和离散段**分别排序**
- ✅ 每段内部独立进行Random Key映射
- ✅ 打印段影响`gene_OS[0:jobCount]`，离散段影响`gene_OS[jobCount:end]`

---

#### 1.5 映射方法：`continuousToDiscrete_MS()` ⚠️ **核心修改**

**旧实现**（错误）：
```java
public void continuousToDiscrete_MS(Problem problem) {
    for (int i = 0; i < position_MS_continuous.length; i++) {
        int jobNo = gene_OS[i];  // ❌ 离散段不应该从OS读取！
        
        if (i < jobCount) {
            // 打印段：映射到打印机
        } else {
            // 离散段：依赖OS顺序计算工序编号 ❌ 错误！
            int[] jobOccurrence = new int[jobCount];
            for (int j = jobCount; j <= i; j++) {
                jobOccurrence[gene_OS[j]]++;  // ❌ 依赖OS
            }
        }
    }
}
```

**新实现**（正确）⭐：
```java
public void continuousToDiscrete_MS(Problem problem) {
    int jobCount = problem.getJobCount();
    
    //===== 打印段：与OS对应，映射到绝对打印机编号 =====
    for (int i = 0; i < jobCount; i++) {
        int jobNo = gene_OS[i];  // ✅ 从OS读取（打印段对应）
        double continuousValue = position_MS_print_continuous[i];
        
        // 找到可用打印机
        List<Integer> availablePrinters = getAvailablePrinters(jobNo);
        
        // 映射到绝对打印机编号
        int idx = (int) (continuousValue * availablePrinters.size());
        gene_MS[i] = availablePrinters.get(idx);  // 存储绝对编号
    }
    
    //===== 离散段：固定结构，按工件顺序，映射到相对机器索引 =====
    int msIndex = jobCount;
    
    // ⚠️ 关键：按工件编号顺序遍历（不从OS读取！）
    for (int jobNo = 0; jobNo < jobCount; jobNo++) {  // ✅ 固定顺序
        int discreteOpsCount = operationCountArr[jobNo] - 2;
        
        for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
            int operNo = 2 + localOperNo;
            int operIdx = operationToIndex[jobNo][operNo];
            
            // ✅ 从固定位置读取连续值
            int discreteIndex = msIndex - jobCount;
            double continuousValue = position_MS_discrete_continuous[discreteIndex];
            
            // 找到可用机器
            List<Integer> availableMachines = getAvailableMachines(operIdx);
            
            // 映射到相对索引（1-based）
            int relativeIndex = (int) (continuousValue * availableMachines.size()) + 1;
            gene_MS[msIndex] = relativeIndex;  // ⚠️ 存储相对索引
            
            msIndex++;
        }
    }
}
```

**关键改变**：
- ✅ 打印段：从`gene_OS[i]`读取jobNo，映射到**绝对打印机编号**
- ✅ 离散段：**按工件编号顺序**遍历（`for jobNo = 0 to jobCount`），不依赖OS
- ✅ 离散段：存储**相对机器索引**（1表示第1个可用机器，2表示第2个）
- ✅ 离散段MS结构固定：始终是工件0的所有离散工序，然后工件1的所有离散工序，...

---

#### 1.6 其他方法修改

**复制构造函数**：
```java
public Particle(Particle p) {
    // 复制所有4个打印段向量
    this.position_OS_print_continuous = p.position_OS_print_continuous.clone();
    this.position_MS_print_continuous = p.position_MS_print_continuous.clone();
    // ...
    
    // 复制所有4个离散段向量
    this.position_OS_discrete_continuous = p.position_OS_discrete_continuous.clone();
    this.position_MS_discrete_continuous = p.position_MS_discrete_continuous.clone();
    // ...
}
```

**updatePBest()**：
```java
public void updatePBest() {
    if (this.fitness > this.pBestFitness) {
        // 更新4个打印段pBest
        this.pBest_OS_print_continuous = this.position_OS_print_continuous.clone();
        // ...
        
        // 更新4个离散段pBest
        this.pBest_OS_discrete_continuous = this.position_OS_discrete_continuous.clone();
        // ...
    }
}
```

---

### 2. `AlgorthmFrame/pso/PSO.java`

#### 2.1 初始化粒子群

**修改**：
```java
// 旧代码
swarm[i] = new Particle(chromosome, r, true);

// 新代码
swarm[i] = new Particle(chromosome, r, true, input);  // 传入Problem对象
```

---

#### 2.2 速度更新

**旧实现**（单一循环）：
```java
private void updateVelocity(Particle particle) {
    int n = particle.position_OS_continuous.length;
    
    for (int i = 0; i < n; i++) {
        // OS速度更新
        particle.velocity_OS_continuous[i] = ...;
        
        // MS速度更新
        particle.velocity_MS_continuous[i] = ...;
    }
}
```

**新实现**（四个循环）：
```java
private void updateVelocity(Particle particle) {
    double w_current = w_max - (w_max - w_min) * currentIteration / maxIterations;
    
    //===== 打印段速度更新 =====
    int printLength = particle.position_OS_print_continuous.length;
    
    // OS打印段
    for (int i = 0; i < printLength; i++) {
        particle.velocity_OS_print_continuous[i] = 
            w_current * particle.velocity_OS_print_continuous[i] +
            c1 * r1 * (particle.pBest_OS_print_continuous[i] - particle.position_OS_print_continuous[i]) +
            c2 * r2 * (gBest.pBest_OS_print_continuous[i] - particle.position_OS_print_continuous[i]);
    }
    
    // MS打印段
    for (int i = 0; i < printLength; i++) {
        particle.velocity_MS_print_continuous[i] = ...;  // 同上
    }
    
    //===== 离散段速度更新 =====
    int discreteLength = particle.position_OS_discrete_continuous.length;
    
    // OS离散段
    for (int i = 0; i < discreteLength; i++) {
        particle.velocity_OS_discrete_continuous[i] = ...;  // 同上
    }
    
    // MS离散段
    for (int i = 0; i < discreteLength; i++) {
        particle.velocity_MS_discrete_continuous[i] = ...;  // 同上
    }
}
```

**关键改变**：
- ✅ 打印段和离散段**分别更新**
- ✅ 每段有独立的OS和MS速度向量
- ✅ 总共4个独立的速度更新循环

---

#### 2.3 位置更新

**旧实现**：
```java
private void updatePosition(Particle particle) {
    int n = particle.position_OS_continuous.length;
    
    for (int i = 0; i < n; i++) {
        particle.position_OS_continuous[i] += particle.velocity_OS_continuous[i];
        particle.position_MS_continuous[i] += particle.velocity_MS_continuous[i];
    }
    
    particle.continuousToDiscrete_OS();
    particle.continuousToDiscrete_MS(input);
}
```

**新实现**：
```java
private void updatePosition(Particle particle) {
    //===== 打印段位置更新 =====
    int printLength = particle.position_OS_print_continuous.length;
    for (int i = 0; i < printLength; i++) {
        particle.position_OS_print_continuous[i] += particle.velocity_OS_print_continuous[i];
        particle.position_MS_print_continuous[i] += particle.velocity_MS_print_continuous[i];
        
        // 边界截断
        particle.position_OS_print_continuous[i] = clamp(particle.position_OS_print_continuous[i], 0, 1);
        particle.position_MS_print_continuous[i] = clamp(particle.position_MS_print_continuous[i], 0, 1);
    }
    
    //===== 离散段位置更新 =====
    int discreteLength = particle.position_OS_discrete_continuous.length;
    for (int i = 0; i < discreteLength; i++) {
        particle.position_OS_discrete_continuous[i] += particle.velocity_OS_discrete_continuous[i];
        particle.position_MS_discrete_continuous[i] += particle.velocity_MS_discrete_continuous[i];
        
        // 边界截断
        particle.position_OS_discrete_continuous[i] = clamp(particle.position_OS_discrete_continuous[i], 0, 1);
        particle.position_MS_discrete_continuous[i] = clamp(particle.position_MS_discrete_continuous[i], 0, 1);
    }
    
    //===== 映射到离散空间 =====
    particle.continuousToDiscrete_OS(input);      // 两阶段分别排序
    particle.continuousToDiscrete_MS(input);      // 两阶段分别映射
}
```

---

## 🎨 可视化：编码结构对比

### 旧实现（错误）

```
position_OS_continuous:  [0.3, 0.7, 0.5, | 0.2, 0.8, 0.4, ...]
                          ←─ 混在一起 ────────────────────→

position_MS_continuous:  [0.9, 0.4, 0.6, | 0.1, 0.7, 0.3, ...]
                          ←─ 混在一起，无法区分打印和离散 ─→

❌ 问题：
- 打印段和离散段混在一起
- 无法处理MS离散段的固定结构
- 离散段MS依赖OS顺序（错误）
```

### 新实现（正确）⭐

```
//===== 打印段（jobCount = 3） =====
position_OS_print_continuous:     [0.3, 0.7, 0.5]  ←─ 打印工序顺序
position_MS_print_continuous:     [0.9, 0.4, 0.6]  ←─ 对应的打印机选择
                                   ↓    ↓    ↓
gene_OS[0:3]:                     [J1,  J0,  J2]   ←─ 映射后的工件打印顺序
gene_MS[0:3]:                     [2,   1,   3]    ←─ 绝对打印机编号

//===== 离散段（discreteLength = 6） =====
position_OS_discrete_continuous:  [0.2, 0.8, 0.4, 0.6, 0.1, 0.9]  ←─ 离散工序顺序（可变）
position_MS_discrete_continuous:  [0.5, 0.3, 0.7, 0.2, 0.8, 0.4]  ←─ 机器选择（固定结构）
                                   ↓    ↓    ↓    ↓    ↓    ↓
gene_OS[3:9]:                     [J0,  J2,  J1,  J0,  J1,  J2]   ←─ 映射后的离散工序顺序（随PSO变化）

gene_MS[3:9]: 固定结构！         [1,   2,   3,   1,   2,   1]    ←─ 相对机器索引（不随OS变化）
              工件0的2个离散工序: ↑    ↑
              工件1的2个离散工序:           ↑    ↑
              工件2的2个离散工序:                     ↑    ↑
```

**解释**：
- 打印段：OS和MS一一对应，都随PSO变化
- 离散段OS：随PSO变化（可以是任意排列）
- 离散段MS：**固定结构**，始终按工件0→工件1→工件2的顺序排列离散工序

---

## ✅ 验证要点

### 1. 打印段验证

```java
// 验证：gene_OS[i] 和 gene_MS[i] 对应
for (int i = 0; i < jobCount; i++) {
    int jobNo = gene_OS[i];
    int machine = gene_MS[i];
    // machine应该是该jobNo可用的打印机绝对编号
}
```

### 2. 离散段验证

```java
// 验证：gene_MS离散段结构固定
int msIndex = jobCount;
for (int jobNo = 0; jobNo < jobCount; jobNo++) {
    int discreteOpsCount = operationCountArr[jobNo] - 2;
    for (int oper = 0; oper < discreteOpsCount; oper++) {
        int relativeIndex = gene_MS[msIndex];
        // relativeIndex 应该在 [1, availableMachinesCount] 范围内
        msIndex++;
    }
}
```

### 3. 映射一致性验证

```java
// 验证：连续→离散→评估 流程正确
particle.continuousToDiscrete_OS(input);
particle.continuousToDiscrete_MS(input);
double makespan = evaluate(particle.gene_OS, particle.gene_MS);
// makespan 应该是合法的调度结果
```

---

## 📊 总结

### 修改前的问题

1. ❌ 打印段和离散段混在单一连续向量中
2. ❌ MS离散段依赖OS顺序（应该是固定结构）
3. ❌ MS离散段存储绝对机器编号（应该是相对索引）
4. ❌ 无法正确处理两阶段的编码差异

### 修改后的优势

1. ✅ 打印段和离散段**完全分离**
2. ✅ MS离散段**固定结构**，不随OS变化
3. ✅ 正确区分**绝对打印机编号**和**相对机器索引**
4. ✅ 符合原始染色体编码的设计
5. ✅ PSO算法理论正确（标准连续PSO + 正确的离散映射）

---

**修改完成！现在PSO的两阶段编码完全符合原算法的设计要求！** 🎉

