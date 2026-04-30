#!/usr/bin/env python3
"""
MusicShell APP 图标生成器（晶体共振最终版）

Crystalline Resonance - 晶体共振
- 1024px × 1024px
- 圆角方形
- 主色：#216F55（墨绿）
- 无文字
- Material Design 风格
- 3D 晶体玻璃质感
"""

from PIL import Image, ImageDraw, ImageFilter
import math

# 配置参数
SIZE = 1024
BG_COLOR = "#216F55"  # 深绿背景
BG_COLOR_CENTER = "#2A8B6A"  # 背景中心（稍亮）
CORNER_RADIUS = 224   # 圆角半径

# 频谱条配置
NUM_BARS = 9
BAR_GAP = 20          # 频谱条间距
MAX_BAR_HEIGHT = 520  # 最大频谱条高度
MIN_BAR_HEIGHT = 60   # 最小频谱条高度

# 频谱条高度（不对称，像在跳舞）
BAR_HEIGHTS = [
    0.2,   # 第1根（最左）
    0.45,  # 第2根
    0.7,   # 第3根
    0.9,   # 第4根
    1.0,   # 第5根（最高）
    0.8,   # 第6根
    0.55,  # 第7根
    0.35,  # 第8根
    0.15,  # 第9根（最右）
]

# 频谱条宽度（中间宽，两侧窄）
BAR_WIDTHS = [
    60,    # 第1根
    75,    # 第2根
    85,    # 第3根
    90,    # 第4根
    100,   # 第5根（最宽）
    90,    # 第6根
    85,    # 第7根
    75,    # 第8根
    60,    # 第9根
]

# 投影配置
SHADOW_OFFSET_X = 8
SHADOW_OFFSET_Y = 8
SHADOW_BLUR = 8
SHADOW_COLOR = (15, 58, 44, 128)  # 深绿 #0F3A2C，50%透明度

# 高光配置
HIGHLIGHT_TOP_ALPHA = 204  # 80%透明度
HIGHLIGHT_EDGE_ALPHA = 77  # 30%透明度

# 波浪线配置
WAVE_LINE_ALPHA = 51  # 20%透明度
WAVE_LINE_WIDTH = 5


