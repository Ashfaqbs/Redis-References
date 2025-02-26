Let's perform the same **RedisJSON CRUD operations** on this running Redis docker instance.

---

## **🔹 Step 1: Connect to Redis with Authentication**
Since our Redis instance requires authentication, we can connect using:  

```bash
redis-cli -u redis://myuser:mypassword@localhost:6379
```
or  
```bash
redis-cli
```
Then manually authenticate:
```bash
AUTH myuser mypassword
```
If authentication is successful, you should see:
```bash
OK
```

---

## **🔹 Step 2: Enable RedisJSON Module**
Since we're using the official Redis image (not Redis Stack), RedisJSON might not be available by default.  
To check:
```bash
MODULE LIST
```
If RedisJSON is missing, we need a **Redis Stack** container instead:  
```bash
docker run -d --name my-redis-db \
  -v redis_data:/data \
  -e REDIS_USER=myuser \
  -e REDIS_PASSWORD=mypassword \
  -p 6379:6379 \
  redis/redis-stack:latest --requirepass mypassword
```

---

## **🔹 Step 3: Store Users in RedisJSON**  

### **➤ Add a New User**  
```bash
JSON.SET user:101 $ '{
  "id": 101,
  "name": "Ashfaq",
  "age": 25,
  "city": "Bangalore",
  "email": "ashfaq@example.com"
}'
```

```bash
JSON.SET user:102 $ '{
  "id": 102,
  "name": "John",
  "age": 30,
  "city": "Mumbai",
  "email": "john@example.com"
}'
```

---

## **🔹 Step 4: Retrieve User Data**  

### **➤ Get Full User Data**
```bash
JSON.GET user:101
```

🔹 **Expected Output:**  
```json
{
  "id": 101,
  "name": "Ashfaq",
  "age": 25,
  "city": "Bangalore",
  "email": "ashfaq@example.com"
}
```

### **➤ Get Only the User’s Name**
```bash
JSON.GET user:101 $.name
```
🔹 **Expected Output:**  
```json
["Ashfaq"]
```

---

## **🔹 Step 5: Modify User Data**  

### **➤ Update User Age**
```bash
JSON.SET user:101 $.age 26
```
🔹 Verify:
```bash
JSON.GET user:101 $.age
```
🔹 **Output:** `[26]`

### **➤ Add a New Field (Phone Number)**
```bash
JSON.SET user:101 $.phone '"+919876543210"'
```
🔹 **Now the user has a phone number.**

---

## **🔹 Step 6: Delete Data**  

### **➤ Remove a Specific Field (Phone Number)**
```bash
JSON.DEL user:101 $.phone
```

### **➤ Delete an Entire User**
```bash
JSON.DEL user:102
```

🔹 **Verify:**
```bash
EXISTS user:102
```
🔹 **Output:** `"0"` (User deleted)

---

## **🔹 Step 7: Store Multiple Users (List of Users)**
Since Redis has no "tables," we can store all user IDs in a **set**:
```bash
SADD users 101 102 103
```

### **➤ Get All Users**
```bash
SMEMBERS users
```
🔹 **Output:**  
```bash
"101" "102" "103"
```