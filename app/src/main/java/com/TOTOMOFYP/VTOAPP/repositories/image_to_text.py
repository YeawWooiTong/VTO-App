from fastapi import FastAPI, File, UploadFile, Form
from google import genai
from google.genai import types
from google.cloud import firestore, storage
from pydantic import BaseModel
import enum
import uuid
import datetime
from fastapi.encoders import jsonable_encoder
import json
import os
from google.oauth2 import service_account
from dotenv import load_dotenv

load_dotenv() # Load environment variables from .env file

# -----------------------------
# Define structured schema
# -----------------------------
class Category(enum.Enum):
    ALL = "all"
    UPPER = "top"
    BOTTOM = "bottom"
    FULL_BODY = "full_body"

class Occasion(enum.Enum):
    CASUAL = "Casual"
    FORMAL = "Formal"
    BUSINESS_OFFICE = "Business / Office"
    PARTY_CELEBRATION = "Party / Celebration"
    WEDDING = "Wedding"
    SPORTS_ACTIVE = "Sports / Active"
    TRAVEL_VACATION = "Travel / Vacation"
    LOUNGEWEAR_HOME = "Loungewear / Home"
    TRADITIONAL_CULTURAL = "Traditional / Cultural"
    SEASONAL_WEATHER = "Seasonal / Weather-based"

class ClothingItem(BaseModel):
    description: str
    category: Category
    color: str
    pattern: str
    style: str
    occasion: str

# -----------------------------
# Smart Name Generation Function
# -----------------------------
def generate_smart_item_name(user_id: str, item_occasion: str, item_category: str, item_style: str) -> str:
    """
    Generate smart names based on occasion like 'Casual 1', 'Formal 2', 'Business 3', etc.
    based on existing items in the user's wardrobe
    """
    try:
        # Map occasion text to proper display name
        occasion_mapping = {
            "casual": "Casual",
            "formal": "Formal", 
            "business": "Business",
            "office": "Business",
            "business / office": "Business",
            "party": "Party",
            "celebration": "Party",
            "party / celebration": "Party",
            "wedding": "Wedding",
            "sports": "Sports",
            "active": "Sports", 
            "sports / active": "Sports",
            "travel": "Travel",
            "vacation": "Travel",
            "travel / vacation": "Travel",
            "loungewear": "Loungewear",
            "home": "Loungewear",
            "loungewear / home": "Loungewear",
            "traditional": "Traditional",
            "cultural": "Traditional",
            "traditional / cultural": "Traditional",
            "seasonal": "Seasonal",
            "weather": "Seasonal",
            "seasonal / weather-based": "Seasonal"
        }
        
        # Determine base name from occasion first, then style, then category
        occasion_lower = item_occasion.lower().strip()
        style_lower = item_style.lower().strip()
        
        base_name = None
        
        # Try to match occasion first
        for key, value in occasion_mapping.items():
            if key in occasion_lower:
                base_name = value
                break
        
        # If no occasion match, try style
        if not base_name and (style_lower and 
            style_lower != "unknown" and 
            style_lower != "test clothing item" and 
            style_lower != "not specified" and
            style_lower != ""):
            
            # Map common styles to occasions
            if "casual" in style_lower:
                base_name = "Casual"
            elif "formal" in style_lower:
                base_name = "Formal"
            elif "business" in style_lower or "office" in style_lower:
                base_name = "Business"
            elif "party" in style_lower or "evening" in style_lower:
                base_name = "Party"
            elif "sport" in style_lower or "active" in style_lower:
                base_name = "Sports"
            else:
                base_name = item_style.capitalize()
        
        # If no occasion or style match, use category as fallback
        if not base_name:
            if item_category.lower() == "top":
                base_name = "Top"
            elif item_category.lower() == "bottom":
                base_name = "Bottom"
            elif item_category.lower() == "full_body":
                base_name = "Outfit"
            else:
                base_name = "Item"
        
        # Query existing items with the same base name pattern
        user_items_ref = db.collection("users").document(user_id).collection("wardrobeItems")
        existing_items = user_items_ref.get()
        
        # Count items with similar style/base name
        count = 0
        for item_doc in existing_items:
            item_data = item_doc.to_dict()
            existing_display_name = item_data.get("display_name", "")
            
            # Check if this item has the same base name
            if existing_display_name.startswith(base_name + " "):
                try:
                    # Extract number from existing name like "Casual 3"
                    number_part = existing_display_name.split(" ")[-1]
                    if number_part.isdigit():
                        item_number = int(number_part)
                        count = max(count, item_number)
                except:
                    continue
        
        # Generate new name with next number
        new_number = count + 1
        smart_name = f"{base_name} {new_number}"
        
        print(f"Generated smart name: '{smart_name}' for occasion='{item_occasion}', style='{item_style}', category='{item_category}'")
        return smart_name
        
    except Exception as e:
        print(f"Error generating smart name: {e}")
        # Fallback to simple naming
        return f"Item {datetime.datetime.now().strftime('%m%d%H%M')}"

