package com.healthassistant.module.admin.repository;

import com.healthassistant.module.admin.entity.SensitiveWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SensitiveWordRepository extends JpaRepository<SensitiveWord, Long> {

    Optional<SensitiveWord> findByWord(String word);

    boolean existsByWord(String word);
}
