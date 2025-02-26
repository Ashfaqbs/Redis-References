hands-on practice with **Redis Hashes** to store and modify user data.  

---

## **1️⃣ Add Users to Redis (Like Table Rows)**  
Each user will be stored as a **hash** with `user:{id}` as the key.

### **➤ Add a User**
```bash
HSET user:101 id "101" name "Ashfaq" age "25" city "Bangalore"
HSET user:102 id "102" name "John" age "30" city "Mumbai"
```
🔹 **Each user has fields like `id`, `name`, `age`, `city`**, similar to SQL table columns.

---

## **2️⃣ Retrieve User Data**  
### **➤ Get All Fields of a User**
```bash
HGETALL user:101
```
🔹 **Output:**
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

### **➤ Get a Specific Field (Like SELECT name FROM users WHERE id=101)**
```bash
HGET user:101 name
```
🔹 **Output:** `"Ashfaq"`

---

## **3️⃣ Modify User Data**
### **➤ Update User Age**
```bash
HSET user:101 age "26"
```
**Verify Update:**
```bash
HGET user:101 age
```
🔹 **Output:** `"26"`

### **➤ Add a New Field (Like Adding a Column Value)**
```bash
HSET user:101 phone "+919876543210"
```
**Verify:**
```bash
HGETALL user:101
```
🔹 **Now includes `phone`:**
```bash
1) "id"
2) "101"
3) "name"
4) "Ashfaq"
5) "age"
6) "26"
7) "city"
8) "Bangalore"
9) "phone"
10) "+919876543210"
```

---

## **4️⃣ Delete User Data**
### **➤ Remove a Specific Field**
```bash
HDEL user:101 phone
```
**Verify:**
```bash
HGETALL user:101
```
🔹 **Phone number is removed.**

### **➤ Delete an Entire User Record**
```bash
DEL user:102
```
**Verify:**
```bash
EXISTS user:102
```
🔹 **Output:** `"0"` (User deleted)

---

## **5️⃣ Get All User IDs (Like a Primary Key List)**
Since Redis doesn't have tables, we manually maintain a **Set of User IDs**.

### **➤ Add User IDs to a Set**
```bash
SADD users 101 102 103
```
### **➤ Get All Users**
```bash
SMEMBERS users
```
🔹 **Output:** `"101" "102" "103"`

---