package com.healthassistant.module.knowledge.repository;

import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeItemRepository extends JpaRepository<KnowledgeItem, Long>,
        JpaSpecificationExecutor<KnowledgeItem> {

    Optional<KnowledgeItem> findByDocId(String docId);
    
    Optional<KnowledgeItem> findBySourceUrl(String sourceUrl);

    List<KnowledgeItem> findByStatusOrderByCreatedAtAsc(Integer status);

    Page<KnowledgeItem> findByStatus(Integer status, Pageable pageable);

    @Query("SELECT k FROM KnowledgeItem k WHERE k.documentType = :documentType")
    List<KnowledgeItem> findByDocumentType(String documentType);

    List<KnowledgeItem> findBySourceName(String sourceName);

    @Query("SELECT k.sourceUrl FROM KnowledgeItem k WHERE k.sourceUrl IN :urls")
    List<String> findExistingSourceUrls(@Param("urls") List<String> urls);

    long countByStatus(Integer status);

    void deleteByDocId(String docId);

    @Query("SELECT k.sourceName, COUNT(k) FROM KnowledgeItem k GROUP BY k.sourceName")
    List<Object[]> countGroupBySourceName();
}
