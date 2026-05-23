package com.healthassistant.module.knowledge.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class MedicalEntityExtractor {

    private static final Logger log = LoggerFactory.getLogger(MedicalEntityExtractor.class);

    private final Set<String> medicalTerms = new LinkedHashSet<>();

    // Regex patterns for common medical entities in Chinese
    private static final Pattern DISEASE_PATTERN = Pattern.compile(
            "[\\u4e00-\\u9fff]{0,4}(病|症|炎|癌|瘤|感染|损伤|坏死|衰竭|梗死|出血|水肿|中毒|过敏|休克|昏迷)");
    private static final Pattern DRUG_PATTERN = Pattern.compile(
            "[\\u4e00-\\u9fff]{1,6}(素|霉素|西林|唑|汀|普利|沙坦|洛尔|地平|他汀|贝特|双胍|列净|格列|波糖|胰岛素)");
    private static final Pattern EXAM_PATTERN = Pattern.compile(
            "[\\u4e00-\\u9fff]{2,6}(检查|检测|试验|测定|扫描|影像|超声|CT|MRI|X线|心电图|B超)");

    @PostConstruct
    public void init() {
        // Core medical terms dictionary
        addTerms(
                // Diabetes & metabolism
                "糖尿病", "2型糖尿病", "1型糖尿病", "妊娠糖尿病", "高血糖", "低血糖", "糖化血红蛋白", "HbA1c",
                "胰岛素", "胰岛素抵抗", "二甲双胍", "磺脲类", "格列奈类", "阿卡波糖",
                "糖尿病酮症酸中毒", "糖尿病肾病", "糖尿病视网膜病变", "糖尿病足",
                "空腹血糖", "餐后血糖", "OGTT",

                // Cardiovascular
                "高血压", "冠心病", "心肌梗死", "心绞痛", "心力衰竭", "心律失常",
                "ACE抑制剂", "ARB", "钙通道阻滞剂", "β受体阻滞剂", "利尿剂",
                "他汀", "阿司匹林", "氯吡格雷", "华法林",

                // Respiratory
                "肺炎", "慢性阻塞性肺疾病", "COPD", "哮喘", "支气管炎", "肺结核",
                "上呼吸道感染", "流感",

                // Digestive
                "胃炎", "胃溃疡", "十二指肠溃疡", "肝硬化", "脂肪肝", "胰腺炎",
                "炎症性肠病", "克罗恩病", "溃疡性结肠炎",

                // Nervous system
                "脑卒中", "脑梗死", "脑出血", "帕金森病", "阿尔茨海默病", "癫痫",
                "偏头痛", "多发性硬化",

                // Cancer
                "肺癌", "肝癌", "胃癌", "乳腺癌", "结直肠癌", "前列腺癌", "白血病", "淋巴瘤",

                // Others
                "贫血", "甲状腺功能亢进", "甲亢", "甲状腺功能减退", "甲减",
                "骨质疏松", "关节炎", "痛风", "系统性红斑狼疮",

                // Common metrics
                "BMI", "体重指数", "血压", "收缩压", "舒张压", "心率", "体温",
                "白细胞", "红细胞", "血小板", "转氨酶", "肌酐", "尿酸", "胆固醇",
                "甘油三酯", "低密度脂蛋白", "高密度脂蛋白"
        );
        log.info("Medical entity extractor initialized with {} terms", medicalTerms.size());
    }

    private void addTerms(String... terms) {
        Collections.addAll(medicalTerms, terms);
    }

    public List<String> extractEntities(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> found = new ArrayList<>();
        String lower = text.toLowerCase();

        // Dictionary matching
        for (String term : medicalTerms) {
            if (lower.contains(term.toLowerCase()) && !found.contains(term)) {
                found.add(term);
            }
        }

        // Regex-based extraction
        extractByPattern(text, DISEASE_PATTERN, found);
        extractByPattern(text, DRUG_PATTERN, found);
        extractByPattern(text, EXAM_PATTERN, found);

        // Limit to 20 entities
        if (found.size() > 20) {
            return found.subList(0, 20);
        }
        return found;
    }

    private void extractByPattern(String text, Pattern pattern, List<String> results) {
        var matcher = pattern.matcher(text);
        int added = 0;
        while (matcher.find() && added < 5) {
            String entity = matcher.group();
            if (!results.contains(entity)) {
                results.add(entity);
                added++;
            }
        }
    }
}
