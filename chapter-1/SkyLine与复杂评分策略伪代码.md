# SkyLine 天际线 packing 与复杂评分策略 — 伪代码

---

## 0. 活动解码：禁忌搜索 + SkyLine + 复杂评分规则

活动解码将“机器分配 + 工件顺序”染色体转化为每台机器上的分批装载方案与时间，核心为：**按机器分组 → 对每台机器用禁忌搜索优化工件加工顺序 → 用 SkyLine + 复杂评分将顺序解码为多盘方案并计算适应度**。

### 0.1 从染色体到每台机器的工件列表（Decode）

```
输入: 
  items[1..n]         // 全体工件
  genomeMachineArray[1..n]  // 工件 i 分配的机器编号
  genomeItemArray[1..n]     // 工件在染色体中的编号（加工顺序编码）

算法 Decode():
  machineGenomeMap ← 空映射  // 机器编号 → 该机器上的工件列表（按染色体顺序）
  for i = 1 to n do
    m ← genomeMachineArray[i]
    item ← items[genomeItemArray[i]]
    if m 不在 machineGenomeMap 中 then machineGenomeMap[m] ← 空列表
    machineGenomeMap[m].add(item)
  // 得到每台机器上的工件列表，顺序由 genomeItemArray 决定
```

### 0.2 禁忌搜索主算法（对单机工件顺序优化）

对**某一台机器**，给定其工件列表 `printItem[1..n]`，优化这些工件的**加工顺序**（排列），使解码后的分批方案适应度更优。解码方式：按当前顺序送入 SkyLine + 复杂评分得到多盘方案（见 0.3）。

```
输入:
  machine             // 当前机器（含 L, W, prepareTime, reCoatingTime, printH）
  printItem[1..n]     // 分配给该机器的 n 个工件
  MAX_GEN             // 禁忌搜索迭代次数
  N                   // 每代邻域采样次数
  minTabuSize         // 禁忌表最小长度
  isRotateEnable      // 是否允许旋转

输出:
  bestSolution        // 最优分批方案 List<Solution>
  startTime, endTime  // 每批开始/结束时间
  fitness             // 适应度（makespan + 废料惩罚）

算法 TabuSearch.solve():
  1. items ← printItem 转为数组；L ← machine.L；W ← machine.W
  2. 若 n = 0，返回空结果；若 n = 1，直接 Evaluate([0]) 并返回
  3. 初始化禁忌表 tabuList（长度 tabuSize，可动态调整）
  4. 初始解：sequence ← [0, 1, 2, …, n-1]  // 单位排列，表示当前加工顺序
  5. bestSolution ← Evaluate(sequence)
  6. bestSequence ← sequence 的拷贝
  7. for t = 1 to MAX_GEN do
  8.     LocalSolution ← null, LocalSequence ← null
  9.     for n_count = 1 to N do
 10.         tempSequence ← GenerateNeighbor(sequence)   // 邻域：交换 / 2-opt / 插入
 11.         if tempSequence 不在禁忌表 then
 12.             tempSolution ← Evaluate(tempSequence)   // 见 0.3：SkyLine + 复杂评分
 13.             if LocalSolution 为 null 或 CalFitness(tempSolution) < CalFitness(LocalSolution) then
 14.                 LocalSolution ← tempSolution
 15.                 LocalSequence ← tempSequence
 16.     if LocalSequence ≠ null then
 17.         if CalFitness(LocalSolution) < CalFitness(bestSolution) then
 18.             bestSolution ← Evaluate(LocalSequence)   // 重新解码保留时间等
 19.             bestSequence ← LocalSequence
 20.         sequence ← LocalSequence
 21.         EnterTabuList(sequence)
 22. 返回 BatchResult(bestSolution, startTime, endTime, CalFitness(bestSolution))
```

### 0.3 Evaluate：由工件顺序到分批方案（SkyLine + 复杂评分）

**Evaluate(sequence)** 将当前机器上的工件按 `sequence` 重排后，用 **SkyLine 多盘 packing + 复杂评分** 得到分批方案。即第 2、3、4 节的 SkyLine 多盘流程 + 评分策略。

