package com.belkin.linker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraX;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureConfig;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.core.PreviewConfig;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.model.Image;

import java.util.ArrayList;
import java.util.List;


public class CameraActivity extends AppCompatActivity {

    private final int REQUEST_CODE_PERMISSIONS = 42;
    private final String[] REQUIRED_PERMISSIONS = { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE };

    TextureView viewFinder;
    ImageView imgCaptureBtn;
    Button btnSelect;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        //ActionBar
        ActionBar ab = getSupportActionBar();
        ab.hide();

        viewFinder = (TextureView) findViewById(R.id.view_finder);
        if (allPermissionsGranted()) {
            viewFinder.post(this::startCamera);
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        viewFinder.addOnLayoutChangeListener((View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) -> updateTransform());

        imgCaptureBtn = (ImageView) findViewById(R.id.imgCaptureBtn);

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(v -> {
            ArrayList<Image> images = new ArrayList<>();
            ImagePicker.create(this)
                    //.returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                    .folderMode(true) // folder mode (false by default)
                    .toolbarFolderTitle("Folder") // folder selection title
                    .toolbarImageTitle("Tap to select") // image selection title
                    .toolbarArrowColor(Color.BLACK) // Toolbar 'up' arrow color
                    .includeVideo(false) // Show video on image picker
                    //.single() // single mode
                    .multi() // multi mode (default mode)
                    .limit(10) // max images can be selected (99 by default)
                    .showCamera(true) // show camera or not (true by default)
                    .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
                    .origin(images) // original selected images, used in multi mode
                    //.exclude(images) // exclude anything that in image.getPath()
                    //.excludeFiles(files) // same as exclude but using ArrayList<File>
                    //.theme(R.style.CustomImagePickerTheme) // must inherit ef_BaseTheme. please refer to sample
                    .enableLog(true) // disabling log
                    .start(); // start image picker activity with request code
        });
    }

    @Override
    protected void onActivityResult(int requestCode, final int resultCode, Intent data) {
        if (ImagePicker.shouldHandle(requestCode, resultCode, data)) {
            List<Image> images = ImagePicker.getImages(data);
            for (Image image : images) {
                Analyzer analyzer = new Analyzer(getActivity());
                analyzer.analyze(image.getPath());
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post(this::startCamera);
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }

    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getBaseContext(), permission) == PackageManager.PERMISSION_DENIED)
                return false;
        }
        return true;
    }


    private void startCamera() {
        CameraX.unbindAll();

        Rational aspectRatio = new Rational(viewFinder.getWidth(), viewFinder.getHeight());
        Size screen = new Size(viewFinder.getWidth(), viewFinder.getHeight());

        PreviewConfig config = new PreviewConfig.Builder().setTargetAspectRatio(aspectRatio).setTargetResolution(screen).build();
        Preview preview = new Preview(config);

        preview.setOnPreviewOutputUpdateListener(output -> {
            ViewGroup parent = (ViewGroup) viewFinder.getParent();
            parent.removeView(viewFinder);
            parent.addView(viewFinder, 0);
            viewFinder.setSurfaceTexture(output.getSurfaceTexture());
            updateTransform();
        });

        ImageCaptureConfig imageCaptureConfig = new ImageCaptureConfig.Builder().setCaptureMode(ImageCapture.CaptureMode.MAX_QUALITY)
                .setTargetRotation(getWindowManager().getDefaultDisplay().getRotation()).build();
        final ImageCapture imgCapture = new ImageCapture(imageCaptureConfig);
        imgCaptureBtn.setOnClickListener(v -> {
            imgCaptureBtn.setEnabled(false);
            imgCapture.takePicture(new ImageCapture.OnImageCapturedListener() {
                @Override
                public void onCaptureSuccess(ImageProxy image, int rotationDegrees) {
                    Analyzer analyzer = new Analyzer(getActivity());
                    analyzer.analyze(image, rotationDegrees);
                    imgCaptureBtn.setEnabled(true);
                    //if (error with download ask user to update it's google play services)
                }
            });

        });

        CameraX.bindToLifecycle(this, preview, imgCapture);
    }

    private Activity getActivity() {
        return this;
    }

    private void updateTransform() {
        Matrix mx = new Matrix();
        float w = viewFinder.getMeasuredWidth();
        float h = viewFinder.getMeasuredHeight();

        float cX = w / 2f;
        float cY = h / 2f;
        int rotationDgr;
        int rotation = (int) viewFinder.getRotation();

        switch (rotation) {
            case Surface.ROTATION_0:
                rotationDgr = 0;
                break;
            case Surface.ROTATION_90:
                rotationDgr = 90;
                break;
            case Surface.ROTATION_180:
                rotationDgr = 180;
                break;
            case Surface.ROTATION_270:
                rotationDgr = 270;
                break;
            default:
                return;
        }

        if (isEmulator())
            rotationDgr -= 90;

        mx.postRotate((float) rotationDgr, cX, cY);
        viewFinder.setTransform(mx);

    }

    public static boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT);
    }
}
