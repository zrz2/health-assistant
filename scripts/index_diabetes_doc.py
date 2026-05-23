"""Index a comprehensive diabetes document for RAG testing."""
import json
import os
import urllib.request

DASHSCOPE_API_KEY = os.environ.get("DASHSCOPE_API_KEY", "")
EMBED_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
ES_URL = "http://localhost:9200"

diabetes_doc = {
    "一、疾病概述": "2型糖尿病（Type 2 Diabetes Mellitus, T2DM）是一种以胰岛素抵抗和胰岛B细胞功能减退为特征的慢性代谢性疾病。中国成人糖尿病患病率已达11.2%，其中2型糖尿病占90%以上。糖尿病的典型症状包括：多饮、多尿、多食、体重下降（三多一少）。诊断标准：空腹血糖≥7.0mmol/L，或OGTT 2小时血糖≥11.1mmol/L，或HbA1c≥6.5%。",

    "二、病因与发病机制": "2型糖尿病的发病机制主要包括胰岛素抵抗和胰岛素分泌不足。危险因素包括：遗传因素（一级亲属患病风险增加3-5倍）、超重与肥胖（BMI≥24kg/m2）、不健康饮食习惯（高糖高脂饮食）、缺乏体力活动、年龄≥40岁、妊娠期糖尿病病史、高血压与血脂异常。胰岛素抵抗主要发生在肝脏、肌肉和脂肪组织，导致葡萄糖摄取和利用减少。",

    "三、血糖控制目标": "根据中国2型糖尿病防治指南（2024版），一般成人2型糖尿病患者的血糖控制目标为：空腹血糖4.4-7.0mmol/L，餐后2小时血糖<10.0mmol/L，糖化血红蛋白（HbA1c）<7.0%。对于病程短、预期寿命长、无明显心脑血管疾病的患者，HbA1c可控制在<6.5%。对于高龄、病程长、有严重低血糖史或严重并发症的患者，HbA1c控制目标可适当放宽至<8.0%。",

    "四、药物治疗": "2型糖尿病的药物治疗应遵循个体化原则。一线治疗药物：二甲双胍，推荐起始剂量为500mg每日1-2次，逐渐增加至1000mg每日2次，最大剂量不超过2550mg/日。二线治疗药物包括：磺脲类（格列美脲、格列齐特等）、DPP-4抑制剂（西格列汀、沙格列汀等）、SGLT-2抑制剂（达格列净、恩格列净等）、GLP-1受体激动剂（利拉鲁肽、司美格鲁肽等）、噻唑烷二酮类（吡格列酮等）。胰岛素治疗：当口服药控制不佳时启用。基础胰岛素起始剂量为0.1-0.2U/kg/日，根据空腹血糖调整，每3-4天调整1-3U。",

    "五、医学营养治疗": "每日总热量应根据患者的体重、活动量和血糖水平个体化制定。碳水化合物应占总热量的45-60%，优先选择低血糖生成指数（GI）的食物，如全麦、燕麦、豆类、蔬菜等。蛋白质占总热量的15-20%，优质蛋白来源包括鱼、禽肉、蛋、豆制品。脂肪占总热量的25-35%，限制饱和脂肪酸（<7%总热量）和反式脂肪酸。每日食盐摄入量<6g，膳食纤维摄入量推荐25-30g/日。建议少食多餐，定时定量。严格限制含糖饮料和甜食。",

    "六、运动治疗": "运动是2型糖尿病综合治疗的重要组成部分。推荐每周进行至少150分钟中等强度有氧运动（如快走、慢跑、游泳、骑自行车），每周3-5次，每次30-60分钟。如无禁忌，建议每周进行2-3次抗阻训练。运动前应评估心血管风险。血糖<5.6mmol/L时应补充碳水化合物后再运动。血糖>16.7mmol/L时应避免剧烈运动。运动时应携带糖果以防低血糖。",

    "七、血糖监测": "自我血糖监测（SMBG）是糖尿病管理的重要组成部分。使用口服药的患者：每周监测2-4次空腹和餐后血糖。使用基础胰岛素的患者：每日监测空腹血糖。使用多次胰岛素注射的患者：每日监测三餐前后及睡前血糖。HbA1c每3-6个月检测一次。目标范围内时间（TIR）应>70%（血糖3.9-10.0mmol/L）。低于目标范围时间（TBR）应<4%（血糖<3.9mmol/L）。",

    "八、并发症预防与筛查": "糖尿病并发症包括急性并发症（糖尿病酮症酸中毒、高血糖高渗状态、低血糖症）和慢性并发症（微血管病变和大血管病变）。每年应进行以下筛查：眼底检查（糖尿病视网膜病变）、尿微量白蛋白/肌酐比值（糖尿病肾病）、足部检查（周围神经病变和血管病变）、血脂全套、肝肾功能、心电图。血压应控制在130/80mmHg以下。LDL-C应控制在<2.6mmol/L（合并心血管疾病者<1.8mmol/L）。",

    "九、特殊情况管理": "低血糖处理：血糖<3.9mmol/L即为低血糖。意识清醒者可口服15-20g葡萄糖或含糖食物，15分钟后复测血糖。意识障碍者应立即就医，静脉推注50%葡萄糖40-60ml。围手术期管理：择期手术前HbA1c应控制在<8.5%以下。手术当日停用口服降糖药，根据血糖水平调整胰岛素。妊娠期糖尿病：空腹血糖控制在<5.3mmol/L，餐后1小时<7.8mmol/L，餐后2小时<6.7mmol/L。",

    "十、健康教育": "糖尿病自我管理教育是所有糖尿病患者都应接受的基础治疗。内容包括：疾病基本知识、饮食计划制定、运动指导、药物使用方法（包括胰岛素注射技术）、血糖监测技能、低血糖识别与处理、足部护理、心理调适。建议患者每年接受一次糖尿病自我管理教育更新。戒烟限酒：男性每日酒精摄入量不超过25g，女性不超过15g。每年接种流感疫苗，按需接种肺炎球菌疫苗。"
}

