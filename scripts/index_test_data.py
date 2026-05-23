"""Index test medical knowledge into ES with DashScope embeddings."""
import json
import os
import urllib.request
import uuid

DASHSCOPE_API_KEY = os.environ.get("DASHSCOPE_API_KEY", "")
EMBED_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/embeddings"
ES_URL = "http://localhost:9200"

# Test medical documents
documents = [
    {
        "doc_id": "doc_dm_001",
        "parent_doc_id": "doc_dm_001",
        "title": "2型糖尿病管理指南",
        "content": "2型糖尿病的血糖控制目标：空腹血糖应控制在4.4-7.0mmol/L，餐后2小时血糖应控制在10.0mmol/L以下。糖化血红蛋白（HbA1c）应控制在7.0%以下。二甲双胍是2型糖尿病的一线治疗药物，推荐起始剂量为500mg每日两次。如果血糖控制不达标，可联合使用磺脲类药物或胰岛素治疗。饮食控制方面，建议每日碳水化合物摄入量占总热量的45-60%，脂肪占25-35%，蛋白质占15-20%。",
        "section_path": "糖尿病 > 2型糖尿病 > 治疗指南 > 血糖控制",
        "heading_level": 3,
        "document_type": "临床指南",
        "evidence_level": 5,
        "publication_date": "2024-03",
        "source_name": "中国2型糖尿病防治指南2024版",
        "medical_entities": ["2型糖尿病", "二甲双胍", "血糖", "HbA1c", "胰岛素", "磺脲类"]
    },
    {
        "doc_id": "doc_dm_002",
        "parent_doc_id": "doc_dm_001",
        "title": "糖尿病饮食管理",
        "content": "糖尿病患者饮食管理原则：少食多餐，每日3餐加2-3次加餐。选择低血糖生成指数（GI）的食物，如全麦面包、燕麦、豆类等。每日食盐摄入量不超过6克。严格限制含糖饮料和高糖食品。推荐每日摄入膳食纤维25-30克。戒烟限酒，男性每日酒精摄入量不超过25克，女性不超过15克。定期监测血糖，根据血糖水平调整饮食方案。",
        "section_path": "糖尿病 > 2型糖尿病 > 饮食管理",
        "heading_level": 2,
        "document_type": "临床指南",
        "evidence_level": 5,
        "publication_date": "2024-03",
        "source_name": "中国2型糖尿病防治指南2024版",
        "medical_entities": ["糖尿病", "血糖生成指数", "膳食纤维", "血糖监测"]
    },
    {
        "doc_id": "doc_ht_001",
        "parent_doc_id": "doc_ht_001",
        "title": "高血压治疗建议",
        "content": "高血压诊断标准：在未使用降压药物的情况下，非同日3次测量诊室血压，收缩压≥140mmHg和/或舒张压≥90mmHg。高血压治疗目标：一般患者血压应降至140/90mmHg以下，能耐受者可进一步降至130/80mmHg以下。一线降压药物包括：血管紧张素转换酶抑制剂（ACEI）、血管紧张素受体拮抗剂（ARB）、钙通道阻滞剂（CCB）和利尿剂。生活方式干预包括：限制钠盐摄入（每日<5g）、控制体重（BMI<24kg/m2）、规律运动（每周≥150分钟中等强度）。",
        "section_path": "心血管疾病 > 高血压 > 治疗建议",
        "heading_level": 3,
        "document_type": "临床指南",
        "evidence_level": 5,
        "publication_date": "2024-01",
        "source_name": "中国高血压防治指南2024版",
        "medical_entities": ["高血压", "ACEI", "ARB", "CCB", "利尿剂", "收缩压", "舒张压", "BMI"]
    },
    {
        "doc_id": "doc_fever_001",
        "parent_doc_id": "doc_fever_001",
        "title": "发热的鉴别诊断与处理",
        "content": "发热是指体温超过37.3°C。根据体温高低分为：低热（37.3-38°C）、中度发热（38.1-39°C）、高热（39.1-41°C）、超高热（41°C以上）。成人发热的常见原因包括：上呼吸道感染、泌尿系感染、胃肠道感染等。对于体温低于38.5°C的发热，可先采用物理降温方法，如温水擦浴、冷敷额头和腋下。体温超过38.5°C时可使用退热药物，如对乙酰氨基酚（扑热息痛），成人每次325-650mg，每4-6小时一次，每日最大剂量不超过2g。布洛芬成人每次200-400mg，每6-8小时一次，每日最大剂量不超过1.2g。如果发热持续超过3天，或伴有剧烈头痛、呼吸困难、意识改变等，应立即就医。",
        "section_path": "内科 > 发热 > 鉴别诊断与处理",
        "heading_level": 3,
        "document_type": "临床指南",
        "evidence_level": 4,
        "publication_date": "2023-06",
        "source_name": "临床诊疗指南-感染性疾病分册",
        "medical_entities": ["发热", "对乙酰氨基酚", "布洛芬", "上呼吸道感染", "物理降温"]
    },
    {
        "doc_id": "doc_cold_001",
        "parent_doc_id": "doc_cold_001",
        "title": "普通感冒与流感的区别及治疗",
        "content": "普通感冒主要由鼻病毒引起，症状包括：鼻塞、流涕、打喷嚏、咽痛、咳嗽等，全身症状较轻，一般不发热或仅有低热。流感由流感病毒引起，起病急，全身症状重，表现为高热（39-40°C）、头痛、全身肌肉酸痛、乏力等。普通感冒治疗以对症为主：鼻塞可用伪麻黄碱，流涕可用抗组胺药如氯苯那敏，咳嗽可用右美沙芬。流感应在发病48小时内使用抗病毒药物如奥司他韦，成人每次75mg，每日2次，疗程5天。预防措施：勤洗手、戴口罩、保持社交距离、每年接种流感疫苗。",
        "section_path": "内科 > 呼吸系统 > 感冒与流感",
        "heading_level": 2,
        "document_type": "医学教科书",
        "evidence_level": 4,
        "publication_date": "2024-02",
        "source_name": "实用内科学第16版",
        "medical_entities": ["感冒", "流感", "鼻病毒", "伪麻黄碱", "氯苯那敏", "右美沙芬", "奥司他韦", "流感疫苗"]
    },
]


