# SSE 消息推送实现记录

## 一、需求背景

用户不在聊天页面时（比如在首页浏览职位），需要实时收到新消息通知：
- 导航栏显示未读数红点
- 收到消息时弹窗提醒
- 点击进入消息中心后标记已读

## 二、技术选型

| 方案 | 优点 | 缺点 |
|------|------|------|
| 轮询 | 实现简单 | 延迟高、浪费资源 |
| WebSocket | 全双工实时 | 每个页面都要建连接，重量级 |
| SSE | 单向推送、自动重连、轻量 | 只能服务端→客户端 |

选择 SSE 的原因：通知推送是单向的（服务端推给客户端），不需要客户端回传数据。SSE 基于 HTTP，浏览器原生支持 `EventSource`，自动重连，比 WebSocket 轻量得多。

## 三、整体架构

```
┌─────────────────┐    SSE (HTTP长连接)    ┌──────────────────┐
│   前端 AppShell   │◄──────────────────────│  SseController   │
│   stores/message │                       │  /api/message/   │
│   EventSource    │                       │   subscribe      │
└────────┬────────┘                       └────────┬─────────┘
         │                                         │
         │ unreadCount                              │ SseEmitterManager
         │                                         │ (userId → SseEmitter)
         │                                         │
┌────────▼────────┐                       ┌────────▼─────────┐
│  Element Plus   │                       │ MessageService   │
│  ElNotification │                       │   sendMessage()  │
└─────────────────┘                       └──┬──────────┬───┘
                                             │          │
                                      ┌──────▼──┐  ┌───▼──────────┐
                                      │  MySQL   │  │    Redis     │
                                      │ message  │  │ message:unread│
                                      │  表      │  │  :{userId}   │
                                      └─────────┘  └──────────────┘
```

## 四、时序图

### 4.1 用户登录建立 SSE 连接

```
 用户         前端 AppShell          SseController       SseEmitterManager         Redis
 │                │                      │                      │                    │
 │  登录成功      │                      │                      │                    │
 │───────────────►│                      │                      │                    │
 │                │  connectSSE()        │                      │                    │
 │                │───►│                 │                      │                    │
 │                │    │                 │                      │                    │
 │                │  GET /subscribe?token=xxx                   │                    │
 │                │─────────────────────►│                      │                    │
 │                │                      │  getUserId(token)    │                    │
 │                │                      │───►│                 │                    │
 │                │                      │    │                 │                    │
 │                │                      │  addEmitter(userId)  │                    │
 │                │                      │─────────────────────►│                    │
 │                │                      │                      │ put(userId,emitter)│
 │                │                      │                      │───►│               │
 │                │                      │                      │    │               │
 │                │  SSE 长连接建立       │                      │                    │
 │                │◄─────────────────────│                      │                    │
 │                │                      │                      │                    │
 │                │  GET /unread-count   │                      │                    │
 │                │─────────────────────────────────────────────────────────────────►│
 │                │  未读数=3            │                      │                    │
 │                │◄─────────────────────────────────────────────────────────────────│
 │                │                      │                      │                    │
 │  导航栏红点(3) │                      │                      │                    │
 │◄───────────────│                      │                      │                    │
```

### 4.2 收到新消息推送

```
 发送者       ChatMessageConsumer       MySQL message表        Redis          SseEmitterManager     接收者前端(AppShell)
 │                    │                      │                   │                   │                      │
 │  聊天消息消费      │                      │                   │                   │                      │
 │───────────────────►│                      │                   │                   │                      │
 │                    │  INSERT message      │                   │                   │                      │
 │                    │─────────────────────►│                   │                   │                      │
 │                    │                      │                   │                   │                      │
 │                    │  INCR unread:+1      │                   │                   │                      │
 │                    │────────────────────────────────────────►│                   │                      │
 │                    │                      │                   │                   │                      │
 │                    │  sendToUser(message) │                   │                   │                      │
 │                    │─────────────────────────────────────────────────────────────►│                      │
 │                    │                      │                   │                   │  SSE 事件推送         │
 │                    │                      │                   │                   │─────────────────────►│
 │                    │                      │                   │                   │                      │
 │                    │                      │                   │                   │  unreadCount++       │
 │                    │                      │                   │                   │  红点+1              │
 │                    │                      │                   │                   │  ElNotification弹窗  │
 │                    │                      │                   │                   │  "新消息: 你好"       │
```

