# PSO粒子向量含义详解

## 粒子的向量结构

每个粒子（Particle）包含**8个向量**，分为两个阶段：

```
粒子 Particle
├── 打印段（Print Phase）
│   ├── position_OS_print_continuous[jobCount]      // 位置：OS排序key
│   ├── position_MS_print_continuous[jobCount]      // 位置：MS选择key
│   ├── velocity_OS_print_continuous[jobCount]      // 速度：OS变化率
│   └── velocity_MS_print_continuous[jobCount]      // 速度：MS变化率
│
└── 离散段（Discrete Phase）
    ├── position_OS_discrete_continuous[discreteLength]   // 位置：OS排序key
    ├── position_MS_discrete_continuous[discreteLength]   // 位置：MS选择key
    ├── velocity_OS_discrete_continuous[discreteLength]   // 速度：OS变化率
    └── velocity_MS_discrete_continuous[discreteLength]   // 速度：MS变化率
```

---

## 详细解释

### 1. 打印段位置向量

#### 1.1 `position_OS_print_continuous[i]` - 打印工序的排序key

**含义**：第i个打印工序在打印顺序中的优先级key

**维度**：`jobCount`（工件数量）

**范围**：`[-4, 4]`（扩展边界）

**作用**：
- 通过**sigmoid归一化 + 排序**确定打印顺序
- 值越小，排序后越靠前（越早打印）
- 值越大，排序后越靠后（越晚打印）

**示例**：
```java
// 假设有3个工件
position_OS_print_continuous = [-2.1, 1.5, 0.3]

// Sigmoid归一化
normalized = [0.109, 0.818, 0.574]

// 排序索引（从小到大）
sorted_indices = [0, 2, 1]

// 得到打印顺序
gene_OS[0:3] = [工件0, 工件2, 工件1]
// 解释：工件0最早打印，然后工件2，最后工件1
```

**物理意义**：
- `position_OS_print_continuous[0]` = -2.1 → 工件0的打印优先级很高（值小）
- `position_OS_print_continuous[1]` = 1.5 → 工件1的打印优先级低（值大）
- `position_OS_print_continuous[2]` = 0.3 → 工件2的打印优先级中等

---

#### 1.2 `position_MS_print_continuous[i]` - 打印机选择key

**含义**：第i个打印工序选择哪台打印机的倾向key

**维度**：`jobCount`

**范围**：`[-6, 6]`（扩展边界，比OS范围更大）

**作用**：
- 通过**sigmoid归一化**映射到可用打印机列表
- 值越小，倾向选择列表前面的机器
- 值越大，倾向选择列表后面的机器

**示例**：
```java
// 工件0的打印机选择
position_MS_print_continuous[0] = -3.2

// Sigmoid归一化
normalized = sigmoid(-3.2) = 0.039

// 可用打印机列表（假设工件0可以在这3台打印机上打印）
availablePrinters = [1, 3, 5]

// 选择索引
idx = (int)(0.039 * 3) = 0

// 选择的打印机
gene_MS[0] = availablePrinters[0] = 1  // 选择1号打印机
```

**物理意义**：
- `position_MS_print_continuous[i]` < 0 → 倾向选择编号较小的打印机
- `position_MS_print_continuous[i]` ≈ 0 → 选择中间的打印机
- `position_MS_print_continuous[i]` > 0 → 倾向选择编号较大的打印机

---

### 2. 离散段位置向量

#### 2.1 `position_OS_discrete_continuous[i]` - 离散工序的排序key

**含义**：第i个离散工序在加工顺序中的优先级key

**维度**：`discreteLength`（所有工件的离散工序总数）

**范围**：`[-4, 4]`

**作用**：
- 确定离散工序的加工顺序
- 通过排序映射到gene_OS的离散段

**示例**：
```java
// 假设有2个工件，每个工件有2个离散工序（工序2和工序3）
// 离散段总共4个工序
position_OS_discrete_continuous = [1.2, -0.5, 2.3, -1.8]

// Sigmoid归一化
normalized = [0.769, 0.378, 0.909, 0.142]

// 排序索引
sorted_indices = [3, 1, 0, 2]

// 初始gene_OS离散段（按工件顺序）
original = [工件0-工序2, 工件0-工序3, 工件1-工序2, 工件1-工序3]

// 排序后的gene_OS离散段
gene_OS[jobCount:end] = [工件1-工序3, 工件0-工序3, 工件0-工序2, 工件1-工序2]
```

**物理意义**：控制离散加工阶段哪个工序先加工、哪个后加工

---

#### 2.2 `position_MS_discrete_continuous[i]` - 离散工序的机器选择key

