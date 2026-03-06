# 最左最下规则（Bottom-Left Rule）装箱算法

## 算法简介

最左最下规则（BL规则）是二维矩形装箱问题中的一种经典启发式算法。该算法的核心思想是：
**在放置每个物品时，让它在满足不重叠约束的前提下，尽可能地向左下角移动**。

## 算法原理

### 基本步骤

1. **初始化**：从空容器开始
2. **逐个放置**：按照给定序列顺序依次放置物品
3. **BL规则定位**：对每个物品执行以下步骤：
   - 生成候选放置点（包括原点、已放置物品的角点）
   - 对每个候选点应用BL规则：
     - 先尽量**向下**移动，直到碰到容器底边或其他物品
     - 再尽量**向左**移动，直到碰到容器左边或其他物品
   - 选择**最下最左**的可行位置

### 算法特点

**优点：**
- ✅ 简单直观，易于理解和实现
- ✅ 计算速度快，适合大规模问题
- ✅ 产生相对紧凑的排样方案
- ✅ 是装箱问题研究中的经典基准算法

**缺点：**
- ⚠️ 结果高度依赖物品的排列顺序
- ⚠️ 容易产生局部最优解
- ⚠️ 可能在右上角留下较多空隙

## 文件说明

### BottomLeftPacking.java
核心算法实现类，包含：
- `packing()` - 单盘装箱方法
- `packings()` - 多盘分批装箱方法
- `findBestPosition()` - 寻找最佳放置位置
- `applyBottomLeftRule()` - 应用BL规则
- `moveDown()` / `moveLeft()` - 向下/向左移动逻辑

### Run.java
测试运行类，提供：
- 多种排序策略测试（原始、面积降序、长边降序、周长降序、宽度降序）
- 性能统计（运行时间、利用率、托盘数等）
- 结果可视化数据保存

## 使用方法

### 1. 直接运行测试

```bash
# 编译并运行
cd chapter-1
javac -cp src/main/java src/main/java/AlgorithmFrame/bachSelect/bottomLeft/Run.java
java -cp src/main/java AlgorithmFrame.bachSelect.bottomLeft.Run
```

### 2. 修改测试数据路径

在 `Run.java` 中修改：
```java
String itemPath = "src/main/resources/PrintItem/printItem_10/printItem_10_01";
String machinePath = "src/main/resources/Machine/machine_2";
```

### 3. 在其他算法中使用

```java
// 创建物品数组（按某种顺序排列）
Item[] items = getItemsInOrder();

// 创建BL算法实例
BottomLeftPacking blPacking = new BottomLeftPacking(
    machineWidth,    // 容器宽度
    machineHeight,   // 容器高度
    items,           // 物品数组
    true             // 是否允许旋转
);

// 执行多盘装箱
List<Solution> solutions = blPacking.packings();

// 处理结果
for (Solution solution : solutions) {
    System.out.println("利用率: " + solution.rate);
    System.out.println("物品数: " + solution.placeItemList.size());
}
```

## 与天际线算法的对比

| 特性 | BL规则 | 天际线算法 |
|------|--------|-----------|
| **基本策略** | 向左下移动 | 基于天际线选择 |
| **实现复杂度** | 简单 | 中等 |
| **计算效率** | 快 | 中等 |
| **空间利用率** | 依赖排序 | 通常更好 |
| **适用场景** | 基准对比 | 实际应用 |

## 实验建议

### 作为对比基准

1. **排序策略实验**
   - 测试不同排序对BL算法的影响
   - 找出最适合BL规则的排序方式

2. **与天际线对比**
   - 比较相同排序下的利用率
   - 分析运行时间差异
   - 统计空隙分布特点

3. **与优化算法结合**
   - BL作为解码器
   - PSO/GA优化排列顺序
   - 评估优化算法的有效性

### 评价指标

- 平均利用率
- 使用托盘数
- 运行时间
- 物品放置率（成功放置的物品占比）

## 输出结果

程序会在 `visualization_results/` 目录下生成以下文件：
- `BL_原始顺序.txt` - 原始顺序的装箱结果
- `BL_面积降序.txt` - 面积降序的装箱结果
- `BL_长边降序.txt` - 长边降序的装箱结果
- `BL_周长降序.txt` - 周长降序的装箱结果
- `BL_宽度降序.txt` - 宽度降序的装箱结果

每个文件包含详细的放置信息，可用于可视化。

## 参考文献

1. Baker, B. S., Coffman, E. G., & Rivest, R. L. (1980). "Orthogonal packings in two dimensions". SIAM Journal on Computing, 9(4), 846-855.

2. Chazelle, B. (1983). "The bottomn-left bin-packing heuristic: An efficient implementation". IEEE Transactions on Computers, 100(8), 697-707.

3. Burke, E. K., Kendall, G., & Whitwell, G. (2004). "A new placement heuristic for the orthogonal stock-cutting problem". Operations Research, 52(4), 655-671.

## 常见问题

**Q: 为什么我的利用率很低？**  
A: BL算法对排序非常敏感，尝试不同的排序策略。通常面积降序或长边降序效果较好。

**Q: 如何提高装箱效率？**  
A: 可以与优化算法（如PSO、GA）结合，用它们来搜索最优的物品排列顺序。

**Q: 与天际线算法有什么区别？**  
A: BL算法更简单，但天际线算法通常能获得更好的利用率。BL适合作为基准对比算法。

---

**创建日期**: 2025-12-25  
**作者**: 张海龙  
**用途**: 毕业设计对比实验

