# Cache Handling on Update — Spring Cache

## The Problem

`@CachePut` is used in `createNote()` but if it is not used in `updateNote()`, updating a note does not refresh the cache. The next read after an update returns stale data from the cache instead of the updated value from the database.

---

## Approach 1: Use `@CachePut` on the Update Method

`@CachePut` forces a cache write on every invocation, making it the simplest fix.

```java
@CachePut(value = "note", key = "#id")
public Note updateNote(Long id, Note noteDetails) {
    Optional<Note> optionalNote = noteRepository.findById(id);
    if (optionalNote.isPresent()) {
        Note note = optionalNote.get();
        note.setTitle(noteDetails.getTitle());
        note.setContent(noteDetails.getContent());
        note.setAddedDate(noteDetails.getAddedDate());
        note.setLive(noteDetails.isLive());
        return noteRepository.save(note);
    } else {
        return null;
    }
}
```

**How it works:**
1. Fetches the note from the database.
2. Updates the fields.
3. Saves to the database.
4. Automatically writes the updated note back into the cache.

---

## Approach 2: `@CacheEvict` + Manual Cache Insert

Evict the stale entry first, then explicitly insert the updated value. This gives more control over when and how the cache is populated.

```java
@CacheEvict(value = "note", key = "#id")
public Note updateNote(Long id, Note noteDetails) {
    Optional<Note> optionalNote = noteRepository.findById(id);
    if (optionalNote.isPresent()) {
        Note note = optionalNote.get();
        note.setTitle(noteDetails.getTitle());
        note.setContent(noteDetails.getContent());
        note.setAddedDate(noteDetails.getAddedDate());
        note.setLive(noteDetails.isLive());
        Note updatedNote = noteRepository.save(note);
        cacheManager.getCache("note").put(id, updatedNote);
        return updatedNote;
    } else {
        return null;
    }
}
```

**How it works:**
1. `@CacheEvict` removes the outdated cache entry.
2. Note is updated and saved to the database.
3. Updated note is explicitly inserted into the cache.

---

## Comparison

| Approach | Pros | Cons |
|----------|------|------|
| `@CachePut` | Automatic cache update, less code | Slightly less control |
| `@CacheEvict` + manual insert | Explicit control over cache lifecycle | Requires injecting `CacheManager` and manual put |

**Recommended:** Use `@CachePut` unless fine-grained cache lifecycle control is needed.

---

## CacheManager Configuration Requirement

`cacheManager.getCache("note").put(id, updatedNote)` requires a `CacheManager` bean to be configured.

### For Redis as cache store

```properties
spring.cache.type=redis
spring.redis.host=localhost
spring.redis.port=6379
```

### Inject CacheManager into the service

```java
@Service
public class NoteService {

    private final NoteRepository noteRepository;
    private final CacheManager cacheManager;

    public NoteService(NoteRepository noteRepository, CacheManager cacheManager) {
        this.noteRepository = noteRepository;
        this.cacheManager = cacheManager;
    }

    public Note updateNote(Long id, Note noteDetails) {
        Optional<Note> optionalNote = noteRepository.findById(id);
        if (optionalNote.isPresent()) {
            Note note = optionalNote.get();
            note.setTitle(noteDetails.getTitle());
            note.setContent(noteDetails.getContent());
            note.setAddedDate(noteDetails.getAddedDate());
            note.setLive(noteDetails.isLive());

            Note updatedNote = noteRepository.save(note);

            Cache cache = cacheManager.getCache("note");
            if (cache != null) {
                cache.put(id, updatedNote);
            }

            return updatedNote;
        } else {
            return null;
        }
    }
}
```

---

## Debugging the Cache

To verify the cache is being updated:

```java
Cache cache = cacheManager.getCache("note");
if (cache != null) {
    cache.put(id, updatedNote);
    System.out.println("Updated cache for note ID: " + id);
}
```

Also useful: inspect Redis directly with `redis-cli` to confirm key existence and TTL after an update.
