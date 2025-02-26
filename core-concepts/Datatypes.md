# **Redis Data Types & CRUD Operations**

## **1. Strings**
A string in Redis is a simple key-value pair where the value is a string (up to 512MB).

### **CRUD Operations:**
```bash
SET user:name "Ashfaq"        # Create
GET user:name                 # Read
SET user:name "Mohammed"       # Update
DEL user:name                  # Delete
```

---

## **2. Lists**
A list in Redis is a collection of ordered values, similar to an array.

### **CRUD Operations:**
```bash
RPUSH users "Ashfaq" "Mohammed" "Ali"  # Create (add elements to the right)
LPUSH users "Ahmed"                     # Add to the left
LRANGE users 0 -1                        # Read (get all elements)
LPOP users                                # Remove from left
RPOP users                                # Remove from right
DEL users                                 # Delete entire list
```

---

## **3. Sets**
Sets store unique, unordered values.

### **CRUD Operations:**
```bash
SADD countries "India" "USA" "UK"    # Create
SMEMBERS countries                   # Read (get all elements)
SREM countries "UK"                   # Remove an element
DEL countries                         # Delete entire set
```

---

## **4. Sorted Sets (Zsets)**
Like sets, but each value has a score, allowing sorting.

### **CRUD Operations:**
```bash
ZADD scores 100 "Ashfaq" 200 "Mohammed" 150 "Ali"  # Create
ZRANGE scores 0 -1 WITHSCORES                     # Read (get elements with scores)
ZREM scores "Mohammed"                             # Remove an element
ZINCRBY scores 50 "Ashfaq"                         # Update score of an element
DEL scores                                        # Delete entire sorted set
```

---

## **5. Hashes**
Hashes are like objects (key-value pairs inside a key).

### **CRUD Operations:**
```bash
HSET user:100 name "Ashfaq" age "25" country "India"  # Create
HGET user:100 name                                    # Read (get a field)
HGETALL user:100                                      # Read all fields
HDEL user:100 age                                     # Delete a field
DEL user:100                                         # Delete entire hash
```

---

## **6. Bitmaps**
Bitmaps are used for storing bits efficiently (e.g., tracking user logins).

### **CRUD Operations:**
```bash
SETBIT user:login 0 1     # Set bit at position 0
GETBIT user:login 0       # Get bit at position 0
BITCOUNT user:login       # Count number of 1s
DEL user:login           # Delete bitmap
```

---

## **7. HyperLogLogs**
Used for approximate unique counts.

### **CRUD Operations:**
```bash
PFADD unique_visitors "user1" "user2" "user3"  # Create
PFCOUNT unique_visitors                        # Read (estimate unique count)
PFMERGE all_visitors unique_visitors another_hll  # Merge HyperLogLogs
DEL unique_visitors                            # Delete
```

---

## **8. Streams (Real-time event data)**
Streams allow data ingestion with timestamps.

### **CRUD Operations:**
```bash
XADD mystream * user "Ashfaq" message "Hello"   # Create event with auto timestamp
XRANGE mystream - +                             # Read all events
XDEL mystream <message-id>                      # Delete an event
DEL mystream                                    # Delete stream
```

---