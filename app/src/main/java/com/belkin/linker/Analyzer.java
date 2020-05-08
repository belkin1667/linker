package com.belkin.linker;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.net.Uri;
import android.util.Log;
import android.widget.Toast;

import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionImageMetadata;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Analyzer implements ImageAnalysis.Analyzer {

    private final static String CLASS_LOG_TAG = "Analyzer";

    /**
     * OCR ML model is downloaded to user device
     * Free
     */
    final int LOCAL_MODEL = 0;

    /**
     * OCR ML model is in cloud
     * Need payments
     *
     * @see "https://console.firebase.google.com/u/0/project/linker-6ef34/overview"
     */
    final int CLOUD_MODEL = 1;

    private int model = LOCAL_MODEL;

    void useLocalModel() {
        Log.i(CLASS_LOG_TAG, "useLocalModel() method call");
        model = LOCAL_MODEL;
    }

    void useCloudModel() {
        Log.i(CLASS_LOG_TAG, "useCloudModel() method call");
        model = CLOUD_MODEL;
    }

    private Context context;
    private Activity activity;

    Analyzer(Activity activity) {
        super();
        this.activity = activity;
        this.context = activity.getBaseContext();
    }

    private int degreesToFirebaseRotation(int rotationDegrees) {
        Log.i(CLASS_LOG_TAG, "degreesToFirebaseRotation() method call");
        switch (rotationDegrees) {
            case 0:
                return FirebaseVisionImageMetadata.ROTATION_0;
            case 90:
                return FirebaseVisionImageMetadata.ROTATION_90;
            case 180:
                return FirebaseVisionImageMetadata.ROTATION_180;
            case 270:
                return FirebaseVisionImageMetadata.ROTATION_270;
            default:
                Log.e(CLASS_LOG_TAG, "Illegal argument! rotationDegrees = " + rotationDegrees + " Rotation must be 0, 90, 180, or 270.");
                throw new IllegalArgumentException(
                        "Rotation must be 0, 90, 180, or 270.");
        }
    }

    @Override
    public void analyze(ImageProxy imageProxy, int degrees) {
        Log.i(CLASS_LOG_TAG, "analyze(ImageProxy, int) method call");

        if (imageProxy == null || imageProxy.getImage() == null) {
            return;
        }

        int rotation = degreesToFirebaseRotation(degrees);
        Image mediaImage = imageProxy.getImage();
        FirebaseVisionImage image = FirebaseVisionImage.fromMediaImage(mediaImage, rotation);
        analyze(image);

    }

    public void analyze(String imagePath) {
        try {
            //FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(context, Uri.parse(imagePath));
            FirebaseVisionImage image = FirebaseVisionImage.fromFilePath(context, Uri.fromFile(new File(imagePath)));
            analyze(image);
        } catch (IOException e) {
            Log.e(CLASS_LOG_TAG, "Failed to analyze image from path " + imagePath + ". Cause: " + e.getMessage());
        }
    }

    public void analyze(FirebaseVisionImage image) {
        FirebaseVisionTextRecognizer detector;

        if (model == LOCAL_MODEL) {
            detector = FirebaseVision.getInstance().getOnDeviceTextRecognizer();
            Log.i(CLASS_LOG_TAG, "Using local model");
        } else if (model == CLOUD_MODEL) {
            detector = FirebaseVision.getInstance().getCloudTextRecognizer();
            Log.i(CLASS_LOG_TAG, "Using cloud model");
        } else
            return;

        //Task<FirebaseVisionText> result = ..
        detector.processImage(image)
                .addOnSuccessListener(firebaseVisionText -> { // Task completed successfully
                    Log.i(CLASS_LOG_TAG, "Recognition completed successfully");
                    List<String> urls = getUrls(firebaseVisionText.getText());
                    if (urls.isEmpty()) {
                        String msg = "Text recognized, however links were not detected. Please, try again";
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                        Log.i(CLASS_LOG_TAG, "Links were not detected. Make sure it starts with 'https://', 'http://' or 'ftp://");
                    } else {
                        DataBase.addNewLink(urls);

                        for (String url : urls) {
                            Toast.makeText(context, url, Toast.LENGTH_SHORT).show();
                            Log.i(CLASS_LOG_TAG, "URL detected: " + url);
                        }

                        if (urls.size() == 1) {
                            //String msg = "Url recognized. You can access it from the main page";
                            //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                            Log.i(CLASS_LOG_TAG, "Only one URL recognized. Opening the browser");
                        } else {
                            //String msg = "Multiple urls recognized. You can access them from the main page";
                            //Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                            Log.i(CLASS_LOG_TAG, "Multiple urls recognized. Going back to MainActivity");
                        }
                        Thread thread = new Thread(() -> {
                            try {
                                Thread.sleep(1500);
                            } catch (InterruptedException e) {
                                Log.e(CLASS_LOG_TAG, e.getMessage());
                            }
                            activity.finish();
                        });
                        thread.start();

                    }
                })
                .addOnFailureListener(e -> { // Task failed with an exception
                    Log.i(CLASS_LOG_TAG, "Recognition failed");
                    Log.e(CLASS_LOG_TAG, Objects.requireNonNull(e.getMessage()));

                    String msg = "Text recognition failed\n";
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                });

    }




    /**
     * We call that on startup with hope the needed model will be downloaded as soon as possible;
     * It is used to prevent: "Waiting for the text recognition model to be downloaded. Please wait."
     * exception when recognizing.
     */
    public static void warmUp() {
        Log.i(CLASS_LOG_TAG, "warmUp() method call");
        Bitmap image = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
        image.eraseColor(android.graphics.Color.GREEN);
        FirebaseVisionImage firebaseVisionImage = FirebaseVisionImage.fromBitmap(image);
        FirebaseVision.getInstance().getOnDeviceTextRecognizer()
                .processImage(firebaseVisionImage)
                .addOnSuccessListener(null)
                .addOnFailureListener(null);
    }

    public List<String> getUrls(String src) {
        Log.i(CLASS_LOG_TAG, "getUrls(String) method call");
        List<String> result = new ArrayList<>();
        String regex = "(http|ftp|https)://([\\w+?\\.\\w+])+([a-zA-Z0-9\\~\\!\\@\\#\\$\\%\\^\\&\\*\\(\\)_\\-\\=\\+\\\\\\/\\?\\.\\:\\;\\'\\,]*)?";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(src);
        while (matcher.find()) {
            String url = src.substring(matcher.start(), matcher.end());
            result.add(url);
        }
        return result;
    }

}