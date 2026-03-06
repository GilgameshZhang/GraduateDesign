# PSO扩展边界修复说明

> **问题**: 修改边界从 `[0, 1]` 到 `[-4, 4]` 和 `[-6, 6]` 后，出现无效机器编号（0）错误

---

## 🚨 问题描述

### 错误信息

```
java.lang.RuntimeException: gene_MS包含无效的机器编号: 0
	at ProblemFrame.CaculateFitness.evaluate(CaculateFitness.java:366)
	at AlgorthmFrame.pso.PSO.evaluateParticle(PSO.java:365)
	at AlgorthmFrame.pso.PSO.solve(PSO.java:215)
```

### 原因分析

#### 1. 边界修改

**旧边界**（原始设计）:
```java
// 位置边界
particle.position_OS_print_continuous[i] = Math.max(0.0, Math.min(1.0, ...));
particle.position_MS_print_continuous[i] = Math.max(0.0, Math.min(1.0, ...));

// 速度限制
double v_max = 0.2;
```

**新边界**（修改后）:
```java
// 位置边界扩大
particle.position_OS_print_continuous[i] = Math.max(-4, Math.min(4.0, ...));
particle.position_MS_print_continuous[i] = Math.max(-6, Math.min(6.0, ...));

// 速度限制增大
double v_max = 1.4;
```

**目的**: 扩大搜索空间，允许粒子在更大的范围内移动。

#### 2. 映射问题

**旧映射逻辑**（假设连续值在 `[0, 1]`）:
```java
// 打印段MS映射
double continuousValue = position_MS_print_continuous[i];  // 假设 ∈ [0, 1]
int idx = (int) (continuousValue * availablePrinters.size());
gene_MS[i] = availablePrinters.get(idx);

// 离散段MS映射
double continuousValue = position_MS_discrete_continuous[discreteIndex];  // 假设 ∈ [0, 1]
int relativeIndex = (int) (continuousValue * availableMachines.size()) + 1;
gene_MS[msIndex] = relativeIndex;
```

**问题**：
- 当 `continuousValue < 0` 时，`idx` 会是负数
- 当 `continuousValue < 0` 时，`relativeIndex` 可能是 `≤ 0`（无效的机器索引）

**示例**：
```java
// 假设 availableMachines.size() = 3
continuousValue = -0.5  // 负值
relativeIndex = (int) (-0.5 * 3) + 1 = (int)(-1.5) + 1 = -1 + 1 = 0  ❌ 无效！

continuousValue = -2.0  // 更大的负值
relativeIndex = (int) (-2.0 * 3) + 1 = -6 + 1 = -5  ❌ 无效！
```

---

## ✅ 解决方案

### 使用Sigmoid函数归一化

添加一个**Sigmoid函数**，将任意范围的连续值映射到 `(0, 1)` 范围：

```java
/**
 * Sigmoid函数：将任意实数映射到(0, 1)范围
 * 用于处理扩展边界后的连续位置向量
 * 
 * @param x 输入值（可以是任意实数）
 * @return 映射后的值，范围在(0, 1)
 */
private double sigmoid(double x) {
    return 1.0 / (1.0 + Math.exp(-x));
}
```

**Sigmoid函数的性质**：
- `sigmoid(-∞) → 0`
- `sigmoid(0) = 0.5`
- `sigmoid(+∞) → 1`
- 单调递增
- 输出范围：`(0, 1)`

**映射效果**：
```
输入值      Sigmoid输出
-6.0    →  0.0025  (接近0)
-4.0    →  0.0180
-2.0    →  0.1192
-1.0    →  0.2689
 0.0    →  0.5000  (中间值)
 1.0    →  0.7311
 2.0    →  0.8808
 4.0    →  0.9820
 6.0    →  0.9975  (接近1)
```

### 修改后的映射逻辑

#### 打印段MS映射

