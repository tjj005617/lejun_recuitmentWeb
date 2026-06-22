# 聊天对话实现记录

## 一、需求概述

用户之间可以实时聊天，支持：
- 文字消息收发
- 简历附件发送（从已有简历中选择）
- 消息已送达状态（sending → sent）
- 未读消息通知（红点 + SSE 推送）
- 历史消息加载

## 二、整体架构

```
┌─────────────┐     WebSocket      ┌──────────────────┐
│   前端 Vue   │◄──────────────────►│  ChatWebSocket   │
│ MessageCenter│                    │   (Spring WS)    │
└─────────────┘                    └────────┬─────────┘
                                            │
                                   ┌────────▼─────────┐
                                   │    RabbitMQ       │
                                   │  chat.queue       │
                                   └────────┬─────────┘
                                            │
                                   ┌────────▼─────────┐
                                   │ ChatMessageConsumer│
                                   └──┬──────────┬────┘
                                      │          │
                             ┌────────▼──┐  ┌────▼──────────┐
                             │  MongoDB   │  │  MySQL        │
                             │ chat_message│ │  message      │
                             └────────────┘  │ (通知表)       │
                                             └──────┬────────┘
                                                    │
                                             ┌──────▼────────┐
                                             │  SSE + Redis   │
                                             │  未读数推送     │
                                             └───────────────┘
```

选择 MongoDB 存聊天消息的原因：聊天数据量大、无复杂关联查询、写多读少，MongoDB 的文档模型天然适合。

## 三、数据模型

### 3.1 聊天消息（MongoDB）

```java
@Document(collection = "chat_message")
public class ChatMessage {
    @Id
    private String id;              // MongoDB ObjectId
    private String conversationId;  // 会话ID，如 "3_5"
    private Long senderId;
    private Long receiverId;
    private Long jobId;             // 关联职位
    private String type;            // "text" 或 "resume"
    private String content;
    private Long resumeId;          // 简历附件ID
    private String resumeName;
    private Boolean read;
    private String status;          // "sending" / "sent" / "failed"
    private LocalDateTime createdAt;

    // 会话ID生成：小ID在前，保证唯一
    public static String generateConversationId(Long userId1, Long userId2) {
        return userId1 < userId2 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }
}
```

### 3.2 通知消息（MySQL）

```java
@TableName("message")
public class Message {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Long jobId;
    private String type;    // "chat" / "application" / "system"
    private String content;
    private Integer isRead;
}
```

## 四、WebSocket 协议

### 客户端 → 服务端

| type | 说明 | 字段 |
|------|------|------|
| `text` | 文字消息 | content, jobId(可选) |
| `resume` | 简历附件 | resumeId, content, jobId(可选) |
| `read` | 标记已读 | 无 |
| `ack` | 消息确认 | messageId |

### 服务端 → 客户端

| type | 说明 | 字段 |
|------|------|------|
| `history` | 历史消息 | messages[] |
| `message` | 新消息 | data: ChatMessage |
| `ack` | 已送达确认 | messageId |
| `ack_timeout` | 投递超时 | data: ChatMessage |
| `read` | 对方已读 | userId |
| `error` | 错误 | message |

## 五、连接建立流程（详细）

### 5.1 WebSocket 连接建立

前端点击某个会话时，建立 WebSocket 连接：

```
用户点击会话 → 前端构造连接URL → WebSocket握手 → 服务端验证 → 注册会话 → 返回历史消息
```

#### 5.1.1 前端发起连接

```javascript
// MessageCenter.vue
const connectWebSocket = (conversationId) => {
  const token = localStorage.getItem('token')
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  // 拼接 URL: ws://localhost:8080/ws/chat?token=xxx&conversationId=3_5
  ws = new WebSocket(
    `${protocol}//${location.host}/ws/chat?token=${token}&conversationId=${conversationId}`
  )
}
```

#### 5.1.2 WebSocket 路由注册

```java
// WebSocketConfig.java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chatWebSocket, "/ws/chat")
                .setAllowedOrigins("*");
    }
}
```

Spring 收到 `/ws/chat` 请求后，路由到 `ChatWebSocket` 处理器。

#### 5.1.3 服务端连接建立（afterConnectionEstablished）

```
客户端 HTTP Upgrade 请求
    │
    ▼
解析 URL 参数: token, conversationId
    │
    ▼
参数缺失? ──是──► 关闭连接(NOT_ACCEPTABLE)
    │
    否
    ▼
JWT 验证: jwtUtil.getUserId(token)
    │
    ▼
userId 为 null? ──是──► 关闭连接(NOT_ACCEPTABLE)
    │
    否
    ▼
