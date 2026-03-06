# BL算法（Bottom-up Left-justified）实现说明

## 📋 算法概述

BL法（Bottom-up Left-justified）是一种经典的二维装箱启发式算法，通过**滑动机制**实现紧密装箱。

---

## 🎯 核心思想

### 基本步骤
1. 从候选位置开始放置物品
2. **向下滑动**：移动物品向下，直到碰到底部或其他物品
3. **向左滑动**：移动物品向左，直到碰到左边界或其他物品
4. **选择最优**：在所有候选位置中选择滑动后Y最小、X最小的位置

### 伪代码

```
BL_Packing(W, H, Items):
    P ← empty list                // 已放置物品
    C ← {(0, 0)}                  // 候选位置（初始只有原点）

    for each item in Items:
        bestPosition ← NULL
        bestY ← ∞
        bestX ← ∞

        for each (x, y) in C:
            if x + item.width > W or y + item.height > H:
                continue

            // 滑动机制
            finalY ← slideDown(x, y, item, P)
            finalX ← slideLeft(x, finalY, item, P)

            // 检查重叠
            if hasOverlap(finalX, finalY, item, P):
                continue

            // BL规则：选择Y最小，Y相同时X最小
            if finalY < bestY or (finalY == bestY and finalX < bestX):
                bestY ← finalY
                bestX ← finalX
                bestPosition ← (finalX, finalY)

        if bestPosition != NULL:
            place item at bestPosition
            P.add(item)
            
            // 更新候选位置
            C.add((bestX + item.width, bestY))      // 右侧
            C.add((bestX, bestY + item.height))     // 顶部

    return P
```

---

## 🔍 关键函数实现

### 1. slideDown（向下滑动）

```java
private double slideDown(double x, double startY, double itemL, double itemW, 
                         List<PlaceItem> placedItems) {
    double y = startY;
    double step = 0.1;  // 滑动步长
    
    while (y > 0) {
        double nextY = Math.max(0, y - step);
        
        // 检查是否与已放置物品重叠
        if (hasOverlap(x, nextY, itemL, itemW, placedItems)) {
            break;  // 碰到物品，停止
        }
        
        y = nextY;
        
        if (y == 0) break;  // 到达底部
    }
    
    return y;
}
```

**说明**：
- 从起始Y位置逐步向下移动
- 每次移动`step`距离
- 碰到物品或底部时停止

---

### 2. slideLeft（向左滑动）

```java
private double slideLeft(double startX, double y, double itemL, double itemW,
                         List<PlaceItem> placedItems) {
    double x = startX;
    double step = 0.1;
    
    while (x > 0) {
        double nextX = Math.max(0, x - step);
        
        if (hasOverlap(nextX, y, itemL, itemW, placedItems)) {
            break;  // 碰到物品，停止
        }
        
        x = nextX;
        
        if (x == 0) break;  // 到达左边界
    }
    
    return x;
}
```

**说明**：
- 在**向下滑动后的Y坐标**基础上向左移动
- 碰到物品或左边界时停止

---

### 3. hasOverlap（重叠检测）

```java
private boolean hasOverlap(double x, double y, double itemL, double itemW,
                           List<PlaceItem> placedItems) {
    for (PlaceItem placed : placedItems) {
        // 矩形1: (x, y) 到 (x+itemL, y+itemW)
        // 矩形2: (placed.x, placed.y) 到 (placed.x+placed.l, placed.y+placed.w)
        
        boolean noOverlapX = (x + itemL <= placed.x) || (placed.x + placed.l <= x);
        boolean noOverlapY = (y + itemW <= placed.y) || (placed.y + placed.w <= y);
        
        if (!noOverlapX && !noOverlapY) {
            return true;  // 有重叠
        }
    }
    
    return false;
}
```

**说明**：
- 检查新物品是否与已放置物品重叠
- 使用矩形相交判断

---

### 4. updateCandidates（更新候选位置）

```java
private void updateCandidates(List<CandidatePosition> candidates, PlaceItem placedItem) {
    // 添加右侧候选点
    double rightX = placedItem.x + placedItem.l;
    double rightY = placedItem.y;
    candidates.add(new CandidatePosition(rightX, rightY));
    
    // 添加顶部候选点
    double topX = placedItem.x;
    double topY = placedItem.y + placedItem.w;
    candidates.add(new CandidatePosition(topX, topY));
    
    // 可选：移除被支配的候选点
    removeDominatedCandidates(candidates);
}
```

**说明**：
- 每放置一个物品，生成两个新候选点
- 右侧点：`(x + width, y)`
- 顶部点：`(x, y + height)`

---

## 📊 算法示例

### 示例：放置3个物品

**初始状态**：
```
候选位置: C = {(0, 0)}
已放置: P = {}
```

**物品1：2×3**

1. 尝试候选位置(0, 0)
2. 向下滑动：已在底部，y=0
3. 向左滑动：已在左边，x=0
4. 最终位置：(0, 0)

```
  Y
  3|       |
  2|       |
  1| [  1  ]
  0|_[__1__]______ X
    0  1  2  3
```

候选位置更新：C = {(2, 0), (0, 3)}

---

**物品2：1×2**

1. 候选(2, 0)：
   - 向下滑：y=0
   - 向左滑：碰到物品1，x=2
   - 最终：(2, 0)

