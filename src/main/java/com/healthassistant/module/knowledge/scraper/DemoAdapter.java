package com.healthassistant.module.knowledge.scraper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Demo adapter that provides sample health articles for testing the scraper pipeline.
 * Activated with "scraper-demo" profile for offline testing.
 *
 * Usage:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=dev,scraper-demo
 */
@Component
@Profile("scraper-demo")
public class DemoAdapter implements SourceAdapter {

    private static final Logger log = LoggerFactory.getLogger(DemoAdapter.class);

    private static final String[][] SAMPLE_ARTICLES = {
            {
                "世界卫生组织 | 糖尿病关键事实",
                "糖尿病是一种慢性代谢性疾病，其特征是血糖水平升高。根据WHO数据，全球约有4.22亿人患有糖尿病。"
                        + "糖尿病主要分为1型糖尿病、2型糖尿病和妊娠期糖尿病。其中2型糖尿病占所有糖尿病的90%以上。"
                        + "糖尿病的常见症状包括多饮、多尿、体重下降、疲劳和视力模糊。"
                        + "如果不加以控制，糖尿病可导致严重并发症，包括心脏病、中风、肾衰竭、失明和下肢截肢。"
                        + "预防措施包括保持健康体重、定期体育锻炼、健康饮食和避免烟草使用。"
                        + "治疗包括口服降糖药物和胰岛素注射，同时需要定期监测血糖水平。"
                        + "早期诊断和良好的血糖控制可以显著减少并发症风险。",
                    "https://www.who.int/news-room/fact-sheets/detail/diabetes",
                    "2024-04-15", "WHO", "clinical_guideline"
            },
            {
                "世界卫生组织 | 高血压",
                "高血压是全球范围内导致过早死亡的主要危险因素之一。全球约有12.8亿成年人患有高血压。"
                        + "高血压的定义为收缩压≥140mmHg和/或舒张压≥90mmHg。"
                        + "高血压通常没有明显症状，被称为沉默的杀手。未经控制的高血压可导致心脏病、中风和肾衰竭。"
                        + "减少盐摄入量、保持健康体重、定期体育锻炼、限制酒精摄入和戒烟是控制高血压的关键生活方式措施。"
                        + "常用的降压药物包括钙通道阻滞剂、ACE抑制剂、ARB、β受体阻滞剂和利尿剂。"
                        + "治疗目标一般为血压<140/90mmHg，合并糖尿病或肾病时目标更严格为<130/80mmHg。",
                    "https://www.who.int/news-room/fact-sheets/detail/hypertension",
                    "2024-06-20", "WHO", "clinical_guideline"
            },
            {
                "中国疾控中心 | 慢性病防控核心信息",
                "心脑血管疾病、癌症、慢性呼吸系统疾病和糖尿病是我国主要的慢性非传染性疾病。"
                        + "我国慢性病导致的死亡占总死亡的88.5%，导致的疾病负担占总疾病负担的70%以上。"
                        + "高血压患病率呈上升趋势，18岁及以上居民高血压患病率为27.5%。"
                        + "糖尿病患病率为11.9%，且呈年轻化趋势。"
                        + "防控策略包括：健康生活方式推广、早筛早诊早治、规范管理和康复服务。"
                        + "推荐成人每年测量一次血压，40岁以上人群每年检测一次空腹血糖。"
                        + "慢性病患者应遵医嘱规律服药，定期复查，建立健康生活方式。",
                    "https://www.chinacdc.cn/jkzt/mxfcrjbhjcfkz/",
                    "2024-03-01", "中国疾控中心", "clinical_guideline"
            },
            {
                "中国疾控中心 | 全民健康生活方式行动",
                "健康生活方式是预防慢性病最经济有效的手段。核心内容包括合理膳食、适量运动、戒烟限酒和心理平衡。"
                        + "合理膳食：食物多样，谷类为主；多吃蔬菜水果和薯类；每天吃奶类大豆或其制品；适量鱼禽蛋瘦肉；少盐少油。"
                        + "适量运动：成人每周至少150分钟中等强度有氧运动或75分钟高强度有氧运动。"
                        + "健康体重：保持BMI在18.5-24kg/m2之间。腰围控制：男性<90cm，女性<85cm。"
                        + "戒烟限酒：任何形式的烟草使用都有害健康。建议成年男性每日酒精摄入量不超过25g，女性不超过15g。"
                        + "心理平衡：保持积极乐观的心态，学会压力管理，保持良好睡眠。",
                    "https://www.chinacdc.cn/jkzt/jjkz/",
                    "2024-01-10", "中国疾控中心", "clinical_guideline"
            },
            {
                "丁香医生 | 如何正确测量血压",
                "家庭自测血压是高血压管理的重要手段。正确的测量方法可以确保血压读数的准确性。"
                        + "测量前30分钟内不要吸烟、喝咖啡或茶，不要进行剧烈运动。"
                        + "测量前应静坐休息5分钟，保持放松。坐在有靠背的椅子上，双脚平放地面。"
                        + "选择合适大小的袖带，袖带下缘位于肘窝上2-3厘米处。"
                        + "测量时保持手臂与心脏齐平，手掌向上。"
                        + "建议在早晨服药前和晚上睡前各测量一次，每次测量2-3遍，取平均值。"
                        + "记录每次的测量结果，就诊时带给医生参考。家庭自测血压的诊断标准为≥135/85mmHg。",
                    "https://dxy.com/article/7149",
                    "2024-05-20", "丁香医生", "health_encyclopedia"
            },
            {
                "丁香医生 | 二甲双胍用药指南",
                "二甲双胍是2型糖尿病的一线治疗药物，临床应用已有60余年历史。"
                        + "其主要作用机制包括抑制肝脏糖异生、改善外周组织胰岛素敏感性、延缓肠道葡萄糖吸收。"
                        + "推荐起始剂量为500mg每日1-2次，随餐服用以减少胃肠道反应。可根据血糖控制情况逐渐增加剂量。"
                        + "标准剂量为1000mg每日两次，最大推荐剂量为2550mg每日。"
                        + "常见不良反应包括恶心、腹泻、食欲不振等胃肠道反应，通常随用药时间延长而减轻。"
                        + "肾功能不全患者（eGFR<30ml/min/1.73m2）禁用。肝功能严重受损患者慎用。"
                        + "长期使用可能导致维生素B12缺乏，建议定期监测。",
                    "https://dxy.com/article/8023",
                    "2024-02-15", "丁香医生", "drug_manual"
            },
    };

