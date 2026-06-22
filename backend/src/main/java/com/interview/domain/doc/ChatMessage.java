package com.interview.domain.doc;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * 聊天消息文档（MongoDB）
 * 存储用户之间的聊天记录，包括文字消息和简历附件
 */
@Data
@Document(collection = "chat_message")
public class ChatMessage {

    @Id
    private String id;

    /** 会话ID（由两个用户ID拼接，如 "3_5"，保证同一对用户的消息在同一个会话） */
    private String conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 接收者ID */
    private Long receiverId;

    /** 关联职位ID */
    private Long jobId;

    /** 消息类型：text-文字消息，resume-简历附件，video_call-视频面试通话 */
    private String type;

    /** 消息内容（文字内容或简历描述） */
    private String content;

    /** 简历附件ID（type=resume时有值） */
    private Long resumeId;

    /** 简历附件名称（冗余，方便展示） */
    private String resumeName;

    /** 视频通话状态（type=video_call时有值）：ringing-响铃中，accepted-已接听，rejected-已拒绝，ended-已结束 */
    private String videoCallStatus;

    /** 是否已读：false-未读，true-已读 */
    private Boolean read;

    /** 投递状态：sending-发送中，sent-已送达，failed-发送失败 */
    private String status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /**
     * 生成会话ID：较小的用户ID在前，保证同一对用户的会话ID唯一
     */
    public static String generateConversationId(Long userId1, Long userId2) {
        return userId1 < userId2 ? userId1 + "_" + userId2 : userId2 + "_" + userId1;
    }
}
