#!/usr/bin/env python3
# -*- coding: utf-8 -*-

def verify_instance_format(file_path):
    """验证算例文件格式的完整性"""
    print(f"验证文件: {file_path}")

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        # 检查基本行数
        if len(lines) < 3:
            print("❌ 文件行数太少")
            return False

        # 解析第一行
        first_line = lines[0].strip().split()
        if len(first_line) != 2:
            print("❌ 第一行格式错误")
            return False

        total_machines = int(first_line[0])
        total_jobs = int(first_line[1])
        expected_lines = 3 + total_jobs

        print(f"机器数: {total_machines}, 工件数: {total_jobs}")
        print(f"期望总行数: {expected_lines}, 实际行数: {len(lines)}")

        if len(lines) != expected_lines:
            print("❌ 总行数不匹配")
            return False

        # 检查每个工件的数据格式
        errors = []
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()

            if len(job_line) < 4:
                errors.append(f"工件{job_idx+1}: 数据太少")
                continue

            try:
                ops_total = int(job_line[0])
                ops_total_repeat = int(job_line[4])

                if ops_total != ops_total_repeat:
                    errors.append(f"工件{job_idx+1}: 工序总数不一致 ({ops_total} != {ops_total_repeat})")

                # 解析工序数据
                pos = 5
                for op_idx in range(ops_total):
                    if pos >= len(job_line):
                        errors.append(f"工件{job_idx+1}工序{op_idx+1}: 数据缺失")
                        break

                    machines_count = int(job_line[pos])
                    pos += 1

                    # 检查是否有足够的机器-时间对
                    required_data = machines_count * 2
                    available_data = len(job_line) - pos

                    if available_data < required_data:
                        errors.append(f"工件{job_idx+1}工序{op_idx+1}: 备选{machines_count}台机器，数据不足 (需要{required_data}个值，只有{available_data}个)")
                        pos = len(job_line)  # 跳过剩余检查
                        break
                    else:
                        # 检查机器编号范围
                        for m_idx in range(machines_count):
                            machine_id = int(job_line[pos + m_idx * 2])
                            if machine_id < 1 or machine_id > total_machines:
                                errors.append(f"工件{job_idx+1}工序{op_idx+1}: 机器编号{machine_id}超出范围(1-{total_machines})")

                        pos += required_data

            except (ValueError, IndexError) as e:
                errors.append(f"工件{job_idx+1}: 解析错误 - {e}")

        if errors:
            print(f"❌ 发现 {len(errors)} 个错误:")
            for error in errors[:10]:  # 只显示前10个错误
                print(f"  • {error}")
            if len(errors) > 10:
                print(f"  ... 还有 {len(errors) - 10} 个错误")
            return False
        else:
            print("✅ 所有格式检查通过")
            return True

    except Exception as e:
        print(f"❌ 文件读取错误: {e}")
        return False

if __name__ == "__main__":
    verify_instance_format("src/main/resources/instance/J20P3B2D5_01.txt")