2. 候选(0, 3)：
   - 向下滑：碰到物品1，y=3
   - 向左滑：x=0
   - 最终：(0, 3)

选择(2, 0)（Y更小）

```
  Y
  5|       |
  4|       |
  3| [  1  ]
  2| [  1  ][2]
  1| [  1  ][2]
  0|_[__1__][2]___ X
    0  1  2  3
```

候选位置更新：C = {(0, 3), (3, 0), (2, 2)}

---

**物品3：1×1**

1. 候选(0, 3)：最终(0, 3)
2. 候选(3, 0)：最终(3, 0)
3. 候选(2, 2)：最终(2, 2)

选择(3, 0)（Y=0最小）

```
  Y
  5|       |
  4|       |
  3| [  1  ]
  2| [  1  ][2]
  1| [  1  ][2]
  0|_[__1__][2][3] X
    0  1  2  3  4
```

---

## 🔄 与Skyline算法对比

| 特性 | Skyline算法 | BL算法 | 对比 |
|------|------------|--------|------|
| **数据结构** | 天际线队列 | 候选位置列表 | BL更简单 |
| **放置策略** | 天际线高度 | 滑动机制 | BL更紧密 |
| **重叠检测** | 隐式（天际线特性） | 显式检测 | Skyline更快 |
| **空间利用** | 较好 | 更好 | BL更优 |
| **实现复杂度** | 中等 | 较高 | Skyline简单 |
| **时间复杂度** | O(n×k×log k) | O(n×C×m) | Skyline更快 |

其中：
- n: 物品数量
- k: 天际线数量
- C: 候选点数量
- m: 已放置物品数量

---

## ⚙️ 优化技巧

### 1. 滑动步长优化

```java
// 自适应步长
double step = Math.min(0.1, Math.min(itemL, itemW) / 10);
```

**好处**：
- 小物品用小步长，提高精度
- 大物品用大步长，提高速度

---

### 2. 候选点支配过滤

```java
private void removeDominatedCandidates(List<CandidatePosition> candidates) {
    // 如果候选点A的(x, y)都 >= 候选点B，则A被B支配，可移除
    for (CandidatePosition a : candidates) {
        for (CandidatePosition b : candidates) {
            if (a != b && b.x <= a.x && b.y <= a.y && 
                (b.x < a.x || b.y < a.y)) {
                candidates.remove(a);
                break;
            }
        }
    }
}
```

**好处**：减少候选点数量，提高搜索速度

---

### 3. 早停策略

```java
// 如果找到了原点(0, 0)位置，直接使用
if (finalX == 0 && finalY == 0) {
    return new PlaceItem(...);
}
```

**好处**：最优位置无需继续搜索

---

## 📈 算法特点

### 优点 ✅
- **紧密装箱**：滑动机制使物品尽可能靠近左下角
- **空间利用高**：通常比贪心策略好5-15%
- **易于理解**：算法逻辑清晰
- **适合多批次**：无法放入的物品自动进入下一批次

### 缺点 ⚠️
- **计算复杂度高**：需要显式重叠检测，O(n²m)
- **候选点增长**：候选点数量可能很大
- **局部最优**：启发式算法，不保证全局最优

---

## 🎯 应用场景

### 适合场景 ✅
- 二维装箱问题
- 板材切割
- 集装箱装载
- 3D打印平台布局（本项目）

### 关键参数
- **L, W, H**：容器尺寸
- **滑动步长**：精度vs速度权衡
- **候选点管理**：支配过滤策略

---

## 🔍 调试技巧

### 输出候选位置

```java
private void debugCandidates(List<CandidatePosition> candidates) {
    System.out.println("候选位置:");
    for (CandidatePosition c : candidates) {
        System.out.printf("  (%.2f, %.2f)\n", c.x, c.y);
    }
}
```

### 输出滑动轨迹

```java
System.out.printf("物品%d: 起始(%.2f,%.2f) -> 向下(%.2f,%.2f) -> 向左(%.2f,%.2f)\n",
    itemIndex, startX, startY, startX, finalY, finalX, finalY);
```

---

## ✅ 实现总结

### 核心代码结构

```
BLFPacker
├── packInBatches()           // 批次嵌套主循环
├── packSingleBatch()         // 单批次嵌套
├── findBestPositionBL()      // BL法核心：遍历候选位置
│   ├── slideDown()           // 向下滑动
│   ├── slideLeft()           // 向左滑动
│   └── hasOverlap()          // 重叠检测
├── updateCandidates()        // 更新候选位置
├── removeDominatedCandidates() // 候选点优化
└── calculateDimensions()     // 朝向旋转计算
```

### 算法复杂度

- **时间复杂度**：O(n × C × m)
  - n: 物品数量
  - C: 候选点数量（最坏O(n)）
  - m: 已放置物品数量（重叠检测）
  
- **空间复杂度**：O(n + C)
  - 已放置物品列表：O(n)
  - 候选位置列表：O(C)

---

## 🚀 使用建议

1. **首次测试**：使用小算例验证正确性
2. **性能调优**：调整滑动步长
3. **质量评估**：对比Skyline算法的利用率
4. **可视化检查**：生成布局图验证

BL算法已完整实现，可以开始测试！

