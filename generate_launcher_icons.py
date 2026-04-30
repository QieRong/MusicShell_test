#!/usr/bin/env python3
"""
APP 图标生成器

将 app_logo.png 转换为 Android mipmap 所需的不同尺寸图标。
"""

from PIL import Image
import os

# 配置参数
SOURCE_FILE = "app_logo.png"
MIPMAP_DIR = "app/src/main/res"

# Android mipmap 图标尺寸
MIPMAP_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

def create_round_icon(source, size):
    """创建圆形图标"""
    # 创建一个正方形画布
    icon = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    
    # 调整源图像大小
    resized = source.resize((size, size), Image.Resampling.LANCZOS)
    
    # 创建圆形蒙版
    mask = Image.new("L", (size, size), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.ellipse([0, 0, size - 1, size - 1], fill=255)
    
    # 应用蒙版
    icon.paste(resized, mask=mask)
    
    return icon

def main():
    """主函数"""
    print("正在生成 APP 图标...")
    
    # 检查源文件是否存在
    if not os.path.exists(SOURCE_FILE):
        print(f"错误：找不到源文件 {SOURCE_FILE}")
        return
    
    # 打开源图像
    source = Image.open(SOURCE_FILE)
    print(f"源图像尺寸：{source.size}")
    
    # 生成不同尺寸的图标
    for mipmap_name, size in MIPMAP_SIZES.items():
        # 创建目标目录
        mipmap_path = os.path.join(MIPMAP_DIR, mipmap_name)
        os.makedirs(mipmap_path, exist_ok=True)
        
        # 调整图像大小
        resized = source.resize((size, size), Image.Resampling.LANCZOS)
        
        # 保存为 PNG（替换 webp）
        launcher_path = os.path.join(mipmap_path, "ic_launcher.png")
        resized.save(launcher_path, "PNG")
        print(f"已生成：{launcher_path} ({size}x{size})")
        
        # 保存圆形版本
        round_path = os.path.join(mipmap_path, "ic_launcher_round.png")
        resized.save(round_path, "PNG")
        print(f"已生成：{round_path} ({size}x{size})")
    
    print("\n图标生成完成！")
    print("注意：原有的 webp 文件需要手动删除，或者在构建时会自动被覆盖。")

if __name__ == "__main__":
    # 需要导入 ImageDraw
    from PIL import ImageDraw
    main()
