# Pub/Sub and Streams

Redis supports two messaging models: Pub/Sub (fire-and-forget) and Streams (persistent, consumer-group-aware log).

---

## Pub/Sub

Pub/Sub is a publish/subscribe messaging pattern. Publishers send messages to channels. Subscribers listening on those channels receive them in real time.

**Key property:** Messages are not persisted. If no subscriber is connected when a message is published, it is lost. This is fire-and-forget.

### Basic commands

```bash
# Terminal 1: Subscribe to a channel
SUBSCRIBE notifications:user:42

# Terminal 2: Publish a message
PUBLISH notifications:user:42 '{"type":"order_shipped","orderId":"999"}'

# Terminal 1 receives:
# 1) "message"
# 2) "notifications:user:42"
# 3) "{\"type\":\"order_shipped\",\"orderId\":\"999\"}"
```

### Pattern-based subscription

```bash
PSUBSCRIBE notifications:*      # subscribe to all channels matching the pattern
PUNSUBSCRIBE notifications:*    # unsubscribe
```

### Use cases

- Real-time chat messages
- Live dashboard updates (push to connected clients)
- Broadcasting invalidation events (e.g., "cache for user:42 is stale, evict it")
- Short-lived notifications where loss of a message is acceptable

### What Pub/Sub cannot do

- No message persistence — missed messages are gone
- No consumer groups — all subscribers receive every message
- No replay — cannot go back and re-read past messages
- No backpressure — if consumers are slow, messages are dropped

---

## Pub/Sub in Spring Boot

```java
// Publisher
@Autowired
private RedisTemplate<String, String> redisTemplate;

public void publishEvent(String channel, String message) {
    redisTemplate.convertAndSend(channel, message);
}

// Subscriber (message listener)
@Component
public class NotificationListener implements MessageListener {

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String body = new String(message.getBody());
        // handle message
    }
}

// Configuration
@Bean
public RedisMessageListenerContainer messageListenerContainer(
        RedisConnectionFactory factory,
        NotificationListener listener) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(factory);
    container.addMessageListener(listener, new PatternTopic("notifications:*"));
    return container;
}
```

---

## Redis Streams

Streams are a persistent, append-only log — similar to Kafka topics but built into Redis. Unlike Pub/Sub, messages survive after they are published and can be replayed. Consumers can be organized into groups, and each message is acknowledged explicitly.

**Think of it as:** a lightweight Kafka inside Redis.

### Basic stream commands

```bash
# Add a message (auto-generated ID based on timestamp)
XADD orders * orderId 101 status "placed" userId 42

# Add with explicit ID
XADD orders 1700000000000-0 orderId 102 status "placed"

# Read messages from the beginning
XRANGE orders - +

# Read last 10 messages
XREVRANGE orders + - COUNT 10

# Read messages newer than a given ID
XREAD COUNT 10 STREAMS orders 1700000000000-0

# Stream length
XLEN orders

# Delete a message
XDEL orders 1700000000000-0
```

### Consumer Groups

Consumer groups allow multiple consumers to share the work of processing a stream, each message being delivered to only one consumer in the group.

```bash
# Create a consumer group starting from the beginning ($  = only new messages)
XGROUP CREATE orders order-processors $ MKSTREAM

# Consumer reads its pending messages
XREADGROUP GROUP order-processors worker-1 COUNT 10 STREAMS orders >

# Acknowledge a message (remove from pending list)
XACK orders order-processors <message-id>

# See pending (unacknowledged) messages for the group
XPENDING orders order-processors - + 10
```

**Flow:**
1. Producer adds messages with `XADD`.
2. Each consumer in the group reads with `XREADGROUP` using `>` (new, undelivered messages).
3. Consumer processes the message and calls `XACK` to confirm.
4. Unacknowledged messages stay in a pending list — another consumer can claim them via `XCLAIM`.

### Stream trimming (retention)

```bash
# Keep only the last 1000 entries
XADD orders MAXLEN 1000 * orderId 103 status "placed"

# Trim explicitly
XTRIM orders MAXLEN 1000
```

### Streams in Spring Boot

```java
// Add to stream
redisTemplate.opsForStream()
    .add("orders", Map.of("orderId", "101", "status", "placed"));

// Read from stream
List<MapRecord<String, Object, Object>> messages = redisTemplate
    .opsForStream()
    .read(StreamOffset.fromStart("orders"));

// Consumer group read
redisTemplate.opsForStream()
    .read(Consumer.from("order-processors", "worker-1"),
          StreamReadOptions.empty().count(10),
          StreamOffset.create("orders", ReadOffset.lastConsumed()));
```

---

## Pub/Sub vs Streams vs Kafka

| Feature | Pub/Sub | Redis Streams | Kafka |
|---------|---------|---------------|-------|
| Persistence | No | Yes | Yes |
| Consumer groups | No (broadcast) | Yes | Yes |
| Message replay | No | Yes (up to retention) | Yes |
| Ordering | Per channel | Per stream (by ID) | Per partition |
| Throughput | Very high | High | Very high |
| Operational complexity | None | Low | High |
| Best for | Real-time broadcasts, cache invalidation | Lightweight event streaming, task queues | Large-scale, multi-service event streaming |

**Rule of thumb:**
- Pub/Sub for real-time ephemeral broadcast (live notifications, cache invalidation signals).
- Redis Streams for durable task queues or event logs that don't need Kafka's scale.
- Kafka when you need cross-service event streaming at high throughput with long retention.
