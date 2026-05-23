package com.healthassistant.module.admin.service;

import com.healthassistant.module.admin.dto.DailyTrendResponse;
import com.healthassistant.module.admin.dto.DashboardStatsResponse;
import com.healthassistant.module.admin.dto.SourceDistributionResponse;
import com.healthassistant.module.chat.repository.ChatMessageRepository;
import com.healthassistant.module.chat.repository.ChatSessionRepository;
import com.healthassistant.module.knowledge.repository.KnowledgeItemRepository;
import com.healthassistant.module.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final KnowledgeItemRepository knowledgeItemRepository;

    public AdminDashboardService(UserRepository userRepository,
                                  ChatSessionRepository sessionRepository,
                                  ChatMessageRepository messageRepository,
                                  KnowledgeItemRepository knowledgeItemRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.knowledgeItemRepository = knowledgeItemRepository;
    }

    public DashboardStatsResponse getStats() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long positiveCount = messageRepository.countByFeedbackType(1);

        return DashboardStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalSessions(sessionRepository.count())
                .totalMessages(messageRepository.count())
                .totalKnowledgeItems(knowledgeItemRepository.count())
                .todayActiveSessions(sessionRepository.countByUpdatedAtAfter(todayStart))
                .indexedItems(knowledgeItemRepository.countByStatus(3))
                .positiveRate(positiveCount > 0 ?
                        (double) positiveCount / messageRepository.count() : 0.0)
                .build();
    }

    public DailyTrendResponse getTrends() {
        List<DailyTrendResponse.TrendPoint> newUsers = new ArrayList<>();
        List<DailyTrendResponse.TrendPoint> messages = new ArrayList<>();
        List<DailyTrendResponse.TrendPoint> positiveRates = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.atTime(LocalTime.MAX);

            long userCount = userRepository.countByCreatedAtBetween(dayStart, dayEnd);
            newUsers.add(DailyTrendResponse.TrendPoint.builder()
                    .date(date.toString())
                    .value(userCount)
                    .build());

            long msgCount = messageRepository.countByCreatedAtBetween(dayStart, dayEnd);
            messages.add(DailyTrendResponse.TrendPoint.builder()
                    .date(date.toString())
                    .value(msgCount)
                    .build());
        }

        return DailyTrendResponse.builder()
                .newUsers(newUsers)
                .messages(messages)
                .positiveRates(positiveRates)
                .build();
    }

    public SourceDistributionResponse getSourceDistribution() {
        List<Object[]> results = knowledgeItemRepository.countGroupBySourceName();
        List<SourceDistributionResponse.SourceCount> sources = new ArrayList<>();
        for (Object[] row : results) {
            sources.add(SourceDistributionResponse.SourceCount.builder()
                    .sourceName((String) row[0])
                    .count((Long) row[1])
                    .build());
        }
        return SourceDistributionResponse.builder().sources(sources).build();
    }
}
