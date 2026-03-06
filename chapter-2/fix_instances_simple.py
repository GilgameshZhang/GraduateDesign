#!/usr/bin/env python
# -*- coding: utf-8 -*-
import os
import random

# 设置随机种子
random.seed(42)

def fix_instance(filepath):
    """修复单个算例文件"""
    print(f"\n处理: {os.path.basename(filepath)}")
    
    with open(filepath, 'r') as f:
        lines = [line.strip() for line in f.readlines()]
    
    # 解析第1行
    parts = lines[0].split()
    total_machines = int(parts[0])
    job_count = int(parts[1])
    
    # 解析第2行
    parts = lines[1].split()
    print_count = int(parts[0])
    
    # 解析第3行
    parts = lines[2].split()
    batch_count = int(parts[0])
    
    # 计算离散处理机范围
    discrete_count = total_machines - print_count - batch_count
    discrete_start = print_count + batch_count + 1
    discrete_end = total_machines
    
    print(f"  机器配置: 打印{print_count} + 批处理{batch_count} + 离散{discrete_count} = {total_machines}")
    print(f"  离散处理机: M{discrete_start}-M{discrete_end}")
    
    # 修复每个工件
    fixed_lines = lines[:3]
    
    for job_idx in range(job_count):
        line_idx = 3 + job_idx
        if line_idx >= len(lines):
            print(f"  ❌ J{job_idx}: 缺失")
            break
        
        parts = lines[line_idx].split()
        discrete_oper_count = int(parts[0])
        
        # 构建新行
        new_parts = parts[:5]  # 保留: 离散工序数 长 宽 高 离散工序数
        
        idx = 5
        for oper_idx in range(discrete_oper_count):
            if idx >= len(parts):
                break
            
            machine_count = int(parts[idx])
            idx += 1
            
            # 收集机器-时间对
            machines_times = []
            for _ in range(machine_count):
                if idx + 1 >= len(parts):
                    break
                machine_no = int(parts[idx])
                time = int(parts[idx + 1])
                idx += 2
                
                # 检查并修复
                fixed = False
                
                # 检查机器编号是否在离散范围内
                if machine_no < discrete_start or machine_no > discrete_end:
                    # 随机选择一个离散机器
                    machine_no = random.randint(discrete_start, discrete_end)
                    fixed = True
                
                # 检查时间是否合理（600-1200）
                if time == 0 or time < 600 or time > 1200:
                    time = random.randint(600, 1200)
                    fixed = True
                
                if fixed:
                    print(f"    J{job_idx} 工序{oper_idx+1}: 修复为 M{machine_no}:{time}")
                
                machines_times.append((machine_no, time))
            
            # 添加到新行
            new_parts.append(str(len(machines_times)))
            for m, t in machines_times:
                new_parts.append(str(m))
                new_parts.append(str(t))
        
        fixed_lines.append(' '.join(new_parts))
    
    # 写回文件
    with open(filepath, 'w') as f:
        f.write('\n'.join(fixed_lines) + '\n')
    
    print(f"  ✅ 已修复")

def main():
    instance_dir = r'C:\Users\Zhang Hailong\Desktop\毕设相关\毕设程序\Article\chapter-2\src\main\resources\instance'
    
    if not os.path.exists(instance_dir):
        print(f"目录不存在: {instance_dir}")
        return
    
    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()
    
    print(f"找到 {len(files)} 个算例文件\n")
    
    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_instance(filepath)
        except Exception as e:
            print(f"  ❌ 错误: {e}")
            import traceback
            traceback.print_exc()
    
    print(f"\n{'='*60}")
    print("所有算例文件已处理完成！")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()