# -----------------------------
# Initialize FastAPI & Gemini
# -----------------------------
app = FastAPI(title="Clothing Metadata API")
client = genai.Client()  # Gemini API client

# Initialize Firebase with credentials
try:
    # Path to your service account key file
    credentials_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    
    if not credentials_path:
        print("❌ Error: GOOGLE_APPLICATION_CREDENTIALS not found in environment variables.")
        print("Please create a .env file and add GOOGLE_APPLICATION_CREDENTIALS='./path/to/your/firebase-adminsdk.json'")
        exit(1)
        
    if os.path.exists(credentials_path):
    
    if os.path.exists(credentials_path):
        # Load credentials from file
        credentials = service_account.Credentials.from_service_account_file(credentials_path)
        db = firestore.Client(credentials=credentials)
        storage_client = storage.Client(credentials=credentials)
        bucket = storage_client.bucket("vto-app-f7833.firebasestorage.app")
        print("✅ Firebase credentials loaded successfully")
    else:
        print(f"❌ Credentials file not found: {credentials_path}")
        print("Please download firebase-credentials.json from Firebase Console")
        exit(1)
except Exception as e:
    print(f"❌ Firebase initialization failed: {e}")
    exit(1)


@app.post("/categorize/{user_id}")
async def categorize_clothing(user_id: str, file: UploadFile = File(...)):
    try:
        # Read uploaded image
        image_bytes = await file.read()

        # Prompt Gemini to generate description + structured JSON
        prompt = (
            "Interpret this clothing item and provide:\n"
            "1. A simple description.\n"
            "2. Structured JSON with fields:\n"
            "   - description (text)\n"
            "   - category (enum: all, top, bottom, full_body)\n"
            "   - color (text)\n"
            "   - pattern (text)\n"
            "   - style (text)\n"
            "   - occasion (choose from: Casual, Formal, Business / Office, Party / Celebration, Wedding, Sports / Active, Travel / Vacation, Loungewear / Home, Traditional / Cultural, Seasonal / Weather-based)\n"
            "\nFor occasion field, pick the most appropriate from the list above. If multiple apply, choose the primary one."
        )

        # Call Gemini with structured output
        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=[
                types.Part.from_bytes(
                    data=image_bytes,
                    mime_type="image/png"
                ),
                prompt
            ],
            config={
                "response_mime_type": "application/json",
                "response_schema": ClothingItem,
                "thinking_config": types.ThinkingConfig(thinking_budget=0)  # disables extra thinking
            }
        )

        # Parse Gemini output safely
        try:
            structured_json = json.loads(response.text)
        except Exception:
            structured_json = {"error": "Failed to parse Gemini output", "raw_text": response.text}

        # Generate smart name based on AI analysis results
        item_occasion = structured_json.get("occasion", "")
        item_style = structured_json.get("style", "")
        item_category = structured_json.get("category", "other")
        smart_name = generate_smart_item_name(user_id, item_occasion, item_category, item_style)

        # Upload image to Firebase Storage - FIXED PATH
        item_id = str(uuid.uuid4())

        # Use PNG format to preserve transparency from segmentation
        storage_path = f"users/{user_id}/wardrobe/{item_id}.png"
        blob = bucket.blob(storage_path)

        # Upload with error handling
        try:
            blob.upload_from_string(image_bytes, content_type="image/png")
            print(f"✅ Successfully uploaded to Firebase Storage")
        except Exception as upload_error:
            print(f"❌ Upload failed: {upload_error}")
            raise upload_error

        # Make blob public and generate proper URL
        try:
            blob.make_public()
            print(f"✅ Successfully made blob public")
        except Exception as public_error:
            print(f"❌ Make public failed: {public_error}")
            print(f"⚠️  Continuing with URL generation...")
            
        bucket_name = bucket.name
        blob_name = blob.name.replace("/", "%2F")  # URL encode the path
        image_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket_name}/o/{blob_name}?alt=media"

        print(f"=== IMAGE UPLOAD DEBUG ===")
        print(f"User ID: {user_id}")
        print(f"Item ID: {item_id}")
        print(f"Storage path: {storage_path}")
        print(f"Bucket name: {bucket_name}")
        print(f"Blob name (encoded): {blob_name}")
        print(f"Generated URL: {image_url}")
        print(f"File name: {file.filename}")
        print(f"Image bytes size: {len(image_bytes)}")
        print("=========================")

        # Save metadata + image URL to Firestore with error handling
        try:
            doc_ref = db.collection("users").document(user_id).collection("wardrobeItems").document(item_id)
            firestore_data = {
                "image_name": file.filename,
                "image_url": image_url,
                "metadata": structured_json,
                "timestamp": datetime.datetime.utcnow().isoformat(),
                "display_name": smart_name  # Generated smart name
            }
            doc_ref.set(jsonable_encoder(firestore_data))
            print(f"✅ Successfully saved to Firestore")
            print(f"Document path: users/{user_id}/wardrobeItems/{item_id}")
        except Exception as firestore_error:
            print(f"❌ Firestore save failed: {firestore_error}")
            raise firestore_error

        return {
            "message": "Clothing categorized and saved to Firebase",
            "item_id": item_id,
            "image_url": image_url,
            "metadata": structured_json,
            "timestamp": datetime.datetime.utcnow().isoformat()
        }

    except Exception as e:
        return {"error": str(e)}

