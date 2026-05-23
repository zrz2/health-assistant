"""Test RAG pipeline end-to-end."""
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
        print(f"  HTTP Error {e.code}: {e.read().decode()}")
        raise

def test_rag():
    # Step 1: Login
    print("1. Login...")
    result = post("/auth/login", {"username": "test", "password": "Test1234"})
    token = result["data"]["accessToken"]
    print(f"   OK - token: {token[:40]}...")

    # Step 2: Create session (firstMessage is required)
    print("2. Create session...")
    result = post("/chat/sessions", {"title": "RAG Test", "firstMessage": "你好"}, token=token)
    sid = result["data"]["sessionId"]
    print(f"   OK - session: {sid}")

    # Step 3: Send message and stream SSE manually
    print("3. Send message (SSE stream)...")
    query = "血糖控制不好怎么办"
    print(f"   Query: {query}")

    body = json.dumps({"sessionId": sid, "content": query}).encode("utf-8")
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

    try:
        with urllib.request.urlopen(req, timeout=90) as resp:
            chunks = []
            sources = None
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
                                print(f"   CLARIFICATION: {data.get('content')}")
                        except json.JSONDecodeError:
                            pass

            full = "".join(chunks)
            print(f"   Response: {len(chunks)} chunks, {len(full)} chars")
            print(f"   ---")
            for line in full[:600].split("\n"):
                print(f"   {line}")
            print(f"   ---")
            print(f"   Sources: {sources}")

            if sources:
                print("\n*** RAG PIPELINE: ACTIVE ***")
                print(f"    Retrieved sources: {sources}")
            else:
                print("\n*** RAG PIPELINE: NOT ACTIVE ***")
                print("    No sources in response - check app logs")

    except urllib.error.HTTPError as e:
        print(f"   HTTP Error {e.code}")
        print(f"   Body: {e.read().decode()}")


if __name__ == "__main__":
    test_rag()
