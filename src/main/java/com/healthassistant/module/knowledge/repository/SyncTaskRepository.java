package com.healthassistant.module.knowledge.repository;

import com.healthassistant.module.knowledge.entity.SyncTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SyncTaskRepository extends JpaRepository<SyncTask, Long> {

    Optional<SyncTask> findByTaskId(String taskId);

    List<SyncTask> findByStatusOrderByCreatedAtDesc(Integer status);

    Page<SyncTask> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<SyncTask> findBySourceName(String sourceName);
}