```
函数 Evaluate(sequence):
  输入: sequence[0..n-1] 为 0..n-1 的排列（工件下标的重排）
  1. 按顺序重排工件: 
     for i = 0 to n-1 do orderedItems[i] ← items[sequence[i]]
  2. 调用 SkyLine 多盘 packing（使用复杂评分策略）:
     solutions ← SkyLinePackings(L, W, orderedItems, isRotateEnable)
     // SkyLinePackings 内部：见第 2、3 节
     //   - 优先队列取最下最左天际线
     //   - 对每条天际线，遍历未放置工件，用 Score(·) 选最大评分工件
     //   - 按 hl/hr 与 score 决定 PlaceLeft / PlaceRight
     //   - 放不下则 CombineSkyLine；满盘则新盘
  3. return solutions  // List<Solution>，每个 Solution 为一批的放置结果
```

### 0.4 适应度与禁忌表

```
CalFitness(solutions):
  按批顺序计算每批 startTime、endTime（考虑机器 prepareTime、reCoatingTime、printH）
  废料率 wasteRate ← 各批 (1 - solution.rate) 的平均
  return 最大完工时间 endTime[最后] + wasteRate × 100   // 越小越优

GenerateNeighbor(sequence):
  随机选两种之一：
    (1) 交换：随机 r1≠r2，swap(sequence[r1], sequence[r2])
    (2) 2-opt：随机 r1<r2，反转 sequence[r1..r2]
    (3) 插入：随机 r1,r2，将 sequence[r1] 插入到 r2 位置
  return 新序列

EnterTabuList(sequence):
  若禁忌表未满则追加；否则去掉最早进入的一条，将 sequence 加入表尾

IsInTabuList(sequence):
  若 sequence 与禁忌表中某条完全相同则返回 true，否则 false
```

### 0.5 活动解码整体流程小结

```
1. 染色体 (genomeMachineArray, genomeItemArray) 
   → Decode → 每台机器 m 对应工件列表 machineGenomeMap[m]

2. 对每台机器 m：
   TabuSearch(machine, machineGenomeMap[m], …).solve()
   ├─ 初始顺序 [0,1,…,n-1]，Evaluate = SkyLinePackings(按顺序) → 多批 Solution
   ├─ 迭代：邻域生成新顺序 → 非禁忌则 Evaluate（ again SkyLinePackings + 复杂评分）→ 比较适应度
   └─ 输出该机器最优分批方案及 makespan、废料等

3. 各机器结果汇总 → 全局 Cmax、总适应度（用于上层 GA 等）
```

以下第 1～8 节给出 SkyLine 与复杂评分策略的详细数据结构与伪代码（Score 规则、PlaceLeft/Right、CombineSkyLine 等），供 Evaluate 中 SkyLinePackings 的具体实现对照。

---

## 1. 数据结构与符号说明

### 1.1 天际线（SkyLine）

```
数据结构 SkyLine:
    x   : 天际线左端点的 x 坐标（水平位置）
    y   : 天际线左端点的 y 坐标（竖直高度，从下往上）
    len : 天际线段的长度（沿 x 方向）

比较规则（用于优先队列）:
    先按 y 升序（y 小的优先），y 相同时按 x 升序（x 小的优先）
    → 每次取“最下最左”的天际线段
```

### 1.2 矩形与托盘

```
托盘: 宽度 W，高度 H（上界）
矩形 item: 长 l，宽 w（放置时在托盘平面上的投影），高 h（用于批次高度等）
可旋转: isRotateEnable；若允许，可交换 l 与 w
```

### 1.3 左右墙高度

```
对当前天际线段 skyLine：
    hl : 当前段“左墙”高度（左侧相邻天际线比当前段高出的高度）
    hr : 当前段“右墙”高度（右侧相邻天际线比当前段高出的高度）
    若左侧无相邻段，则 hl = H - skyLine.y；右侧同理 hr。
```

### 1.4 比较函数

```
compareDouble(a, b) 返回值:
    = 0  当 |a - b| < ε 时（视为相等）
    = -1 当 a < b
    = 1  当 a > b
```

---

## 2. 主流程：单托盘天际线 Packing