### 4.3 用户打开消息中心标记已读

```
 用户         前端 MessageCenter      HTTP /api/message/read       MySQL              Redis
 │                │                         │                        │                  │
 │  点击消息中心   │                         │                        │                  │
 │───────────────►│                         │                        │                  │
 │                │  markAsRead(senderId)   │                        │                  │
 │                │────────────────────────►│                        │                  │
 │                │                         │  SELECT COUNT(*)       │                  │
 │                │                         │  WHERE isRead=0        │                  │
 │                │                         │───────────────────────►│                  │
 │                │                         │  unreadCount=5         │                  │
 │                │                         │◄───────────────────────│                  │
 │                │                         │                        │                  │
 │                │                         │  UPDATE isRead=1       │                  │
 │                │                         │───────────────────────►│                  │
 │                │                         │                        │                  │
 │                │                         │  DECR unread BY 5      │                  │
 │                │                         │─────────────────────────────────────────►│
 │                │                         │  remaining=0           │                  │
 │                │                         │◄─────────────────────────────────────────│
 │                │                         │                        │                  │
 │                │  标记成功               │                        │                  │
 │                │◄────────────────────────│                        │                  │
 │                │                         │                        │                  │
 │                │  fetchUnreadCount()     │                        │                  │
 │                │───►│                    │                        │                  │
 │                │  红点消失               │                        │                  │
```

## 五、连接建立流程（详细）

### 5.1 整体连接流程

```
用户登录成功
    │
    ▼
AppShell 监听 isLoggedIn 变化
    │
    ▼
调用 messageStore.connectSSE()
    │
    ├─► 1. 先断开旧连接（防止重复）
    │
    ├─► 2. 拉取当前未读数（GET /api/message/unread-count）
    │      → Redis 优先，无缓存查 DB 回填
    │
    └─► 3. 创建 EventSource 连接
           URL: /api/message/subscribe?token=jwtToken
```

### 5.2 前端发起连接

```javascript
// stores/message.js
const connectSSE = () => {
  // 1. 先清理旧连接，防止重复
  disconnectSSE()

  const token = localStorage.getItem('token')
  if (!token) return

  // 2. 登录时先拉取数据库真实未读数（Redis 缓存）
  fetchUnreadCount()

  // 3. 建立 SSE 长连接
  eventSource = new EventSource(`/api/message/subscribe?token=${token}`)

  // 4. 监听 message 事件 → 未读数+1 + 弹窗
  eventSource.addEventListener('message', (e) => {
    const data = JSON.parse(e.data)
    unreadCount.value++
    showNotification(data)
  })

  // 5. 连接建立成功 → 再次同步数据库真实未读数（防止推送遗漏）
  eventSource.addEventListener('open', () => {
    fetchUnreadCount()
  })

  // 6. 连接断开 → 3秒后手动重连
  eventSource.onerror = () => {
    eventSource.close()
    eventSource = null
    reconnectTimer = setTimeout(connectSSE, 3000)
  }
}
```

#### 连接时序

