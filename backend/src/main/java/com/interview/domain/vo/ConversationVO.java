package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话列表VO
 */
@Data
public class ConversationVO {

    /** 对方用户ID */
    private Long userId;
    private String nickname;
    private String avatar;
    /** 最后一条消息内容 */
    private String lastMessage;
    /** 最后一条消息时间 */
    private LocalDateTime lastTime;
    /** 未读消息数 */
    private Integer unreadCount;
    /** 关联职位ID */
    private Long jobId;
    /** 关联职位标题 */
    private String jobTitle;
}
