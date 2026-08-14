package com.roomai.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int PICK_IMAGE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Button uploadButton = findViewById(R.id.uploadButton);
        Button designButton = findViewById(R.id.designButton);

        Spinner roomTypeSpinner = findViewById(R.id.roomTypeSpinner);
        Spinner styleSpinner = findViewById(R.id.styleSpinner);

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

        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_IMAGE);
        });

        designButton.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "AI design feature coming soon!",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE &&
                resultCode == RESULT_OK &&
                data != null) {

            Uri imageUri = data.getData();

            Toast.makeText(
                    this,
                    "Room photo selected!",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }
}
