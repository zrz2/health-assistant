"""Test RAG pipeline with specific medical queries that bypass clarification."""
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
        body = e.read().decode()
        print(f"  HTTP {e.code}: {body}")
        return json.loads(body)


def send_sse(path, data, token):
    """Send a message and stream SSE response."""
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
    return "".join(chunks), sources, clarification


# Login
print("1. Login...")
result = post("/auth/login", {"username": "test", "password": "Test1234"})
token = result["data"]["accessToken"]

# Test 1: Specific drug query
print("\n=== Test 1: Specific drug query ===")
result = post("/chat/sessions", {"title": "Drug Query", "firstMessage": "二甲双胍是什么药"}, token=token)
sid = result["data"]["sessionId"]
print(f"Session: {sid}")

response, sources, clarification = send_sse("/chat/messages",
    {"sessionId": sid, "content": "二甲双胍治疗2型糖尿病的推荐剂量是多少"}, token)

if clarification:
    print(f"CLARIFICATION: {clarification[:100]}")
else:
    print(f"Response ({len(response)} chars): {response[:300]}")
    print(f"Sources: {sources}")
    if sources:
        print(">>> RAG ACTIVE! <<<")
    else:
        print(">>> RAG NOT ACTIVE <<<")

# Test 2: Specific fever query
print("\n=== Test 2: Specific fever query ===")
result = post("/chat/sessions", {"title": "Fever Query", "firstMessage": "发烧的定义是什么"}, token=token)
sid = result["data"]["sessionId"]
print(f"Session: {sid}")

response, sources, clarification = send_sse("/chat/messages",
    {"sessionId": sid, "content": "成人发热38.5度以上应该使用什么退烧药，剂量是多少"}, token)

if clarification:
    print(f"CLARIFICATION: {clarification[:100]}")
else:
    print(f"Response ({len(response)} chars): {response[:300]}")
    print(f"Sources: {sources}")
    if sources:
        print(">>> RAG ACTIVE! <<<")
    else:
        print(">>> RAG NOT ACTIVE <<<")

# Test 3: Specific hypertension query
print("\n=== Test 3: Specific hypertension query ===")
result = post("/chat/sessions", {"title": "HTN Query", "firstMessage": "高血压的标准是什么"}, token=token)
sid = result["data"]["sessionId"]
print(f"Session: {sid}")

response, sources, clarification = send_sse("/chat/messages",
    {"sessionId": sid, "content": "高血压患者血压控制目标和一线降压药物有哪些"}, token)

if clarification:
    print(f"CLARIFICATION: {clarification[:100]}")
else:
    print(f"Response ({len(response)} chars): {response[:300]}")
    print(f"Sources: {sources}")
    if sources:
        print(">>> RAG ACTIVE! <<<")
    else:
        print(">>> RAG NOT ACTIVE <<<")
