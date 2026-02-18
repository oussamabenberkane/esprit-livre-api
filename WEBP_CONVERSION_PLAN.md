# WebP Image Conversion Plan

## Overview
Convert all uploaded images to WebP format automatically, and migrate existing images.

**Requirements:**
- Convert on upload (immediate)
- Quality: 80-85% (balanced)
- Keep original dimensions
- Replace originals (WebP only)
- Migrate all existing images

## Implementation

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

### 3. Create ImageMigrationService
**File:** `src/main/java/com/oussamabenberkane/espritlivre/service/ImageMigrationService.java`

Handles migration of existing images:
- Scan media directories for non-WebP files
- Convert each to WebP
- Update database URL references (change `.jpg`/`.png` → `.webp`)
- Delete original after successful conversion
- Track progress (total, processed, errors)

### 4. Create MigrationStatusDTO
**File:** `src/main/java/com/oussamabenberkane/espritlivre/service/dto/MigrationStatusDTO.java`

Simple DTO for migration status:
- `inProgress`, `totalImages`, `processedImages`, `successCount`, `errorCount`, `errors`

### 5. Add Admin Migration Endpoints
**File:** `src/main/java/com/oussamabenberkane/espritlivre/web/rest/AdminResource.java`

Add two endpoints:
- `POST /api/admin/images/migrate` - Start migration (async)
- `GET /api/admin/images/migration-status` - Check progress

### 6. Convert default.png
Convert `src/main/resources/media/default.png` to `default.webp` and update `FileStorageService.defaultPlaceholderPath`.

## Files Summary

**New files:**
1. `service/ImageConversionService.java` - WebP conversion logic
2. `service/ImageMigrationService.java` - Migration orchestration
3. `service/dto/MigrationStatusDTO.java` - Status tracking DTO

**Modified files:**
1. `service/FileStorageService.java` - Add conversion on upload
2. `web/rest/AdminResource.java` - Add migration endpoints

## Database Updates (during migration)

| Entity | Field | Change |
|--------|-------|--------|
| Book | coverImageUrl | `/media/books/cover_1.jpg` → `.webp` |
| Author | profilePictureUrl | `/media/authors/author_1.jpg` → `.webp` |
| BookPack | coverUrl | `/media/book-packs/pack_1.png` → `.webp` |
| Tag | imageUrl | `/media/categories/category_1.png` → `.webp` |

## Error Handling

**On upload:** If conversion fails, throw `BadRequestAlertException` - don't save corrupted file.

**On migration:** Log error, skip file, continue with others. Report failures in status endpoint. Keep original if conversion fails.

## Verification

1. **Upload test:** Upload a JPEG via admin panel, verify saved as `.webp`
2. **Quality check:** Compare file sizes (expect ~30-50% reduction)
3. **Migration test:** Run migration endpoint, verify all images converted
4. **Database check:** Verify URL references updated in database
5. **Frontend check:** Verify images display correctly after migration
