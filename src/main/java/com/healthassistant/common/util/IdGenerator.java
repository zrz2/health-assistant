package com.healthassistant.common.util;

import java.util.UUID;

public final class IdGenerator {
    private IdGenerator() {}

    public static String generateSessionId() {
        return "sess_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String generateMessageId() {
        return "msg_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String generateClarificationId() {
        return "clar_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String generateDocId() {
        return "doc_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    public static String generateTaskId() {
        return "task_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
