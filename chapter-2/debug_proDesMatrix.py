#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调试脚本：检查算例文件解析后的proDesMatrix
模拟Input.java的解析逻辑，输出每个工序可用的机器和时间
"""

def parse_instance_file(filepath):
    """解析算例文件并输出proDesMatrix"""
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # 第一行：机器数和工件数
    first_line = lines[0].strip().split()
    machine_num = int(first_line[0])
    job_num = int(first_line[1])
    
    print(f"机器数: {machine_num}, 工件数: {job_num}")
    print("=" * 80)
    
    # 第二行：打印机
    printer_line = lines[1].strip().split()
    print_machine_count = int(printer_line[0])
    
    # 第三行：批处理机
    batch_line = lines[2].strip().split()
    batch_machine_count = int(batch_line[0])
    
    print(f"打印机数: {print_machine_count}, 批处理机数: {batch_machine_count}")
    print(f"离散机范围: {print_machine_count + batch_machine_count + 1} - {machine_num}")
    print("=" * 80)
    
    # 解析每个工件
    operationTotalIndex = 0
    
    for job_idx in range(job_num):
        job_line = lines[3 + job_idx].strip().split()
        
        if len(job_line) < 5:
            print(f"工件{job_idx}: 数据不完整")
            continue
        
        operation_count = int(job_line[0])
        L, W, H = job_line[1], job_line[2], job_line[3]
        operation_count_repeat = int(job_line[4])
        
        print(f"\n工件{job_idx} (L={L}, W={W}, H={H}): {operation_count}个工序")
        
        k = 5
        for oper_idx in range(operation_count):
            if k >= len(job_line):
                print(f"  工序{oper_idx}: 数据缺失")
                break
            
            selected_machine_count = int(job_line[k])
            k += 1
            
            # 创建模拟的proDesMatrix行
            time_array = [0] * machine_num
            
            for m in range(selected_machine_count):
                if k + 1 >= len(job_line):
                    print(f"  工序{oper_idx}: 机器数据不完整")
                    break
                
                machine_no = int(job_line[k])
                operation_time = int(job_line[k + 1])
                
                # 保存到time_array中（模拟proDesMatrix）
                time_array[machine_no - 1] = operation_time
                
                k += 2
            
            # 输出工序信息
            oper_type = "打印" if oper_idx == 0 else ("批处理" if oper_idx == 1 else f"离散{oper_idx-1}")
            print(f"  工序{oper_idx} ({oper_type}) [索引{operationTotalIndex}]:")
            
            # 输出所有备选机器
            available_machines = []
            for i, time in enumerate(time_array):
                if time > 0:
                    available_machines.append(f"M{i+1}:{time}")
            
            if available_machines:
                print(f"    备选机器: {', '.join(available_machines)}")
            else:
                print(f"    ⚠️ 没有可用机器（所有时间都是0）")
            
            # 检查是否有问题
            if oper_idx >= 2:  # 离散工序
                has_valid_machine = any(time > 0 for time in time_array)
                if not has_valid_machine:
                    print(f"    ❌ 错误：离散工序没有有效的加工时间！")
            
            operationTotalIndex += 1
    
    print("\n" + "=" * 80)
    print(f"总工序数: {operationTotalIndex}")

if __name__ == "__main__":
    import sys
    import os
    
    # 默认检查J20P3B2D5_01.txt
    instance_dir = "src/main/resources/instance"
    default_file = os.path.join(instance_dir, "J20P3B2D5_01.txt")
    
    filepath = sys.argv[1] if len(sys.argv) > 1 else default_file
    
    if os.path.exists(filepath):
        print(f"分析算例文件: {filepath}")
        print("=" * 80)
        parse_instance_file(filepath)
    else:
        print(f"文件不存在: {filepath}")

