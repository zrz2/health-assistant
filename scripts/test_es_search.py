"""Test ES search with proper UTF-8 handling."""
import json
import urllib.request

ES_URL = "http://localhost:9200"

def es_search(body):
    data = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(f"{ES_URL}/health_knowledge/_search", data=data, method="POST")
    req.add_header("Content-Type", "application/json; charset=utf-8")
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read().decode("utf-8"))

# Test 1: IK keyword search
print("=== Test 1: IK keyword search for diabetes ===")
result = es_search({
    "query": {"match": {"content": {"query": "糖尿病", "analyzer": "ik_smart"}}}
})
hits = result["hits"]
print(f"Hits: {hits['total']['value']}")
for h in hits["hits"]:
    s = h['_source']
    print(f"  - {s['section_path']} (score: {h['_score']:.2f})")

# Test 2: IK search for 'fever'
print("\n=== Test 2: IK keyword search for fever ===")
result = es_search({
    "query": {"match": {"content": {"query": "发烧了怎么办", "analyzer": "ik_smart"}}}
})
hits = result["hits"]
print(f"Hits: {hits['total']['value']}")
for h in hits["hits"]:
    s = h['_source']
    print(f"  - {s['section_path']} (score: {h['_score']:.2f})")

# Test 3: evidence_level filter
print("\n=== Test 3: Filtered search (evidence >= 4) ===")
result = es_search({
    "query": {
        "bool": {
            "must": [{"match": {"content": "治疗"}}],
            "filter": [{"range": {"evidence_level": {"gte": 4}}}]
        }
    }
})
hits = result["hits"]
print(f"Hits: {hits['total']['value']}")
for h in hits["hits"]:
    s = h['_source']
    print(f"  - {s['section_path']} (evidence: {s['evidence_level']})")

# Test 4: All documents summary
print("\n=== Test 4: All documents ===")
result = es_search({"query": {"match_all": {}}, "size": 10})
hits = result["hits"]
print(f"Total: {hits['total']['value']}")
for h in hits["hits"]:
    s = h['_source']
    print(f"  - [{s['document_type']}] {s['section_path']} (evidence={s['evidence_level']}, entities={s['medical_entities'][:3]}...)")
    print(f"    vector dims: {len(s['content_vector'])}")
