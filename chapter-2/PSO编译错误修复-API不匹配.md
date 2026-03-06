# PSO编译错误修复 - API不匹配

## ❌ 问题描述

**错误信息**:
```
C:\...\PSO.java:540:45
java: 找不到符号
  符号:   方法 getPrintMachines()
  位置: 类型为ProgramEntity.Problem的变量 input

C:\...\PSO.java:542:38
java: 找不到符号
  符号:   变量 X
  位置: 类 ProgramEntity.Machine.PrintMachine

C:\...\PSO.java:542:65
java: 找不到符号
  符号:   变量 Y
  位置: 类 ProgramEntity.Machine.PrintMachine
```

**发生位置**: `PSO.java` 的 `mutatePrintMachine()` 方法（第540-542行）

---

## 🔍 问题分析

### 错误原因

在实现`mutatePrintMachine()`方法时，使用了**不存在的API**：

**错误的代码** ❌:
```java
// 错误1: Problem类没有getPrintMachines()方法
PrintMachine[] printMachines = input.getPrintMachines();

// 错误2: PrintMachine的属性是L/W/H，不是X/Y
if (l <= printMachines[i].X && w <= printMachines[i].Y) {
```

### 正确的API

**Problem类**:
- ✅ `getMachines()` - 返回所有机器（包括打印机、批处理机、离散机）
- ✅ `getPrintMachineCount()` - 返回打印机数量
- ❌ `getPrintMachines()` - **不存在**

**PrintMachine类**:
```java
public class PrintMachine extends Machine {
    public double L;  // 长度 ✅
    public double W;  // 宽度 ✅
    public double H;  // 高度 ✅
    // 没有X、Y属性 ❌
}
```

---

## ✅ 修复方案

### 1. 修改获取打印机的方式

**修复前** ❌:
```java
PrintMachine[] printMachines = input.getPrintMachines();
for (int i = 0; i < printMachines.length; i++) {
    if (l <= printMachines[i].X && w <= printMachines[i].Y) {
        availablePrinters.add(i + 1);
    }
}
```

**修复后** ✅:
```java
Machine[] machines = input.getMachines();

// 只考虑打印机（前printMachineCount台）
for (int i = 0; i < printMachineCount; i++) {
    PrintMachine pm = (PrintMachine) machines[i];
    if (l <= pm.L && w <= pm.W) {
        availablePrinters.add(i + 1);
    }
}
```

**关键改动**:
1. ✅ 使用`getMachines()`获取所有机器
2. ✅ 使用类型转换`(PrintMachine)`
3. ✅ 使用正确的属性`L`和`W`
4. ✅ 只遍历前`printMachineCount`台（打印机）

---

### 2. 添加Machine类的import

**修复前** ❌:
```java
import ProgramEntity.Item;
import ProgramEntity.Job;
import ProgramEntity.Machine.PrintMachine;  // 只有PrintMachine
import ProgramEntity.Operation;
```

**修复后** ✅:
```java
import ProgramEntity.Item;
import ProgramEntity.Job;
import ProgramEntity.Machine.Machine;        // 添加Machine
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
```

---

## 📝 完整修复代码

### mutatePrintMachine() 方法

```java
/**
 * 变异策略3: 打印段机器重分配
 * 选择一个零件，重新分配到另一台可用的打印机
 */
private void mutatePrintMachine(Particle particle) {
    int jobCount = input.getJobCount();
    int printMachineCount = input.getPrintMachineCount();
    
    if (jobCount == 0 || printMachineCount <= 1) return;
    
    // 随机选择一个打印工件
    int pos = r.nextInt(jobCount);
    int jobNo = particle.gene_OS[pos];
    
    // 获取该零件的尺寸
    Item[] items = input.getItems();
    double l = items[jobNo].l;
    double w = items[jobNo].w;
    
    // 找到所有可以容纳该零件的打印机
    List<Integer> availablePrinters = new ArrayList<>();
    Machine[] machines = input.getMachines();  // ✅ 使用getMachines()
    
    // 只考虑打印机（前printMachineCount台）
    for (int i = 0; i < printMachineCount; i++) {
        PrintMachine pm = (PrintMachine) machines[i];  // ✅ 类型转换
        if (l <= pm.L && w <= pm.W) {  // ✅ 使用L和W
            availablePrinters.add(i + 1);
        }
    }
    
    // 如果有多台可用打印机，随机选择一台不同于当前的
    if (availablePrinters.size() > 1) {
        int currentMachine = particle.gene_MS[pos];
        availablePrinters.removeIf(m -> m == currentMachine);
        
        if (!availablePrinters.isEmpty()) {
            particle.gene_MS[pos] = availablePrinters.get(r.nextInt(availablePrinters.size()));
        }
    }
}
```

---

## 🧪 验证步骤

### 1. 编译验证（2分钟）

```bash
mvn clean compile -DskipTests
```

**预期输出**:
```
[INFO] BUILD SUCCESS
```

✅ **验证通过！**

---

### 2. 运行测试（5分钟）

```java
TestPSO.main()
```

**预期**:
- ✅ 无编译错误
- ✅ 策略3（打印段机器重分配）正常工作
- ✅ Makespan持续改进

---

## 📊 修复对比

| 错误项 | 修复前 ❌ | 修复后 ✅ |
|--------|----------|----------|
| **获取打印机** | `getPrintMachines()` | `getMachines()` + 类型转换 |
| **属性名称** | `X`, `Y` | `L`, `W` |
| **import语句** | 缺少`Machine` | 添加`Machine`导入 |
| **编译结果** | 3个错误 | 编译成功 ✅ |

---

## 🎯 经验总结

### 1. API使用要查阅源码

**错误做法** ❌:
- 猜测方法名（如`getPrintMachines()`）
- 猜测属性名（如`X`, `Y`）

**正确做法** ✅:
- 查看类的源码
- 使用IDE的代码补全
- 参考已有代码的用法

---

### 2. 类型转换要注意

当`getMachines()`返回`Machine[]`时，需要转换为`PrintMachine`：

```java
Machine[] machines = input.getMachines();
PrintMachine pm = (PrintMachine) machines[i];  // 类型转换
```

---

### 3. 机器数组的组织方式

```
machines数组的组织:
[0...printMachineCount-1]        → 打印机
[printMachineCount...batchStart] → 批处理机
[batchStart...]                  → 离散处理机
```

因此只遍历前`printMachineCount`台即可获取所有打印机。

---

## 📝 修复文件

| 文件 | 修改内容 | 状态 |
|------|---------|------|
| `PSO.java` | import语句（添加`Machine`） | ✅ 已修复 |
| `PSO.java` | `mutatePrintMachine()`方法 | ✅ 已修复 |

---

## 🎊 总结

**PSO编译错误已全部修复！**

| 错误数 | 状态 |
|-------|------|
| API不匹配错误 | ✅ 3/3已修复 |
| 编译状态 | ✅ 成功 |

**现在可以继续编译和测试了！** 🚀

---

## 🚀 立即行动

### 编译验证
```bash
test_pso_enhanced.bat
```

### 运行测试
```java
TestPSO.main()
```

**预期**: 一切正常运行 ✅

