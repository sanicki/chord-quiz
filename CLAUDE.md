# Chord Quiz App - Development Context

## Build Environment
- **CI-ONLY:** This project is designed to be built ONLY via GitHub Actions.
- **Local Gradle:** Do not attempt to run or configure local Gradle/SDK settings.
- **Verification:** Only perform static analysis and code edits.

## Current Feature: Chord Groups Save/Delete

### Status: In Progress - Database Layer Complete, Repository Layer Partially Implemented

---

## Files Status (Last 24 Hours)

### ✅ Complete Files
- `app/src/main/java/com/chordquiz/app/data/db/entity/GroupEntity.kt`
- `app/src/main/java/com/chordquiz/app/data/db/dao/GroupDao.kt`
- `app/src/main/java/com/chordquiz/app/data/db/GroupManager.kt`
- `app/src/main/java/com/chordquiz/app/data/repository/GroupsRepository.kt`
- `app/src/main/java/com/chordquiz/app/di/DatabaseModule.kt`
- `app/src/main/java/com/chordquiz/app/di/RepositoryModule.kt`

### ⚠️ Partially Implemented
- `app/src/main/java/com/chordquiz/app/data/repository/GroupsRepositoryImpl.kt`
  - Basic CRUD methods work correctly
  - `getChordIdsForGroup()` needs fixing
  - `deleteChordsFromGroup()` needs implementation

### ✅ Database Updated
- `app/src/main/java/com/chordquiz/app/data/db/ChordQuizDatabase.kt`
  - Version bumped to 2
  - Added GroupEntity to entities
  - Added groupDao() abstract method

---

## Architectural Decisions

### Entity-DAO-Manager-Repository Pattern

```
App Layer
  ↓
ViewModel / UseCases
  ↓
Repository Layer (GroupsRepository)
  ↓
Manager Layer (GroupManager)
  ↓
DAO Layer (GroupDao)
  ↓
Database (Room)
```

### Why We Use GroupManager (Not Direct DAO in Repository)

The app uses a 4-layer architecture to separate concerns:

1. **Repository** - High-level business logic, exposes Flow for UI
2. **Manager** - Delegation wrapper, adds small logic layer
3. **DAO** - Pure database queries
4. **Database** - Room persistence

This differs from typical 3-layer apps but allows:
- Easy swapping of DAO implementations
- Testable managers with mock DAOs
- Clear separation of query logic vs. business logic

### Key Design Principles

1. **Never delete entire groups from the database**
   - Groups are metadata containers
   - Deleting a group should only remove chord associations
   - Chords persist for historical reference

2. **Use comma-separated chordIds in the group**
   - Efficient storage (no many-to-many table needed)
   - Simpler queries
   - Acceptable for this app's scale

3. **Groups have instrument scope**
   - Each group belongs to one instrument
   - `instrumentId` is mandatory field
   - Groups are queried by instrument

4. **Custom vs Built-in groups**
   - Custom groups have `name` starting with `"Custom:"`
   - `isCustom()`, `toName()` helpers extract original name

---

## Current Implementation Plan

### Immediate Next Steps (Database Layer Fixes)

1. **Update ChordDao.kt** - Add missing methods:
   ```kotlin
   @Query("SELECT * FROM chords WHERE instrumentId = :instrumentId AND id = :id")
   suspend fun getChordById(instrumentId: String, id: String): ChordDefinitionEntity?

   @Query("DELETE FROM chords WHERE id IN (:ids)")
   suspend fun deleteChordsById(ids: List<String>): Int
   ```

2. **Fix GroupsRepositoryImpl.kt**:
   ```kotlin
   // Fix getChordIdsForGroup
   override fun getChordIdsForGroup(groupId: Long): Flow<List<ChordDefinition>> =
       groupManager.getGroupsFlow("") // Get all groups
           .first() // Get first group matching ID (groups are instrument-scoped)
           ?.let { group ->
               group.chordIdsList().map { chordId ->
                   // Get instrumentId from group, not from chordDao
                   val instrumentId = group.instrumentId
                   val entity = chordDao.getChordById(instrumentId, chordId)
                   entity?.toDomain() // Convert to domain model
               } ?: emptyList()
           }
       .flowOf() // Handle empty case

   // Fix deleteChordsFromGroup
   override suspend fun deleteChordsFromGroup(group: GroupEntity): Int {
       val chordIdsToRemove = group.chordIdsList()
       chordDao.deleteChordsById(chordIdsToRemove)
       return chordIdsToRemove.count()
   }
   ```

