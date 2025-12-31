# Soft Delete Implementation - Esprit Livre

## Overview
This document describes the soft delete implementation for the Esprit Livre application. The implementation adds soft delete capability to core entities while preserving referential integrity and maintaining order history.

## Implemented Features

### 1. Soft Delete Fields Added

All soft-deletable entities now have:
- `active` (Boolean) - Defaults to `true`, set to `false` on soft delete
- `deletedAt` (Instant) - Timestamp when entity was soft deleted
- `deletedBy` (String) - User who performed the soft delete

**Entities with soft delete:**
- ✅ **Book** - Already had `active`, added `deletedAt` and `deletedBy`
- ✅ **Tag** - Already had `active`, added `deletedAt` and `deletedBy`
- ✅ **Author** - Added all three fields
- ✅ **BookPack** - Added all three fields
- ✅ **Order** - Added all three fields

**OrderItem Enhancement:**
- ✅ Added `productTitleSnapshot` field for denormalization

### 2. Automatic Query Filtering

All soft-deletable entities use `@SQLRestriction("active = true")` to automatically exclude soft-deleted records from queries.

**Files Modified:**
- [Book.java](src/main/java/com/oussamabenberkane/espritlivre/domain/Book.java)
- [Tag.java](src/main/java/com/oussamabenberkane/espritlivre/domain/Tag.java)
- [Author.java](src/main/java/com/oussamabenberkane/espritlivre/domain/Author.java)
- [BookPack.java](src/main/java/com/oussamabenberkane/espritlivre/domain/BookPack.java)
- [Order.java](src/main/java/com/oussamabenberkane/espritlivre/domain/Order.java)
- [OrderItem.java](src/main/java/com/oussamabenberkane/espritlivre/domain/OrderItem.java)

### 3. Database Migration

**Liquibase Changelog:** [20251231000000_add_soft_delete_fields.xml](src/main/resources/config/liquibase/changelog/20251231000000_add_soft_delete_fields.xml)

The migration adds:
- Soft delete columns to all entities
- Product title snapshot to OrderItem
- Sets existing records to `active = true`

### 4. Service Layer Updates

All services now have two delete methods:

**Soft Delete (default `delete()` method):**
```java
public void delete(Long id) {
    repository.findById(id).ifPresent(entity -> {
        entity.setActive(false);
        entity.setDeletedAt(Instant.now());
        SecurityUtils.getCurrentUserLogin().ifPresent(entity::setDeletedBy);
        repository.save(entity);
    });
}
```

**Hard Delete (`deleteForever()` method):**
```java
public void deleteForever(Long id) {
    repository.deleteById(id);
}
```

**Updated Services:**
- ✅ [BookService.java](src/main/java/com/oussamabenberkane/espritlivre/service/BookService.java)
- ✅ [TagService.java](src/main/java/com/oussamabenberkane/espritlivre/service/TagService.java)
- ✅ [AuthorService.java](src/main/java/com/oussamabenberkane/espritlivre/service/AuthorService.java)
- ✅ [BookPackService.java](src/main/java/com/oussamabenberkane/espritlivre/service/BookPackService.java)
- ✅ [OrderService.java](src/main/java/com/oussamabenberkane/espritlivre/service/OrderService.java)

### 5. Scheduled Cleanup Job

**New Service:** [CleanupService.java](src/main/java/com/oussamabenberkane/espritlivre/service/CleanupService.java)

**Schedule:** Runs daily at 2:00 AM (cron: `0 0 2 * * *`)

**Operations:**

1. **Denormalize Order Data**
   - Finds DELIVERED and CANCELLED orders
   - Copies product titles to `OrderItem.productTitleSnapshot`
   - Preserves order history even after product hard deletion

2. **Clean Up Soft-Deleted Books**
   - Finds Books with `active = false` and `deletedAt` not null
   - Excludes Books referenced by active orders (PENDING, PROCESSING, SHIPPED)
   - Nullifies FK references in OrderItems
   - Deletes book cover images
   - Hard deletes the Book entity

3. **Clean Up Soft-Deleted BookPacks**
   - Same logic as Books
   - Deletes book pack cover images
   - Hard deletes the BookPack entity

4. **Clean Up Soft-Deleted Authors**
   - Finds Authors with `active = false` and `deletedAt` not null
   - Excludes Authors referenced by active Books
   - Nullifies FK references in Books
   - Deletes author profile pictures
   - Hard deletes the Author entity

**Logging:**
The job logs the number of entities deleted for each category.

## How It Works

### Soft Delete Flow

1. **User Deletes Entity** (e.g., Book)
   - BookService.delete(id) is called
   - Entity's `active` set to `false`
   - `deletedAt` set to current timestamp
   - `deletedBy` set to current user's login

