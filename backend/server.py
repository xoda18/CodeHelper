from flask import Flask, request, jsonify
from openai import OpenAI
import os
import tiktoken

app = Flask(__name__)
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

LOG_FILE = "chat.log"
MODEL = "gpt-4o-mini"
MAX_CONTEXT_TOKENS = 10000


# Token counting

def count_tokens(text: str) -> int:
    enc = tiktoken.encoding_for_model(MODEL)
    return len(enc.encode(text))


# Log file

def load_context() -> str:
    if not os.path.exists(LOG_FILE):
        return ""
    with open(LOG_FILE, "r") as f:
        return f.read()

def save_to_log(role: str, content: str):
    with open(LOG_FILE, "a") as f:
        f.write(f"[{role.upper()}] {content}\n\n")

def overwrite_log(content: str):
    with open(LOG_FILE, "w") as f:
        f.write(content)


# Context compression

def compress_context(context: str) -> str:
    response = client.chat.completions.create(
        model=MODEL,
        messages=[{
            "role": "user",
            "content": (
                "Summarize the following conversation history as concisely as possible. "
                "Keep the key facts, code explanations, and important decisions. "
                "Output only the summary, no extra text.\n\n"
                f"{context}"
            )
        }]
    )
    return f"[COMPRESSED CONTEXT]\n{response.choices[0].message.content}\n\n"


#Build project context

def build_project_context(project_files: list) -> str:
    if not project_files:
        return ""
    parts = ["Here is the full project source code:\n"]
    for f in project_files:
        parts.append(f"--- {f['path']} ---\n{f['content']}\n")
    return "\n".join(parts)


# Main endpoints

@app.route("/analyze", methods=["POST"])
def analyze():
    data = request.json
    code = data.get("code", "")
    project_files = data.get("project_files", [])
    user_message = f"Explain the code part in the context of this project:\n\n{code}"

    context = load_context()
    if context and count_tokens(context) > MAX_CONTEXT_TOKENS:
        context = compress_context(context)
        overwrite_log(context)

    system_parts = []
    project_context = build_project_context(project_files)
    if project_context:
        system_parts.append(project_context)
    if context:
        system_parts.append(f"Previous conversation context:\n{context}")

    messages = []
    if system_parts:
        messages.append({"role": "system", "content": "\n\n".join(system_parts)})
    messages.append({"role": "user", "content": user_message})

    response = client.chat.completions.create(model=MODEL, messages=messages)
    result = response.choices[0].message.content

    save_to_log("user", user_message)
    save_to_log("ai", result)

    return jsonify({"result": result})


@app.route("/chat", methods=["POST"])
def chat():
    user_message = request.json.get("message", "")

    context = load_context()
    if context and count_tokens(context) > MAX_CONTEXT_TOKENS:
        context = compress_context(context)
        overwrite_log(context)

    messages = []
    if context:
        messages.append({"role": "system", "content": f"Previous conversation context:\n{context}"})
    messages.append({"role": "user", "content": user_message})

    response = client.chat.completions.create(model=MODEL, messages=messages)
    result = response.choices[0].message.content

    save_to_log("user", user_message)
    save_to_log("ai", result)

    return jsonify({"result": result})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
