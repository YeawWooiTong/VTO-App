# SAM2 Segmentation API Integration

This feature integrates the SAM2 (Segment Anything Model 2) segmentation API with the VTO (Virtual Try-On) app to automatically remove backgrounds from clothing images.

## Overview

When a user captures an image of a clothing item, they can now:

1. Draw a bounding box around the clothing item
2. Send the image and bounding box coordinates to the SAM2 API
3. Receive a segmented image with the background removed
4. Save the segmented image to their wardrobe

## Implementation Details

### Components

- **ApiClient**: Handles HTTP requests to the SAM2 segmentation API
- **BoundingBoxView**: A custom Compose UI component that allows drawing a bounding box on an image
- **SegmentationScreen**: Screen that displays the captured image and allows users to draw a bounding box
- **ImageUtils**: Utility functions for working with images (loading, saving, rotation)

### Workflow

1. User takes a photo in the CameraScreen
2. The app navigates to the SegmentationScreen with the captured image
3. User draws a bounding box around the clothing item
4. When the user confirms, the app:
   - Saves the image to a temporary file
   - Sends the image and bounding box coordinates to the SAM2 API
   - Receives a segmented image with transparent background
   - Uploads the segmented image to Firebase Storage
   - Adds the image to the user's wardrobe

## API Configuration

The SAM2 API endpoint is configured in the `ApiClient` class. Before deploying in production:

1. Replace the placeholder URL in ApiClient.kt with your actual SAM2 API endpoint
```kotlin
private val apiUrl = "http://YOUR_SAM2_API_SERVER:8080/segment"
```

2. Ensure your server has sufficient resources to handle image processing

## SAM2 API Server

The SAM2 API server is a FastAPI application that:

1. Accepts image uploads and bounding box coordinates
2. Runs the SAM2 model to segment the object
3. Returns a segmented image with transparent background

The API accepts the following parameters:
- `image`: The image file
- `x`: X coordinate of the top-left corner of the bounding box
- `y`: Y coordinate of the top-left corner of the bounding box
- `width`: Width of the bounding box
- `height`: Height of the bounding box

Response format:
```json
{
  "success": true,
  "scores": [0.95],
  "image_base64": "base64_encoded_segmented_image"
}
```

## Dependencies

- OkHttp: For making HTTP requests to the API
- AndroidX Camera: For capturing images
- Compose Foundation: For UI components

## Credits

SAM2 implementation based on Meta's Segment Anything 2 model: https://github.com/facebookresearch/segment-anything 