def create_rounded_rectangle(draw, bbox, radius, fill):
    """绘制圆角矩形"""
    x0, y0, x1, y1 = bbox
    
    # 确保坐标有效
    if x1 <= x0 or y1 <= y0:
        return
    
    # 确保圆角半径不超过矩形尺寸的一半
    max_radius = min((x1 - x0) // 2, (y1 - y0) // 2)
    radius = min(radius, max_radius)
    
    if radius <= 0:
        # 如果圆角太小，直接绘制矩形
        draw.rectangle([x0, y0, x1, y1], fill=fill)
        return
    
    # 绘制矩形主体
    draw.rectangle([x0 + radius, y0, x1 - radius, y1], fill=fill)
    draw.rectangle([x0, y0 + radius, x1, y1 - radius], fill=fill)
    
    # 绘制四个圆角
    draw.pieslice([x0, y0, x0 + 2 * radius, y0 + 2 * radius], 180, 270, fill=fill)
    draw.pieslice([x1 - 2 * radius, y0, x1, y0 + 2 * radius], 270, 360, fill=fill)
    draw.pieslice([x0, y1 - 2 * radius, x0 + 2 * radius, y1], 90, 180, fill=fill)
    draw.pieslice([x1 - 2 * radius, y1 - 2 * radius, x1, y1], 0, 90, fill=fill)


def create_background():
    """创建带径向渐变的背景"""
    bg = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(bg)
    
    # 绘制圆角方形背景
    padding = 40
    create_rounded_rectangle(
        draw,
        [padding, padding, SIZE - padding, SIZE - padding],
        CORNER_RADIUS,
        BG_COLOR
    )
    
    # 添加径向渐变（中心稍亮）
    center_x, center_y = SIZE // 2, SIZE // 2
    max_distance = math.sqrt(center_x**2 + center_y**2)
    
    # 解析颜色
    r1, g1, b1 = int(BG_COLOR_CENTER[1:3], 16), int(BG_COLOR_CENTER[3:5], 16), int(BG_COLOR_CENTER[5:7], 16)
    r2, g2, b2 = int(BG_COLOR[1:3], 16), int(BG_COLOR[3:5], 16), int(BG_COLOR[5:7], 16)
    
    # 创建渐变蒙版
    gradient = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    gradient_draw = ImageDraw.Draw(gradient)
    
    # 使用同心圆创建渐变
    for r in range(500, 0, -2):
        ratio = r / 500
        alpha = int(30 * (1 - ratio))  # 越靠近中心越亮
        color = (r1, g1, b1, alpha)
        gradient_draw.ellipse(
            [center_x - r, center_y - r, center_x + r, center_y + r],
            fill=color
        )
    
    # 应用圆角蒙版
    mask = Image.new("L", (SIZE, SIZE), 0)
    mask_draw = ImageDraw.Draw(mask)
    create_rounded_rectangle(
        mask_draw,
        [padding, padding, SIZE - padding, SIZE - padding],
        CORNER_RADIUS,
        255
    )
    
    gradient.putalpha(mask)
    bg = Image.alpha_composite(bg, gradient)
    
    return bg


def create_shadow_layer(bars_info):
    """创建投影层"""
    shadow = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(shadow)
    
    # 背景投影
    padding = 40
    create_rounded_rectangle(
        draw,
        [padding + 8, padding + 8, SIZE - padding + 8, SIZE - padding + 8],
        CORNER_RADIUS,
        SHADOW_COLOR
    )
    
    # 频谱条投影
    for bar_x, bar_y_top, bar_y_bottom, bar_width, bar_radius in bars_info:
        create_rounded_rectangle(
            draw,
            [bar_x + SHADOW_OFFSET_X, bar_y_top + SHADOW_OFFSET_Y,
             bar_x + bar_width + SHADOW_OFFSET_X, bar_y_bottom + SHADOW_OFFSET_Y],
            bar_radius,
            SHADOW_COLOR
        )
    
    # 模糊投影
    shadow = shadow.filter(ImageFilter.GaussianBlur(radius=SHADOW_BLUR))
    
    return shadow


def create_wave_line(bars_info, total_width, start_x, base_y):
    """创建底部连接波浪线"""
    wave = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(wave)
    
    # 波浪线参数
    wave_width = total_width + 80
    wave_start_x = start_x - 40
    
    # 绘制正弦波曲线（多波叠加）
    points = []
    num_points = 200
    
    for i in range(num_points + 1):
        x = wave_start_x + (wave_width * i / num_points)
        
        # 多个正弦波叠加，形成更自然的波形
        y_offset = 0
        y_offset += 20 * math.sin(2 * math.pi * 2 * i / num_points)  # 主波
        y_offset += 10 * math.sin(2 * math.pi * 4 * i / num_points + 1)  # 次波
        y_offset += 5 * math.sin(2 * math.pi * 7 * i / num_points + 2)  # 细波
        
        y = base_y + y_offset
        points.append((x, y))
    
    # 绘制曲线
    wave_color = (180, 255, 220, WAVE_LINE_ALPHA)
    for i in range(len(points) - 1):
        draw.line([points[i], points[i + 1]], fill=wave_color, width=WAVE_LINE_WIDTH)
    
    return wave


def create_crystal_bar(x, y_top, y_bottom, width, radius):
    """
    创建单根晶体质感频谱条
    
    渐变：顶部纯白 → 中间淡绿 → 底部深墨绿
    """
    bar_height = y_bottom - y_top
    
    # 创建条形图层
    bar_img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    bar_draw = ImageDraw.Draw(bar_img)
    
    # 1. 主体渐变（从上到下）
    for y in range(y_top, y_bottom):
        # 计算渐变比例（顶部0，底部1）
        progress = (y - y_top) / max(bar_height, 1)
        
        # 渐变分段：
        # 顶部 20%：纯白 #FFFFFF
        # 中间 60%：快速衰减到淡绿 #E2F5EC
        # 底部 20%：深墨绿 #1A5A44
        
        if progress < 0.2:
            # 顶部 20%：纯白
            r, g, b = 255, 255, 255
            a = 255
        elif progress < 0.8:
            # 中间 60%：从白到淡绿
            local_progress = (progress - 0.2) / 0.6
            # 使用平方函数让衰减更快
            local_progress = local_progress ** 0.7
            
            r = int(255 + (226 - 255) * local_progress)
            g = int(255 + (245 - 255) * local_progress)
            b = int(255 + (236 - 255) * local_progress)
            a = 255
        else:
            # 底部 20%：从淡绿到深墨绿
            local_progress = (progress - 0.8) / 0.2
            
            r = int(226 + (26 - 226) * local_progress)
            g = int(245 + (90 - 245) * local_progress)
            b = int(236 + (68 - 236) * local_progress)
            a = 255
        
        bar_draw.rectangle([x, y, x + width, y + 1], fill=(r, g, b, a))
    
    # 2. 顶部高光（圆角处极细纯白高光）
    highlight_height = 6
    highlight_width = max(width - 8, 10)  # 确保宽度大于0
    highlight_x = x + 4
    highlight_y = y_top + 2
    
    # 确保圆角半径不超过矩形尺寸的一半
    highlight_radius = min(3, highlight_width // 2, highlight_height // 2)
    
    create_rounded_rectangle(
        bar_draw,
        [highlight_x, highlight_y, highlight_x + highlight_width, highlight_y + highlight_height],
        highlight_radius,
        (255, 255, 255, HIGHLIGHT_TOP_ALPHA)
    )
    
    # 3. 左侧边缘高光（1px 细线）
    edge_x = x + 3
    edge_height = max(y_bottom - y_top - 16, 10)  # 确保高度大于0
    
    create_rounded_rectangle(
        bar_draw,
        [edge_x, y_top + 8, edge_x + 1, y_top + 8 + edge_height],
        1,
        (255, 255, 255, HIGHLIGHT_EDGE_ALPHA)
    )
    
    return bar_img


def create_icon():
    """创建 APP 图标（晶体共振最终版）"""
    # 创建画布
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    
    # 1. 计算频谱条位置
    total_bars_width = sum(BAR_WIDTHS) + (NUM_BARS - 1) * BAR_GAP
    start_x = (SIZE - total_bars_width) // 2
    base_y = SIZE // 2 + MAX_BAR_HEIGHT // 2 - 60
    
    bars_info = []
    current_x = start_x
    
    for i in range(NUM_BARS):
        bar_width = BAR_WIDTHS[i]
        height_ratio = BAR_HEIGHTS[i]
        bar_height = int(MIN_BAR_HEIGHT + (MAX_BAR_HEIGHT - MIN_BAR_HEIGHT) * height_ratio)
        
        bar_y_top = base_y - bar_height
        bar_y_bottom = base_y
        bar_radius = bar_width // 2
        
        bars_info.append((current_x, bar_y_top, bar_y_bottom, bar_width, bar_radius))
        current_x += bar_width + BAR_GAP
    
    # 2. 创建背景
    bg = create_background()
    img = Image.alpha_composite(img, bg)
    
    # 3. 创建投影
    shadow = create_shadow_layer(bars_info)
    img = Image.alpha_composite(img, shadow)
    
    # 4. 创建波浪线（在条形下方）
    wave = create_wave_line(bars_info, total_bars_width, start_x, base_y)
    img = Image.alpha_composite(img, wave)
    
    # 5. 绘制晶体频谱条
    for i, (bar_x, bar_y_top, bar_y_bottom, bar_width, bar_radius) in enumerate(bars_info):
        bar_layer = create_crystal_bar(bar_x, bar_y_top, bar_y_bottom, bar_width, bar_radius)
        img = Image.alpha_composite(img, bar_layer)
    
    return img


def main():
    """主函数"""
    print("正在创建 MusicShell APP 图标（晶体共振最终版）...")
    
    # 创建图标
    icon = create_icon()
    
    # 保存图标
    output_path = "app-icon.png"
    icon.save(output_path, "PNG")
    
    print(f"图标已保存到: {output_path}")
    print(f"图标尺寸: {icon.size[0]}px × {icon.size[1]}px")
    print("\n晶体共振效果：")
    print("  - 3D 晶体玻璃质感，晶莹剔透")
    print("  - 9 根频谱条，不对称高度，像在跳舞")
    print("  - 渐变：顶部纯白 → 中间淡绿 → 底部深墨绿")
    print("  - 顶部圆角处极细纯白高光（80%透明度）")
    print("  - 左侧边缘 1px 高光（30%透明度）")
    print("  - 深绿色投影（#0F3A2C，50%透明度）")
    print("  - 底部波浪线连接（20%透明度）")
    print("  - 背景径向渐变（中心稍亮）")


if __name__ == "__main__":
    main()
