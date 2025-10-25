package com.example.demo.order;

public enum OrderProgressStage {
    /**
     * 买家已支付定金或完成预定。
     */
    DEPOSIT_PAID,
    /**
     * 卖家已确认具体的看房时间。
     */
    VIEWING_SCHEDULED,
    /**
     * 买卖双方已经提交看房反馈。
     */
    FEEDBACK_SUBMITTED,
    /**
     * 房屋交接完成。
     */
    HANDOVER_COMPLETED,
    /**
     * 管理员审核通过并完成款项发放。
     */
    FUNDS_RELEASED
}
