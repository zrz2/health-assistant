package com.healthassistant.common.constant;

public final class Constants {
    private Constants() {}

    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 100;

    public static final String REDIS_KEY_PREFIX = "ha:";
    public static final String TOKEN_BLACKLIST_PREFIX = "ha:jwt:blacklist:";
    public static final String RATE_LIMIT_PREFIX = "ha:rate:";
    public static final String SESSION_STATE_PREFIX = "ha:session:";
    public static final String SEARCH_CACHE_PREFIX = "ha:cache:search:";

    public static final String DEFAULT_ADMIN_USERNAME = "admin";
    public static final String DEFAULT_ADMIN_PASSWORD = "admin123";

    public static final String MEDICAL_DISCLAIMER =
            "\n\n---\n*以上内容仅供参考，不构成医疗诊断或治疗建议。如有健康问题，请及时就医。*";
}
