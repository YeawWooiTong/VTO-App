import cv2
import torch
import base64
import numpy as np
from fastapi import FastAPI, HTTPException, File, UploadFile, Form
from pydantic import BaseModel
from fastapi.middleware.cors import CORSMiddleware
import io
from sam2.build_sam import build_sam2
from sam2.sam2_image_predictor import SAM2ImagePredictor
from sam2.automatic_mask_generator import SAM2AutomaticMaskGenerator
from transformers import AutoProcessor, AutoModelForZeroShotObjectDetection 
from PIL import Image

# Create FastAPI app
app = FastAPI(title="SAM2 Segmentation API")

# Add CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Setup CUDA and model configurations
torch.autocast(device_type="cuda", dtype=torch.bfloat16).__enter__()

if torch.cuda.get_device_properties(0).major >= 8:
    torch.backends.cuda.matmul.allow_tf32 = True
    torch.backends.cudnn.allow_tf32 = True

DEVICE = torch.device('cuda' if torch.cuda.is_available() else 'cpu')
# Load GroundingDINO
grounding_processor = AutoProcessor.from_pretrained("IDEA-Research/grounding-dino-tiny")
grounding_model = AutoModelForZeroShotObjectDetection.from_pretrained(
    "IDEA-Research/grounding-dino-tiny"
).to(DEVICE)

CHECKPOINT = "checkpoints/sam2.1_hiera_tiny.pt"
CONFIG = "configs/sam2.1/sam2.1_hiera_t.yaml"



# Load the model
sam2_model = build_sam2(CONFIG, CHECKPOINT, device=DEVICE, apply_postprocessing=False)
mask_generator = SAM2AutomaticMaskGenerator(sam2_model)


@app.get("/")
async def root():
    return {"message": "SAM2 Segmentation API is running. Use POST /segment endpoint."}



@app.post("/segment")
async def segment_image(file: UploadFile = File(...)):
    try:
        contents = await file.read()
        image_pil = Image.open(io.BytesIO(contents)).convert("RGB")
        image_np = np.array(image_pil)
        
        # Step 1: detect all clothing items
        inputs = grounding_processor(
            images=image_pil,
            text="clothes . shirt . t-shirt . pants . dress . jacket . coat . skirt . blouse . sweater . hoodie . jeans . trousers . shorts . gown . jumpsuit .",
            return_tensors="pt"
        )
        inputs = {k: v.to(DEVICE) for k, v in inputs.items()}
        with torch.no_grad():
            outputs = grounding_model(**inputs)
        results = grounding_processor.post_process_grounded_object_detection(
            outputs,
            input_ids=inputs["input_ids"],
            box_threshold=0.35, text_threshold=0.25,
            target_sizes=[image_np.shape[:2]]
        )
        boxes = results[0]["boxes"].cpu().numpy()
        if len(boxes) == 0:
            return {"error": "No clothes detected"}

        # Step 2: segment each with SAM2
        predictor = SAM2ImagePredictor(sam2_model)
        predictor.set_image(image_np)
        all_masks = []
        for box in boxes:
            masks, _, _ = predictor.predict(box=np.array([box]), multimask_output=False)
            if len(masks) > 0:
                mask_bool = masks[0].astype(bool)
                all_masks.append(mask_bool)

        if len(all_masks) == 0:
            return {"error": "No masks generated"}

        # Step 3: merge masks
        combined_mask = np.any(all_masks, axis=0)  # shape: H x W, boolean mask

        # Step 4: crop or prepare output
        # optional: find bounding box around combined_mask
        ys, xs = np.where(combined_mask)
        y1, y2 = ys.min(), ys.max()
        x1, x2 = xs.min(), xs.max()
        # crop image_np
        cropped_img = image_np[y1:y2, x1:x2]
        # apply mask to crop
        cropped_mask = combined_mask[y1:y2, x1:x2]
        # build RGBA
        alpha = (cropped_mask.astype(np.uint8) * 255)
        cropped_rgba = np.dstack((cropped_img, alpha))
        
        # encode as base64 if you want
        _, buffer = cv2.imencode(".png", cv2.cvtColor(cropped_rgba, cv2.COLOR_RGBA2BGRA))
        b64_img = base64.b64encode(buffer).decode("utf-8")
        return {"mask": b64_img}

    except Exception as e:
        return {"error": str(e)}

    
if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8080)






# @app.post("/segment")
# async def segment_image(
#     x: int = Form(...), 
#     y: int = Form(...), 
#     width: int = Form(...), 
#     height: int = Form(...),
#     image: UploadFile = File(...)
# ):
#     try:
#         # Read the uploaded image
#         image_bytes = await image.read()
#         nparr = np.frombuffer(image_bytes, np.uint8)
#         image_bgr = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
        
#         if image_bgr is None:
#             raise HTTPException(status_code=400, detail="Invalid image format")
        
#         image_rgb = cv2.cvtColor(image_bgr, cv2.COLOR_BGR2RGB)
        
#         # Set up the predictor
#         predictor = SAM2ImagePredictor(sam2_model)
#         predictor.set_image(image_rgb)
        
#         # Process bounding box coordinates [x1, y1, x2, y2]
#         box_array = np.array([[x, y, x + width, y + height]])
        
#         # Run prediction with bounding box
#         masks, scores, logits = predictor.predict(
#             box=box_array,  # Use bounding box instead of point coordinates
#             multimask_output=False
#         )
        
#         # Get the mask (should only have one since multimask_output=False)
#         mask = masks[0].astype(np.uint8)
        
#         # Apply the mask to the image for visualization
#         segmented_rgb = image_rgb * mask[:, :, np.newaxis]
#         alpha = mask * 255
#         segmented_rgba = np.dstack((segmented_rgb, alpha))
        
#         # Convert to BGRA for OpenCV
#         bgra_image = cv2.cvtColor(segmented_rgba, cv2.COLOR_RGBA2BGRA)
        
#         # Encode the image as base64
#         is_success, buffer = cv2.imencode(".png", bgra_image)
#         if not is_success:
#             raise HTTPException(status_code=500, detail="Failed to encode the image")
        
#         img_bytes = buffer.tobytes()
#         img_base64 = base64.b64encode(img_bytes).decode('utf-8')
        
#         return {
#             "success": True,
#             "scores": scores.tolist() if isinstance(scores, np.ndarray) else [float(scores)],
#             "image_base64": img_base64
#         }
        
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))