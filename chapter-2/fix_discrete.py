#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import glob
import random

def fix_file(filepath):
    """修复单个文件"""
    print(f"\n处理: {os.path.basename(filepath)}")
    
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # 解析配置
    first_line = lines[0].strip().split()
    total_machines = int(first_line[0])
    total_jobs = int(first_line[1])
    
    printer_line = lines[1].strip().split()
    num_printers = int(printer_line[0])
    
    batch_line = lines[2].strip().split()
    num_batch = int(batch_line[0])
    
    discrete_start = num_printers + num_batch + 1
    
    print(f"离散机编号范围: {discrete_start}-{total_machines}")
    
    fixed_count = 0
    
    # 处理每个工件
    for job_idx in range(total_jobs):
        job_line_parts = lines[3 + job_idx].strip().split()
        
        ops_total = int(job_line_parts[0])
        pos = 5
        
        # 跳过打印和批处理（每个3个字段）
        for _ in range(num_printers + num_batch):
            if pos + 2 < len(job_line_parts):
                pos += 3
        
        # 处理离散工序
        while pos < len(job_line_parts):
            if pos >= len(job_line_parts):
                break
                
            machines_count = int(job_line_parts[pos])
            pos += 1
            
            for _ in range(machines_count):
                if pos + 1 >= len(job_line_parts):
                    break
                    
                machine_id = int(job_line_parts[pos])
                time_val = int(job_line_parts[pos + 1])
                
                # 只修复离散机器的时间
                if machine_id >= discrete_start and machine_id <= total_machines:
                    if time_val < 600 or time_val > 1200:
                        new_time = random.randint(600, 1200)
                        job_line_parts[pos + 1] = str(new_time)
                        print(f"  J{job_idx+1} 机器{machine_id}: {time_val}→{new_time}")
                        fixed_count += 1
                
                pos += 2
        
        # 更新行
        lines[3 + job_idx] = ' '.join(job_line_parts) + '\n'
    
    if fixed_count > 0:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(lines)
        print(f"✅ 修复{fixed_count}个")
    else:
        print("✓ 无需修复")

# 主程序
instance_dir = "src/main/resources/instance"
for filepath in sorted(glob.glob(os.path.join(instance_dir, "*.txt"))):
    fix_file(filepath)

print("\n" + "="*60)
print("✅ 全部完成！")

