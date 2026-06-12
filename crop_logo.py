from PIL import Image

# Open the 1024x1024 logo image
img = Image.open('/Users/judyhoang/.gemini/antigravity/brain/69e3c5d1-0c14-4cab-9130-8442b3cce7b0/media__1781194408342.png')

# The text and slogan are in the bottom region (from y=600 to y=1000)
# Let's crop: left=0, top=600, right=1024, bottom=950
# We can adjust these numbers to get a clean bounds around the text and slogan
box = (0, 600, 1024, 980)
cropped = img.crop(box)

# Save the cropped logo text to drawables
cropped.save('/Users/judyhoang/AndroidStudioProjects/mobile_rootie_admin_finalproject/app/src/main/res/drawable/imv_logo_text.png')
print("Cropped image size:", cropped.size)
