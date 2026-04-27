# Redis in Docker: Setup, Structure, and Programmatic Use

---

## 1. Docker Setup for Redis (with Volume and Password)

### Create a Docker volume

```powershell
docker volume create redis_data
```

### Run Redis container with password authentication

```powershell
docker run -d `
  --name redis-server `
  -p 6379:6379 `
  -v redis_data:/data `
  --restart unless-stopped `
  redis:latest `
  redis-server --requirepass "YourStrongPassword123"
```

- `-d`: Run in background
- `-p`: Map Redis port
- `-v`: Use named volume for persistence
- `--restart unless-stopped`: Auto-restart on crash or reboot
- `--requirepass`: Enables password authentication

---

## 2. Connecting to Redis with Authentication

### From Redis CLI on the host

```bash
redis-cli -a YourStrongPassword123
```

### From inside the Docker container

```bash
docker exec -it redis-server redis-cli -a YourStrongPassword123
```

---

## 3. Redis DB Structure vs RDBMS

| RDBMS Concept | Redis Equivalent         |
| ------------- | ------------------------ |
| Server        | Redis server (container) |
| Database      | Numbered DBs (0-15)      |
| Schema        | None                     |
| Table         | None                     |
| Rows/Columns  | Key-value structures     |

- Redis has 16 logical DBs (`0` to `15`) by default.
- Switch with: `SELECT <number>`
- Use `INFO keyspace` to inspect which DBs have keys.

---

## 4. Redis Key-Value Data Types

| Type   | Command Example                 | Use Case                    |
| ------ | ------------------------------- | --------------------------- |
| String | `SET user:1 "Ashfa"`            | Simple key-value            |
| Hash   | `HSET user:2 name "Ali" age 30` | Like a user record (object) |
| List   | `LPUSH messages "Hello"`        | Queues, chat messages       |
| Set    | `SADD tags "nodejs"`            | Unique values (unordered)   |
| ZSet   | `ZADD scores 100 "ashfa"`       | Sorted leaderboard, scores  |

---

## 5. Example Usage

```redis
AUTH YourStrongPassword123

SELECT 0
SET user:1001 "Ashfa"

SELECT 1
GET user:1001  # (nil, because it's stored in DB 0)

SELECT 0
GET user:1001  # Returns "Ashfa"
```

---

## 6. Programmatic Access to Redis

### Python (`redis` package)

```python
import redis

r = redis.Redis(
    host='localhost',
    port=6379,
    password='YourStrongPassword123',
    db=1
)

r.set('user:1001', 'Ashfa')
print(r.get('user:1001'))  # b'Ashfa'
```

### Node.js (`ioredis`)

```javascript
const Redis = require('ioredis');

const redis = new Redis({
  host: '127.0.0.1',
  port: 6379,
  password: 'YourStrongPassword123',
  db: 2
});

redis.set('user:1002', 'Ali');
redis.get('user:1002').then(console.log);
```

### Java (Jedis)

```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.auth("YourStrongPassword123");
jedis.select(3);
jedis.set("user:1003", "Zara");
System.out.println(jedis.get("user:1003"));
```

---

## 7. Notes on Redis DBs

- Default DB: `0`
- DB is selected at connection time, not in runtime queries.
- Logical DBs act as namespaces, not isolated databases — all share the same memory and keyspace notifications.
- Key prefixing (e.g., `prod:user:1`) is a common alternative to using multiple DBs for environment separation.

---

## Summary

Redis is a DB server but unlike RDBMS — no schema, no tables. Pure key-value store.

Fast, lightweight, ideal for:
- Caching
- Session management
- Real-time data (leaderboards, pub/sub)

Use one of the 16 logical DBs (or configure more if needed). Secure with `--requirepass` or use ACLs for user-level access control.
