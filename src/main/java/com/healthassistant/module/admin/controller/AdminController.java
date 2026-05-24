package com.healthassistant.module.admin.controller;

import com.healthassistant.common.result.Result;
import com.healthassistant.module.admin.dto.*;
import com.healthassistant.module.admin.entity.SensitiveWord;
import com.healthassistant.module.admin.service.*;
import com.healthassistant.module.knowledge.entity.KnowledgeItem;
import com.healthassistant.module.knowledge.entity.SyncTask;
import com.healthassistant.module.knowledge.scraper.ScraperService;
import com.healthassistant.module.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final AdminUserService userService;
    private final AdminKnowledgeService knowledgeService;
    private final SensitiveWordService sensitiveWordService;

    public AdminController(AdminDashboardService dashboardService,
                           AdminUserService userService,
                           AdminKnowledgeService knowledgeService,
                           SensitiveWordService sensitiveWordService) {
        this.dashboardService = dashboardService;
        this.userService = userService;
        this.knowledgeService = knowledgeService;
        this.sensitiveWordService = sensitiveWordService;
    }

    // ==================== Dashboard ====================

    @GetMapping("/dashboard/stats")
    public Result<DashboardStatsResponse> getStats() {
        return Result.success(dashboardService.getStats());
    }

    @GetMapping("/dashboard/trends")
    public Result<DailyTrendResponse> getTrends() {
        return Result.success(dashboardService.getTrends());
    }

    @GetMapping("/dashboard/source-distribution")
    public Result<SourceDistributionResponse> getSourceDistribution() {
        return Result.success(dashboardService.getSourceDistribution());
    }

    @GetMapping("/dashboard/recent-queries")
    public Result<List<Map<String, Object>>> getRecentQueries() {
        return Result.success(dashboardService.getRecentQueries(20));
    }

    // ==================== User Management ====================

    @GetMapping("/users")
    public Result<Page<User>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer userType,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20) Pageable pageable) {
        return Result.success(userService.listUsers(keyword, userType, status, pageable));
    }

    @GetMapping("/users/{id}")
    public Result<UserDetailResponse> getUserDetail(@PathVariable Long id) {
        return Result.success(userService.getUserDetail(id));
    }

    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        Long currentUserId = getCurrentUserId();
        userService.updateStatus(id, status, currentUserId);
        return Result.success();
    }

    @PutMapping("/users/{id}/role")
    public Result<Void> updateUserRole(@PathVariable Long id, @RequestParam Integer userType) {
        Long currentUserId = getCurrentUserId();
        userService.updateRole(id, userType, currentUserId);
        return Result.success();
    }

    @DeleteMapping("/users/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        userService.deleteUser(id, currentUserId);
        return Result.success();
    }

    // ==================== Knowledge Management ====================

    @GetMapping("/knowledge/items")
    public Result<Page<KnowledgeItem>> listKnowledgeItems(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sourceName,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) Integer status,
            @PageableDefault(size = 20) Pageable pageable) {
        return Result.success(knowledgeService.listItems(keyword, sourceName, documentType, status, pageable));
    }

    @GetMapping("/knowledge/items/{docId}")
    public Result<KnowledgeItem> getKnowledgeItem(@PathVariable String docId) {
        return Result.success(knowledgeService.getItem(docId));
    }

    @PostMapping("/knowledge/items")
    public Result<KnowledgeItem> createKnowledgeItem(@Valid @RequestBody KnowledgeImportRequest request) {
        return Result.success(knowledgeService.importSingle(request));
    }

    @DeleteMapping("/knowledge/items/{docId}")
    public Result<Void> deleteKnowledgeItem(@PathVariable String docId) {
        knowledgeService.deleteItem(docId);
        return Result.success();
    }

    @PostMapping("/knowledge/items/batch-delete")
    public Result<Void> batchDeleteKnowledgeItems(@RequestBody List<String> docIds) {
        knowledgeService.batchDelete(docIds);
        return Result.success();
    }

    @PostMapping("/knowledge/import/single")
    public Result<KnowledgeItem> importSingle(@Valid @RequestBody KnowledgeImportRequest request) {
        return Result.success(knowledgeService.importSingle(request));
    }

    @PostMapping("/knowledge/import/batch")
    public Result<Map<String, String>> importBatch(
            @Valid @RequestBody KnowledgeImportRequest.BatchImportRequest request) {
        knowledgeService.importBatch(request);
        return Result.success(Map.of("status", "accepted", "total", String.valueOf(request.getArticles().size())));
    }

    @PostMapping("/knowledge/scrape")
    public Result<ScraperService.ScrapeReport> triggerScrape() {
        return Result.success(knowledgeService.triggerScrape());
    }

    @PostMapping("/knowledge/scrape/{sourceName}")
    public Result<ScraperService.SourceReport> triggerScrapeSource(@PathVariable String sourceName) {
        return Result.success(knowledgeService.triggerScrapeSource(sourceName));
    }

    @PostMapping("/knowledge/reindex")
    public Result<Map<String, Integer>> reindexAll() {
        int count = knowledgeService.reindexAll();
        return Result.success(Map.of("reindexed", count));
    }

    @PostMapping("/knowledge/reindex/{docId}")
    public Result<Void> reindexItem(@PathVariable String docId) {
        knowledgeService.reindexItem(docId);
        return Result.success();
    }

    @GetMapping("/knowledge/import-history")
    public Result<Page<SyncTask>> getImportHistory(@PageableDefault(size = 20) Pageable pageable) {
        return Result.success(knowledgeService.getImportHistory(pageable));
    }

    @GetMapping("/knowledge/scrape-report")
    public Result<ScraperService.ScrapeReport> getScrapeReport() {
        return Result.success(knowledgeService.getScrapeReport());
    }

    // ==================== Sensitive Words ====================

    @GetMapping("/sensitive-words")
    public Result<List<SensitiveWord>> listSensitiveWords() {
        return Result.success(sensitiveWordService.listAll());
    }

    @PostMapping("/sensitive-words")
    public Result<SensitiveWord> addSensitiveWord(@Valid @RequestBody SensitiveWordRequest request) {
        sensitiveWordService.addWord(request.getWord());
        return Result.success();
    }

    @DeleteMapping("/sensitive-words/{id}")
    public Result<Void> deleteSensitiveWord(@PathVariable Long id) {
        sensitiveWordService.deleteWord(id);
        return Result.success();
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    public Result<Map<String, String>> healthCheck() {
        return Result.success(Map.of(
                "status", "UP",
                "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }

    private Long getCurrentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return null;
    }
}