```
 用户              AppShell              stores/message.js          SseController          SseEmitterManager
  │                   │                         │                         │                        │
  │  登录成功         │                         │                         │                        │
  │──────────────────►│                         │                         │                        │
  │                   │ watch(isLoggedIn)       │                         │                        │
  │                   │────────────────────────►│                         │                        │
  │                   │                         │                         │                        │
  │                   │                         │  fetchUnreadCount()     │                        │
  │                   │                         │─────────────────────────────────────────────────────►│
  │                   │                         │  unreadCount=3          │                        │
  │                   │                         │◄─────────────────────────────────────────────────────│
  │                   │                         │                         │                        │
  │                   │                         │  GET /subscribe?token=xx│                        │
  │                   │                         │────────────────────────►│                        │
  │                   │                         │                         │  getUserId(token)      │
  │                   │                         │                         │───►│                   │
  │                   │                         │                         │    │                   │
  │                   │                         │                         │  addEmitter(userId)    │
  │                   │                         │                         │───────────────────────►│
  │                   │                         │                         │                        │ put(userId, emitter)
  │                   │                         │                         │                        │
  │                   │                         │  SSE 长连接建立          │                        │
  │                   │                         │◄────────────────────────│                        │
  │                   │                         │                         │                        │
  │                   │                         │  open 事件触发           │                        │
  │                   │                         │  fetchUnreadCount()     │                        │
  │                   │                         │─────────────────────────────────────────────────────►│
  │                   │                         │  再次同步未读数          │                        │
  │                   │                         │                         │                        │
  │  导航栏红点(3)    │◄────────────────────────│                         │                        │
```

### 5.3 后端处理连接

```java
// SseController.java
@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter subscribe(@RequestParam("token") String token) {
    // 1. 从 JWT 解析用户ID
    Long userId = jwtUtil.getUserId(token);
    if (userId == null) {
        return null;  // 无效token，不建立连接
    }

    // 2. 创建 SseEmitter（0L = 不超时，永久保持）
    SseEmitter emitter = new SseEmitter(0L);

    // 3. 注册到连接管理器（替换旧连接）
    sseEmitterManager.addEmitter(userId, emitter);

    // 4. 注册生命周期回调（连接断开时清理）
    emitter.onCompletion(() -> sseEmitterManager.removeEmitter(userId));
    emitter.onTimeout(() -> sseEmitterManager.removeEmitter(userId));
    emitter.onError(e -> sseEmitterManager.removeEmitter(userId));

    return emitter;  // Spring 自动维护 HTTP 长连接
}
```

#### 连接管理器核心逻辑

```java
// SseEmitterManager.java
@Component
public class SseEmitterManager {
    // userId → SseEmitter，每个用户只有一个连接
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /** 注册连接（新连接替换旧连接） */
    public void addEmitter(Long userId, SseEmitter emitter) {
        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            old.complete();  // 关闭旧连接
        }
        log.info("SSE连接注册: userId={}, 当前在线数={}", userId, emitters.size());
    }

    /** 移除连接 */
    public void removeEmitter(Long userId) {
        emitters.remove(userId);
        log.info("SSE连接移除: userId={}, 当前在线数={}", userId, emitters.size());
    }

    /** 向指定用户推送事件 */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) return;  // 用户离线，静默丢弃
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException e) {
            log.warn("SSE推送失败: userId={}, error={}", userId, e.getMessage());
            emitters.remove(userId);  // 推送失败，移除无效连接
        }
    }
}
```

### 5.4 断开与重连

```
场景1: 用户登出
    AppShell 监听 isLoggedIn 变为 false
        → messageStore.disconnectSSE()
        → eventSource.close()
        → 清除 reconnectTimer
        → unreadCount = 0

场景2: 网络波动断开
    EventSource 触发 onerror
        → eventSource.close()
        → 3秒后自动调用 connectSSE() 重连
        → 后端创建新 SseEmitter
        → 旧连接被替换

场景3: 多标签页
    每个标签页独立建立 SSE 连接
    后端 SseEmitterManager 只保留最后一个连接
    → 最新标签页收到推送
    → 旧标签页连接被 complete()
```

## 六、消息推送流程（详细）

### 6.1 触发推送的场景

| 场景 | 触发方 | 推送内容 |
|------|--------|----------|
| 聊天消息 | ChatMessageConsumer | "{senderName}: 你好" |
| 投递通知 | ApplicationServiceImpl | "xxx 投递了你的职位" |
| 系统通知 | SystemService | "系统维护通知" |

### 6.2 推送完整流程