**含义**：第i个离散工序选择哪台加工机器的倾向key

**维度**：`discreteLength`

**范围**：`[-6, 6]`

**作用**：
- 为离散工序选择加工机器
- 映射到相对机器索引

**示例**：
```java
// 工件0的工序2的机器选择
position_MS_discrete_continuous[0] = 2.1

// Sigmoid归一化
normalized = sigmoid(2.1) = 0.891

// 该工序可用机器列表
availableMachines = [5, 6, 7, 8, 9]  // 5台可选

// 相对索引
relativeIndex = (int)(0.891 * 5) + 1 = 4 + 1 = 5

// 存储相对索引
gene_MS[jobCount] = 5  // 表示选择第5台机器，即9号机器
```

**物理意义**：控制离散工序在哪台机器上加工

---

### 3. 速度向量

所有速度向量的含义相同：**表示对应位置向量的变化率**

#### 3.1 `velocity_OS_print_continuous[i]`

**含义**：打印段第i个工序的排序key的变化速度

**范围**：`[-v_max*0.5, v_max*0.5]`，其中`v_max=1.4`，所以是`[-0.7, 0.7]`

**作用**：
- 控制打印顺序的变化快慢
- 速度大→位置变化快→打印顺序容易改变
- 速度小→位置变化慢→打印顺序相对稳定

**更新公式**：
```java
v = w*v + c1*r1*(pBest - x) + c2*r2*(gBest - x)
x = x + v
```

**示例**：
```java
// 当前位置
position_OS_print_continuous[0] = 1.5

// 当前速度
velocity_OS_print_continuous[0] = 0.3

// 更新后
position_OS_print_continuous[0] = 1.5 + 0.3 = 1.8
// 工件0的打印优先级降低（值变大）→ 可能打印顺序靠后
```

#### 3.2 `velocity_MS_print_continuous[i]`

**含义**：打印段第i个工序的机器选择key的变化速度

**范围**：`[-v_max, v_max]`，即`[-1.4, 1.4]`

**作用**：控制打印机选择的变化

**示例**：
```java
// 当前：工件0倾向选择1号打印机
position_MS_print_continuous[0] = -3.0  // sigmoid ≈ 0.047
// 可用打印机 [1, 3, 5]，选择idx=0，即1号

// 速度
velocity_MS_print_continuous[0] = 2.5

// 更新后
position_MS_print_continuous[0] = -3.0 + 2.5 = -0.5  // sigmoid ≈ 0.378
// 选择idx=1，即3号打印机
// 解释：机器选择从1号变成了3号
```

#### 3.3 离散段速度向量

`velocity_OS_discrete_continuous[i]` 和 `velocity_MS_discrete_continuous[i]` 的含义与打印段类似，只是作用于离散工序。

---

## 完整示例：一个粒子的所有向量

假设有**2个工件**，每个工件有**1个打印工序 + 2个离散工序**：

```java
Particle p = new Particle(problem, r);

// ========== 打印段（2个工件的打印工序）==========

// OS排序key（决定打印顺序）
p.position_OS_print_continuous = [-1.2, 2.3]
// 解释：
//   工件0: -1.2 (优先级高，先打印)
//   工件1: 2.3  (优先级低，后打印)

// MS选择key（决定打印机）
p.position_MS_print_continuous = [-3.5, 1.8]
// 解释：
//   工件0: -3.5 (倾向选前面的打印机)
//   工件1: 1.8  (倾向选后面的打印机)

// 速度
p.velocity_OS_print_continuous = [0.2, -0.3]
p.velocity_MS_print_continuous = [0.5, -0.8]

// ========== 离散段（4个离散工序）==========

// OS排序key（决定加工顺序）
p.position_OS_discrete_continuous = [0.8, -1.5, 2.1, -0.3]
// 解释：工件0的2个工序，工件1的2个工序
// 排序后确定哪个工序先加工

// MS选择key（决定加工机器）
p.position_MS_discrete_continuous = [1.2, -2.3, 0.5, 3.1]
// 解释：每个工序选择哪台机器加工

// 速度
p.velocity_OS_discrete_continuous = [0.1, -0.2, 0.3, -0.1]
p.velocity_MS_discrete_continuous = [0.4, -0.6, 0.2, 0.5]
```

---

## 向量到调度方案的映射过程

### 步骤1：解码OS（工序顺序）