保存连接元信息
    │
    ├─► sessionInfoMap[sessionId] = SessionInfo(userId, conversationId)
    │
    └─► conversationSessions[conversationId].add(session)
    │
    ▼
查询历史消息: chatService.getHistory(conversationId)
    │
    ▼
发送给客户端: {type:"history", messages:[...]}
```

关键代码：

```java
@Override
public void afterConnectionEstablished(WebSocketSession session) throws Exception {
    // 1. 解析 URL 参数
    String query = session.getUri().getQuery();  // "token=xxx&conversationId=3_5"
    String token = null, conversationId = null;
    for (String param : query.split("&")) {
        String[] kv = param.split("=", 2);
        if ("token".equals(kv[0])) token = kv[1];
        if ("conversationId".equals(kv[0])) conversationId = kv[1];
    }

    // 2. JWT 验证
    Long userId = jwtUtil.getUserId(token);

    // 3. 注册到两个 Map
    sessionInfoMap.put(session.getId(), new SessionInfo(userId, conversationId));
    conversationSessions.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet())
                        .add(session);

    // 4. 推送历史消息
    List<ChatMessage> history = chatService.getHistory(conversationId);
    sendMessage(session, Map.of("type", "history", "messages", history));
}
```

### 5.2 会话ID生成机制

会话ID由两个用户ID拼接而成，保证双方使用同一个会话：

```java
// ChatMessage.java
public static String generateConversationId(Long userId1, Long userId2) {
    return userId1 < userId2 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
}
```

示例：
- 用户3 和 用户5 聊天 → conversationId = "3_5"（不论谁发起）
- 前端调用: `genConversationId(currentUserId, targetUserId)` 保证一致

### 5.3 双端连接示意

```
用户3(求职者)                        用户5(HR)
    │                                    │
    │  ws://host/ws/chat?token=A&conversationId=3_5
    │───────────────────►│               │
    │                    │  ChatWebSocket │
    │                    │               │
    │                    │  ws://host/ws/chat?token=B&conversationId=3_5
    │                    │◄───────────────────────────│
    │                    │               │
    │  conversationSessions["3_5"] = {session3, session5}
    │  sessionInfoMap[session3] = SessionInfo(userId=3, convId="3_5")
    │  sessionInfoMap[session5] = SessionInfo(userId=5, convId="3_5")
    │                    │               │
    │  {type:"history"}  │               │  {type:"history"}
    │◄───────────────────│               │◄───────────────────│
```

两人都收到相同的历史消息，后续任何一方发消息，`broadcastToConversation` 都会同时推送给两人。

### 5.4 连接断开处理

```java
@Override
public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
    SessionInfo info = sessionInfoMap.remove(session.getId());
    if (info != null) {
        Set<WebSocketSession> sessions = conversationSessions.get(info.conversationId);
        if (sessions != null) {
            sessions.remove(session);
            if (sessions.isEmpty()) {
                conversationSessions.remove(info.conversationId);  // 无人则清理
            }
        }
    }
}
```

## 六、消息收发时序图

### 6.1 发送消息（接收者在线）

```
 发送者前端          ChatWebSocket        RabbitMQ       ChatMessageConsumer      MongoDB        接收者前端
    │                    │                  │                  │                   │                │
    │  乐观更新(status=sending)             │                  │                   │                │
    │───►│               │                  │                  │                   │                │
    │    │               │                  │                  │                   │                │
    │    │ wsSend(text)  │                  │                  │                   │                │
    │───►│──────────────►│                  │                  │                   │                │
    │    │               │ sendChatMessage  │                  │                   │                │
    │    │               │─────────────────►│                  │                   │                │
    │    │               │                  │  消费消息         │                   │                │
    │    │               │                  │─────────────────►│                   │                │
    │    │               │                  │                  │ 检查在线→true      │                │
    │    │               │                  │                  │───►│              │                │
    │    │               │                  │                  │    │              │                │
    │    │               │                  │                  │  保存(sending)    │                │
    │    │               │                  │                  │──────────────────►│                │
    │    │               │                  │                  │                   │                │
    │    │               │                  │  延迟ACK(30s)    │                   │                │
    │    │               │                  │◄─────────────────│                   │                │
    │    │               │                  │                  │                   │                │
    │    │               │ broadcastToConversation             │                   │                │
    │    │◄──────────────│─────────────────────────────────────│──────────────────►│                │
    │    │               │                  │                  │                   │  替换临时消息   │
    │    │               │                  │                  │                   │───────────────►│
    │    │               │                  │                  │                   │                │
    │    │               │                  │                  │                   │  ACK确认       │
    │    │               │◄────────────────────────────────────│───────────────────│◄───────────────│
    │    │               │                  │                  │                   │                │
    │    │               │ updateStatus(sent)                  │                   │                │
    │    │               │─────────────────────────────────────│──────────────────►│                │
    │    │               │                  │                  │                   │                │
    │    │  {type:"ack"} │                  │                  │                   │                │
    │◄───│               │                  │                  │                   │                │
    │  status=sent       │                  │                  │                   │                │
    │  转圈消失          │                  │                  │                   │                │
