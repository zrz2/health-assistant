package com.healthassistant.module.evaluation.service;

import com.healthassistant.module.evaluation.entity.EvaluationRun;
import com.healthassistant.module.evaluation.repository.EvaluationRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.LocalDateTime;

@Service
public class EvaluationScriptRunner {

    private static final Logger log = LoggerFactory.getLogger(EvaluationScriptRunner.class);

    private final EvaluationRunRepository runRepository;

    @Value("${app.evaluation.python-path:python}")
    private String pythonPath;

    @Value("${app.evaluation.script-path:scripts/evaluate_ragas.py}")
    private String scriptPath;

    public EvaluationScriptRunner(EvaluationRunRepository runRepository) {
        this.runRepository = runRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void launch(String runId, int numQuestions) {
        try {
            EvaluationRun run = runRepository.findByRunId(runId)
                    .orElseThrow();
            run.setStatus("RUNNING");
            run.setStartedAt(LocalDateTime.now());
            runRepository.save(run);

            String apiBase = "http://localhost:8080";
            String projectDir = System.getProperty("user.dir");
            String scriptFullPath = projectDir + File.separator + scriptPath.replace("/", File.separator);

            ProcessBuilder pb = new ProcessBuilder(
                    pythonPath, scriptFullPath,
                    "--api-base", apiBase,
                    "--num-questions", String.valueOf(numQuestions),
                    "--run-id", runId
            );
            pb.directory(new File(projectDir));
            pb.inheritIO();

            log.info("Starting evaluation script: {} with runId={}", scriptFullPath, runId);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Python script exited with code " + exitCode);
            }
        } catch (Exception e) {
            log.error("Evaluation script failed for runId={}: {}", runId, e.getMessage());
            try {
                EvaluationRun run = runRepository.findByRunId(runId).orElse(null);
                if (run != null) {
                    run.setStatus("FAILED");
                    run.setErrorLog(e.getMessage());
                    run.setCompletedAt(LocalDateTime.now());
                    runRepository.save(run);
                }
            } catch (Exception ex) {
                log.error("Failed to update failed run status", ex);
            }
        }
    }
}
