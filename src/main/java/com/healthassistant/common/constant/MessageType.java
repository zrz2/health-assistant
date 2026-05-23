package com.healthassistant.common.constant;

public enum MessageType {
    USER(1, "用户消息"),
    ASSISTANT(2, "助手回复"),
    SYSTEM(3, "系统消息"),
    CLARIFICATION(4, "澄清提问");

    private final int code;
    private final String description;

    MessageType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