@app.post("/upload_outfit/{user_id}")
async def upload_outfit(
    user_id: str,
    file: UploadFile = File(...),
    outfit_name: str = Form(...)  # User provides outfit name
):
    try:
        # Read uploaded image
        image_bytes = await file.read()

        # Prompt Gemini to generate outfit occasion + metadata
        prompt = (
            "Analyze this outfit image and provide structured JSON with:\n"
            "1. description (short text)\n"
            "2. occasion (choose one: Casual, Formal, Business / Office, Party / Celebration, "
            "Wedding, Sports / Active, Travel / Vacation, Loungewear / Home, Traditional / Cultural, Seasonal)\n"
            "3. color (main colors)\n"
            "4. style (short style description)\n"
        )

        # Call Gemini for structured metadata
        response = client.models.generate_content(
            model="gemini-2.5-flash",
            contents=[
                types.Part.from_bytes(data=image_bytes, mime_type="image/png"),
                prompt
            ],
            config={
                "response_mime_type": "application/json"
            }
        )

        # Parse Gemini response safely
        try:
            structured_json = json.loads(response.text)
        except Exception:
            structured_json = {"error": "Failed to parse Gemini output", "raw_text": response.text}

        # Generate unique ID for outfit
        item_id = str(uuid.uuid4())

        # Firebase Storage path (outfits)
        storage_path = f"users/{user_id}/outfits/{item_id}.png"
        blob = bucket.blob(storage_path)

        try:
            blob.upload_from_string(image_bytes, content_type="image/png")
            blob.make_public()
            print(f"✅ Uploaded outfit image to Storage")
        except Exception as e:
            print(f"❌ Upload failed: {e}")
            raise e

        # Generate public URL
        bucket_name = bucket.name
        blob_name = blob.name.replace("/", "%2F")
        image_url = f"https://firebasestorage.googleapis.com/v0/b/{bucket_name}/o/{blob_name}?alt=media"

        # Firestore path (outfits)
        try:
            doc_ref = db.collection("users").document(user_id).collection("outfits").document(item_id)
            firestore_data = {
                "outfit_name": outfit_name,  # user-provided
                "image_url": image_url,
                "metadata": structured_json,
                "timestamp": datetime.datetime.utcnow().isoformat()
            }
            doc_ref.set(jsonable_encoder(firestore_data))
            print(f"✅ Saved outfit metadata to Firestore")
        except Exception as firestore_error:
            print(f"❌ Firestore save failed: {firestore_error}")
            raise firestore_error

        return {
            "message": "Outfit uploaded and saved successfully",
            "item_id": item_id,
            "outfit_name": outfit_name,
            "image_url": image_url,
            "metadata": structured_json
        }

    except Exception as e:
        return {"error": str(e)}
# -----------------------------
# Run locally
# -----------------------------
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
