import os
from PIL import Image, ImageDraw

def generate_icons():
    source_path = "/Users/judyhoang/.gemini/antigravity/brain/69e3c5d1-0c14-4cab-9130-8442b3cce7b0/media__1781195387236.png"
    res_path = "/Users/judyhoang/AndroidStudioProjects/mobile_rootie_admin_finalproject/app/src/main/res"
    
    # Define densities and sizes
    densities = {
        "mdpi": 48,
        "hdpi": 72,
        "xhdpi": 96,
        "xxhdpi": 144,
        "xxxhdpi": 192
    }
    
    # Open source image
    img = Image.open(source_path)
    
    # Get background color from pixel (0, 0)
    bg_color = img.getpixel((0, 0))
    print(f"Detected background color: {bg_color}")
    
    # Convert RGB to Hex
    if len(bg_color) >= 3:
        hex_color = f"#{bg_color[0]:02X}{bg_color[1]:02X}{bg_color[2]:02X}"
        print(f"Hex background color: {hex_color}")
    
    for density, size in densities.items():
        # Create output directories if they don't exist
        dest_dir = os.path.join(res_path, f"mipmap-{density}")
        os.makedirs(dest_dir, exist_ok=True)
        
        # 1. Generate square icon (ic_launcher)
        img_square = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save as WEBP
        square_webp_path = os.path.join(dest_dir, "ic_launcher.webp")
        img_square.save(square_webp_path, "WEBP")
        
        # Also save as PNG in case a fallback is needed (and delete webp if not used, or keep webp)
        square_png_path = os.path.join(dest_dir, "ic_launcher.png")
        img_square.save(square_png_path, "PNG")
        
        # 2. Generate round icon (ic_launcher_round)
        # Create circular mask
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        draw.ellipse((0, 0, size, size), fill=255)
        
        # Apply circular mask
        img_round = Image.new("RGBA", (size, size))
        img_round.paste(img_square, (0, 0), mask=mask)
        
        # Save round as WEBP
        round_webp_path = os.path.join(dest_dir, "ic_launcher_round.webp")
        img_round.save(round_webp_path, "WEBP")
        
        # Save round as PNG
        round_png_path = os.path.join(dest_dir, "ic_launcher_round.png")
        img_round.save(round_png_path, "PNG")
        
        print(f"Generated icons for mipmap-{density} (size {size}x{size})")

if __name__ == "__main__":
    generate_icons()