def get_embedding(text):
    """Get embedding from DashScope text-embedding-v3."""
    data = json.dumps({
        "model": "text-embedding-v3",
        "input": text
    }).encode()

    req = urllib.request.Request(EMBED_URL, data=data, method="POST")
    req.add_header("Authorization", f"Bearer {DASHSCOPE_API_KEY}")
    req.add_header("Content-Type", "application/json")

    with urllib.request.urlopen(req) as resp:
        result = json.loads(resp.read().decode())
        return result["data"][0]["embedding"]


def index_documents():
    """Index all documents into ES with embeddings."""
    for doc in documents:
        print(f"Embedding: {doc['title']}...")
        embedding = get_embedding(doc["content"])
        print(f"  Embedding dims: {len(embedding)}")

        es_doc = {
            "content": doc["content"],
            "content_vector": embedding,
            "section_path": doc["section_path"],
            "heading_level": doc["heading_level"],
            "parent_doc_id": doc["parent_doc_id"],
            "document_type": doc["document_type"],
            "evidence_level": doc["evidence_level"],
            "publication_date": doc["publication_date"],
            "source_name": doc["source_name"],
            "medical_entities": doc["medical_entities"],
        }

        url = f"{ES_URL}/health_knowledge/_doc/{doc['doc_id']}"
        data = json.dumps(es_doc).encode()
        req = urllib.request.Request(url, data=data, method="PUT")
        req.add_header("Content-Type", "application/json")

        try:
            with urllib.request.urlopen(req) as resp:
                result = json.loads(resp.read().decode())
                print(f"  Indexed: {result.get('result')}")
        except Exception as e:
            print(f"  Error: {e}")

    # Refresh index
    urllib.request.urlopen(urllib.request.Request(
        f"{ES_URL}/health_knowledge/_refresh", method="POST"))
    print("Index refreshed.")


if __name__ == "__main__":
    index_documents()
