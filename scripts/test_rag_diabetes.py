"""Comprehensive RAG test with diabetes queries."""
import json
import urllib.request
import urllib.error

BASE = "http://localhost:8080/api/v1"

def post(path, data, token=None):
    body = json.dumps(data).encode("utf-8")
    headers = {"Content-Type": "application/json; charset=utf-8"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request(BASE + path, data=body, headers=headers, method="POST")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())


def send_sse(path, data, token):
    body = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(
        BASE + path,
        data=body,
        headers={
            "Content-Type": "application/json; charset=utf-8",
            "Authorization": "Bearer " + token,
            "Accept": "text/event-stream",
        },
        method="POST",
    )
    chunks = []
    sources = None
    clarification = None
    try:
        with urllib.request.urlopen(req, timeout=90) as resp:
            buffer = b""
            while True:
                chunk = resp.read(1)
                if not chunk:
                    break
                buffer += chunk
                if chunk == b"\n":
                    line = buffer.decode("utf-8").strip()
                    buffer = b""
                    if line.startswith("data:"):
                        try:
                            data = json.loads(line[5:].strip())
                            t = data.get("type")
                            if t == "message":
                                chunks.append(data.get("content", ""))
                            elif t == "done":
                                sources = data.get("sources")
                            elif t == "clarification":
                                clarification = data.get("content")
                        except json.JSONDecodeError:
                            pass
    except Exception as e:
        print(f"    Stream error: {e}")
    return "".join(chunks), sources, clarification


# Login
print("=== RAG Diabetes Knowledge Test ===\n")
result = post("/auth/login", {"username": "test", "password": "Test1234"})
token = result["data"]["accessToken"]

tests = [
    ("Drug Query", "二甲双胍的推荐起始剂量和最大剂量是多少"),
    ("Glucose Target", "2型糖尿病患者的血糖控制目标是什么，包括空腹和餐后"),
    ("Diet Advice", "糖尿病患者应该如何安排饮食，碳水化合物应占多少比例"),
    ("Exercise Advice", "糖尿病患者每周应进行多少运动，运动时需要注意什么"),
    ("Complication Screening", "糖尿病患者每年应做哪些检查来预防并发症"),
    ("Hypoglycemia", "糖尿病患者发生低血糖应该怎么处理"),
    ("Monitoring", "糖尿病患者应该如何监测血糖，HbA1c多久测一次"),
    ("Pathogenesis", "2型糖尿病的主要发病机制是什么"),
]

print(f"Running {len(tests)} test queries...\n")

results = []
for name, query in tests:
    print(f"[{name}] {query}")

    # Create a fresh session for each test
    result = post("/chat/sessions",
        {"title": f"Test: {name}", "firstMessage": query},
        token=token)
    sid = result["data"]["sessionId"]

    response, sources, clarification = send_sse("/chat/messages",
        {"sessionId": sid, "content": query}, token)

    if clarification:
        print(f"  => CLARIFICATION (unexpected): {clarification[:80]}")
        results.append((name, False, "clarification"))
    else:
        has_sources = sources is not None and len(sources) > 0
        # Check if response references our knowledge base
        refs_source = "来源" in response or "文献" in response or "指南" in response
        status = "RAG OK" if has_sources else "NO RAG"
        print(f"  => {status} | sources={sources[:80] if sources else 'None'}...")
        print(f"  => Response: {response[:150]}...")
        results.append((name, has_sources, response[:100]))
    print()

# Summary
print("=" * 50)
print("SUMMARY:")
rag_count = sum(1 for _, ok, _ in results if ok)
print(f"RAG active: {rag_count}/{len(tests)} queries")
for name, ok, detail in results:
    status = "PASS" if ok else "FAIL"
    print(f"  [{status}] {name}: {detail[:60]}...")