    @Override
    public String getSourceName() {
        return "DemoDataSource";
    }

    @Override
    public String getDocumentType() {
        return "health_encyclopedia";
    }

    @Override
    public int getDefaultEvidenceLevel() {
        return 3;
    }

    @Override
    public List<String> discoverUrls() {
        List<String> urls = new ArrayList<>();
        for (String[] article : SAMPLE_ARTICLES) {
            urls.add(article[2]); // URL is at index 2
        }
        log.info("Demo: providing {} sample article URLs", urls.size());
        return urls;
    }

    /**
     * Returns pre-built metadata from the sample data, no HTML parsing needed.
     */
    @Override
    public ParsedMetadata parseMetadata(ContentCleaner.CleanResult cleaned, String html) {
        for (String[] article : SAMPLE_ARTICLES) {
            if (article[2].equals(cleaned.url())) {
                return new ParsedMetadata(
                        article[0],      // title
                        article[3],      // publication date
                        article[5],      // document type
                        article[4],      // source name
                        article[2],      // source URL
                        switch (article[4]) {
                            case "WHO" -> 5;
                            case "中国疾控中心" -> 4;
                            case "丁香医生" -> 2;
                            default -> 3;
                        }
                );
            }
        }
        return new ParsedMetadata(cleaned.title(), cleaned.publishDate(),
                "health_encyclopedia", "DemoDataSource", cleaned.url(), 3);
    }

    @Override
    public int maxArticlesPerRun() {
        return SAMPLE_ARTICLES.length;
    }
}
