# WebP Image Conversion Plan

## Overview
Convert all uploaded images to WebP format automatically, and migrate existing images.

**Requirements:**
- Convert on upload (immediate)
- Quality: 80-85% (balanced)
- Keep original dimensions
- Replace originals (WebP only)
- Migrate existing images (manually on remote)

---

## Part 1: Code Changes (Local)

### 1. Create ImageConversionService
**File:** `src/main/java/com/oussamabenberkane/espritlivre/service/ImageConversionService.java`

New service with:
- `convertToWebP(InputStream)` - converts any image to WebP bytes
- `convertToWebP(BufferedImage, float quality)` - with quality parameter
- `isWebP(String contentType)` - checks if already WebP

Uses TwelveMonkeys `imageio-webp` (already in pom.xml at line 308).

```java
// Key logic:
ImageWriter writer = ImageIO.getImageWritersByMIMEType("image/webp").next();
ImageWriteParam param = writer.getDefaultWriteParam();
param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
param.setCompressionQuality(0.82f);  // 82% quality
```

### 2. Modify FileStorageService
**File:** `src/main/java/com/oussamabenberkane/espritlivre/service/FileStorageService.java`

Modify `storeImage()` method (lines 156-182):
- Inject `ImageConversionService`
- After validation, convert to WebP if not already WebP
- Always save with `.webp` extension

```java
// Current (line 169-176):
String extension = getFileExtension(originalFilename, entityName);
String filename = filenamePrefix + "." + extension;
Files.copy(file.getInputStream(), targetLocation, ...);

// New:
byte[] imageBytes;
if (imageConversionService.isWebP(file.getContentType())) {
    imageBytes = file.getBytes();
} else {
    imageBytes = imageConversionService.convertToWebP(file.getInputStream());
}
String filename = filenamePrefix + ".webp";
Files.write(targetLocation, imageBytes);
```

### 3. Convert default.png
Convert `src/main/resources/media/default.png` to `default.webp` and update `FileStorageService.defaultPlaceholderPath`.

### Files Summary (Local)

**New files:**
1. `service/ImageConversionService.java` - WebP conversion logic

**Modified files:**
1. `service/FileStorageService.java` - Add conversion on upload

---

## Part 2: Existing Image Migration (Remote Server)

### Step 1: Install cwebp tool
```bash
# Ubuntu/Debian
sudo apt update && sudo apt install webp

# Verify installation
cwebp -version
```

### Step 2: Backup existing images
```bash
cd /app/media  # or your MEDIA_ROOT_DIR
cp -r . ../media_backup_$(date +%Y%m%d)
```

### Step 3: Convert all images to WebP
```bash
# Convert all jpg/jpeg/png files to webp (82% quality)
cd /app/media

# Books
for f in books/*.{jpg,jpeg,png,JPG,JPEG,PNG}; do
  [ -f "$f" ] && cwebp -q 82 "$f" -o "${f%.*}.webp" && rm "$f"
done

# Authors
for f in authors/*.{jpg,jpeg,png,JPG,JPEG,PNG}; do
  [ -f "$f" ] && cwebp -q 82 "$f" -o "${f%.*}.webp" && rm "$f"
done

# Book-packs
for f in book-packs/*.{jpg,jpeg,png,JPG,JPEG,PNG}; do
  [ -f "$f" ] && cwebp -q 82 "$f" -o "${f%.*}.webp" && rm "$f"
done

# Categories
for f in categories/*.{jpg,jpeg,png,JPG,JPEG,PNG}; do
  [ -f "$f" ] && cwebp -q 82 "$f" -o "${f%.*}.webp" && rm "$f"
done

# Users
for f in users/*.{jpg,jpeg,png,JPG,JPEG,PNG}; do
  [ -f "$f" ] && cwebp -q 82 "$f" -o "${f%.*}.webp" && rm "$f"
done

# Default placeholder
cwebp -q 82 default.png -o default.webp && rm default.png
```

### Step 4: Update database URLs
Run these SQL queries to update image references:

```sql
-- Books
UPDATE book
SET cover_image_url = REGEXP_REPLACE(cover_image_url, '\.(jpg|jpeg|png|JPG|JPEG|PNG)$', '.webp')
WHERE cover_image_url IS NOT NULL
  AND cover_image_url NOT LIKE '%.webp';

-- Authors
UPDATE author
SET profile_picture_url = REGEXP_REPLACE(profile_picture_url, '\.(jpg|jpeg|png|JPG|JPEG|PNG)$', '.webp')
WHERE profile_picture_url IS NOT NULL
  AND profile_picture_url NOT LIKE '%.webp';

-- Book packs
UPDATE book_pack
SET cover_url = REGEXP_REPLACE(cover_url, '\.(jpg|jpeg|png|JPG|JPEG|PNG)$', '.webp')
WHERE cover_url IS NOT NULL
  AND cover_url NOT LIKE '%.webp';

-- Tags (categories)
UPDATE tag
SET image_url = REGEXP_REPLACE(image_url, '\.(jpg|jpeg|png|JPG|JPEG|PNG)$', '.webp')
WHERE image_url IS NOT NULL
  AND image_url NOT LIKE '%.webp';
```

### Step 5: Verify migration
```bash
# Check no non-webp images remain
find /app/media -type f \( -name "*.jpg" -o -name "*.jpeg" -o -name "*.png" \) | wc -l
# Should output: 0

# Check webp files exist
find /app/media -type f -name "*.webp" | wc -l
```

---

## Verification

1. **Upload test:** Upload a JPEG via admin panel, verify saved as `.webp`
2. **Quality check:** Compare file sizes (expect ~30-50% reduction)
3. **Remote check:** Verify all images converted and display correctly
4. **Database check:** Verify URL references updated in database
