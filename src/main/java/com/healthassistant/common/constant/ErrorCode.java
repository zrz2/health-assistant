package com.healthassistant.common.constant;

public final class ErrorCode {
    private ErrorCode() {}

    // 用户模块 (1xxx)
    public static final int USER_NOT_FOUND = 1001;
    public static final int PASSWORD_ERROR = 1002;
    public static final int USERNAME_EXISTS = 1003;
    public static final int EMAIL_EXISTS = 1004;
    public static final int TOKEN_EXPIRED = 1005;
    public static final int TOKEN_INVALID = 1006;
    public static final int USER_DISABLED = 1007;

    // 对话模块 (2xxx)
    public static final int SESSION_NOT_FOUND = 2001;
    public static final int MESSAGE_NOT_FOUND = 2002;
    public static final int CLARIFICATION_NOT_FOUND = 2003;
    public static final int STREAM_ERROR = 2004;
    public static final int LLM_CALL_FAILED = 2005;

    // 知识库模块 (3xxx)
    public static final int KNOWLEDGE_NOT_FOUND = 3001;
    public static final int IMPORT_FAILED = 3002;
    public static final int SYNC_FAILED = 3003;
    public static final int EMBEDDING_FAILED = 3004;

    // 管理模块 (4xxx)
    public static final int CONFIG_NOT_FOUND = 4001;
    public static final int SENSITIVE_WORD_EXISTS = 4002;
    public static final int CONTENT_REJECTED = 4003;
    public static final int ADMIN_NOT_FOUND = 4004;
    public static final int CANNOT_DISABLE_SELF = 4005;
    public static final int CANNOT_DELETE_SELF = 4006;
    public static final int CANNOT_CHANGE_OWN_ROLE = 4007;

    // 通用 HTTP 响应码
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;

    // 系统 (9xxx)
    public static final int RATE_LIMITED = 9001;
    public static final int INTERNAL_ERROR = 9002;
}
