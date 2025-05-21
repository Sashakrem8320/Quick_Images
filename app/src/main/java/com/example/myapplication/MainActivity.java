package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private boolean isLayoutOne = true;
    private Map<Integer, String> imagePaths = new HashMap<>();
    private EditText editText;
    private SharedPreferences sharedPreferences;
    private int currentImageNumber;

    // Новые поля
    private RecyclerView recyclerView;
    private ImageAdapter imageAdapter;
    private List<String> imagePathList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_one);

        recyclerView = findViewById(R.id.recyclerViewImages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        imageAdapter = new ImageAdapter(this, imagePathList);
        recyclerView.setAdapter(imageAdapter);

        ImageButton addButton = findViewById(R.id.addButton);
        ImageButton switchButton = findViewById(R.id.imageButtonSwitch);
        sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        currentImageNumber = sharedPreferences.getInt("currentImageNumber", 1);
        loadSavedImages();

        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddActivity.class);
            intent.putExtra("imageNumber", currentImageNumber);
            startActivityForResult(intent, 1);
        });

        switchButton.setOnClickListener(v -> switchLayout());
    }

    private void initializeLayoutTwo() {
        editText = findViewById(R.id.editText);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(v -> displayImage());
    }

    private void switchLayout() {
        if (isLayoutOne) {
            setContentView(R.layout.layout_two);
            initializeLayoutTwo();
        } else {
            setContentView(R.layout.layout_one);

            recyclerView = findViewById(R.id.recyclerViewImages);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
            imageAdapter = new ImageAdapter(this, imagePathList);
            recyclerView.setAdapter(imageAdapter);

            ImageButton addButton = findViewById(R.id.addButton);
            addButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, AddActivity.class);
                intent.putExtra("imageNumber", currentImageNumber);
                startActivityForResult(intent, 1);
            });

            ImageButton switchButton = findViewById(R.id.imageButtonSwitch);
            switchButton.setOnClickListener(v -> switchLayout());
        }
        isLayoutOne = !isLayoutOne;

        ImageButton switchButton = findViewById(R.id.imageButtonSwitch);
        if (switchButton != null) {
            switchButton.setOnClickListener(v -> switchLayout());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            String imagePath = data.getStringExtra("imagePath");
            int imageNumber = data.getIntExtra("imageNumber", 0);
            imagePaths.put(imageNumber, imagePath);
            imagePathList.add(imagePath);
            imageAdapter.notifyDataSetChanged();

            Toast.makeText(this, "Image saved with number: " + imageNumber, Toast.LENGTH_SHORT).show();

            currentImageNumber++;
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("currentImageNumber", currentImageNumber);
            editor.apply();
        }
    }

    private void displayImage() {
        try {
            int imageNumber = Integer.parseInt(editText.getText().toString());
            String imagePath = imagePaths.get(imageNumber);
            if (imagePath != null) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 2; // уменьшить размер для загрузки
                ImageView displayedImage = findViewById(R.id.displayedImage);
                displayedImage.setImageBitmap(BitmapFactory.decodeFile(imagePath, options));
            } else {
                Toast.makeText(this, "Image not found", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid image number", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadSavedImages() {
        imagePaths.clear();
        imagePathList.clear();

        for (int i = 1; i < currentImageNumber; i++) {
            File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            if (storageDir != null) {
                File imageFile = new File(storageDir, "JPEG_" + i + "_.jpg");
                if (!imageFile.exists()) {
                    imageFile = new File(storageDir, "GALLERY_" + i + ".jpg");
                }
                if (imageFile.exists()) {
                    imagePaths.put(i, imageFile.getAbsolutePath());
                    imagePathList.add(imageFile.getAbsolutePath());
                }
            }
        }
        imageAdapter.notifyDataSetChanged();
    }
}