def get_embedding(text):
    data = json.dumps({"model": "text-embedding-v3", "input": text}).encode()
    req = urllib.request.Request(EMBED_URL, data=data, method="POST")
    req.add_header("Authorization", f"Bearer {DASHSCOPE_API_KEY}")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode())["data"][0]["embedding"]

# Index each section as a separate document chunk
parent_doc_id = "doc_diabetes_guide_2024"
doc_id_counter = 0

for section_title, content in diabetes_doc.items():
    doc_id_counter += 1
    doc_id = f"{parent_doc_id}_chunk{doc_id_counter:02d}"
    print(f"Indexing {doc_id}: {section_title}...")

    embedding = get_embedding(content)
    print(f"  Embedding dims: {len(embedding)}")

    es_doc = {
        "content": f"{section_title}\n{content}",
        "content_vector": embedding,
        "section_path": f"糖尿病 > {section_title}",
        "heading_level": 2,
        "parent_doc_id": parent_doc_id,
        "document_type": "临床指南",
        "evidence_level": 5,
        "publication_date": "2024-03",
        "source_name": "中国2型糖尿病防治指南2024版",
        "medical_entities": ["2型糖尿病", "血糖", "HbA1c", "二甲双胍", "胰岛素", "糖尿病管理"]
    }

    url = f"{ES_URL}/health_knowledge/_doc/{doc_id}"
    data = json.dumps(es_doc).encode()
    req = urllib.request.Request(url, data=data, method="PUT")
    req.add_header("Content-Type", "application/json")
    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read().decode())
        print(f"  Indexed: {result.get('result')}")

# Refresh index
urllib.request.urlopen(urllib.request.Request(
    f"{ES_URL}/health_knowledge/_refresh", method="POST"))
print(f"\nTotal documents indexed: {doc_id_counter}")
print("Done!")