以聊天消息为例：

```
ChatMessageConsumer 消费聊天消息
    │
    ├─► 1. 保存到 MongoDB（chat_message 集合）
    │
    ├─► 2. 写 MySQL 通知表（message 表）
    │      → type="chat", content="消息内容", isRead=0
    │
    ├─► 3. Redis 未读数 +1
    │      → INCR message:unread:{receiverId}
    │
    └─► 4. SSE 推送给接收者
           → SseEmitterManager.sendToUser(receiverId, "message", notification)
```

#### 关键代码

```java
// MessageServiceImpl.java
@Override
public Message sendMessage(Long senderId, Message message) {
    message.setSenderId(senderId);
    message.setIsRead(0);
    messageMapper.insert(message);                         // 1. 写 MySQL

    notificationCacheService.incrementUnread(
        message.getReceiverId());                          // 2. Redis +1

    Map<String, Object> notification = new HashMap<>();    // 3. 构建推送数据
    notification.put("senderId", senderId);
    notification.put("content", message.getContent());
    notification.put("jobId", message.getJobId());
    notification.put("type", message.getType());

    sseEmitterManager.sendToUser(
        message.getReceiverId(), "message", notification); // 4. SSE 推送

    return message;
}
```

### 6.3 推送数据格式

```json
{
  "senderId": 5,
  "content": "你好，我对这个职位很感兴趣",
  "jobId": 42,
  "type": "chat"
}
```

### 6.4 前端接收推送

```javascript
// stores/message.js
eventSource.addEventListener('message', (e) => {
  const data = JSON.parse(e.data)

  // 1. 未读数 +1
  unreadCount.value++

  // 2. 弹窗通知
  ElNotification({
    title: '新消息',
    message: data.content || '您有一条新消息',
    type: 'info',
    duration: 5000
  })
})
```

### 6.5 用户离线时的推送处理

```
接收者不在页面（SSE 连接不存在）
    │
    ▼
ChatMessageConsumer 写 MySQL + Redis
    │
    ▼
SseEmitterManager.sendToUser() → emitter == null → 静默丢弃
    │
    ▼
用户下次登录
    │
    ▼
connectSSE() → fetchUnreadCount() → Redis 有缓存 → 返回未读数
    │
    ▼
导航栏显示红点（从 Redis 读取，不丢失）
```

## 七、未读数管理

### 7.1 Redis 缓存策略

Redis key：`message:unread:{userId}`，value 为未读数。

```java
// 收到消息 → +1
public void incrementUnread(Long userId) {
    stringRedisTemplate.opsForValue().increment(getKey(userId));
}

// 标记已读 → -N
public void decrementUnread(Long userId, int count) {
    Long remaining = stringRedisTemplate.opsForValue().decrement(getKey(userId), count);
    if (remaining < 0) {
        stringRedisTemplate.opsForValue().set(getKey(userId), "0");  // 防止负数
    }
}

// 查询未读数（Redis 优先，无缓存查 DB 回填）
public int getUnreadCount(Long userId) {
    String val = stringRedisTemplate.opsForValue().get(getKey(userId));
    if (val != null) return Integer.parseInt(val);

    // 缓存穿透保护：查 DB 回填
    Long count = messageMapper.selectCount(...);
    stringRedisTemplate.opsForValue().set(getKey(userId), String.valueOf(count));
    return count.intValue();
}
```

### 7.2 已读标记

用户打开会话时，标记该发送者的所有未读消息为已读：

```java
@Override
public void markAsRead(Long receiverId, Long senderId) {
    // 查询未读数
    Long unreadCount = messageMapper.selectCount(
        new LambdaQueryWrapper<Message>()
            .eq(Message::getReceiverId, receiverId)
            .eq(Message::getSenderId, senderId)
            .eq(Message::getIsRead, 0)
    );

    // 批量更新为已读
    messageMapper.update(null,
        new LambdaUpdateWrapper<Message>()
            .eq(Message::getReceiverId, receiverId)
            .eq(Message::getSenderId, senderId)
            .eq(Message::getIsRead, 0)
            .set(Message::getIsRead, 1)
    );

    // Redis 未读数减少
    if (unreadCount > 0) {
        notificationCacheService.decrementUnread(receiverId, unreadCount.intValue());
    }
}
```

