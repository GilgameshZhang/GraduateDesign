import os
import glob
import random

def fix_instance_format(file_path):
    """修复算例文件中的格式问题"""
    print(f"🔧 修复算例: {os.path.basename(file_path)}")

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        if len(lines) < 3:
            print(f"  ❌ 文件行数太少: {len(lines)}")
            return False

        # 解析第一行获取机器数
        first_line = lines[0].strip().split()
        total_machines = int(first_line[0])
        total_jobs = int(first_line[1])

        # 解析第二行获取打印机数量
        printer_line = lines[1].strip().split()
        num_printers = int(printer_line[0])

        # 解析第三行获取批处理机数量
        batch_line = lines[2].strip().split()
        num_batch = int(batch_line[0])

        # 离散机编号范围
        discrete_start = num_printers + num_batch + 1
        discrete_end = total_machines

        fixed_lines = lines[:3]  # 保留前3行配置

        # 修复每个工件的数据
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()

            try:
                ops_total = int(job_line[0])
                L, W, H = job_line[1], job_line[2], job_line[3]

                # 重构工件行
                fixed_job = [str(ops_total), L, W, H, str(ops_total)]

                pos = 5  # 从工序数据开始
                for op_idx in range(ops_total):
                    if pos >= len(job_line):
                        print(f"  ❌ 工件{job_idx+1}工序{op_idx+1}数据缺失")
                        # 为缺失的工序添加默认数据
                        if op_idx == 0:  # 打印工序
                            fixed_job.extend(['1', str((job_idx % num_printers) + 1), '0'])
                        elif op_idx == 1:  # 批处理工序
                            batch_id = (job_idx % num_batch) + num_printers + 1
                            fixed_job.extend(['1', str(batch_id), '0'])
                        else:  # 离散工序
                            machines_count = random.randint(2, min(3, discrete_end - discrete_start + 1))
                            fixed_job.append(str(machines_count))
                            available_machines = list(range(discrete_start, discrete_end + 1))
                            selected = random.sample(available_machines, machines_count)
                            for machine_id in selected:
                                time_val = random.randint(600, 1200)
                                fixed_job.extend([str(machine_id), str(time_val)])
                        continue

                    machines_count = int(job_line[pos])
                    pos += 1

                    # 收集该工序的所有机器-时间对
                    op_data = [str(machines_count)]
                    collected_pairs = 0

                    while pos + 1 < len(job_line) and collected_pairs < machines_count:
                        try:
                            machine_id = int(job_line[pos])
                            time_val = int(job_line[pos + 1])
                            op_data.extend([str(machine_id), str(time_val)])
                            pos += 2
                            collected_pairs += 1
                        except (ValueError, IndexError):
                            break

                    # 如果收集的机器对不够，补充随机数据
                    while collected_pairs < machines_count:
                        if discrete_start <= discrete_end:
                            machine_id = random.randint(discrete_start, discrete_end)
                            time_val = random.randint(600, 1200)
                            op_data.extend([str(machine_id), str(time_val)])
                        collected_pairs += 1

                    fixed_job.extend(op_data)

                fixed_lines.append(' '.join(fixed_job))

            except (ValueError, IndexError) as e:
                print(f"  ❌ 工件{job_idx+1}解析错误: {e}")
                # 创建一个基本的工件数据
                basic_job = [str(3), str(random.randint(10, 600)), str(random.randint(10, 600)), str(random.randint(10, 400)), str(3)]
                # 打印工序
                basic_job.extend(['1', str((job_idx % num_printers) + 1), '0'])
                # 批处理工序
                batch_id = (job_idx % num_batch) + num_printers + 1
                basic_job.extend(['1', str(batch_id), '0'])
                # 离散工序
                machines_count = random.randint(2, min(3, discrete_end - discrete_start + 1))
                basic_job.append(str(machines_count))
                available_machines = list(range(discrete_start, discrete_end + 1))
                selected = random.sample(available_machines, machines_count)
                for machine_id in selected:
                    time_val = random.randint(600, 1200)
                    basic_job.extend([str(machine_id), str(time_val)])
                fixed_lines.append(' '.join(basic_job))

        # 写回文件
        with open(file_path, 'w', encoding='utf-8') as f:
            f.write('\n'.join(fixed_lines))

        print(f"  ✅ 修复完成")
        return True

    except Exception as e:
        print(f"  ❌ 修复失败: {e}")
        return False

def main():
    """修复所有算例文件"""
    instance_dir = "src/main/resources/instance"
    pattern = os.path.join(instance_dir, "*.txt")

    success_count = 0
    total_count = 0

    for file_path in glob.glob(pattern):
        total_count += 1
        if fix_instance_format(file_path):
            success_count += 1

    print("
" + "="*60)
    print("📊 修复结果汇总")
    print("="*60)
    print(f"总算例文件数: {total_count}")
    print(f"成功修复: {success_count}")
    print(f"修复失败: {total_count - success_count}")

if __name__ == "__main__":
    main()