```java
// 旧代码（有bug）
double continuousValue = position_MS_print_continuous[i];
int idx = (int) (continuousValue * availablePrinters.size());
gene_MS[i] = availablePrinters.get(idx);

// 新代码（修复）
double continuousValue = position_MS_print_continuous[i];
double normalizedValue = sigmoid(continuousValue);  // ⚠️ 归一化到(0, 1)

int idx = (int) (normalizedValue * availablePrinters.size());
idx = Math.max(0, Math.min(idx, availablePrinters.size() - 1));  // ⚠️ 确保索引有效
gene_MS[i] = availablePrinters.get(idx);
```

#### 离散段MS映射

```java
// 旧代码（有bug）
double continuousValue = position_MS_discrete_continuous[discreteIndex];
int relativeIndex = (int) (continuousValue * availableMachines.size()) + 1;
gene_MS[msIndex] = relativeIndex;

// 新代码（修复）
double continuousValue = position_MS_discrete_continuous[discreteIndex];
double normalizedValue = sigmoid(continuousValue);  // ⚠️ 归一化到(0, 1)

// normalizedValue ∈ [0, 1]，映射到 [1, size]
int relativeIndex = (int) (normalizedValue * availableMachines.size()) + 1;
relativeIndex = Math.max(1, Math.min(relativeIndex, availableMachines.size()));  // ⚠️ 确保在[1, size]范围内
gene_MS[msIndex] = relativeIndex;
```

---

## 📊 修复前后对比

### 场景1：负值输入

```java
availableMachines.size() = 3
continuousValue = -2.0

// 旧代码
relativeIndex = (int) (-2.0 * 3) + 1 = -6 + 1 = -5  ❌ 无效！

// 新代码
normalizedValue = sigmoid(-2.0) = 0.1192
relativeIndex = (int) (0.1192 * 3) + 1 = 0 + 1 = 1  ✅ 有效！（第1个可用机器）
```

### 场景2：大正值输入

```java
availableMachines.size() = 3
continuousValue = 5.0

// 旧代码
relativeIndex = (int) (5.0 * 3) + 1 = 15 + 1 = 16  ❌ 越界！

// 新代码
normalizedValue = sigmoid(5.0) = 0.9933
relativeIndex = (int) (0.9933 * 3) + 1 = 2 + 1 = 3  ✅ 有效！（第3个可用机器）
relativeIndex = Math.min(3, 3) = 3  ✅ 确保不越界
```

### 场景3：[0, 1]范围内的值（保持兼容）

```java
availableMachines.size() = 3
continuousValue = 0.6

// 旧代码
relativeIndex = (int) (0.6 * 3) + 1 = 1 + 1 = 2  ✅ 有效

// 新代码
normalizedValue = sigmoid(0.6) = 0.6457
relativeIndex = (int) (0.6457 * 3) + 1 = 1 + 1 = 2  ✅ 有效（略有偏移，但仍合理）
```

---

## 🎯 为什么不使用简单的截断？

**方案1：简单截断到 `[0, 1]`**
```java
double normalizedValue = Math.max(0.0, Math.min(1.0, continuousValue));
```

**问题**：
- 丧失了扩展边界的意义
- 所有 `< 0` 的值都映射到0，所有 `> 1` 的值都映射到1
- 失去了连续性和平滑性

**方案2：线性归一化**
```java
// 假设边界是 [-4, 4]
double normalizedValue = (continuousValue + 4) / 8;  // 映射到 [0, 1]
```

**问题**：
- 需要硬编码边界值
- 如果值超出 `[-4, 4]`，仍然会失效
- 不够鲁棒

**方案3：Sigmoid归一化**（采用）✅
```java
double normalizedValue = sigmoid(continuousValue);
```

**优点**：
- ✅ 可以处理任意范围的输入（`-∞` 到 `+∞`）
- ✅ 平滑且连续
- ✅ 保持单调性
- ✅ 无需硬编码边界值
- ✅ 鲁棒性强

---

## 📝 修改清单

### 修改的文件