## 八、前端集成

### 8.1 AppShell 导航栏

登录后建立 SSE 连接，登出时断开：

```javascript
// AppShell.vue
const messageStore = useMessageStore()

watch(isLoggedIn, (val) => {
  if (val) {
    messageStore.connectSSE()
  } else {
    messageStore.disconnectSSE()
    messageStore.unreadCount.value = 0
  }
})
```

导航栏显示未读数红点：

```html
<span v-if="messageStore.unreadCount.value > 0" class="navbar__badge">
  {{ messageStore.unreadCount.value > 99 ? '99+' : messageStore.unreadCount.value }}
</span>
```

### 8.2 弹窗通知

收到 SSE 推送时弹窗提醒：

```javascript
const showNotification = (data) => {
  ElNotification({
    title: '新消息',
    message: data.content || '您有一条新消息',
    type: 'info',
    duration: 5000
  })
}
```

### 8.3 消息中心已读

进入消息中心打开会话时，调用已读接口：

```javascript
const selectConversation = async (conv) => {
  conv.unreadCount = 0
  await markAsRead(conv.userId)        // HTTP 标记已读
  messageStore.fetchUnreadCount()      // 刷新导航栏未读数
}
```

## 九、SSE vs WebSocket 对比

本项目同时使用了 SSE 和 WebSocket，各有分工：

| 维度 | SSE | WebSocket |
|------|-----|-----------|
| 用途 | 通知推送（未读数、弹窗） | 聊天消息收发 |
| 连接范围 | 全局一个连接（AppShell） | 每个会话一个连接（MessageCenter） |
| 方向 | 服务端→客户端 | 双向 |
| 生命周期 | 登录后一直保持 | 进入聊天页建立，离开断开 |
| 数据 | 通知摘要（senderId, content） | 完整聊天消息（MongoDB 文档） |

## 十、踩坑记录

### 10.1 SSE 连接断开不重连

**问题**：网络波动后 SSE 断开，收不到推送。

**原因**：`EventSource` 虽然有自动重连，但服务端 `SseEmitter` 出错后被移除，客户端重连时需要重新注册。

**修复**：前端 `onerror` 中手动重连：

```javascript
eventSource.onerror = () => {
  eventSource.close()
  reconnectTimer = setTimeout(connectSSE, 3000)  // 3秒后重连
}
```

### 10.2 未读数不准

**问题**：Redis 未读数和数据库不一致。

**原因**：直接操作数据库（如批量删除消息）没有同步更新 Redis。

**修复**：`getUnreadCount` 做了兜底——Redis 无缓存时查 DB 回填。对于关键操作（标记已读、发送消息），显式调用 `increment/decrement`。

### 10.3 聊天消息不触发通知

**问题**：用户在首页时，收到聊天消息没有通知提醒。

**原因**：聊天消息只存 MongoDB，不写 MySQL `message` 表，SSE 推送不到。

**修复**：`ChatMessageConsumer` 保存聊天消息后，同时写 MySQL 通知表触发 SSE 推送。

## 十一、文件清单

| 文件 | 说明 |
|------|------|
| `SseEmitterManager.java` | SSE 连接管理器（userId→Emitter 映射） |
| `SseController.java` | SSE 订阅端点 `/api/message/subscribe` |
| `MessageService.java` | 消息服务接口 |
| `MessageServiceImpl.java` | 消息服务实现（写DB+Redis+SSE） |
| `NotificationCacheService.java` | 未读数缓存接口 |
| `NotificationCacheServiceImpl.java` | Redis 未读数实现 |
| `stores/message.js` | 前端 SSE 连接 + 未读数状态 |
| `AppShell.vue` | 导航栏集成未读数红点 |