```
算法: SkyLinePacking(单盘)

输入: W, H, items[], isRotateEnable
输出: Solution(placeItemList, 高度方差等)

1. placeItemList ← 空列表
2. used[1..n] ← 全部 false
3. 初始化天际线优先队列 Q:
   Q ← 仅包含一条天际线 SkyLine(x=0, y=0, len=W)

4. while Q 非空 且 已放置数量 < n do
5.     skyLine ← Q.poll()   // 取当前最下最左的天际线
6.     计算 skyLine 的 hl, hr:
       hl ← H - skyLine.y,  hr ← H - skyLine.y
       遍历 Q 中每条 line:
         若 line.x + line.len = skyLine.x 则 hl ← line.y - skyLine.y
         若 skyLine.x + skyLine.len = line.x 则 hr ← line.y - skyLine.y
7.     【复杂评分策略】选择要放置的矩形:
       maxScore ← -1, maxItemIndex ← -1, isRotate ← false
       for i = 1 to n do
         if not used[i] then
           score ← Score(items[i].l, items[i].w, skyLine, hl, hr)
           if score > maxScore then maxScore ← score, maxItemIndex ← i, isRotate ← false
           if isRotateEnable then
             rotateScore ← Score(items[i].w, items[i].l, skyLine, hl, hr)
             if rotateScore > maxScore then maxScore ← rotateScore, maxItemIndex ← i, isRotate ← true
8.     if maxScore ≥ 0 then
9.         【按评分与左右墙关系决定靠左/靠右放置】
        if hl ≥ hr then
           if maxScore = 2 then PlaceRight(items[maxItemIndex], skyLine, isRotate)
           else PlaceLeft(items[maxItemIndex], skyLine, isRotate)
        else
           if maxScore ∈ {4, 0} then PlaceRight(...)   // 代码中多盘为 6 或 0
           else PlaceLeft(...)
10.        used[maxItemIndex] ← true
11.        更新 totalS、totalG 等
12.    else
13.        CombineSkyLine(skyLine)   // 当前段放不下任何矩形，上移并合并相邻天际线
14. end while
15. 计算高度方差等，返回 Solution(placeItemList, ...)
```

---

## 3. 主流程：多托盘（分批）Packing

```
算法: SkyLinePackings(多盘)

与单盘类似，外层循环“当前批次”：
  - 每盘开始时: Q 清空，Q 仅含 SkyLine(0, 0, W)；used 在整个多盘过程中共用
  - 内层 while 与单盘一致，但：
      (1) 已放置数量用 counter 累计，counter 达到 n 时所有工件放完
      (2) 若使用复杂评分：仍遍历所有未放置工件选最大评分；若使用简化策略：仅尝试当前 counter 对应工件，放不下则 break 内层循环并开启新批次
  - 每盘结束得到一批 placeItemList，加入 solutions；然后下一盘
```

---

## 4. 复杂评分策略：Score(l, w, skyLine, hl, hr)

**含义**：在**当前天际线段**上放置一个**投影长为 l、宽为 w**的矩形时的优先程度。  
**返回值**：-1 表示不可放；0～12 表示可放，数值越大越优先。  
**约定**：l 为沿天际线方向（x）的边长，w 为竖直向上方向占用的高度。

```
函数 Score(l, w, skyLine, hl, hr):
  // 可行性
  if skyLine.len < l then return -1
  if skyLine.y + w > H then return -1

  // 左墙高度 ≥ 右墙高度 (hl ≥ hr)
  if hl ≥ hr then
    if l = skyLine.len 且 w = hl     then return 12
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w = hl  then return 11
    if l = skyLine.len 且 w = hr     then return 10
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w = hr  then return 9
    if l = skyLine.len 且 w > hl     then return 8
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w > hl  then return 7
    if l ≤ 0.9·skyLine.len 且 w = hl then return 6
    if l = skyLine.len 且 w < hl 且 w > hr then return 5
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w < hl 且 w > hr then return 4
    if l ≤ 0.9·skyLine.len 且 w = hr then return 3
    if l = skyLine.len 且 w < hr     then return 2
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w < hr  then return 1
    if l ≤ 0.9·skyLine.len 且 w ≠ hl then return 0
  // 右墙高于左墙 (hr > hl)
  else
    if l = skyLine.len 且 w = hl     then return 12
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w = hr  then return 11
    if l = skyLine.len 且 w = hr     then return 10
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w = hl  then return 9
    if l = skyLine.len 且 w > hl     then return 8
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w > hr  then return 7
    if l ≤ 0.9·skyLine.len 且 w = hr then return 6   // 靠右
    if l = skyLine.len 且 w < hr 且 w > hl then return 5
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w < hr 且 w > hl then return 4
    if l ≤ 0.9·skyLine.len 且 w = hl then return 3
    if l ∈ (0.9·skyLine.len, skyLine.len] 且 w < hl  then return 1
    if l = skyLine.len 且 w < hl     then return 2
    if l ≤ 0.9·skyLine.len 且 w ≠ hr then return 0   // 靠右
  return score
```