```

### 6.2 发送消息（接收者离线）

```
 发送者前端          ChatWebSocket        RabbitMQ       ChatMessageConsumer      MongoDB        MySQL         Redis         SSE          接收者前端
    │                    │                  │                  │                   │              │             │             │               │
    │  乐观更新(sending)  │                  │                  │                   │              │             │             │               │
    │───►│               │                  │                  │                   │              │             │             │               │
    │    │ wsSend(text)  │                  │                  │                   │              │             │             │               │
    │───►│──────────────►│                  │                  │                   │              │             │             │               │
    │    │               │ sendChatMessage  │                  │                   │              │             │             │               │
    │    │               │─────────────────►│                  │                   │              │             │             │               │
    │    │               │                  │  消费消息         │                   │              │             │             │               │
    │    │               │                  │─────────────────►│                   │              │             │             │               │
    │    │               │                  │                  │ 检查在线→false     │              │             │             │               │
    │    │               │                  │                  │───►│              │              │             │             │               │
    │    │               │                  │                  │    │              │              │             │             │               │
    │    │               │                  │                  │  保存(sent)       │              │             │             │               │
    │    │               │                  │                  │──────────────────►│              │             │             │               │
    │    │               │                  │                  │                   │              │             │             │               │
    │    │               │ broadcastToConversation             │                   │              │             │             │               │
    │    │◄──────────────│─────────────────────────────────────│──────────────────►│              │             │             │               │
    │    │               │                  │                  │                   │              │             │             │               │
    │    │  status=sent   │                  │                  │                   │              │             │             │               │
    │    │  不转圈        │                  │                  │                   │              │             │             │               │
    │    │               │                  │                  │                   │              │             │             │               │
    │    │               │                  │                  │  写通知(type=chat) │              │             │             │               │
    │    │               │                  │                  │──────────────────────────────────►│             │             │               │
    │    │               │                  │                  │  未读数+1          │              │             │             │               │
    │    │               │                  │                  │──────────────────────────────────────────────►│             │               │
    │    │               │                  │                  │  SSE推送           │              │             │             │               │
    │    │               │                  │                  │─────────────────────────────────────────────────────────────│               │
    │    │               │                  │                  │                   │              │             │             │  (离线，丢弃)  │
```

### 6.3 打开会话 + 已读标记

```
 用户         前端 MessageCenter       ChatWebSocket        MongoDB       HTTP /api/message/read     MySQL         Redis
 │                │                        │                   │                   │                  │             │
 │  点击会话      │                        │                   │                   │                  │             │
 │───────────────►│                        │                   │                   │                  │             │
 │                │  connectWebSocket      │                   │                   │                  │             │
 │                │───────────────────────►│                   │                   │                  │             │
 │                │                        │  getHistory       │                   │                  │             │
 │                │                        │──────────────────►│                   │                  │             │
 │                │                        │  返回历史消息      │                   │                  │             │
 │                │◄───────────────────────│◄──────────────────│                   │                  │             │
 │                │                        │                   │                   │                  │             │
 │                │  markAsRead(senderId)  │                   │                   │                  │             │
 │                │───────────────────────────────────────────────────────────────►│                  │             │
 │                │                        │                   │                   │  批量更新isRead=1 │             │
 │                │                        │                   │                   │─────────────────►│             │
 │                │                        │                   │                   │  未读数 -N        │             │
 │                │                        │                   │                   │────────────────────────────────►│
 │                │                        │                   │                   │                  │             │
 │                │  fetchUnreadCount()    │                   │                   │                  │             │
 │                │───►│                   │                   │                   │                  │             │
 │                │  红点消失              │                   │                   │                  │             │
```

## 七、消息收发流程

### 7.1 发送流程（详细）

```
用户输入文字 → wsSend({type:"text", content})
  → ChatWebSocket.handleText()
  → 构建 ChatMessage，投递到 RabbitMQ
  → ChatMessageConsumer.handleMessage()
    → 检查接收者是否在线
    → 保存到 MongoDB（status=sending 或 sent）
    → WebSocket 广播给会话参与者
    → 写 MySQL 通知表 + Redis 未读数 + SSE 推送
