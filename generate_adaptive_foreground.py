import os
import math
from PIL import Image

def generate_adaptive_foreground():
    source_path = "/Users/judyhoang/.gemini/antigravity/brain/69e3c5d1-0c14-4cab-9130-8442b3cce7b0/media__1781195387236.png"
    drawable_path = "/Users/judyhoang/AndroidStudioProjects/mobile_rootie_admin_finalproject/app/src/main/res/drawable"
    
    # 1. Delete the old XML foreground file to prevent compilation errors
    old_xml = os.path.join(drawable_path, "ic_launcher_foreground.xml")
    if os.path.exists(old_xml):
        os.remove(old_xml)
        print("Removed old XML launcher foreground drawable")
        
    # 2. Extract the white logo from the original PNG
    img = Image.open(source_path).convert("RGBA")
    width, height = img.size
    
    # Target background color (62, 77, 68)
    bg_r, bg_g, bg_b = 62, 77, 68
    
    # Create a new transparent image for the logo
    logo_only = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    pixels_in = img.load()
    pixels_out = logo_only.load()
    
    min_x, min_y = width, height
    max_x, max_y = 0, 0
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels_in[x, y]
            # Calculate Euclidean distance
            dist = math.sqrt((r - bg_r)**2 + (g - bg_g)**2 + (b - bg_b)**2)
            if dist > 40:
                # Keep logo color (off-white)
                pixels_out[x, y] = (r, g, b, a)
                # Keep track of bounding box
                if x < min_x: min_x = x
                if y < min_y: min_y = y
                if x > max_x: max_x = x
                if y > max_y: max_y = y
    
    print(f"Bounding box of extracted logo: ({min_x}, {min_y}) to ({max_x}, {max_y})")
    
    # Bounding box width and height
    bbox_w = max_x - min_x + 1
    bbox_h = max_y - min_y + 1
    
    # Crop the logo
    cropped_logo = logo_only.crop((min_x, min_y, max_x + 1, max_y + 1))
    
    # We want to place this logo inside a 512x512 transparent canvas
    canvas_size = 512
    # We scale the logo so that its maximum dimension is 280 pixels (about 55% of the canvas size)
    # This leaves a comfortable padding for adaptive icon scaling / circular crop.
    target_max_dim = 280
    
    scale = target_max_dim / max(bbox_w, bbox_h)
    new_w = int(bbox_w * scale)
    new_h = int(bbox_h * scale)
    
    resized_logo = cropped_logo.resize((new_w, new_h), Image.Resampling.LANCZOS)
    
    # Create the final canvas and paste the logo in the center
    final_foreground = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 0))
    paste_x = (canvas_size - new_w) // 2
    paste_y = (canvas_size - new_h) // 2
    final_foreground.paste(resized_logo, (paste_x, paste_y))
    
    # Save the output as PNG in the drawable folder
    output_path = os.path.join(drawable_path, "ic_launcher_foreground.png")
    final_foreground.save(output_path, "PNG")
    print(f"Successfully generated adaptive icon foreground at {output_path}")

if __name__ == "__main__":
    generate_adaptive_foreground()
