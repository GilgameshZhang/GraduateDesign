# ALNS-SL算法实现说明

## 概述

本模块实现了论文中的ALNS-SL（Adaptive Large Neighborhood Search + Skyline Packing）对比算法，目标函数改为最小化最大完工时间Cmax（Makespan）。

## 算法特点

- **目标函数**: 最小化Cmax（最大完工时间）
- **装箱方法**: Skyline装箱算法（支持旋转）
- **邻域搜索**: Destroy + Repair框架
- **接受准则**: 模拟退火（SA）
- **自适应机制**: 轮盘赌选择算子 + 动态权重更新

## 核心组件

### 1. 数据结构
- `ALNSJob`: 作业表示（尺寸、释放期、处理时间）
- `ALNSMachine`: 机器表示（平台尺寸、处理参数）
- `ALNSBatch`: 批次表示（作业列表、装箱坐标、时间信息）
- `ALNSSolution`: 解表示（每台机器的批次序列）

### 2. Destroy算子（11个）

| 算子 | 名称 | 说明 |
|------|------|------|
| RRJ | Random Removal Job | 随机移除作业 |
| SR | Shaw Removal | 基于相似度移除（释放期+处理时间） |
| WRJ | Worst Removal Job | 移除使Cmax下降最大的作业 |
| PTR | Processing Time Removal | 移除处理时间不合适的作业 |
| IR | Idle Removal | 移除最长空闲段的作业 |
| RRB | Random Removal Batch | 随机移除整批 |
| PTVR | Processing Time Variance Removal | 移除处理时间方差最大的批次 |
| RTVR | Release Time Variance Removal | 移除释放期方差最大的批次 |
| DTVR | Min Processing Time Variance Removal | 移除最小处理时间方差最大的批次 |
| MTR | Max Cmax Contribution Removal | 移除对Cmax贡献最大的批次 |
| TR | Critical Machine Removal | 移除关键机器上的所有作业 |

### 3. Repair算子（7个）

| 算子 | 名称 | 说明 |
|------|------|------|
| GID | Greedy Insertion by Min Processing | 按最小处理时间降序贪婪插入 |
| GIR | Greedy Insertion by Release | 按释放期升序贪婪插入 |
| SI | Slack Insertion | 按紧迫度插入（r_j + min p_j） |
| RBI | Regret Batch Insertion | 三段式评分插入 |
| RI | Regret Insertion | 基于Cmax增量的regret插入 |
| PTI | Processing Time Insertion | 优先插入到处理时间最短的机器 |
| RIJ | Random Insertion | 随机插入 |

### 4. 局部搜索（3个邻域）
- **Relocation**: 将作业从当前批次移动到另一批次
- **Swap**: 交换两个作业的位置
- **Split**: 将一个批次分割成两个批次

## 参数设置

采用论文给出的ALNS-SL参数作为基线：

```java
T0 = 14.0           // 初始温度
beta = 0.7378       // 温度衰减系数
theta = 0.1243      // 移除比例
gamma1 = 0.3442     // RBI权重1
gamma2 = 0.2679     // RBI权重2
r = 0.0412          // 权重更新参数
sigma1 = 33         // 找到新最优解的奖励
sigma2 = 24         // 接受但不是最优的奖励
sigma3 = 13         // 拒绝的奖励
```

## 使用方法

### 方法1: 直接使用ALNSRunner

```java
import AlgorithmFrame.alns.ALNSRunner;
import AlgorithmFrame.alns.ALNSParameters;
import AlgorithmFrame.alns.ALNSSolution;

public class Example {
    public static void main(String[] args) {
        String machinePath = "path/to/machine_file";
        String itemPath = "path/to/item_file";
        
        // 使用默认参数
        ALNSSolution solution = ALNSRunner.runALNSWithDefaultParams(machinePath, itemPath);
        
        // 或使用自定义参数
        ALNSParameters params = new ALNSParameters();
        params.maxIterations = 5000;
        params.timeLimitMs = 300000; // 5分钟
        
        ALNSSolution solution2 = ALNSRunner.runALNS(machinePath, itemPath, params, 12345L);
        
        System.out.println("最优Cmax: " + solution.cmax);
    }
}
```

### 方法2: 手动构建

```java
import AlgorithmFrame.alns.*;

// 1. 准备数据
ALNSJob[] jobs = ...; // 构建作业数组
ALNSMachine[] machines = ...; // 构建机器数组

// 2. 设置参数
ALNSParameters params = new ALNSParameters();

// 3. 运行算法
ALNS alns = new ALNS(jobs, machines, params, 12345L);
ALNSSolution solution = alns.solve();

// 4. 获取结果
System.out.println("Cmax: " + solution.cmax);
```

## 对比实验

使用`ComparisonExperiment`类进行算法对比：

```java
import AlgorithmFrame.alns.ComparisonExperiment;

ComparisonExperiment.runComparison(
    "chapter-1/src/main/resources/Machine/machine_2",
    "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01",
    5  // 运行5次取平均
);
```

## 输出结果

算法会输出：
1. 初始解的Cmax
2. 每次找到更优解时的迭代次数和Cmax值
3. 最终的最优Cmax
4. 总运行时间和迭代次数
5. 每台机器的批次详细信息

## 注意事项

1. **数据格式**: 算法使用第一章的数据格式（Machine和Item）
2. **处理时间计算**: 处理时间 = 准备时间 + (层数 × 换层时间)
3. **释放期**: 如果数据中没有提供释放期，默认为0
4. **装箱可行性**: 使用Skyline算法保证装箱可行性
5. **随机种子**: 可以设置固定种子保证结果可复现

## 算法流程

```
1. 生成初始解（按r_j升序 + p*_j降序贪婪插入）
2. ALNS主循环：
   a. 轮盘赌选择Destroy和Repair算子
   b. Destroy: 移除q=⌈θ×n⌉个作业
   c. Repair: 重新插入移除的作业
   d. LocalSearch: 三个邻域搜索改进
   e. SA接受准则
   f. 更新算子分数
   g. 降温: T = T × beta
   h. 每50次迭代更新算子权重
3. 返回最优解
```

## 扩展

如需扩展算法：
1. 添加新的Destroy算子：实现`DestroyOperator`接口
2. 添加新的Repair算子：实现`RepairOperator`接口
3. 修改局部搜索：在`LocalSearch`类中添加新邻域
4. 调整参数：修改`ALNSParameters`类

## 参考

本实现基于论文中的ALNS-SL算法框架，所有算子和参数均与论文保持一致，仅将目标函数从总拖期改为最大完工时间Cmax。
