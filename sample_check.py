import json
import random

with open("app/src/main/assets/words.json", "r", encoding="utf-8") as f:
    data = json.load(f)

random.seed(42)
sample = random.sample(data, 10)
with open("sample_words.txt", "w", encoding="utf-8") as out:
    for idx, w in enumerate(sample, 1):
        out.write(f"{idx}. [{w['subject']}/{w['level']}] {w['word']} - {w['phonetic']} | Mẹo: {w['tip']}\n")