```

关键代码（Consumer）：

```java
@RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
public void handleMessage(ChatMessage message) {
    // 1. 检查接收者是否在线，决定初始状态
    boolean receiverOnline = chatWebSocket.isUserInConversation(
            message.getReceiverId(), message.getConversationId());
    message.setStatus(receiverOnline ? "sending" : "sent");

    // 2. 持久化到 MongoDB
    ChatMessage saved = chatService.saveMessage(message);

    // 3. 如果接收者在线，发送延迟消息用于 ACK 超时检测
    if (receiverOnline) {
        chatMessageProducer.sendDelayedAckTimeout(saved);
    }

    // 4. 通过 WebSocket 推送给在线的会话参与者
    chatWebSocket.broadcastToConversation(
        saved.getConversationId(),
        Map.of("type", "message", "data", saved)
    );

    // 5. 触发通知推送（SSE + Redis 未读数）
    sendNotification(saved);
}
```

### 7.2 ACK 确认流程

当接收者在线时，需要 ACK 确认消息已送达：

```
接收者收到 message 事件
  → wsSend({type:"ack", messageId})
  → ChatWebSocket.handleAck()
  → chatService.updateMessageStatus(messageId, "sent")
  → 广播 ack 给会话
  → 发送者收到 ack → 更新本地状态为 sent
```

关键代码（前端）：

```javascript
ws.onmessage = (event) => {
  const data = JSON.parse(event.data)
  switch (data.type) {
    case 'message':
      const msg = data.data
      // 用临时消息替换逻辑，避免重复
      const existIdx = messages.value.findIndex(m =>
        m.id === msg.id || (m.id?.startsWith?.('temp_') &&
          m.senderId === msg.senderId && m.content === msg.content)
      )
      if (existIdx >= 0) {
        messages.value[existIdx] = msg  // 替换为真实消息
      } else {
        messages.value.push(msg)
      }
      // 对方发来的消息，发送 ACK
      if (msg.senderId !== currentUserId.value && msg.id) {
        wsSend({ type: 'ack', messageId: msg.id })
      }
      break

    case 'ack':
      // 消息已送达确认
      const ackMsg = messages.value.find(m => m.id === data.messageId)
      if (ackMsg) ackMsg.status = 'sent'
      break
  }
}
```

### 7.3 乐观更新

发送消息时先在本地显示，不用等后端返回：

```javascript
const sendTextMessage = () => {
  const content = newMessage.value.trim()
  newMessage.value = ''

  // 乐观更新：先在本地显示（status=sending）
  const tempId = 'temp_' + Date.now()
  messages.value.push({
    id: tempId,
    senderId: currentUserId.value,
    receiverId: selectedUserId.value,
    content,
    type: 'text',
    status: 'sending',
    createdAt: new Date().toISOString()
  })

  wsSend({ type: 'text', content })
}
```

当后端广播回来时，用真实消息替换临时消息（匹配 senderId + content）。

### 7.4 未读通知

接收者不在聊天页面时，消息直接标记 `sent`，同时写入 MySQL 通知表触发 SSE 推送：

```java
private void sendNotification(ChatMessage chatMsg) {
    Message notice = new Message();
    notice.setSenderId(chatMsg.getSenderId());
    notice.setReceiverId(chatMsg.getReceiverId());
    notice.setType("chat");
    notice.setContent("resume".equals(chatMsg.getType())
            ? "[简历] " + chatMsg.getContent()
            : chatMsg.getContent());
    notice.setIsRead(0);
    messageService.sendMessage(chatMsg.getSenderId(), notice);
    // → 写 MySQL + Redis 未读数 +1 + SSE 推送
}
```

## 八、后端关键实现

### 8.1 ChatService

```java
public interface ChatService {
    ChatMessage saveMessage(ChatMessage message);
    List<ChatMessage> getHistory(String conversationId);
    long markAsRead(String conversationId, Long receiverId);
    void updateMessageStatus(String messageId, String status);
}
```

### 8.2 updateMessageStatus（踩坑重点）

MongoDB 的 `_id` 是 `ObjectId` 类型，不能用 String 匹配：

```java
@Override
public void updateMessageStatus(String messageId, String status) {
    // 必须用 ObjectId 匹配，String 永远匹配不上
    Query query = new Query(Criteria.where("_id").is(new ObjectId(messageId)));
    Update update = new Update().set("status", status);
    mongoTemplate.updateFirst(query, update, ChatMessage.class);
}
```

### 8.3 WebSocket 连接管理

```java
// 会话ID → 该会话中的所有连接
private final Map<String, Set<WebSocketSession>> conversationSessions = new ConcurrentHashMap<>();

