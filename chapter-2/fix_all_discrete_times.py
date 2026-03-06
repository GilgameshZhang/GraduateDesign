#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import glob
import random

def fix_instance_file(file_path):
    """修复算例文件中离散工序的加工时间"""
    print(f"处理: {os.path.basename(file_path)}")
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        # 解析机器配置
        first_line = lines[0].strip().split()
        total_machines = int(first_line[0])
        total_jobs = int(first_line[1])
        
        printer_line = lines[1].strip().split()
        num_printers = int(printer_line[0])
        
        batch_line = lines[2].strip().split()
        num_batch = int(batch_line[0])
        
        num_discrete = total_machines - num_printers - num_batch
        discrete_start_id = num_printers + num_batch + 1
        
        fixed_count = 0
        modified = False
        
        # 修复每个工件
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()
            
            if len(job_line) < 5:
                continue
            
            ops_total = int(job_line[0])
            pos = 5
            
            # 跳过打印和批处理工序
            for _ in range(num_printers + num_batch):
                if pos + 2 < len(job_line):
                    pos += 3
                else:
                    break
            
            # 处理离散工序
            discrete_op_idx = 0
            while pos < len(job_line) and discrete_op_idx < num_discrete:
                if pos >= len(job_line):
                    break
                
                # 读取备选机器数
                machines_count = int(job_line[pos])
                pos += 1
                
                # 修复每台备选机器的时间
                for m_idx in range(machines_count):
                    if pos + 1 >= len(job_line):
                        break
                    
                    machine_id = int(job_line[pos])
                    time_val = int(job_line[pos + 1])
                    
                    # 检查并修复时间值
                    if machine_id >= discrete_start_id:  # 确认是离散机器
                        if time_val < 600 or time_val > 1200:
                            # 生成新的随机时间
                            new_time = random.randint(600, 1200)
                            job_line[pos + 1] = str(new_time)
                            print(f"  工件{job_idx+1}离散工序{discrete_op_idx+1}机器{machine_id}: {time_val} → {new_time}")
                            fixed_count += 1
                            modified = True
                    
                    pos += 2
                
                discrete_op_idx += 1
        
        # 如果有修改，重新读取并处理所有工件行
        if modified:
            # 重新处理所有工件行
            new_job_lines = []
            for job_idx in range(total_jobs):
                job_line = lines[3 + job_idx].strip().split()
                
                ops_total = int(job_line[0])
                pos = 5
                
                # 跳过打印和批处理工序
                for _ in range(num_printers + num_batch):
                    if pos + 2 < len(job_line):
                        pos += 3
                    else:
                        break
                
                # 处理离散工序
                discrete_op_idx = 0
                while pos < len(job_line) and discrete_op_idx < num_discrete:
                    if pos >= len(job_line):
                        break
                    
                    machines_count = int(job_line[pos])
                    pos += 1
                    
                    for m_idx in range(machines_count):
                        if pos + 1 >= len(job_line):
                            break
                        
                        machine_id = int(job_line[pos])
                        time_val = int(job_line[pos + 1])
                        
                        if machine_id >= discrete_start_id:
                            if time_val < 600 or time_val > 1200:
                                new_time = random.randint(600, 1200)
                                job_line[pos + 1] = str(new_time)
                        
                        pos += 2
                    
                    discrete_op_idx += 1
                
                new_job_lines.append(' '.join(job_line) + '\n')
            
            # 更新文件
            lines[3:3+total_jobs] = new_job_lines
            
            with open(file_path, 'w', encoding='utf-8') as f:
                f.writelines(lines)
            
            print(f"  ✅ 修复了 {fixed_count} 个时间值")
        else:
            print(f"  ✓ 无需修复")
    
    except Exception as e:
        print(f"  ❌ 错误: {e}")

def main():
    """处理所有算例文件"""
    instance_dir = "src/main/resources/instance"
    pattern = os.path.join(instance_dir, "*.txt")
    
    print("=" * 70)
    print("开始检查并修复所有算例文件的离散工序加工时间...")
    print("=" * 70)
    print()
    
    files = sorted(glob.glob(pattern))
    for file_path in files:
        fix_instance_file(file_path)
        print()
    
    print("=" * 70)
    print(f"✅ 完成！共处理 {len(files)} 个算例文件")

if __name__ == "__main__":
    main()

