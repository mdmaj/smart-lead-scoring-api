import os
import json
import time

from fastapi import FastAPI
from dotenv import load_dotenv
from google import genai

load_dotenv()

app = FastAPI()

client = genai.Client(
    api_key=os.getenv("GEMINI_API_KEY")
)


@app.get("/")
def root():
    return {
        "message": "Lead Scoring AI Service is running"
    }


@app.post("/score")
def score_lead(lead: dict):

    prompt = f"""
You are a lead scoring AI.

Analyze the following lead and return a JSON object.

Lead:
{json.dumps(lead, indent=2)}

Scoring rules:
- Score must be between 0 and 100.
- HOT: score >= 80
- WARM: score >= 50 and < 80
- COLD: score < 50

Consider:
1. Buying intent
2. Urgency
3. Budget
4. Message quality
5. Business/company information

Return ONLY valid JSON in this exact format:

{{
    "score": 0,
    "category": "HOT",
    "reason": "short explanation"
}}
"""

    # Try Gemini up to 3 times
    response = None

    for attempt in range(3):
        try:
            response = client.models.generate_content(
                model="gemini-3.6-flash",
                contents=prompt
            )

            # Gemini request succeeded
            break

        except Exception as e:
            print(
                f"Gemini attempt {attempt + 1} failed: {e}"
            )

            # Wait before retrying
            if attempt < 2:
                time.sleep(2)

            else:
                # All 3 attempts failed
                raise

    text = response.text.strip()

    # Remove markdown code fences if Gemini adds them
    if text.startswith("```"):
        text = text.replace("```json", "")
        text = text.replace("```", "")
        text = text.strip()

    result = json.loads(text)

    return result