```java
// 打印段
position_OS_print_continuous = [-1.2, 2.3]
sigmoid归一化 → [0.231, 0.909]
排序 → gene_OS[0:2] = [工件0, 工件1]

// 离散段
position_OS_discrete_continuous = [0.8, -1.5, 2.1, -0.3]
sigmoid归一化 → [0.690, 0.182, 0.891, 0.426]
排序 → gene_OS[2:6] = [工件0-工序3, 工件1-工序3, 工件0-工序2, 工件1-工序2]
```

### 步骤2：解码MS（机器选择）

```java
// 打印段
工件0: position_MS = -3.5 → sigmoid = 0.029
       可用打印机[1,2,3] → 选择idx=0 → 1号打印机

工件1: position_MS = 1.8 → sigmoid = 0.858
       可用打印机[1,2,3] → 选择idx=2 → 3号打印机

// 离散段（类似）
```

### 步骤3：得到完整调度方案

```
调度方案：
1. 工件0在1号打印机上打印
2. 工件1在3号打印机上打印
3. 批处理（自动）
4. 工件0的工序3在X号机器上加工
5. 工件1的工序3在Y号机器上加工
6. 工件0的工序2在Z号机器上加工
7. 工件1的工序2在W号机器上加工
```

---

## PSO更新过程的物理意义

### 速度更新

```java
v[i] = w*v[i] + c1*r1*(pBest[i] - x[i]) + c2*r2*(gBest[i] - x[i])
```

**三个力的平衡**：

1. **惯性项** `w*v[i]`：
   - 保持当前的搜索方向
   - w大→搜索范围广（全局探索）
   - w小→精细搜索（局部开发）

2. **认知项** `c1*r1*(pBest[i] - x[i])`：
   - 向自己的历史最优靠拢
   - 个体经验的学习

3. **社会项** `c2*r2*(gBest[i] - x[i])`：
   - 向全局最优靠拢
   - 群体智慧的学习

### 位置更新

```java
x[i] = x[i] + v[i]
```

**效果**：
- 速度正→位置增大→可能改变排序/机器选择
- 速度负→位置减小→可能改变排序/机器选择
- 速度为0→位置不变→保持当前方案

---

## 每个维度的影响

| 向量维度 | 直接影响 | 间接影响 | 优化目标 |
|---------|---------|---------|---------|
| `position_OS_print` | 打印顺序 | 批次大小、批处理时间 | 最小化打印makespan |
| `position_MS_print` | 打印机选择 | 负载均衡、并行度 | 均衡打印机负载 |
| `position_OS_discrete` | 离散工序顺序 | 机器等待时间 | 减少空闲时间 |
| `position_MS_discrete` | 加工机器选择 | 关键路径、负载均衡 | 缩短关键路径 |

---

## 调试：查看向量值

```java
// 在PSO中添加
public void printParticleVectors(Particle p) {
    System.out.println("\n========== 粒子向量详情 ==========");
    
    // 打印段
    System.out.println("\n【打印段】");
    System.out.println("OS位置: " + Arrays.toString(p.position_OS_print_continuous));
    System.out.println("MS位置: " + Arrays.toString(p.position_MS_print_continuous));
    System.out.println("OS速度: " + Arrays.toString(p.velocity_OS_print_continuous));
    System.out.println("MS速度: " + Arrays.toString(p.velocity_MS_print_continuous));
    
    // 离散段
    System.out.println("\n【离散段】");
    System.out.println("OS位置: " + Arrays.toString(p.position_OS_discrete_continuous));
    System.out.println("MS位置: " + Arrays.toString(p.position_MS_discrete_continuous));
    System.out.println("OS速度: " + Arrays.toString(p.velocity_OS_discrete_continuous));
    System.out.println("MS速度: " + Arrays.toString(p.velocity_MS_discrete_continuous));
    
    // 解码后的染色体
    System.out.println("\n【解码后】");
    System.out.println("gene_OS: " + Arrays.toString(p.gene_OS));
    System.out.println("gene_MS: " + Arrays.toString(p.gene_MS));
    System.out.println("fitness: " + p.fitness);
    
    System.out.println("=====================================\n");
}

// 使用
printParticleVectors(gBest);
```

---

## 总结

每个粒子有**4类向量**：

1. **OS位置**：通过排序确定工序执行顺序
2. **MS位置**：通过归一化选择确定机器分配
3. **OS速度**：控制工序顺序的变化速度
4. **MS速度**：控制机器选择的变化速度

这些向量通过PSO的速度-位置更新公式不断优化，最终找到最优的调度方案（最小makespan）。

**关键理解**：
- 位置向量 = Random Key（通过sigmoid归一化和排序/选择映射到离散方案）
- 速度向量 = 搜索方向（决定位置如何变化）
- PSO在连续空间搜索，通过解码得到离散调度方案