3. **Run database migration** (version 1 → 2)
   - Add migration callback in DatabaseModule
   - Handle empty migration (no existing groups table)

---

## Pending Tasks

- [ ] Add `getChordById()` and `deleteChordsById()` to ChordDao
- [ ] Fix `getChordIdsForGroup()` implementation
- [ ] Implement actual chord deletion in `deleteChordsFromGroup()`
- [ ] Add database migration callback
- [ ] Test group save/delete flow
- [ ] Implement viewmodel logic (keep current plan)

---

## Future Considerations (Not Implemented Yet)

### Virtual "View" Table for Chord-Group Relationships

We discussed implementing a virtual table that joins chords with their groups:

```sql
-- Option 1: SQL View in Room
CREATE VIEW chord_groups AS
SELECT c.*, g.* FROM chords c
JOIN groups g ON c.instrumentId = g.instrumentId AND c.id IN (g.chordIds)
```

**Decision:** Not implementing this now.

**Why:**
- Room doesn't support views natively
- Would require custom queries in DAO
- Adds unnecessary complexity
- Current implementation (chordIds comma-separated) is simpler and sufficient

### Alternative: Database Triggers

```sql
-- Auto-populate chord associations
CREATE TRIGGER insert_chord_to_groups
AFTER INSERT ON chords
FOR EACH ROW
WHEN NEW.instrumentId IN (SELECT instrumentId FROM groups)
BEGIN
    INSERT INTO chord_groups (chord_id, group_id)
    SELECT NEW.id, g.id
    FROM groups g
    WHERE g.instrumentId = NEW.instrumentId AND NEW.id IN (g.chordIds);
END;
```

**Decision:** Not implementing this now.

**Why:**
- Triggers add fragility
- Room doesn't support triggers
- Current approach (explicit chord-to-group mapping) is more maintainable

---

## Key Code Patterns

### GroupEntity Helpers

```kotlin
@Entity(tableName = "groups")
data class GroupEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val instrumentId: String,
    val chordIds: String, // comma-separated chord IDs
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun chordIdsList(): List<String> = chordIds.split(",").filter { it.isNotBlank() }
    fun isCustom(): Boolean = name.startsWith("Custom:")
    fun toName(): String = if (name.startsWith("Custom:")) name.substring(7) else name
    
    companion object {
        fun fromName(name: String) = "Custom:$name"
    }
}
```

### DAO Query Examples

```kotlin
@Dao
interface GroupDao {
    @Query("SELECT * FROM groups WHERE instrumentId = :instrumentId ORDER BY name")
    fun getGroupsByInstrument(instrumentId: String): Flow<List<GroupEntity>>

    @Insert
    suspend fun insert(group: GroupEntity): Long

    @DeleteEntity
    suspend fun deleteGroup(id: Long)
    
    @Query("SELECT * FROM groups WHERE instrumentId = :instrumentId")
    fun getAllGroups(instrumentId: String): Flow<List<GroupEntity>>
}
```

### Repository Implementation Pattern

```kotlin
class GroupsRepositoryImpl @Inject constructor(
    private val groupManager: GroupManager,
    private val chordDao: ChordDao
) : GroupsRepository {
    
    override fun getGroupsFlow(instrumentId: String): Flow<List<GroupEntity>> =
        groupManager.getGroupsFlow(instrumentId)
        
    override suspend fun insertGroup(group: GroupEntity): Long =
        groupManager.insertGroup(group)
        
    override suspend fun deleteGroup(id: Long): Unit =
        groupManager.deleteGroup(id)
}
```

---

## Migration Notes

When updating from database version 1 to 2:

```kotlin
override fun migrate(database: SupportSQLiteDatabase, version: Int) {
    // version 1 → 2: groups table is new, no existing data to migrate
    // Just create the new table schema if needed
}
```

Room will handle the empty migration gracefully.
