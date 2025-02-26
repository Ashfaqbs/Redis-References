### **Redis Table Concept – How to Store Users' Data?**  

Redis **does not have tables** like SQL databases. Instead, it stores **key-value pairs**. However, we can structure our data in a way that mimics tables.

---

## **📌 How to Store Users in Redis (Like a Table)?**  
Since Redis is a **NoSQL key-value store**, we use **keys** as "table rows" and store data in **hashes or JSON**.

---

### **1️⃣ Using Redis Hashes (Like Rows in a Table)**
Each **user** is stored as a **Redis Hash**, where:
- The **key** is `user:{id}`
- The **fields** act like columns (name, age, city)

**Example: Storing User Data in Hash**
```bash
HSET user:101 id "101" name "Ashfaq" age "25" city "Bangalore"
```
**Fetching the User**
```bash
HGETALL user:101
```
**Output:**
```bash
1) "id"
2) "101"
3) "name"
4) "Ashfaq"
5) "age"
6) "25"
7) "city"
8) "Bangalore"
```

✅ This is similar to a **table row in SQL** but stored as a **key-value hash**.

---

### **2️⃣ Using Redis JSON (Better for Complex Data)**
If RedisJSON is enabled, we can store **entire user objects** in JSON format.

**Storing User as JSON**
```bash
JSON.SET user:101 . '{"id":"101","name":"Ashfaq","age":25,"city":"Bangalore"}'
```
**Retrieving JSON User**
```bash
JSON.GET user:101
```
**Output:**
```json
{"id":"101","name":"Ashfaq","age":25,"city":"Bangalore"}
```

✅ **Advantage**: JSON format allows nested data, making it easier to store and query objects.

---

### **3️⃣ Indexing for Searching Users**
Redis does **not have direct SQL queries**, but we can:
- Store **all user IDs in a Redis Set** for easy retrieval.
- Use **RedisSearch** to enable filtering and full-text search.

**Adding User IDs to a Set (Like a Primary Key List)**
```bash
SADD users 101 102 103
```
**Fetching All User IDs**
```bash
SMEMBERS users
```

✅ This is similar to **storing all user IDs in a table's primary key column**.

---

## **🎯 Summary – How Redis Stores Data Like a Table**
| **SQL Concept**  | **Redis Equivalent** |
|------------------|----------------------|
| Table (`users`) | No direct tables, but use **keys** |
| Row (`user_id=101`) | Store as **Hash or JSON** (`user:101`) |
| Column (`name, age`) | Fields inside Hash (`HSET user:101 name "Ashfaq" age "25"`) |
| Indexing | Use **Sets or RedisSearch** |

Redis is **schema-less**, so you structure it **based on how you query**. For structured data like a table, **Hashes or JSON** are best.

---