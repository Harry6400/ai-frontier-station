#!/usr/bin/env python3
"""Batch translate English content to Chinese using DashScope API."""
import json, subprocess, urllib.request, urllib.error

DB_USER, DB_PASS, DB_NAME = "root", "123456", "ai_frontier_station"
API_KEY = ""  # Will be read from env or DB

def mysql_query(sql):
    r = subprocess.run(["mysql", f"-u{DB_USER}", f"-p{DB_PASS}", "-N", "-e", sql, DB_NAME],
                       capture_output=True, text=True)
    return r.stdout.strip()

def get_api_key():
    """Get bailian API key from environment or try reading from application.yml."""
    import os
    key = os.environ.get("DASHSCOPE_API_KEY", "")
    if key:
        return key
    # Try reading from application.yml
    try:
        with open("backend/src/main/resources/application.yml") as f:
            for line in f:
                if "api-key:" in line and "DASHSCOPE" in line:
                    # Extract env var reference
                    return ""  # Need actual key
    except:
        pass
    return ""

def translate_content(content_id, title, body, api_key):
    """Call DashScope API to translate content."""
    prompt = f"""请将以下AI领域内容翻译并整理为中文，使用结构化Markdown格式。

标题：{title}

原始内容：
{body[:2000]}

要求：
1. 必须使用中文输出
2. 使用结构化Markdown，包含：
   ## 核心要点
   列出3-5个关键要点
   
   ## 详细内容
   用中文详细描述，保持专业性

3. 如果内容已经是中文，直接返回原文
4. 不要添加额外的评论或分析"""

    payload = json.dumps({
        "model": "qwen-turbo",
        "messages": [{"role": "user", "content": prompt}],
        "temperature": 0.1,
        "max_tokens": 1500
    }).encode("utf-8")

    req = urllib.request.Request(
        "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
        data=payload,
        headers={
            "Content-Type": "application/json",
            "Authorization": f"Bearer {api_key}"
        }
    )

    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read())
            return data["choices"][0]["message"]["content"]
    except Exception as e:
        print(f"  Error: {e}")
        return None

def main():
    api_key = get_api_key()
    if not api_key:
        print("ERROR: No DASHSCOPE_API_KEY found. Set it as environment variable.")
        print("Run: export DASHSCOPE_API_KEY=your_key_here")
        return

    # Get English content items
    rows = mysql_query(
        "SELECT id, title, body_markdown FROM ai_content "
        "WHERE body_markdown IS NOT NULL AND body_markdown != ' ' "
        "AND body_markdown REGEXP '^[a-zA-Z ]' AND content_type != 'paper'"
    )

    if not rows:
        print("No English content found.")
        return

    for line in rows.split("\n"):
        parts = line.split("\t", 2)
        if len(parts) < 3:
            continue
        cid, title, body = parts
        print(f"Translating ID {cid}: {title[:50]}...")

        translated = translate_content(cid, title, body, api_key)
        if translated:
            # Escape for SQL
            escaped = translated.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n")
            mysql_query(f"UPDATE ai_content SET body_markdown = '{escaped}' WHERE id = {cid}")
            print(f"  OK - translated {len(translated)} chars")
        else:
            print(f"  FAILED")

if __name__ == "__main__":
    main()
