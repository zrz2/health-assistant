package com.healthassistant.module.knowledge.repository;

import com.healthassistant.module.knowledge.entity.KnowledgeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeSourceRepository extends JpaRepository<KnowledgeSource, Long> {

    Optional<KnowledgeSource> findByName(String name);

    List<KnowledgeSource> findBySourceType(String sourceType);

    List<KnowledgeSource> findByStatus(Integer status);

    List<KnowledgeSource> findByStatusAndSourceType(Integer status, String sourceType);
}