1. **`ProblemFrame/Particle.java`**
   - 添加 `sigmoid()` 方法
   - 修改 `continuousToDiscrete_MS()` 方法

### 具体修改

#### 1. 添加Sigmoid函数

```java
/**
 * Sigmoid函数：将任意实数映射到(0, 1)范围
 */
private double sigmoid(double x) {
    return 1.0 / (1.0 + Math.exp(-x));
}
```

#### 2. 修改打印段MS映射

```java
// 归一化连续值
double normalizedValue = sigmoid(continuousValue);

// 使用归一化后的值进行映射
int idx = (int) (normalizedValue * availablePrinters.size());
idx = Math.max(0, Math.min(idx, availablePrinters.size() - 1));
gene_MS[i] = availablePrinters.get(idx);
```

#### 3. 修改离散段MS映射

```java
// 归一化连续值
double normalizedValue = sigmoid(continuousValue);

// 使用归一化后的值进行映射
int relativeIndex = (int) (normalizedValue * availableMachines.size()) + 1;
relativeIndex = Math.max(1, Math.min(relativeIndex, availableMachines.size()));
gene_MS[msIndex] = relativeIndex;
```

---

## ✅ 验证结果

### 编译检查
- ✅ 无编译错误
- ⚠️ 只有1个警告（未使用的import，可忽略）

### 运行检查
- ✅ 不再出现 `gene_MS包含无效的机器编号: 0` 错误
- ✅ 所有机器索引都在有效范围内
- ✅ PSO算法可以正常运行

---

## 🎨 Sigmoid函数可视化

```
     1.0 ┤                    ╭────────
         │                  ╭─╯        
     0.8 ┤               ╭──╯          
         │             ╭─╯             
     0.6 ┤          ╭──╯               
         │        ╭─╯                  
     0.4 ┤     ╭──╯                    
         │   ╭─╯                       
     0.2 ┤ ╭─╯                         
         │╭╯                           
     0.0 ┼────────┬────────┬────────┬──
        -6       -3        0        3   6
                  输入值 (x)
```

**特点**：
- 平滑的S型曲线
- 在x=0附近变化最快（敏感区域）
- 在x的两端趋于平缓（饱和区域）
- 保证输出永远在 (0, 1) 范围内

---

## 💡 建议

### 1. 边界设置的权衡

**较小边界** (如 `[-1, 1]`):
- ✅ 更接近原始 `[0, 1]`，映射更均匀
- ❌ 搜索空间较小

**较大边界** (如 `[-6, 6]`):
- ✅ 搜索空间更大，探索能力更强
- ⚠️ 大部分值会映射到接近0或1（sigmoid的饱和区域）

**推荐边界**：`[-2, 2]` 或 `[-3, 3]`
- 平衡探索和利用
- 在sigmoid的有效区域内

### 2. 速度限制的建议

当前设置：`v_max = 1.4`

**建议**：
- 如果边界是 `[-4, 4]`，`v_max` 应该约为 `0.5 - 1.0`
- 如果边界是 `[-2, 2]`，`v_max` 应该约为 `0.3 - 0.6`

**原则**：速度不应该太大，否则粒子会频繁撞到边界。

### 3. 参数调优顺序

1. 先确定边界范围（如 `[-2, 2]`）
2. 设置合适的速度限制（如 `v_max = 0.5`）
3. 调整惯性权重 `w`（从0.9递减到0.4）
4. 调整学习因子 `c1`, `c2`（通常 `c1 + c2 ≈ 4.0`）

---

## 🚀 总结

1. **问题根源**：扩展边界后，连续值可能为负数或大于1，导致机器索引计算错误
2. **解决方案**：使用Sigmoid函数将任意范围的连续值归一化到 `(0, 1)`
3. **效果**：完全消除无效机器编号的错误，同时保持扩展边界的优势
4. **鲁棒性**：可以处理任意范围的连续值，无需硬编码边界

**修复完成！PSO现在可以安全地使用扩展边界进行搜索！** 🎉

