# 离散工序FCFS调度优化说明

## 更新内容

已优化离散工序的FCFS（先到先服务）调度策略！✅

---

## 🔄 调度策略变更

### 旧策略（已废弃）
- 预先为每个工序分配一台机器（选择第一个可用机器）
- 导致机器利用率不均衡

### 新策略（动态选择最早可用机器）
- 工序到达时，搜索该工序的所有可用机器
- 计算每台机器的最早开始时间
- 选择最早能开始加工的机器
- 实现负载均衡和最优调度

---

## 📊 核心算法

```java
// 工序到达时动态选择机器
double arrivalTime = partCompletionTime.get(operInfo.partIndex);

int bestMachine = -1;
double bestStartTime = Double.MAX_VALUE;

// 遍历所有可用机器
for (int m = 0; m < proDesMatrix[opIndex].length; m++) {
    double processTime = proDesMatrix[opIndex][m];
    
    if (processTime > 0 && processTime < Double.MAX_VALUE) {
        // 计算在该机器上的最早开始时间
        double startTime = Math.max(arrivalTime, machineAvailable);
        
        // 选择最早能开始的机器
        if (startTime < bestStartTime) {
            bestStartTime = startTime;
            bestMachine = machineIdx;
            bestProcessTime = processTime;
        }
    }
}

// 分配并更新
discreteMachineAvailableTime[bestMachine] = endTime;
partCompletionTime.put(operInfo.partIndex, endTime);
```

---

## ✅ 优势

- ✅ 动态负载均衡
- ✅ 充分利用并行机器
- ✅ 减少整体makespan
- ✅ 提高机器利用率

---

运行测试验证离散机器的利用率更均衡！

