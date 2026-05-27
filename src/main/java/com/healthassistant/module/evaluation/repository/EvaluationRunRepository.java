package com.healthassistant.module.evaluation.repository;

import com.healthassistant.module.evaluation.entity.EvaluationRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EvaluationRunRepository extends JpaRepository<EvaluationRun, Long> {

    Optional<EvaluationRun> findByRunId(String runId);

    Page<EvaluationRun> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
