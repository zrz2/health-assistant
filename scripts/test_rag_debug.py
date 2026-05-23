"""Debug RAG: test with proper session flow (firstMessage simple, then query)."""
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
    with urllib.request.urlopen(req, timeout=30) as resp:
        return json.loads(resp.read().decode("utf-8"))

def send_and_stream(sid, content, token):
    body = json.dumps({"sessionId": sid, "content": content}).encode("utf-8")
    req = urllib.request.Request(
        BASE + "/chat/messages",
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
result = post("/auth/login", {"username": "test", "password": "Test1234"})
token = result["data"]["accessToken"]

queries = [
    "二甲双胍的推荐起始剂量是多少",
    "2型糖尿病患者的空腹血糖控制目标",
    "糖尿病患者饮食中碳水化合物应占多少比例",
    "糖尿病运动治疗建议每周运动多长时间",
]

for query in queries:
    print(f"\n{'='*60}")
    print(f"Query: {query}")

    # Create session with simple first message
    result = post("/chat/sessions",
        {"title": "RAG Test", "firstMessage": "你好"},
        token=token)
    sid = result["data"]["sessionId"]

    # Now send the actual test query
    response, sources, clarification = send_and_stream(sid, query, token)

    if clarification:
        print(f"CLARIFICATION: {clarification[:100]}")
    else:
        print(f"Response: {response[:200]}")
        print(f"Sources: {sources}")
        if sources:
            print(">>> RAG ACTIVE <<<")
        else:
            print(">>> RAG NOT ACTIVE (no sources) <<<")
