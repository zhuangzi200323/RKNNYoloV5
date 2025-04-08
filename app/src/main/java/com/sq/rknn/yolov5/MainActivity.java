package com.sq.rknn.yolov5;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;
import com.sq.rknn.yolov5.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "Yolo_Detect";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};
    private ActivityMainBinding binding;
    private ExecutorService backgroundExecutor = Executors.newCachedThreadPool();// newSingleThreadExecutor();
    private List<String> labelData;
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        labelData = readLabels();

        paint.setColor(Color.BLACK);
        paint.setTextSize(20f);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER));

        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }

    private void startCamera() {
        ListenableFuture cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = (ProcessCameraProvider) cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        //.setTargetRotation(Surface.ROTATION_180) //120cm的机器3399上，需要旋转
                        .build();

                preview.setSurfaceProvider(binding.viewFinder.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                cameraProvider.unbindAll();

                cameraProvider.bindToLifecycle(MainActivity.this, cameraSelector, preview, imageCapture, imageAnalysis);

                setImgAnalyzer();
            } catch (Exception exc) {
                Log.e(TAG, "Use case binding failed", exc);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void updateUI(ImgAnalyzer.Result result) {
        runOnUiThread(() -> {
            binding.inferenceTimeValue.setText(result.processTimeMs + "ms");
            showBitmap(result);
            for (float[] boxInfo : result.outputBox) {
                binding.detectedItem1.setText(String.format("%s", labelData.get((int) boxInfo[5])));
                binding.detectedItemValue1.setText(String.format("%.2f", boxInfo[4]));
            }
        });
    }
    Paint paint = new Paint();

    private void showBitmap(ImgAnalyzer.Result result) {
        if (result.outputBitmap == null) return;

        Bitmap mutableBitmap = result.outputBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);


        canvas.drawBitmap(mutableBitmap, 0.0f, 0.0f, paint);
        for (float[] boxInfo : result.outputBox) {
            canvas.drawText(String.format("%s:%.2f", labelData.get((int) boxInfo[5]), boxInfo[4]),
                    boxInfo[0] - boxInfo[2] / 2, boxInfo[1] - boxInfo[3] / 2, paint);
        }
        binding.resultIv.setImageBitmap(mutableBitmap);
    }

    private List<String> readLabels() {
        List<String> labels = new ArrayList<>();
//        try {
//            InputStream inputStream = getResources().openRawResource(R.raw.yolo_classes);
//            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
//            String line;
//            while ((line = bufferedReader.readLine()) != null) {
//                labels.add(line);
//            }
//            inputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        return labels;
    }

//    private byte[] readModel() {
//        try {
//            int modelID = R.raw.yolov5n_with_pre_post_processing;
//            InputStream inputStream = getResources().openRawResource(modelID);
//            byte[] buffer = new byte[inputStream.available()];
//            inputStream.read(buffer);
//            inputStream.close();
//            return buffer;
//        } catch (IOException e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    private void setImgAnalyzer() {
        imageAnalysis.clearAnalyzer();
        imageAnalysis.setAnalyzer(backgroundExecutor, new ImgAnalyzer(this::updateUI));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        backgroundExecutor.shutdown();
    }
}