**说明**：
- 相等判断用 `compareDouble`（含误差）。
- 评分偏好：**填满当前段长**（l = skyLine.len）且**高度与左/右墙齐平**（w = hl 或 w = hr）得分最高（12/11/10/9）；其次为“接近填满”（l > 0.9·len）或高度齐平；再次为 l ≤ 0.9·len 或 w 不齐平的情况。

---

## 5. 放置规则：靠左 / 靠右

根据**当前段的左右墙高度**和**得分**决定靠左还是靠右放，以利于后续合并与稳定性。

```
若 maxScore ≥ 0，已选定矩形与是否旋转：

若 hl ≥ hr（左墙不低于右墙）:
  if maxScore = 2 then PlaceRight(item, skyLine, isRotate)
  else PlaceLeft(item, skyLine, isRotate)

若 hl < hr（右墙高于左墙）:
  if maxScore ∈ {4, 0} then PlaceRight(...)   // 单盘代码为 4 或 0；多盘为 6 或 0
  else PlaceLeft(...)
```

---

## 6. PlaceLeft / PlaceRight

```
PlaceLeft(item, skyLine, isRotate):
  若不旋转: placeItem ← (x=skyLine.x, y=skyLine.y, l=item.l, w=item.w)
  若旋转:   placeItem ← (x=skyLine.x, y=skyLine.y, l=item.w, w=item.l)
  更新天际线:
    新段1: (skyLine.x, skyLine.y + placeItem.w, placeItem.l)
    新段2: (skyLine.x + placeItem.l, skyLine.y, skyLine.len - placeItem.l)
  仅当长度 > 0 时把新段加入 Q
  return placeItem

PlaceRight(item, skyLine, isRotate):
  若不旋转: placeItem ← (x=skyLine.x+skyLine.len-item.l, y=skyLine.y, l=item.l, w=item.w)
  若旋转:   placeItem ← (x=skyLine.x+skyLine.len-item.w, y=skyLine.y, l=item.w, w=item.l)
  更新天际线:
    新段1: (skyLine.x, skyLine.y, skyLine.len - placeItem.l)
    新段2: (placeItem.x, skyLine.y + placeItem.w, placeItem.l)
  仅当长度 > 0 时把新段加入 Q
  return placeItem
```

---

## 7. 天际线合并：CombineSkyLine

当当前天际线段上**所有未放置矩形评分均为 -1**时，将该段“上移”并与相邻同高或可合并的段合并，避免死锁。

```
CombineSkyLine(skyLine):
  merged ← false
  for each line in Q do
    if skyLine.y ≤ line.y then
      if skyLine.x = line.x + line.len then  // 首尾相连
        从 Q 中移除 line
        skyLine ← (x=line.x, y=line.y, len=line.len+skyLine.len)
        merged ← true; break
      if skyLine.x + skyLine.len = line.x then  // 尾首相连
        从 Q 中移除 line
        skyLine ← (x=skyLine.x, y=line.y, len=skyLine.len+line.len)
        merged ← true; break
  if merged then Q.add(skyLine)
```

---

## 8. 小结

| 模块 | 作用 |
|------|------|
| 天际线优先队列 | 始终在“最下最左”的段上尝试放置，保证左下角优先填充 |
| 复杂评分 Score | 在可行放置中区分 0～12 档，优先选“填满段长、齐平墙高”的放置 |
| 靠左/靠右规则 | 根据 hl、hr 与得分决定靠左或靠右，改善轮廓与稳定性 |
| CombineSkyLine | 无法放置时合并相邻段，推进搜索 |

以上即为 SkyLine 与复杂评分策略的完整伪代码描述，可直接用于论文或实现对照。
