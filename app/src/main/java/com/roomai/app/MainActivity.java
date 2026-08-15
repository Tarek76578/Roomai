package com.roomai.app;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;
import java.io.*;
import java.net.*;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private static final int CAMERA_REQUEST = 101;
    private static final int GALLERY_REQUEST = 102;
    private static final int CAMERA_PERMISSION = 103;

    private ImageView roomImage;
    private Spinner roomTypeSpinner;
    private Spinner styleSpinner;
    private EditText requestInput;
    private Uri selectedImageUri;

    private static final String BACKEND =
            "https://roomai-wagl.onrender.com/generate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        roomImage = findViewById(R.id.roomImage);
        roomTypeSpinner = findViewById(R.id.roomTypeSpinner);
        styleSpinner = findViewById(R.id.styleSpinner);
        requestInput = findViewById(R.id.requestInput);

        Button uploadButton = findViewById(R.id.uploadButton);
        Button designButton = findViewById(R.id.designButton);
        Button downloadButton = findViewById(R.id.downloadButton);
        Button deleteButton = findViewById(R.id.deleteButton);
        Button menuButton = findViewById(R.id.menuButton);

        String[] rooms = {
                "Living Room", "Bedroom", "Kitchen",
                "Bathroom", "Office", "Dining Room"
        };

        String[] styles = {
                "Modern", "Minimalist", "Scandinavian",
                "Luxury", "Industrial", "Bohemian", "Classic"
        };

        roomTypeSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                rooms
        ));

        styleSpinner.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                styles
        ));

        uploadButton.setOnClickListener(v -> showImageOptions());

        designButton.setOnClickListener(v -> generateDesign());

        downloadButton.setOnClickListener(v -> saveCurrentImage());

        deleteButton.setOnClickListener(v -> deleteCurrentImage());

        menuButton.setOnClickListener(v -> showMenu());

        setupCard(R.id.modernCard, "Modern", true);
        setupCard(R.id.luxuryCard, "Luxury", true);
        setupCard(R.id.scandiCard, "Scandinavian", true);
        setupCard(R.id.bedroomCard, "Bedroom", false);
        setupCard(R.id.kitchenCard, "Kitchen", false);
        setupCard(R.id.livingCard, "Living Room", false);
    }

    private void setupCard(int id, String value, boolean style) {
        View card = findViewById(id);

        if (card == null) return;

        card.setOnClickListener(v -> {
            if (style) {
                selectStyle(value);
            } else {
                selectRoom(value);
            }
        });
    }

    private void showMenu() {
        String[] items = {
                "History",
                "AI Enhance",
                "Furniture & Product Search",
                "Settings",
                "About RoomAI"
        };

        new AlertDialog.Builder(this)
                .setTitle("RoomAI Menu")
                .setItems(items, (dialog, which) -> {

                    switch (which) {
                        case 0:
                            Toast.makeText(
                                    this,
                                    "History will contain your saved designs.",
                                    Toast.LENGTH_LONG
                            ).show();
                            break;

                        case 1:
                            Toast.makeText(
                                    this,
                                    "AI Enhance is prepared for a future AI enhancement endpoint.",
                                    Toast.LENGTH_LONG
                            ).show();
                            break;

                        case 2:
                            showProductSearch();
                            break;

                        case 3:
                            showSettings();
                            break;

                        case 4:
                            showAbout();
                            break;
                    }
                })
                .show();
    }

    private void showProductSearch() {
        EditText input = new EditText(this);
        input.setHint("Example: modern sofa");

        new AlertDialog.Builder(this)
                .setTitle("Furniture & Product Search")
                .setMessage("Search for furniture or decoration.")
                .setView(input)
                .setPositiveButton("Search", (dialog, which) -> {

                    String query = input.getText().toString().trim();

                    if (query.isEmpty()) {
                        Toast.makeText(
                                this,
                                "Enter a product name.",
                                Toast.LENGTH_SHORT
                        ).show();
                        return;
                    }

                    openProductSearch(query);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void openProductSearch(String query) {
        try {
            String encoded = URLEncoder.encode(query, "UTF-8");

            String url =
                    "https://www.amazon.com/s?k=" + encoded;

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(url)
            );

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                    this,
                    "Unable to open product search.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void showSettings() {
        String[] options = {
                "Light Mode",
                "Dark Mode"
        };

        new AlertDialog.Builder(this)
                .setTitle("Appearance")
                .setItems(options, (dialog, which) -> {

                    if (which == 1) {
                        getWindow().getDecorView()
                                .setSystemUiVisibility(0);

                        Toast.makeText(
                                this,
                                "Dark mode selected.",
                                Toast.LENGTH_SHORT
                        ).show();
                    } else {
                        Toast.makeText(
                                this,
                                "Light mode selected.",
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                })
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("RoomAI")
                .setMessage(
                        "AI Interior Designer\n\n" +
                        "Turn your room photos into beautiful interior concepts with AI.\n\n" +
                        "RoomAI V3"
                )
                .setPositiveButton("OK", null)
                .show();
    }

    private void generateDesign() {

        if (selectedImageUri == null) {
            Toast.makeText(
                    this,
                    "Please add a room photo first.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        String room =
                roomTypeSpinner.getSelectedItem().toString();

        String style =
                styleSpinner.getSelectedItem().toString();

        String customRequest =
                requestInput.getText().toString().trim();

        Toast.makeText(
                this,
                "Generating your design...",
                Toast.LENGTH_LONG
        ).show();

        new Thread(() -> {

            try {

                InputStream input =
                        getContentResolver()
                                .openInputStream(selectedImageUri);

                ByteArrayOutputStream buffer =
                        new ByteArrayOutputStream();

                byte[] data = new byte[8192];

                int n;

                while ((n = input.read(data)) != -1) {
                    buffer.write(data, 0, n);
                }

                input.close();

                String boundary =
                        "----RoomAI" + System.currentTimeMillis();

                URL url = new URL(BACKEND);

                HttpURLConnection conn =
                        (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                conn.setConnectTimeout(30000);
                conn.setReadTimeout(180000);

                conn.setRequestProperty(
                        "Content-Type",
                        "multipart/form-data; boundary=" + boundary
                );

                OutputStream output =
                        conn.getOutputStream();

                writeField(
                        output,
                        boundary,
                        "room",
                        room
                );

                writeField(
                        output,
                        boundary,
                        "style",
                        style
                );

                writeField(
                        output,
                        boundary,
                        "request",
                        customRequest
                );

                output.write((
                        "--" + boundary + "\r\n" +
                        "Content-Disposition: form-data; " +
                        "name=\"image\"; filename=\"room.jpg\"\r\n" +
                        "Content-Type: image/jpeg\r\n\r\n"
                ).getBytes("UTF-8"));

                output.write(buffer.toByteArray());

                output.write((
                        "\r\n--" + boundary + "--\r\n"
                ).getBytes("UTF-8"));

                output.flush();
                output.close();

                int code =
                        conn.getResponseCode();

                InputStream responseStream =
                        code >= 400
                                ? conn.getErrorStream()
                                : conn.getInputStream();

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(
                                        responseStream
                                )
                        );

                StringBuilder result =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    result.append(line);
                }

                reader.close();
                conn.disconnect();

                JSONObject json =
                        new JSONObject(result.toString());

                if (!json.has("image_url")) {

                    String error =
                            json.optString(
                                    "error",
                                    "Unknown server error"
                            );

                    runOnUiThread(() ->
                            Toast.makeText(
                                    this,
                                    "Generation failed: " + error,
                                    Toast.LENGTH_LONG
                            ).show()
                    );

                    return;
                }

                String imageUrl =
                        json.getString("image_url");

                runOnUiThread(() ->
                        loadResult(imageUrl)
                );

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Connection error: " +
                                        e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }

    private void writeField(
            OutputStream output,
            String boundary,
            String name,
            String value
    ) throws Exception {

        output.write((
                "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; " +
                "name=\"" + name + "\"\r\n\r\n" +
                value + "\r\n"
        ).getBytes("UTF-8"));
    }

    private void loadResult(String imageUrl) {

        Toast.makeText(
                this,
                "Loading result...",
                Toast.LENGTH_SHORT
        ).show();

        new Thread(() -> {

            try {

                URL url =
                        new URL(imageUrl);

                HttpURLConnection conn =
                        (HttpURLConnection)
                                url.openConnection();

                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);

                InputStream input =
                        conn.getInputStream();

                Bitmap bitmap =
                        android.graphics.BitmapFactory
                                .decodeStream(input);

                input.close();
                conn.disconnect();

                runOnUiThread(() -> {

                    if (bitmap == null) {
                        Toast.makeText(
                                this,
                                "Unable to decode image.",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }

                    roomImage.setImageBitmap(bitmap);
                    roomImage.setVisibility(View.VISIBLE);

                    selectedImageUri =
                            saveBitmapTemporarily(bitmap);

                    Button download =
                            findViewById(
                                    R.id.downloadButton
                            );

                    Button delete =
                            findViewById(
                                    R.id.deleteButton
                            );

                    download.setVisibility(View.VISIBLE);
                    delete.setVisibility(View.VISIBLE);

                    Toast.makeText(
                            this,
                            "Design generated successfully!",
                            Toast.LENGTH_LONG
                    ).show();
                });

            } catch (Exception e) {

                runOnUiThread(() ->
                        Toast.makeText(
                                this,
                                "Could not load result.",
                                Toast.LENGTH_LONG
                        ).show()
                );
            }

        }).start();
    }

    private Uri saveBitmapTemporarily(Bitmap bitmap) {

        try {

            ContentValues values =
                    new ContentValues();

            values.put(
                    MediaStore.Images.Media.DISPLAY_NAME,
                    "RoomAI_result_" +
                            System.currentTimeMillis() +
                            ".jpg"
            );

            values.put(
                    MediaStore.Images.Media.MIME_TYPE,
                    "image/jpeg"
            );

            values.put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    Environment.DIRECTORY_PICTURES +
                            "/RoomAI"
            );

            Uri uri =
                    getContentResolver().insert(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            values
                    );

            if (uri != null) {

                OutputStream output =
                        getContentResolver()
                                .openOutputStream(uri);

                bitmap.compress(
                        Bitmap.CompressFormat.JPEG,
                        95,
                        output
                );

                output.close();
            }

            return uri;

        } catch (Exception e) {
            return null;
        }
    }

    private void saveCurrentImage() {

        if (roomImage.getDrawable() == null) {
            Toast.makeText(
                    this,
                    "No generated image.",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        Toast.makeText(
                this,
                "Image is saved in Pictures/RoomAI.",
                Toast.LENGTH_LONG
        ).show();
    }

    private void deleteCurrentImage() {

        roomImage.setImageDrawable(null);
        roomImage.setVisibility(View.GONE);

        Button download =
                findViewById(R.id.downloadButton);

        Button delete =
                findViewById(R.id.deleteButton);

        download.setVisibility(View.GONE);
        delete.setVisibility(View.GONE);

        selectedImageUri = null;

        Toast.makeText(
                this,
                "Image removed.",
                Toast.LENGTH_SHORT
        ).show();
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

        new AlertDialog.Builder(this)
                .setTitle("Add Room Photo")
                .setItems(
                        options,
                        (dialog, which) -> {

                            if (which == 0) {
                                openCamera();
                            } else {
                                openGallery();
                            }
                        }
                )
                .show();
    }

    private void openCamera() {

        if (checkSelfPermission(
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.CAMERA
                    },
                    CAMERA_PERMISSION
            );

            return;
        }

        Intent intent =
                new Intent(
                        MediaStore.ACTION_IMAGE_CAPTURE
                );

        if (intent.resolveActivity(
                getPackageManager()
        ) != null) {

            startActivityForResult(
                    intent,
                    CAMERA_REQUEST
            );

        } else {

            Toast.makeText(
                    this,
                    "Camera is not available.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void openGallery() {

        Intent intent =
                new Intent(
                        Intent.ACTION_OPEN_DOCUMENT
                );

        intent.setType("image/*");

        intent.addCategory(
                Intent.CATEGORY_OPENABLE
        );

        startActivityForResult(
                intent,
                GALLERY_REQUEST
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        try {

            if (requestCode == CAMERA_REQUEST) {

                Bitmap bitmap =
                        (Bitmap)
                                data.getExtras()
                                        .get("data");

                if (bitmap != null) {

                    roomImage.setImageBitmap(bitmap);

                    selectedImageUri =
                            saveBitmapTemporarily(bitmap);

                    roomImage.setVisibility(
                            View.VISIBLE
                    );
                }

            } else if (
                    requestCode == GALLERY_REQUEST
            ) {

                Uri imageUri =
                        data.getData();

                if (imageUri != null) {

                    selectedImageUri =
                            imageUri;

                    roomImage.setImageURI(
                            imageUri
                    );

                    roomImage.setVisibility(
                            View.VISIBLE
                    );
                }
            }

        } catch (Exception e) {

            Toast.makeText(
                    this,
                    "Unable to load image.",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode ==
                CAMERA_PERMISSION) {

            if (grantResults.length > 0 &&
                    grantResults[0] ==
                            PackageManager.PERMISSION_GRANTED) {

                openCamera();

            } else {

                Toast.makeText(
                        this,
                        "Camera permission is required.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        }
    }
}
