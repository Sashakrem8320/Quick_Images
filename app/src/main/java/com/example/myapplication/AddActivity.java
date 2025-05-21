package com.example.myapplication;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class AddActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST = 1;
    private static final int GALLERY_REQUEST = 2;
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private ImageView imageView;
    private Bitmap imageBitmap;
    private int imageNumber;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add);

        imageView = findViewById(R.id.imageView);
        ImageButton buttonCamera = findViewById(R.id.camera);
        ImageButton buttonGallery = findViewById(R.id.galar);
        ImageButton buttonReset = findViewById(R.id.revers);
        ImageButton buttonNo = findViewById(R.id.no);
        ImageButton buttonOk = findViewById(R.id.ok);

        imageNumber = getIntent().getIntExtra("imageNumber", 1);

        buttonCamera.setOnClickListener(v -> checkCameraPermissionAndOpenCamera());
        buttonGallery.setOnClickListener(v -> openGallery());
        buttonReset.setOnClickListener(v -> resetImage());
        buttonOk.setOnClickListener(v -> saveImageAndClose());
        buttonNo.setOnClickListener(v -> closeWithoutSaving());

        checkPermissions();
    }

    private void checkCameraPermissionAndOpenCamera() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        } else {
            openCamera();
        }
    }

    private void saveImageAndClose() {
        if (currentPhotoPath != null) {
            Intent intent = new Intent();
            intent.putExtra("imagePath", currentPhotoPath);
            intent.putExtra("imageNumber", imageNumber);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, "Please select an image first", Toast.LENGTH_SHORT).show();
        }
    }

    private void closeWithoutSaving() {
        finish();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.myapplication.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, CAMERA_REQUEST);
            }
        }
    }

    private File createImageFile() throws IOException {
        String imageFileName = "JPEG_" + imageNumber + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        if (storageDir == null) {
            throw new IOException("Storage directory not found");
        }
        File image = new File(storageDir, imageFileName + ".jpg");
        if (image.exists()) {
            image.delete();
        }
        boolean created = image.createNewFile();
        if (!created) {
            throw new IOException("Failed to create image file");
        }
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST) {
                setPic();
            } else if (requestCode == GALLERY_REQUEST) {
                Uri selectedImage = data.getData();
                if (selectedImage != null) {
                    try {
                        imageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                        imageView.setImageBitmap(imageBitmap);
                        currentPhotoPath = saveBitmapToFile(imageBitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private void setPic() {
        int targetW = imageView.getWidth();
        int targetH = imageView.getHeight();
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor = 1;
        if (targetW > 0 && targetH > 0) {
            scaleFactor = Math.min(photoW / targetW, photoH / targetH);
            if (scaleFactor < 1) scaleFactor = 1;
        }
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;
        imageBitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        imageView.setImageBitmap(imageBitmap);
    }

    private void resetImage() {
        imageView.setImageDrawable(null);
        imageBitmap = null;
        currentPhotoPath = null;
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String saveBitmapToFile(Bitmap bitmap) {
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, "GALLERY_" + imageNumber + ".jpg");
        try (FileOutputStream out = new FileOutputStream(image)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
            return image.getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
