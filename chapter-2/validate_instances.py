import os
import glob

def validate_instance_format(file_path):
    """验证算例文件的格式正确性"""
    errors = []
    warnings = []

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        if len(lines) < 3:
            errors.append(f"文件行数太少: {len(lines)}")
            return errors, warnings

        # 检查第一行：机器数 工件数
        first_line = lines[0].strip().split()
        if len(first_line) != 2:
            errors.append(f"第一行格式错误: {first_line}")
            return errors, warnings

        total_machines = int(first_line[0])
        total_jobs = int(first_line[1])

        # 检查第二行：打印机配置
        printer_line = lines[1].strip().split()
        if len(printer_line) < 1:
            errors.append("打印机配置行格式错误")
            return errors, warnings

        num_printers = int(printer_line[0])
        expected_printer_data = 1 + num_printers * 6  # 打印机数 + 每台打印机6个参数
        if len(printer_line) != expected_printer_data:
            errors.append(f"打印机配置数据不完整: 期望{expected_printer_data}个值，实际{len(printer_line)}个")

        # 检查第三行：批处理机配置
        batch_line = lines[2].strip().split()
        if len(batch_line) < 1:
            errors.append("批处理机配置行格式错误")
            return errors, warnings

        num_batch = int(batch_line[0])
        expected_batch_data = 1 + num_batch * 2  # 批处理机数 + 每台批处理机2个参数
        if len(batch_line) != expected_batch_data:
            errors.append(f"批处理机配置数据不完整: 期望{expected_batch_data}个值，实际{len(batch_line)}个")

        # 检查工件数据行
        expected_total_lines = 3 + total_jobs
        if len(lines) != expected_total_lines:
            errors.append(f"总行数不匹配: 期望{expected_total_lines}行，实际{len(lines)}行")
            return errors, warnings

        # 验证每个工件的数据格式
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()
            if len(job_line) < 4:
                errors.append(f"工件{job_idx+1}数据太少: {len(job_line)}个值")
                continue

            try:
                ops_total = int(job_line[0])  # 工序总数
                L, W, H = int(job_line[1]), int(job_line[2]), int(job_line[3])
                ops_total_repeat = int(job_line[4])  # 重复的工序总数

                if ops_total != ops_total_repeat:
                    errors.append(f"工件{job_idx+1}工序总数不一致: {ops_total} != {ops_total_repeat}")

                # 解析工序数据
                pos = 5  # 从工序数据开始的位置
                for op_idx in range(ops_total):
                    if pos >= len(job_line):
                        errors.append(f"工件{job_idx+1}工序{op_idx+1}数据缺失")
                        break

                    try:
                        machines_count = int(job_line[pos])  # 备选机器数
                        pos += 1

                        # 检查是否有足够的机器-时间对
                        expected_pairs = machines_count
                        available_data = len(job_line) - pos

                        if available_data < expected_pairs * 2:
                            errors.append(f"工件{job_idx+1}工序{op_idx+1}数据不完整: 备选{machines_count}台机器，期望{expected_pairs*2}个值，实际{available_data}个值")
                        else:
                            # 验证机器编号在合理范围内
                            for m_idx in range(machines_count):
                                machine_id = int(job_line[pos + m_idx * 2])
                                time_val = int(job_line[pos + m_idx * 2 + 1])

                                if machine_id < 1 or machine_id > total_machines:
                                    errors.append(f"工件{job_idx+1}工序{op_idx+1}机器编号超出范围: {machine_id} (1-{total_machines})")

                                if time_val < 0:
                                    warnings.append(f"工件{job_idx+1}工序{op_idx+1}加工时间为负: {time_val}")

                            pos += machines_count * 2

                    except (ValueError, IndexError) as e:
                        errors.append(f"工件{job_idx+1}工序{op_idx+1}解析错误: {str(e)}")
                        break

                # 检查是否还有未解析的数据
                if pos < len(job_line):
                    warnings.append(f"工件{job_idx+1}有多余的数据: {len(job_line) - pos}个值未解析")

            except (ValueError, IndexError) as e:
                errors.append(f"工件{job_idx+1}基本信息解析错误: {str(e)}")

    except Exception as e:
        errors.append(f"文件读取或解析错误: {str(e)}")

    return errors, warnings

def main():
    """检查所有算例文件"""
    instance_dir = "src/main/resources/instance"
    pattern = os.path.join(instance_dir, "*.txt")

    all_errors = {}
    all_warnings = {}

    for file_path in glob.glob(pattern):
        file_name = os.path.basename(file_path)
        print(f"\n🔍 检查算例: {file_name}")

        errors, warnings = validate_instance_format(file_path)

        if errors:
            all_errors[file_name] = errors
            print(f"  ❌ 发现 {len(errors)} 个错误:")
            for error in errors:
                print(f"    • {error}")
        else:
            print("  ✅ 格式正确")

        if warnings:
            all_warnings[file_name] = warnings
            print(f"  ⚠️  发现 {len(warnings)} 个警告:")
            for warning in warnings:
                print(f"    • {warning}")

    # 汇总报告
    print("\n" + "="*60)
    print("📊 验证结果汇总")
    print("="*60)

    total_files = len(glob.glob(pattern))
    error_files = len(all_errors)
    warning_files = len(all_warnings)

    print(f"总算例文件数: {total_files}")
    print(f"有错误的文件: {error_files}")
    print(f"有警告的文件: {warning_files}")

    if all_errors:
        print(f"\n❌ 需要修复的文件:")
        for file_name, errors in all_errors.items():
            print(f"  • {file_name}: {len(errors)} 个错误")

    if all_warnings:
        print(f"\n⚠️  有警告的文件:")
        for file_name, warnings in all_warnings.items():
            print(f"  • {file_name}: {len(warnings)} 个警告")

if __name__ == "__main__":
    main()
