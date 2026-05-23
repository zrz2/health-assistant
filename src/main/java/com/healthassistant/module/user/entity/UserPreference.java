package com.healthassistant.module.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "user_preference")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private Integer notificationEnabled = 1;

    @Column(length = 20)
    private String theme = "light";

    @Column(length = 10)
    private String language = "zh-CN";

    @Column(nullable = false)
    private Integer privacyLevel = 1;
}
