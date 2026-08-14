package com.roomai.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int CAMERA_REQUEST = 101;
    private static final int GALLERY_REQUEST = 102;
    private static final int CAMERA_PERMISSION = 103;

    private ImageView roomImage;
    private Spinner roomTypeSpinner;
    private Spinner styleSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            setContentView(R.layout.activity_main);

        Button uploadButton = findViewById(R.id.uploadButton);
        Button designButton = findViewById(R.id.designButton);

        roomTypeSpinner = findViewById(R.id.roomTypeSpinner);
        styleSpinner = findViewById(R.id.styleSpinner);

        roomImage = findViewById(R.id.roomImage);

        String[] roomTypes = {
                "Living Room",
                "Bedroom",
                "Kitchen",
                "Bathroom",
                "Office",
                "Dining Room"
        };

        String[] styles = {
                "Modern",
                "Minimalist",
                "Scandinavian",
                "Luxury",
                "Industrial",
                "Bohemian",
                "Classic"
        };

        roomTypeSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        roomTypes
                )
        );

        styleSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        styles
                )
        );

        uploadButton.setOnClickListener(v -> showImageOptions());

        LinearLayout modernCard = findViewById(R.id.modernCard);
        LinearLayout luxuryCard = findViewById(R.id.luxuryCard);
        LinearLayout scandiCard = findViewById(R.id.scandiCard);
        LinearLayout bedroomCard = findViewById(R.id.bedroomCard);
        LinearLayout kitchenCard = findViewById(R.id.kitchenCard);
        LinearLayout livingCard = findViewById(R.id.livingCard);

        modernCard.setOnClickListener(v -> selectStyle("Modern"));
        luxuryCard.setOnClickListener(v -> selectStyle("Luxury"));
        scandiCard.setOnClickListener(v -> selectStyle("Minimalist"));

        bedroomCard.setOnClickListener(v -> selectRoom("Bedroom"));
        kitchenCard.setOnClickListener(v -> selectRoom("Kitchen"));
        livingCard.setOnClickListener(v -> selectRoom("Living Room"));

        designButton.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "RoomAI: Generate Design pressed",
                    Toast.LENGTH_LONG
            ).show();
        });

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "RoomAI Error: " + e.getClass().getSimpleName() + " - " + e.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        }
    }

    private void selectRoom(String room) {
        String[] rooms = {
                "Living Room",
                "Bedroom",
                "Kitchen",
                "Bathroom",
                "Office",
                "Dining Room"
        };

        for (int i = 0; i < rooms.length; i++) {
            if (rooms[i].equals(room)) {
                roomTypeSpinner.setSelection(i);
                Toast.makeText(
                        this,
                        room + " selected",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }
    }

    private void selectStyle(String style) {
        String[] styles = {
                "Modern",
                "Minimalist",
                "Scandinavian",
                "Luxury",
                "Industrial",
                "Bohemian",
                "Classic"
        };

        for (int i = 0; i < styles.length; i++) {
            if (styles[i].equals(style)) {
                styleSpinner.setSelection(i);
                Toast.makeText(
                        this,
                        style + " selected",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }
        }
    }

    private void showImageOptions() {
        String[] options = {
                "Take Photo",
                "Choose from Gallery"
        };

        new android.app.AlertDialog.Builder(this)
                .setTitle("Add Room Photo")
                .setItems(options, (dialog, which) -> {

                    if (which == 0) {
                        openCamera();
                    } else {
                        openGallery();
                    }

                })
                .show();
    }

    private void openCamera() {

        if (checkSelfPermission(Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION
            );

            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(
                    this,
                    "Camera is not available",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openGallery() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        startActivityForResult(intent, GALLERY_REQUEST);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK || data == null) {
            return;
        }

        try {

            if (requestCode == CAMERA_REQUEST) {

                Bitmap bitmap =
                        (Bitmap) data.getExtras().get("data");

                if (bitmap != null) {
                    roomImage.setImageBitmap(bitmap);
                    roomImage.setVisibility(View.VISIBLE);
                }

            } else if (requestCode == GALLERY_REQUEST) {

                Uri imageUri = data.getData();

                if (imageUri != null) {
                    roomImage.setImageURI(imageUri);
                    roomImage.setVisibility(View.VISIBLE);
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to load image",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == CAMERA_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                openCamera();

            } else {

                Toast.makeText(
                        this,
                        "Camera permission is required",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}
