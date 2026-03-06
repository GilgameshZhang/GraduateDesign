# ALNS-SL 快速入门指南

## 5分钟快速开始

### 第一步：运行快速测试

```bash
# 在IDE中运行
chapter-1/src/test/java/QuickTestALNS.java
```

或者使用命令行：

```bash
cd chapter-1
mvn test -Dtest=QuickTestALNS
```

### 第二步：查看结果

测试程序会自动：
1. ✅ 运行小规模算例（2机器10作业）
2. ✅ 测试自定义参数
3. ✅ 输出最优Cmax和详细解信息

预期输出类似：
```
========================================
ALNS-SL算法快速测试
========================================

【测试1】小规模算例 - 默认参数
----------------------------------------
初始解 Cmax: 156.23
迭代 45: 新最优解 Cmax = 142.67
迭代 128: 新最优解 Cmax = 138.92
...
ALNS完成！总迭代: 1000, 运行时间: 8534ms, 最优Cmax: 135.45
✅ 测试1通过
   最优Cmax: 135.45
   批次总数: 8
```

## 三种使用方式

### 方式1: 最简单 - 使用默认参数

```java
import AlgorithmFrame.alns.ALNSRunner;
import AlgorithmFrame.alns.ALNSSolution;

public class Example {
    public static void main(String[] args) {
        ALNSSolution solution = ALNSRunner.runALNSWithDefaultParams(
            "chapter-1/src/main/resources/Machine/machine_2",
            "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01"
        );
        
        System.out.println("最优Cmax: " + solution.cmax);
    }
}
```

### 方式2: 自定义参数

```java
import AlgorithmFrame.alns.*;

ALNSParameters params = new ALNSParameters();
params.maxIterations = 5000;        // 最大迭代次数
params.timeLimitMs = 300000;        // 时间限制（5分钟）
params.theta = 0.15;                // 移除比例（15%作业）
params.T0 = 20.0;                   // 初始温度

ALNSSolution solution = ALNSRunner.runALNS(
    machinePath, 
    itemPath, 
    params, 
    12345L  // 随机种子
);
```

### 方式3: 运行对比实验

```java
import ComparisonExperiment;

// 运行5次实验并统计结果
ComparisonExperiment.runComparison(
    "chapter-1/src/main/resources/Machine/machine_3",
    "chapter-1/src/main/resources/PrintItem/printItem_20/printItem_20_01",
    5  // 运行次数
);
```

输出结果会自动保存到：
```
chapter-1/src/main/output/ALNS_comparison_results.csv
```

## 常见参数调整

### 提高解质量
```java
params.maxIterations = 10000;      // 增加迭代次数
params.timeLimitMs = 600000;       // 增加时间限制（10分钟）
params.theta = 0.1;                // 减小破坏程度（更细致搜索）
```

### 加快运行速度
```java
params.maxIterations = 1000;       // 减少迭代次数
params.timeLimitMs = 60000;        // 减少时间限制（1分钟）
params.theta = 0.2;                // 增大破坏程度（更激进搜索）
```

### 适用于大规模问题
```java
params.theta = 0.15;               // 中等破坏程度
params.T0 = 30.0;                  // 提高初始温度
params.beta = 0.9;                 // 减慢降温速度
```

## 算法参数说明

| 参数 | 默认值 | 说明 | 调整建议 |
|------|--------|------|----------|
| `T0` | 14.0 | 初始温度 | 大问题增大，小问题减小 |
| `beta` | 0.7378 | 温度衰减系数 | 0.7-0.95，越大降温越慢 |
| `theta` | 0.1243 | 移除比例 | 0.05-0.30，越大破坏越激进 |
| `maxIterations` | 10000 | 最大迭代次数 | 根据时间要求调整 |
| `timeLimitMs` | 300000 | 时间限制（ms） | 5分钟 = 300000ms |

## 输出结果解读

### 控制台输出

```
初始解 Cmax: 156.23                    ← 初始解质量
迭代 45: 新最优解 Cmax = 142.67        ← 找到更优解
迭代 128: 新最优解 Cmax = 138.92       ← 持续改进
迭代 1000: 当前Cmax = 141.23, 最优Cmax = 135.45  ← 定期报告
ALNS完成！总迭代: 1000, 运行时间: 8534ms, 最优Cmax: 135.45
```

### 解对象信息

```java
ALNSSolution solution = ...;

// 获取目标值
double cmax = solution.cmax;

// 获取每台机器的批次
for (int m = 0; m < numMachines; m++) {
    List<ALNSBatch> batches = solution.getMachineBatches(m);
    
    for (ALNSBatch batch : batches) {
        System.out.println("批次: " + batch.jobs.size() + "个作业");
        System.out.println("开始: " + batch.startTime);
        System.out.println("结束: " + batch.endTime);
        
        // 获取装箱坐标
        for (int jobId : batch.jobs) {
            var placement = batch.placements.get(jobId);
            System.out.println("  作业" + jobId + 
                ": [" + placement.x1 + "," + placement.y1 + "]" +
                (placement.rotated ? " (旋转)" : ""));
        }
    }
}
```

## 常见问题

### Q1: 算法运行很慢怎么办？
**A:** 减少迭代次数或时间限制：
```java
params.maxIterations = 1000;
params.timeLimitMs = 60000;  // 1分钟
```

### Q2: 解的质量不好怎么办？
**A:** 增加搜索强度：
```java
params.maxIterations = 10000;
params.timeLimitMs = 600000;  // 10分钟
params.theta = 0.08;  // 减小破坏程度，更细致搜索
```

### Q3: 如何确保结果可复现？
**A:** 使用固定的随机种子：
```java
ALNSRunner.runALNS(machinePath, itemPath, params, 12345L);
```

### Q4: 无法生成初始解？
**A:** 可能原因：
- 作业尺寸超过机器平台尺寸
- 数据文件格式错误
- 检查数据文件路径是否正确

### Q5: 如何与其他算法对比？
**A:** 使用对比实验类：
```java
ComparisonExperiment.main(new String[]{});
```

## 下一步

- 📖 阅读完整文档: `README.md`
- 🔍 查看实现细节: `ALNS_IMPLEMENTATION_SUMMARY.md`
- 🧪 运行对比实验: `ComparisonExperiment.java`
- 🎨 自定义算子: 实现`DestroyOperator`或`RepairOperator`接口

## 技术支持

如遇到问题：
1. 检查数据文件路径和格式
2. 查看控制台错误信息
3. 参考`QuickTestALNS.java`示例代码
4. 阅读详细文档`README.md`

---

**祝您使用愉快！** 🚀
