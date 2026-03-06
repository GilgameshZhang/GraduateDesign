import random
import os

def generate_instance(job_count, printer_count, batch_count, discrete_count, instance_id, base_filename):
    """生成单个算例文件"""

    # 设置随机种子以确保可重现性
    random.seed(instance_id * 1000)

    # 第一行：总机器数 工件数
    total_machines = printer_count + batch_count + discrete_count
    lines = [f"{total_machines} {job_count}"]

    # 第二行：打印机配置
    printer_line = f"{printer_count}"
    for i in range(printer_count):
        length = random.randint(60, 120) * 10  # 600-1200整十数
        width = random.randint(40, 60) * 10    # 400-600整十数
        height = random.randint(40, 60) * 10   # 400-600整十数
        layer_height = round(random.uniform(0.025, 0.12), 3)  # 0.025-0.12
        switch_time = random.randint(300, 600)  # 300-600
        print_time = random.randint(10, 30)     # 10-30
        printer_line += f" {i+1} {length} {width} {height} {layer_height} {switch_time} {print_time}"
    lines.append(printer_line)

    # 第三行：批处理机配置
    batch_line = f"{batch_count}"
    for i in range(batch_count):
        process_time = random.randint(60, 120) * 10  # 600-1200整十数
        batch_line += f" {i+1} {process_time}"
    lines.append(batch_line)

    # 生成工件数据
    for job_id in range(job_count):
        # 工件尺寸
        length = random.randint(10, 600)
        width = random.randint(10, 600)
        height = random.randint(10, 400)

        # 工序数：打印+批处理+离散工序数
        discrete_ops = random.randint(1, 8)  # 1-8个离散工序
        total_ops = 2 + discrete_ops  # 打印+批处理+离散

        job_line = f"{total_ops} {length} {width} {height}"

        # 打印工序
        printer_id = (job_id % printer_count) + 1
        job_line += f" {total_ops} 1 {printer_id} 0"

        # 批处理工序
        batch_id = (job_id % batch_count) + 1 + printer_count
        job_line += f" 1 {batch_id} 0"

        # 离散工序
        for op in range(discrete_ops):
            # 每工序可选机器数：2-5台随机
            machine_count = random.randint(2, min(5, discrete_count))
            job_line += f" {machine_count}"

            # 随机选择机器
            available_machines = list(range(printer_count + batch_count + 1, total_machines + 1))
            selected_machines = random.sample(available_machines, machine_count)

            for machine_id in selected_machines:
                process_time = random.randint(600, 1200)
                job_line += f" {machine_id} {process_time}"

        lines.append(job_line)

    # 写入文件
    filename = f"{base_filename}_{instance_id:02d}.txt"
    with open(filename, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print(f"已生成算例：{filename}")
    return filename

if __name__ == "__main__":
    # 创建instance目录
    instance_dir = "src/main/resources/instance"
    os.makedirs(instance_dir, exist_ok=True)

    # 生成20个工件的10个算例
    print("正在生成20个工件的算例...")
    for i in range(1, 11):
        generate_instance(
            job_count=20,
            printer_count=3,
            batch_count=2,
            discrete_count=5,
            instance_id=i,
            base_filename=f"{instance_dir}/J20P3B2D5"
        )

    # 生成50个工件的10个算例
    print("\n正在生成50个工件的算例...")
    for i in range(1, 11):
        generate_instance(
            job_count=50,
            printer_count=5,
            batch_count=4,
            discrete_count=10,
            instance_id=i,
            base_filename=f"{instance_dir}/J50P5B4D10"
        )

    print("\n✅ 所有算例生成完成！共生成20个算例文件。")
