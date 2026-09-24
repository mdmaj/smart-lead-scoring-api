import os
import time
import json

from dotenv import load_dotenv
from google import genai


# --------------------------------
# 1. Load environment variables
# --------------------------------

load_dotenv()

api_key = os.getenv("GEMINI_API_KEY")

if not api_key:
    raise ValueError("GEMINI_API_KEY is not set")


# --------------------------------
# 2. Create Gemini client
# --------------------------------

client = genai.Client(api_key=api_key)


# --------------------------------
# 3. Lead data
# --------------------------------

lead = {
    "name": "Rahul Sharma",
    "email": "rahul@gmail.com",
    "message": "I need a business loan of 10 lakh urgently for my company.",
    "source": "website",
    "budget": 1000000,
    "company": "ABC Pvt Ltd"
}


# --------------------------------
# 4. Create AI prompt
# --------------------------------

prompt = f"""
You are a lead scoring AI system.

Analyze the following customer lead and assign a lead score from 0 to 100.

Lead information:

Name: {lead.get("name")}
Email: {lead.get("email")}
Message: {lead.get("message")}
Source: {lead.get("source")}
Budget: {lead.get("budget")}
Company: {lead.get("company")}

Scoring guidelines:

- 80-100 = HOT
- 50-79 = WARM
- 0-49 = COLD

Consider:

1. Buying intent
2. Budget
3. Specificity of the requirement
4. Business/company information
5. Urgency expressed in the message

Return ONLY valid JSON.

Use exactly this format:

{{
    "score": 85,
    "category": "HOT",
    "reason": "The lead has strong buying intent and a specific business requirement."
}}
"""


# --------------------------------
# 5. Call Gemini with retry
# --------------------------------

response = None

for attempt in range(3):

    try:

        print(f"Gemini request attempt {attempt + 1}/3...")

        response = client.models.generate_content(
            model="gemini-3.7-flash",
            contents=prompt
        )

        break

    except Exception as e:

        print(f"Gemini request failed: {e}")

        if attempt == 2:
            print("All 3 attempts failed.")
            raise e

        wait_time = 2 ** attempt

        print(
            f"Retrying in {wait_time} seconds..."
        )

        time.sleep(wait_time)


# --------------------------------
# 6. Read Gemini response
# --------------------------------

if response is None:
    raise RuntimeError("Gemini did not return a response")


text = response.text.strip()

print("\nRaw Gemini response:")
print(text)


# --------------------------------
# 7. Clean JSON response
# --------------------------------

if text.startswith("```"):

    text = text.replace("```json", "")
    text = text.replace("```", "")

    text = text.strip()


# --------------------------------
# 8. Convert JSON string to Python
# --------------------------------

try:

    result = json.loads(text)

except json.JSONDecodeError:

    print("\nGemini returned invalid JSON.")
    raise


# --------------------------------
# 9. Print final result
# --------------------------------

print("\nFinal Lead Score:")

print(json.dumps(result, indent=4))