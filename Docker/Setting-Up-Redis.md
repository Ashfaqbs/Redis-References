# Setting Up Redis with Authentication and User Access Control in Docker

## Overview
This document provides a step-by-step guide to setting up Redis inside a Docker container with authentication, user management, and access control. It also includes troubleshooting steps and solutions to issues encountered during the process.

---

## 1. Running Redis in a Docker Container with Persistent Storage
To ensure that Redis data persists across container restarts, we use a named volume.

### Run Redis with a Volume and Authentication
```bash
docker run --name redis-container -d \
  -p 6379:6379 \
  -v redis-data:/data \
  --restart unless-stopped \
  redis:latest --requirepass mypassword
```

- `--requirepass mypassword` sets the default authentication password.
- `-v redis-data:/data` ensures Redis data persists.

### Verify Running Containers
```bash
docker ps
```
Ensure that the Redis container is running.

---

## 2. Connecting to Redis from Inside the Container
To interact with Redis, enter the container and use the Redis CLI.

### Access the Redis Container Shell
```bash
docker exec -it redis-container bash
```

### Connect to Redis CLI
```bash
redis-cli
```

### Authenticate with the Default Password
```bash
AUTH mypassword
OK
```

### List Existing Users
```bash
ACL LIST
```
**Output:**
```
1) "user default on sanitize-payload #hashcode ~* &* +@all"
```

This confirms that only the default user exists.

---

## 3. Adding a New Redis User with Authentication
Connecting with only the default password is not secure. We add a new user with specific credentials.

### Create a New User
```bash
ACL SETUSER myuser on >mypassword allcommands allkeys
```
- `on`: Enables the user.
- `>mypassword`: Sets the user's password.
- `allcommands`: Grants access to all Redis commands.
- `allkeys`: Allows access to all keys.

### Verify User Creation
```bash
ACL LIST
```
**Output:**
```
1) "user default on sanitize-payload #hashcode ~* &* +@all"
2) "user myuser on sanitize-payload #hashcode ~* resetchannels +@all"
```
The new user `myuser` is now created.

---

## 4. Connecting to Redis Using the New User
### Exit Redis CLI
```bash
exit
```

### Connect Using the New User
```bash
redis-cli -u redis://myuser:mypassword@localhost:6379
```
If successful, we should see the Redis prompt.

---

## 5. Connecting to Redis via Redis Desktop Clients
Now that we have a secure user, we can connect using a GUI-based Redis client.

### Steps:
1. Open a Redis client like **RedisInsight** or **TablePlus**.
2. Create a new connection with:
   - **Host:** `localhost`
   - **Port:** `6379`
   - **Username:** `myuser`
   - **Password:** `mypassword`
3. Test the connection.
4. Once connected, we can run commands like `ACL LIST` to verify user access.

---

## 6. Issues Faced and Solutions
### 1️⃣ "AUTH command not found"
**Issue:** Running `AUTH mypassword` in the container shell failed.
**Solution:** The `AUTH` command only works within `redis-cli`. Always run `redis-cli` first.

### 2️⃣ Unable to Connect to Redis with Username
**Issue:** Redis authentication worked only with the password, not with the user.
**Solution:**
- Ensure `ACL SETUSER` is correctly configured.
- Use the correct authentication format: `redis-cli -u redis://username:password@localhost:6379`.

### 3️⃣ GUI Redis Client Connection Failed
**Issue:** GUI tools failed to connect using the user.
**Solution:** Double-check credentials and ensure the Redis server allows external connections.

---

## Conclusion
This guide covered:
- Running Redis in Docker with persistent storage.
- Enabling authentication.
- Creating and managing users securely.
- Connecting using CLI and GUI clients.
- Troubleshooting common issues.

Following these steps ensures a secure and reliable Redis setup in a Docker environment.

![alt text](image.png)


Eg:
```
C:\Users\ashfa>docker exec -it d0 bash
root@d03ca587990c:/data# redis-cli
127.0.0.1:6379> AUTH mypassword
OK
127.0.0.1:6379> ACL LIST
1) "user default on sanitize-payload #89e01536ac207279409d4de1e5253e01f4a1769e696db0d6062ca9b8f56767c8 ~* &* +@all"
127.0.0.1:6379> ACL SETUSER myuser on >mypassword allcommands allkeys
OK
127.0.0.1:6379> ACL LIST
1) "user default on sanitize-payload #89e01536ac207279409d4de1e5253e01f4a1769e696db0d6062ca9b8f56767c8 ~* &* +@all"
2) "user myuser on sanitize-payload #89e01536ac207279409d4de1e5253e01f4a1769e696db0d6062ca9b8f56767c8 ~* resetchannels +@all"

Adding myuser post this we can connect to redis client with the myuser and mypassword creds
```