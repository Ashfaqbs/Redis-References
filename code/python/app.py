import redis
import json

# Redis Connection with Authentication
redis_client = redis.Redis(
    host='localhost', 
    port=6379, 
    username='myuser',  # Change this to your Redis username
    password='mypassword',  # Change this to your Redis password
    decode_responses=True
)

### --- STRING OPERATIONS --- ###
print("\n--- STRING OPERATIONS ---")
redis_client.set("user:name", "Ashfaq")
print("SET user:name ->", redis_client.get("user:name"))

redis_client.set("user:name", "Mohammed Ashfaq")
print("Updated user:name ->", redis_client.get("user:name"))

redis_client.delete("user:name")
print("Deleted user:name ->", redis_client.get("user:name"))


### --- JSON OPERATIONS --- ###
print("\n--- JSON OPERATIONS ---")
user_data = {"name": "Ashfaq", "age": 25, "city": "Bangalore"}

redis_client.set("user:100", json.dumps(user_data))
retrieved_user_data = json.loads(redis_client.get("user:100"))
print("GET user:100 ->", retrieved_user_data)

retrieved_user_data["age"] = 26
redis_client.set("user:100", json.dumps(retrieved_user_data))
print("Updated user:100 ->", json.loads(redis_client.get("user:100")))

redis_client.delete("user:100")
print("Deleted user:100 ->", redis_client.get("user:100"))




# OP
# --- STRING OPERATIONS ---
# SET user:name -> Ashfaq
# Updated user:name -> Mohammed Ashfaq
# Deleted user:name -> None

# --- JSON OPERATIONS ---
# GET user:100 -> {'name': 'Ashfaq', 'age': 25, 'city': 'Bangalore'}
# Updated user:100 -> {'name': 'Ashfaq', 'age': 26, 'city': 'Bangalore'}
# Deleted user:100 -> None