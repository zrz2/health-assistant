"""Test the full RAG pipeline by sending a chat message and checking sources."""
import json
import urllib.request
import sseclient  # pip install sseclient-py
import io

BASE = "http://localhost:8080/api/v1"

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())

# 1. Login
print("=== 1. Login ===")
result = api("POST", "/auth/login", {"username": "test", "password": "Test1234"})
token = result["data"]["accessToken"]
print(f"Token: {token[:50]}...")

# 2. Create session
print("\n=== 2. Create session ===")
result = api("POST", "/chat/sessions", {"title": "RAG Test - Diabetes"}, token=token)
session_id = result["data"]["sessionId"]
print(f"Session: {session_id}")

# 3. Send message with SSE streaming
print("\n=== 3. Send message (SSE) ===")
query = "糖尿病血糖控制不好怎么办"
print(f"Query: {query}")

# Use urllib to stream SSE
req = urllib.request.Request(
    f"{BASE}/chat/sessions/{session_id}/messages",
    data=json.dumps({"content": query}).encode(),
    method="POST"
)
req.add_header("Content-Type", "application/json")
req.add_header("Authorization", f"Bearer {token}")
req.add_header("Accept", "text/event-stream")

chunks = []
sources = None
try:
    with urllib.request.urlopen(req, timeout=60) as resp:
        buffer = ""
        for line_bytes in resp:
            line = line_bytes.decode("utf-8").strip()
            if not line:
                continue
            if line.startswith("data:"):
                data_str = line[5:].strip()
                try:
                    data = json.loads(data_str)
                    event_type = data.get("type")
                    if event_type == "message":
                        chunks.append(data.get("content", ""))
                    elif event_type == "done":
                        sources = data.get("sources")
                        print(f"Done event - sources: {sources}")
                    elif event_type == "clarification":
                        print(f"Clarification needed: {data.get('content')}")
                except json.JSONDecodeError:
                    pass
except Exception as e:
    print(f"Error: {e}")

full_response = "".join(chunks)
print(f"\nFull response ({len(chunks)} chunks, {len(full_response)} chars):")
print(full_response[:500])
print(f"\nSources in response: {sources}")

# 4. Check if RAG was used
if sources:
    print("\n=== RAG PIPELINE: ACTIVE ===")
    print(f"Sources: {sources}")
else:
    print("\n=== RAG PIPELINE: NOT ACTIVE (no sources in response) ===")
