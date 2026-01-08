package com.example.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;

public class ScanBarcodeActivity extends AppCompatActivity {

    private PreviewView previewView;
    private BarcodeScanner scanner;
    private ExecutorService cameraExecutor;
    private static final int CAMERA_PERMISSION_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        previewView = findViewById(R.id.previewView);
        scanner = BarcodeScanning.getClient();
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Request camera permission at runtime
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST
                && grantResults.length > 0
                && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysisUseCase = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysisUseCase.setAnalyzer(cameraExecutor, imageProxy -> {
                    processImageProxy(imageProxy);
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysisUseCase);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void processImageProxy(ImageProxy imageProxy) {
        @androidx.camera.core.ExperimentalGetImage
        android.media.Image mediaImage = imageProxy.getImage();
        if (mediaImage != null) {
            int rotation = imageProxy.getImageInfo().getRotationDegrees();
            InputImage inputImage = InputImage.fromMediaImage(mediaImage, rotation);

            scanner.process(inputImage)
                    .addOnSuccessListener(barcodes -> {
                        for (Barcode barcode : barcodes) {
                            String raw = barcode.getRawValue();
                            if (raw != null && !raw.isEmpty()) {
                                // Return result to calling activity
                                Intent result = new Intent();
                                result.putExtra("barcode", raw);
                                setResult(Activity.RESULT_OK, result);
                                finish();
                                break;
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Log or ignore errors
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        } else {
            imageProxy.close();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scanner != null) scanner.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}


//Used Gemini AI for Genarations and Error Handlings