2. **Entity Hidden from Queries**
   - `@SQLRestriction("active = true")` automatically filters out the entity
   - Users can no longer see or interact with the entity

3. **References Preserved**
   - OrderItems still reference the soft-deleted Book
   - Historical order data remains intact
   - Author-Book relationships maintained

### Cleanup Flow

1. **Daily Job Triggers** (2:00 AM)

2. **Order Denormalization**
   - For each DELIVERED/CANCELLED order:
     - If OrderItem.productTitleSnapshot is null:
       - Copy Book.title or BookPack.title to productTitleSnapshot

3. **Entity Cleanup**
   - For each soft-deleted entity:
     - Check if referenced by active orders/books
     - If not referenced:
       - Nullify FK references
       - Delete media files
       - Hard delete from database

## Important Notes

### Referential Integrity

- **Foreign keys are nullified** before hard deletion to prevent database errors
- **Product title snapshots** preserve order history after hard deletion
- **Relationships are preserved** during soft delete period

### No Admin Restore

- Soft delete is permanent from the UI perspective
- No restore endpoints implemented (as per requirements)
- Admins must manually update the database to restore if needed

### Order Status Dependencies

The cleanup job considers these order statuses:
- **Active Orders:** PENDING, PROCESSING, SHIPPED (prevent deletion)
- **Finalized Orders:** DELIVERED, CANCELLED (allow deletion after denormalization)

### Media File Cleanup

The cleanup job deletes associated media files:
- Book covers via `FileStorageService.deleteBookCover()`
- BookPack covers via `FileStorageService.deleteBookPackCover()`
- Author pictures via `FileStorageService.deleteAuthorPicture()`

## Testing Recommendations

1. **Test Soft Delete**
   - Delete a Book and verify it no longer appears in listings
   - Verify existing OrderItems still reference the Book
   - Check that `active = false`, `deletedAt`, and `deletedBy` are set

2. **Test Order Denormalization**
   - Create an order with Books
   - Set order status to DELIVERED
   - Wait for cleanup job or manually trigger
   - Verify OrderItem.productTitleSnapshot is populated

3. **Test Hard Delete**
   - Soft delete a Book
   - Set all Orders containing the Book to DELIVERED
   - Wait for cleanup job
   - Verify Book is hard deleted
   - Verify OrderItem.book is NULL
   - Verify OrderItem.productTitleSnapshot retains the title
   - Verify cover image file is deleted

4. **Test FK Constraints**
   - Verify that soft-deleted Authors with active Books are NOT hard deleted
   - Verify that soft-deleted Books in active Orders are NOT hard deleted

## Configuration

**Cron Schedule:**
The cleanup job runs at 2:00 AM daily. To change:

```java
@Scheduled(cron = "0 0 2 * * *") // Modify this cron expression
public void performDailyCleanup() {
    // ...
}
```

**Disable Cleanup Job:**
If you need to temporarily disable the cleanup job, comment out the `@Scheduled` annotation in CleanupService.

## Migration Path

To apply these changes to your database:

1. **Run the Application**
   - The Liquibase changelog will automatically execute
   - All existing records will be set to `active = true`

2. **Verify Migration**
   ```sql
   -- Check that columns were added
   SELECT column_name FROM information_schema.columns
   WHERE table_name = 'book' AND column_name IN ('deleted_at', 'deleted_by');

   -- Verify all existing records are active
   SELECT COUNT(*) FROM book WHERE active = true;
   ```

## Future Enhancements

If requirements change, consider:

1. **Admin View Deleted Entities**
   - Create admin endpoints with custom queries that bypass `@SQLRestriction`
   - Use `@Filter` instead of `@SQLRestriction` for more control

2. **Restore Functionality**
   - Add admin-only restore endpoints
   - Set `active = true`, `deletedAt = null`, `deletedBy = null`

3. **Soft Delete Tags**
   - Currently Tags are soft-deleted but keep their Book relationships
   - Consider whether to cascade or preserve

4. **Audit Log**
   - Create a separate audit table to track all deletions
   - Store entity snapshot before deletion

5. **Configurable Retention Period**
   - Add configuration to keep soft-deleted entities for X days
   - Only hard delete after retention period expires

## Summary

This implementation provides:
- ✅ Soft delete for Books, Tags, Authors, BookPacks, Orders
- ✅ Automatic filtering of deleted entities
- ✅ Preservation of order history
- ✅ Daily cleanup job for denormalization and hard deletion
- ✅ Media file cleanup
- ✅ Referential integrity protection
- ✅ Audit trail with deletedAt and deletedBy fields

All changes are backward compatible and non-destructive. Existing data is preserved and automatically marked as active.
