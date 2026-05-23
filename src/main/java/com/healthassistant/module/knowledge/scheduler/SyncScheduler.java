package com.healthassistant.module.knowledge.scheduler;

import com.healthassistant.module.knowledge.entity.SyncTask;
import com.healthassistant.module.knowledge.repository.SyncTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class SyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncTaskRepository syncTaskRepository;

    @Value("${app.crawler.enable-scheduled:false}")
    private boolean enableScheduled;

    public SyncScheduler(SyncTaskRepository syncTaskRepository) {
        this.syncTaskRepository = syncTaskRepository;
    }

    /**
     * Clean up stale sync tasks (older than 7 days) daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupStaleTasks() {
        if (!enableScheduled) {
            log.debug("Scheduled sync is disabled, skipping task cleanup");
            return;
        }

        log.info("Running stale sync task cleanup");
        List<SyncTask> running = syncTaskRepository.findByStatusOrderByCreatedAtDesc(1); // executing
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);

        for (SyncTask task : running) {
            if (task.getStartedAt() != null && task.getStartedAt().isBefore(cutoff)) {
                task.setStatus(3); // failed
                task.setErrorLog("执行超时，自动标记为失败");
                task.setCompletedAt(LocalDateTime.now());
                syncTaskRepository.save(task);
                log.info("Marked stale sync task {} as failed", task.getTaskId());
            }
        }
    }
}
