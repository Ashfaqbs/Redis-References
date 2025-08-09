## 📄 **Redis in Docker: Setup, Structure, and Programmatic Use**

---

### 🐳 **1. Docker Setup for Redis (with Volume and Password)**

#### ✅ Create a Docker volume (if not already created):

```powershell
docker volume create redis_data
```

#### ✅ Run Redis container with industry-style settings and password:

```powershell
docker run -d `
  --name redis-server `
  -p 6379:6379 `
  -v redis_data:/data `
  --restart unless-stopped `
  redis:latest `
  redis-server --requirepass "YourStrongPassword123"
```

* `-d`: Run in background
* `-p`: Map Redis port
* `-v`: Use named volume
* `--restart unless-stopped`: Auto-restart unless stopped manually
* `--requirepass`: Enables password authentication

---

### 🔐 **2. Connecting to Redis with Authentication**

#### ✅ From Redis CLI (host or container):

```bash
redis-cli -a YourStrongPassword123
```

#### ✅ From within Docker container:

```bash
docker exec -it redis-server redis-cli -a YourStrongPassword123
```

---

### 🧠 **3. Understanding Redis DB Structure**

#### Key Differences from RDBMS:

| RDBMS Concept | Redis Equivalent         |
| ------------- | ------------------------ |
| Server        | Redis server (container) |
| Database      | Numbered DBs (0–15)      |
| Schema        | ❌ None                   |
| Table         | ❌ None                   |
| Rows/Columns  | ✅ Key-value structures   |

* Redis has **16 logical DBs** (`0` to `15`) by default.
* Switch with: `SELECT <number>`
* Use `INFO keyspace` to inspect which DBs have keys.

---

### 🔑 **4. Redis Key-Value Data Types**

| Type   | Command Example                 | Use Case                    |
| ------ | ------------------------------- | --------------------------- |
| String | `SET user:1 "Ashfa"`            | Simple key-value            |
| Hash   | `HSET user:2 name "Ali" age 30` | Like a user record (object) |
| List   | `LPUSH messages "Hello"`        | Queues, chat messages       |
| Set    | `SADD tags "nodejs"`            | Unique values (unordered)   |
| ZSet   | `ZADD scores 100 "ashfa"`       | Sorted leaderboard, scores  |

---

### 🧪 **5. Example Usage**

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

### 💻 **6. Programmatic Access to Redis**

#### 🐍 Python (`redis` package)

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

---

#### 🌐 Node.js (`ioredis`)

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

---

#### ☕ Java (Jedis)

```java
Jedis jedis = new Jedis("localhost", 6379);
jedis.auth("YourStrongPassword123");
jedis.select(3);
jedis.set("user:1003", "Zara");
System.out.println(jedis.get("user:1003"));
```

---

### ⚙️ **7. Notes on Redis DBs**

* Default DB: `0`
* You can define a different DB in code using connection parameters.
* You rarely see `SELECT` in code because DB is chosen during connection.
* Logical DBs are more like **namespaces**, not isolated databases.
* Prefixing keys (e.g., `prod:user:1`) is a common way to separate environments/data.

---

### ✅ Final Thoughts

* Redis is a DB server, but unlike RDBMS:

  * No schema, no tables
  * Pure key-value store
* Fast, lightweight, and ideal for:

  * Caching
  * Session management
  * Real-time data (e.g., leaderboard, pub/sub)
* Use one of the 16 logical DBs (or configure more if needed)
* Secure with `--requirepass` or use ACLs for advanced setups