// sessionId → 连接元信息
private final Map<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

// 检查用户是否在会话中有连接
public boolean isUserInConversation(Long userId, String conversationId) {
    Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
    if (sessions == null) return false;
    for (WebSocketSession session : sessions) {
        SessionInfo info = sessionInfoMap.get(session.getId());
        if (info != null && info.userId().equals(userId) && session.isOpen()) {
            return true;
        }
    }
    return false;
}
```

### 8.4 RabbitMQ 配置

```java
// 聊天队列 + 死信队列 + ACK 超时延迟队列
public static final String CHAT_QUEUE = "chat.queue";
public static final String CHAT_DLQ = "chat.dlq";           // 死信
public static final String ACK_TIMEOUT_QUEUE = "chat.ack.timeout"; // 延迟30s
```

## 九、前端关键实现

### 9.1 WebSocket 连接

```javascript
const connectWebSocket = (conversationId) => {
  const token = localStorage.getItem('token')
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  ws = new WebSocket(
    `${protocol}//${location.host}/ws/chat?token=${token}&conversationId=${conversationId}`
  )
  ws.onmessage = (event) => { /* 处理消息 */ }
}
```

### 9.2 简历附件发送

```javascript
const handleSendResume = async (resumeId) => {
  // 乐观更新
  messages.value.push({
    id: 'temp_' + Date.now(),
    senderId: currentUserId.value,
    receiverId: selectedUserId.value,
    type: 'resume',
    content: '这是我的简历，请查看',
    resumeId,
    status: 'sending'
  })

  wsSend({ type: 'resume', resumeId, content: '这是我的简历，请查看' })
}
```

### 9.3 已读标记

打开会话时标记已读，同步更新 Redis 未读数：

```javascript
const selectConversation = async (conv) => {
  selectedUserId.value = conv.userId
  conv.unreadCount = 0

  const conversationId = genConversationId(currentUserId.value, conv.userId)
  connectWebSocket(conversationId)

  await markAsRead(conv.userId)  // HTTP 标记已读
  messageStore.fetchUnreadCount() // 刷新未读数
}
```

## 十、踩坑记录

### 10.1 MongoDB ObjectId 匹配

**问题**：消息状态永远更新不了，一直显示 "sending"。

**原因**：`Criteria.where("_id").is(messageId)` 用 String 匹配 ObjectId，永远匹配不上。

**修复**：`Criteria.where("_id").is(new ObjectId(messageId))`

### 10.2 接收者离线时消息卡在 sending

**问题**：接收者不在聊天页面时，发送的消息一直转圈。

**原因**：ACK 机制依赖接收者在线发送确认，离线时没人发 ACK。

**修复**：Consumer 检查接收者是否在线，离线时直接标记 `sent`。

```java
boolean receiverOnline = chatWebSocket.isUserInConversation(receiverId, conversationId);
message.setStatus(receiverOnline ? "sending" : "sent");
```

### 10.3 接收者离线无通知

**问题**：接收者在首页时，收不到新消息通知。

**原因**：聊天消息只存 MongoDB，不写 MySQL 通知表，SSE 推送不到。

**修复**：Consumer 保存聊天消息后，同时写 MySQL 通知表触发 SSE 推送。

### 10.4 临时消息替换

**问题**：发送者收到后端广播时，消息重复显示。

**原因**：发送者本地已有临时消息，后端广播又加了一条。

**修复**：用 senderId + content 匹配临时消息，替换为真实消息。

```javascript
const existIdx = messages.value.findIndex(m =>
  m.id === msg.id || (m.id?.startsWith?.('temp_') &&
    m.senderId === msg.senderId && m.content === msg.content)
)
if (existIdx >= 0) {
  messages.value[existIdx] = msg
}
```

## 十一、文件清单

| 文件 | 说明 |
|------|------|
| `ChatMessage.java` | MongoDB 文档实体 |
| `ChatMessageRepository.java` | MongoDB Repository |
| `ChatService.java` | 聊天服务接口 |
| `ChatServiceImpl.java` | 聊天服务实现 |
| `ChatWebSocket.java` | WebSocket 处理器 |
| `ChatMessageProducer.java` | RabbitMQ 生产者 |
| `ChatMessageConsumer.java` | RabbitMQ 消费者 |
| `WebSocketConfig.java` | WebSocket 配置 |
| `RabbitMQConfig.java` | RabbitMQ 队列配置 |
| `MessageCenter.vue` | 前端聊天页面 |
| `api/chat.js` | 前端 API 封装 |
