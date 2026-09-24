package com.xgs.water.constant;

/**
 * 抽检模块状态与编码常量
 */
public final class WqConstants {

    private WqConstants() {}

    // 批次状态
    public static final String BATCH_DRAFT = "DRAFT";
    public static final String BATCH_PENDING_REVIEW = "PENDING_REVIEW";
    public static final String BATCH_RETURNED = "RETURNED";
    public static final String BATCH_PENDING_RETEST = "PENDING_RETEST";
    public static final String BATCH_CLOSED = "CLOSED";

    // 单项判定 / 复检结论
    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_FAIL = "FAIL";

    // 不合格项状态
    public static final String FAIL_OPEN = "OPEN";
    public static final String FAIL_CLOSED_PASS = "CLOSED_PASS";
    public static final String FAIL_CLOSED_VOID = "CLOSED_VOID";

    // 照片引用类型
    public static final String PHOTO_SAMPLE = "SAMPLE";
    public static final String PHOTO_RETEST = "RETEST";

    // 指标编码
    public static final String METRIC_RESIDUAL_CHLORINE = "residual_chlorine";
    public static final String METRIC_TURBIDITY = "turbidity";
    public static final String METRIC_TEMPERATURE = "temperature";
    public static final String METRIC_ODOR = "odor";

    // 阈值判定方式
    public static final String JUDGE_RANGE = "range";
    public static final String JUDGE_ENUM = "enum";

    // 流水动作
    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_SUBMIT = "SUBMIT";
    public static final String ACTION_RETURN = "RETURN";
    public static final String ACTION_REVIEW_RETEST = "REVIEW_RETEST";
    public static final String ACTION_RETEST_RESULT = "RETEST_RESULT";
    public static final String ACTION_CLOSE = "CLOSE";
}
