package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.interview.domain.po.Job;
import com.interview.domain.po.Message;
import com.interview.domain.po.User;
import com.interview.domain.vo.ConversationVO;
import com.interview.mapper.JobMapper;
import com.interview.mapper.MessageMapper;
import com.interview.mapper.UserMapper;
import com.interview.service.MessageService;
import com.interview.service.NotificationCacheService;
import com.interview.sse.SseEmitterManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 消息服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final JobMapper jobMapper;
    private final SseEmitterManager sseEmitterManager;
    private final NotificationCacheService notificationCacheService;

    /**
     * 发送消息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Message sendMessage(Long senderId, Message message) {
        message.setSenderId(senderId);
        message.setIsRead(0); // 默认未读
        messageMapper.insert(message);

        // Redis未读数+1
        notificationCacheService.incrementUnread(message.getReceiverId());

        // 查询发送者昵称
        User sndUser = userMapper.selectById(senderId);
        String senderName = sndUser != null
                ? (sndUser.getNickname() != null ? sndUser.getNickname() : sndUser.getUsername())
                : null;

        // SSE推送（SseEmitterManager 内部有 try-catch，不影响事务）
        Map<String, Object> notification = new HashMap<>();
        notification.put("senderId", senderId);
        notification.put("content", message.getContent());
        notification.put("jobId", message.getJobId());
        notification.put("type", message.getType());
        if (senderName != null) {
            notification.put("senderName", senderName);
        }
        sseEmitterManager.sendToUser(message.getReceiverId(), "message", notification);

        log.info("消息发送成功: id={}, senderId={}, receiverId={}", message.getId(), senderId, message.getReceiverId());
        return message;
    }

    /**
     * 获取会话列表
     * 聚合每个会话的最后一条消息和未读数
     */
    @Override
    public List<ConversationVO> getConversations(Long userId) {
        List<Message> allMessages = messageMapper.selectList(
            new LambdaQueryWrapper<Message>()
                .and(w -> w.eq(Message::getSenderId, userId)
                           .or().eq(Message::getReceiverId, userId))
                .orderByDesc(Message::getCreatedAt)
        );

        if (allMessages.isEmpty()) {
            return Collections.emptyList();
        }

        // 按对方用户ID分组，取每个会话的最新消息
        Map<Long, Message> latestMessageMap = new LinkedHashMap<>();
        for (Message msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            if (!latestMessageMap.containsKey(otherUserId)) {
                latestMessageMap.put(otherUserId, msg);
            }
        }

        // 统计每个会话的未读数
        Map<Long, Long> unreadCountMap = allMessages.stream()
            .filter(msg -> msg.getReceiverId().equals(userId) && msg.getIsRead() == 0)
            .collect(Collectors.groupingBy(
                Message::getSenderId,
                Collectors.counting()
            ));

        // 批量查询用户信息
        Set<Long> otherUserIds = latestMessageMap.keySet();
        Map<Long, User> userMap = userMapper.selectBatchIds(otherUserIds).stream()
            .collect(Collectors.toMap(User::getId, u -> u));

        // 找到每个会话中最新一条携带 jobId 的消息（按 createdAt 降序已排好，取第一条有 jobId 的即可）
        Map<Long, Long> conversationJobIdMap = new LinkedHashMap<>();
        for (Message msg : allMessages) {
            Long otherUserId = msg.getSenderId().equals(userId) ? msg.getReceiverId() : msg.getSenderId();
            if (msg.getJobId() != null && !conversationJobIdMap.containsKey(otherUserId)) {
                conversationJobIdMap.put(otherUserId, msg.getJobId());
            }
        }

        // 批量查询关联职位信息
        Set<Long> jobIds = new HashSet<>(conversationJobIdMap.values());
        Map<Long, Job> jobMap = jobIds.isEmpty() ? Collections.emptyMap() :
            jobMapper.selectBatchIds(jobIds).stream()
                .filter(j -> j.getDeleted() == null || j.getDeleted() == 0)
                .collect(Collectors.toMap(Job::getId, j -> j));

        // 组装会话列表
        List<ConversationVO> conversations = new ArrayList<>();
        for (Map.Entry<Long, Message> entry : latestMessageMap.entrySet()) {
            Long otherUserId = entry.getKey();
            Message lastMsg = entry.getValue();

            ConversationVO vo = new ConversationVO();
            vo.setUserId(otherUserId);

            User otherUser = userMap.get(otherUserId);
            if (otherUser != null) {
                vo.setNickname(otherUser.getNickname() != null ? otherUser.getNickname() : otherUser.getUsername());
                vo.setAvatar(otherUser.getAvatar());
            }

            vo.setLastMessage(lastMsg.getContent());
            vo.setLastTime(lastMsg.getCreatedAt());
            vo.setUnreadCount(unreadCountMap.getOrDefault(otherUserId, 0L).intValue());

            // 填充关联职位信息（从会话中最新一条有 jobId 的消息获取）
            Long convJobId = conversationJobIdMap.get(otherUserId);
            if (convJobId != null) {
                vo.setJobId(convJobId);
                Job job = jobMap.get(convJobId);
                if (job != null) {
                    vo.setJobTitle(job.getTitle());
                }
            }

            conversations.add(vo);
        }

        return conversations;
    }

    /**
     * 获取与某用户的聊天记录（双向消息）
     */
    @Override
    public List<Message> getConversation(Long userId, Long otherUserId) {
        return messageMapper.selectList(
            new LambdaQueryWrapper<Message>()
                .and(w -> w
                    .and(w2 -> w2.eq(Message::getSenderId, userId).eq(Message::getReceiverId, otherUserId))
                    .or()
                    .and(w2 -> w2.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, userId))
                )
                .orderByAsc(Message::getCreatedAt)
        );
    }

    /**
     * 标记来自某用户的消息为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAsRead(Long receiverId, Long senderId) {
        // 先查询该sender→receiver的未读消息数
        Long unreadCount = messageMapper.selectCount(
            new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getSenderId, senderId)
                .eq(Message::getIsRead, 0)
        );

        messageMapper.update(null,
            new LambdaUpdateWrapper<Message>()
                .eq(Message::getReceiverId, receiverId)
                .eq(Message::getSenderId, senderId)
                .eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1)
        );

        // Redis未读数减去已读数量
        if (unreadCount > 0) {
            notificationCacheService.decrementUnread(receiverId, unreadCount.intValue());
        }
        log.info("消息标记已读: receiverId={}, senderId={}, count={}", receiverId, senderId, unreadCount);
    }

    /**
     * 获取未读消息总数
     */
    @Override
    public int getUnreadCount(Long userId) {
        return notificationCacheService.getUnreadCount(userId);
    }

    /**
     * 获取最新一条未读消息（用于轮询通知弹窗）
     */
    @Override
    public Map<String, Object> getLatestUnreadMessage(Long userId) {
        Message msg = messageMapper.selectOne(
            new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT 1")
        );
        if (msg == null) return null;

        Map<String, Object> result = new HashMap<>();
        result.put("id", msg.getId());
        result.put("senderId", msg.getSenderId());
        result.put("content", msg.getContent());
        result.put("type", msg.getType());
        result.put("createdAt", msg.getCreatedAt());

        // 查询发送者昵称
        User sender = userMapper.selectById(msg.getSenderId());
        if (sender != null) {
            result.put("senderName", sender.getNickname() != null ? sender.getNickname() : sender.getUsername());
        }
        return result;
    }

    /**
     * 更新会话关联的职位（修改双方最新消息的jobId）
     */
    @Override
    public void updateConversationJob(Long userId, Long otherUserId, Long jobId) {
        // 找到双方之间最新的那条消息，更新其 jobId
        Message latestMsg = messageMapper.selectOne(
            new LambdaQueryWrapper<Message>()
                .and(w -> w
                    .and(inner -> inner.eq(Message::getSenderId, userId).eq(Message::getReceiverId, otherUserId))
                    .or(inner -> inner.eq(Message::getSenderId, otherUserId).eq(Message::getReceiverId, userId))
                )
                .orderByDesc(Message::getCreatedAt)
                .last("LIMIT 1")
        );
        if (latestMsg != null) {
            messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                    .eq(Message::getId, latestMsg.getId())
                    .set(Message::getJobId, jobId)
            );
            log.info("更新会话职位: userId={}, otherUserId={}, jobId={}", userId, otherUserId, jobId);
        }
    }
}
