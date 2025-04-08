package com.sq.rknn.yolov5;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;
import android.os.AsyncTask;
import android.os.SystemClock;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;

public class ImgAnalyzer implements ImageAnalysis.Analyzer {

    public static class Result {
        public Bitmap outputBitmap;
        public long processTimeMs;
        public float[][] outputBox;

        public Result(long l, Bitmap outputImageBitmap, float[][] boxOutput) {
            this.processTimeMs = l;
            this.outputBitmap = outputImageBitmap;
            this.outputBox = boxOutput;
        }
    }

    private final Callback callBack;

    Set<String> resultSet = new HashSet<>();

    public ImgAnalyzer(Callback callBack) {
        this.callBack = callBack;

        resultSet.add("image_out");
        resultSet.add("scaled_box_out_next");
    }

    public Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    @Override
    public void analyze(ImageProxy image) {

        AsyncTask.execute(() -> {
            Bitmap imgBitmap = image.toBitmap();
            Bitmap rawBitmap = Bitmap.createScaledBitmap(imgBitmap, 224, 224, false);
            Bitmap bitmap = rotateBitmap(rawBitmap, 90);
            if (bitmap != null) {
                long startTime = SystemClock.uptimeMillis();
                byte[] rawImageBytes = convertBitmapToByteArray(bitmap);

                long[] shape = {rawImageBytes.length};

//                try (OrtEnvironment env = OrtEnvironment.getEnvironment()) {
//                    try (OnnxTensor inputTensor = OnnxTensor.createTensor(
//                            env,
//                            ByteBuffer.wrap(rawImageBytes),
//                            shape,
//                            OnnxJavaType.UINT8)) {
//                        try (OrtSession.Result output = ortSession.run(Collections.singletonMap("image", inputTensor), resultSet)) {
//                            byte[] rawOutput = ((byte[]) output.get(0).getValue());
//                            float[][] boxOutput = (float[][]) output.get(1).getValue();
//                            callBack.invoke(new Result(SystemClock.uptimeMillis() - startTime, byteArrayToBitmap(rawOutput), boxOutput));
//                        }
//                    } catch (OrtException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
            }

            image.close();
        });

//        // Step 1: convert image into byte array (raw image bytes)
//        Bitmap imgBitmap = image.toBitmap();
//        Bitmap bitmap = Bitmap.createScaledBitmap(imgBitmap, 224, 224, false);
//        //Bitmap bitmap = rotateBitmap(rawBitmap, 90);
//        if (bitmap != null) {
//            long startTime = SystemClock.uptimeMillis();
//            byte[] rawImageBytes = convertBitmapToByteArray(bitmap);
//
//            // Step 2: get the shape of the byte array and make ort tensor
//            long[] shape = {rawImageBytes.length};
//
//            try (OrtEnvironment env = OrtEnvironment.getEnvironment()) {
//                try (OnnxTensor inputTensor = OnnxTensor.createTensor(
//                        env,
//                        ByteBuffer.wrap(rawImageBytes),
//                        shape,
//                        OnnxJavaType.UINT8)) {
//                    // Step 3: call ort inferenceSession run
//                    try (OrtSession.Result output = ortSession.run(Collections.singletonMap("image", inputTensor), resultSet)) {
//                        // Step 4: output analysis
//                        byte[] rawOutput = ((byte[]) output.get(1).getValue());
//                        float[][] boxOutput = (float[][]) output.get(0).getValue();
//                        // Step 5: set output result
//                        callBack.invoke(new Result(SystemClock.uptimeMillis() - startTime, byteArrayToBitmap(rawOutput), boxOutput));
//                    }
//                } catch (OrtException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//        }
//
//        image.close();
    }

    public byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream); // 可以选择其他格式，如JPEG
        return outputStream.toByteArray();
    }

    private Bitmap byteArrayToBitmap(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    @Override
    protected void finalize() {
    }

    public interface Callback {
        void invoke(Result result);
    }
}
