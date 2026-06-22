package com.interview.controller;

import com.interview.common.Result;
import com.interview.domain.po.Message;
import com.interview.domain.vo.ConversationVO;
import com.interview.service.ChatService;
import com.interview.service.MessageService;
import com.interview.util.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 消息控制器
 */
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final ChatService chatService;

    /**
     * 发送消息
     */
    @PostMapping
    public Result<?> sendMessage(@RequestBody Map<String, Object> request) {
        Long senderId = UserContext.getUserId();
        Message message = new Message();
        message.setReceiverId(Long.valueOf(request.get("receiverId").toString()));
        message.setContent((String) request.get("content"));
        message.setType((String) request.get("type"));

        if (request.get("jobId") != null) {
            message.setJobId(Long.valueOf(request.get("jobId").toString()));
        }

        Message sent = messageService.sendMessage(senderId, message);
        return Result.ok(sent);
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    public Result<?> getConversations() {
        Long userId = UserContext.getUserId();
        List<ConversationVO> conversations = messageService.getConversations(userId);
        return Result.ok(conversations);
    }

    /**
     * 获取与某用户的聊天记录
     */
    @GetMapping("/conversation/{userId}")
    public Result<?> getConversation(@PathVariable Long userId) {
        Long currentUserId = UserContext.getUserId();
        List<Message> messages = messageService.getConversation(currentUserId, userId);
        return Result.ok(messages);
    }

    /**
     * 标记来自某用户的消息为已读

     */
    @PutMapping("/read/{senderId}")
    public Result<?> markAsRead(@PathVariable Long senderId) {
        Long receiverId = UserContext.getUserId();
        messageService.markAsRead(receiverId, senderId);
        return Result.ok("已标记为已读");
    }

    /**
     * 获取未读消息总数
     */
    @GetMapping("/unread-count")
    public Result<?> getUnreadCount() {
        Long userId = UserContext.getUserId();
        int count = messageService.getUnreadCount(userId);
        return Result.ok(count);
    }

    /**
     * 获取最新一条未读消息（轮询通知用）
     * 返回 {senderId, senderName, content, type, createdAt}
     * 无未读消息时返回 null
     */
    @GetMapping("/latest-unread")
    public Result<?> getLatestUnreadMessage() {
        Long userId = UserContext.getUserId();
        Map<String, Object> msg = messageService.getLatestUnreadMessage(userId);
        return Result.ok(msg);
    }

    /**
     * 清理 MongoDB 聊天重复消息（临时接口，清理后可删除）
     */
    @PostMapping("/cleanup-duplicates")
    public Result<?> cleanupDuplicateMessages() {
        int deleted = chatService.cleanupDuplicateMessages();
        return Result.ok("清理完成，删除" + deleted + "条重复消息");
    }

    /**
     * 更新会话关联的职位
     */
    @PutMapping("/conversation/job")
    public Result<?> updateConversationJob(@RequestBody Map<String, Object> request) {
        Long userId = UserContext.getUserId();
        Long otherUserId = Long.valueOf(request.get("otherUserId").toString());
        Long jobId = Long.valueOf(request.get("jobId").toString());
        messageService.updateConversationJob(userId, otherUserId, jobId);
        return Result.ok("职位更新成功");
    }
}
