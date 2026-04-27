# Transactions, Pipelining, and Lua Scripting

---

## Transactions (MULTI / EXEC / DISCARD / WATCH)

Redis transactions group multiple commands into a single atomic block. Commands queued inside a transaction are executed sequentially with no interleaving from other clients. Either all commands execute or none do (on DISCARD or WATCH failure).

**Important:** Redis transactions do NOT roll back on command errors. If one command in the block fails (e.g., wrong type), the rest still execute. Atomicity here means "all-or-nothing execution order", not full ACID rollback.

### Basic flow

```bash
MULTI          # start transaction
SET user:1 "Ashfaq"
INCR counter:1
HSET session:1 userId 1 ttl 3600
EXEC           # execute all queued commands atomically
```

```bash
MULTI
SET key "value"
DISCARD        # cancel the transaction, nothing executes
```

### WATCH — Optimistic Locking

`WATCH` monitors one or more keys. If any watched key is modified by another client before `EXEC` is called, the transaction is aborted (EXEC returns nil). This implements optimistic locking.

```bash
WATCH account:balance:1

MULTI
DECRBY account:balance:1 100
INCRBY account:balance:2 100
EXEC
# Returns nil if account:balance:1 was modified between WATCH and EXEC
# Returns list of results otherwise
```

**Pattern — retry on conflict:**
```java
// Spring RedisTemplate with WATCH
redisTemplate.execute(new SessionCallback<>() {
    @Override
    public Object execute(RedisOperations ops) {
        ops.watch("account:balance:1");
        ops.multi();
        ops.opsForValue().decrement("account:balance:1", 100);
        ops.opsForValue().increment("account:balance:2", 100);
        return ops.exec(); // returns null if WATCH key was modified
    }
});
```

### When to use transactions

- Updating multiple related keys that must stay consistent (e.g., transferring a value from one key to another)
- Incrementing a counter + logging in one atomic block
- Conditional updates using WATCH (check-then-set patterns)

---

## Pipelining

By default, each Redis command is a separate network round-trip: send command → wait for response → send next command. Pipelining batches multiple commands into one network call and reads all responses at once.

**Without pipelining:** 100 commands = 100 round-trips
**With pipelining:** 100 commands = 1 round-trip (roughly)

### CLI example

```bash
# Pipe commands via stdin
(echo -e "SET key1 val1\nSET key2 val2\nGET key1") | redis-cli --pipe
```

### Pipelining in Spring Boot (RedisTemplate)

```java
List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
    for (int i = 0; i < 100; i++) {
        connection.set(("key:" + i).getBytes(), ("value:" + i).getBytes());
    }
    return null;
});
```

### Pipelining vs Transactions

| Aspect | Pipelining | MULTI/EXEC |
|--------|-----------|------------|
| Purpose | Reduce network round-trips | Atomic execution |
| Atomicity | No | Yes |
| Server-side queuing | No (commands execute as received) | Yes (queued until EXEC) |
| Can combine | Yes — pipeline a MULTI/EXEC block |

Pipelining is a **client-side optimization** (fewer TCP round-trips). Transactions are a **server-side guarantee** (atomic execution). They can be combined.

---

## Lua Scripting

Lua scripts run atomically on the Redis server. The entire script executes as a single unit — no other command can interleave. This is more powerful than MULTI/EXEC because the script can contain conditional logic.

### Why Lua instead of MULTI/EXEC?

MULTI/EXEC cannot make decisions based on data read mid-transaction (you queue all commands blindly). Lua can: read a value, branch on it, then write.

### Executing a Lua script

```bash
# EVAL script numkeys key1 key2 ... arg1 arg2 ...
EVAL "return redis.call('GET', KEYS[1])" 1 user:name

# Conditional increment — only if value is below a threshold
EVAL "
  local current = tonumber(redis.call('GET', KEYS[1]))
  if current == nil then current = 0 end
  if current < tonumber(ARGV[1]) then
    return redis.call('INCR', KEYS[1])
  else
    return current
  end
" 1 counter:api:user:42 100
```

### Load a script once, call by SHA

For scripts used frequently, load them once and call by their SHA to avoid resending the script every time.

```bash
SCRIPT LOAD "return redis.call('GET', KEYS[1])"
# Returns: "e0e1f9fabfa9d353e2b1f97a5b92e23e8f9e5a2d"

EVALSHA e0e1f9fabfa9d353e2b1f97a5b92e23e8f9e5a2d 1 user:name
```

### Lua in Spring Boot

```java
DefaultRedisScript<Long> script = new DefaultRedisScript<>();
script.setScriptText(
    "local current = tonumber(redis.call('GET', KEYS[1])) or 0 " +
    "if current < tonumber(ARGV[1]) then " +
    "  return redis.call('INCR', KEYS[1]) " +
    "else " +
    "  return current " +
    "end"
);
script.setResultType(Long.class);

Long result = redisTemplate.execute(script, List.of("counter:api:user:42"), "100");
```

### Common Lua use cases

- **Atomic check-and-set:** read a value, conditionally update it
- **Distributed locking:** acquire lock only if not held, set TTL atomically
- **Rate limiting:** increment a counter + check threshold in one atomic step
- **Inventory decrement:** decrement stock only if > 0

### Lua script rules

- KEYS array: all Redis keys the script touches (important for cluster routing)
- ARGV array: all other arguments
- Scripts must complete quickly — they block Redis for their duration
- No blocking commands (BLPOP, SUBSCRIBE) inside scripts
- Scripts are deterministic — avoid time-dependent logic
