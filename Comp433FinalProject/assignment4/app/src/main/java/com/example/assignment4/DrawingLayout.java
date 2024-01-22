package com.example.assignment4;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;


import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.graphics.Bitmap;
import com.google.api.services.vision.v1.model.EntityAnnotation;


public class DrawingLayout extends AppCompatActivity {

    private ImageView imageView1, imageView2, imageView3;
    File imageDir;
    MyDrawingArea drawingArea;
    EditText tags;
    EditText findText;
    ListView mylist;
    private Bitmap drawnImage;
    private Image image;
    private String capturedTags;
    private String date3;

    private final String API_KEY = "AIzaSyDLZB52P135JbAdAlsnpDMC137TZR6kRuU";



    private SQLiteDatabase mydb;

    public void dataBaseSetUp() {
        mydb = this.openOrCreateDatabase("mydb", Context.MODE_PRIVATE, null);
        mydb.execSQL("CREATE TABLE IF NOT EXISTS DRAWINGS3 (timestamp TEXT PRIMARY KEY, image BLOB, date TEXT, tags TEXT, image_type TEXT)");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_layout);
        drawingArea = findViewById(R.id.customView);
        tags = findViewById(R.id.tags);
        findText = findViewById(R.id.findText);
        mylist = findViewById(R.id.mylist);
        dataBaseSetUp();

        // Show the latest images when the app starts
        showLatestImages();

    }

    private class VisionApiTask extends AsyncTask<Bitmap, Void, String> {
        @Override
        protected String doInBackground(Bitmap... bitmaps) {
            if (bitmaps.length == 0) {
                return "No image provided";
            }

            Bitmap drawnImage = bitmaps[0]; // Make sure drawnImage is a Bitmap

            // Encode the drawn image to an Image object
            Image image = convertBitmapToImage(drawnImage);

            try {
                List<String> tagsList = performNetworkRequest(image);

                if (!tagsList.isEmpty()) {
                    // Convert the list of tags to a comma-separated string
                    return TextUtils.join(", ", tagsList);
                } else {
                    return "No tags found";
                }
            } catch (IOException e) {
                e.printStackTrace();
                return "Error";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            // This method runs on the main (UI) thread and allows you to update the UI with the recognized tags.
            if (result != null) {
                // Update your UI with the result (e.g., set the captured tags to an EditText)
                tags.setText(result);

                // Set the recognized tags to the capturedTags variable
                capturedTags = result;

            } else {
                // Handle errors or display a message as needed
                tags.setText("Error or No tags available");
            }
        }
    }

    private Image convertBitmapToImage(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
        byte[] byteArray = stream.toByteArray();

        Image image = new Image();
        image.encodeContent(byteArray);

        return image;
    }



    private void showLatestImages() {
        // Query the database to retrieve the latest images
        Cursor cursor = mydb.rawQuery("SELECT tags, date, image FROM DRAWINGS3 ORDER BY timestamp DESC", null);

        List<ImageItem> imageItems = new ArrayList<>();

        while (cursor.moveToNext()) {
            String tags = cursor.getString(0);
            String date = cursor.getString(1);
            byte[] imageByteArray = cursor.getBlob(2);
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);

            imageItems.add(new ImageItem(tags, date, imageBitmap));
        }

        cursor.close();

        // Create a custom adapter and set it to the ListView
        ImageListAdapter adapter = new ImageListAdapter(this, R.layout.image_list_item, imageItems);
        mylist.setAdapter(adapter);
    }



    public void saveImageToDatabase(Bitmap image, String date, String tags) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] imageBytes = stream.toByteArray();

        ContentValues values = new ContentValues();
        values.put("image", imageBytes);
        values.put("date", date);
        values.put("tags", tags); // Store tags as a comma-separated list
        values.put("image_type", "sketch");  // Set the image_type value

        // Add timestamp with the current date and time
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date currentDate = new Date();
        String timestamp = dateFormat.format(currentDate);
        values.put("timestamp", timestamp);

        mydb.insert("DRAWINGS3", null, values);
    }

    private List<String> performNetworkRequest(Image myimage) throws IOException {
        try {
            // Build the Vision API client
            HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
            GsonFactory jsonFactory = GsonFactory.getDefaultInstance();
            Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
            builder.setVisionRequestInitializer(new VisionRequestInitializer(API_KEY));
            Vision vision = builder.build();

            // Prepare the AnnotateImageRequest
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
            annotateImageRequest.setImage(myimage);

            Feature feature = new Feature();
            feature.setType("LABEL_DETECTION");
            feature.setMaxResults(5); // Adjust the number of labels you want

            annotateImageRequest.setFeatures(Collections.singletonList(feature));

            // Create the BatchAnnotateImagesRequest
            BatchAnnotateImagesRequest batchAnnotateImagesRequest = new BatchAnnotateImagesRequest();
            batchAnnotateImagesRequest.setRequests(Collections.singletonList(annotateImageRequest));

            // Call the Vision.Images.Annotate method
            Vision.Images.Annotate annotate = vision.images().annotate(batchAnnotateImagesRequest);
            annotate.setDisableGZipContent(true);

            BatchAnnotateImagesResponse response = annotate.execute();
            Log.v("MYTAG", response.toPrettyString());
            List<String> recognizedTags = new ArrayList<>();

            if (response.getResponses() != null && !response.getResponses().isEmpty()) {
                List<EntityAnnotation> annotations = response.getResponses().get(0).getLabelAnnotations();

                for (EntityAnnotation annotation : annotations) {
                    if (annotation.getScore() >= 0.85) {
                        recognizedTags.add(annotation.getDescription());
                    }
                }

                if (recognizedTags.isEmpty() && !annotations.isEmpty()) {
                    // If there are no tags with 85% confidence, take the first tag
                    recognizedTags.add(annotations.get(0).getDescription());
                }
            }

            if (recognizedTags.isEmpty()) {
                recognizedTags.add("No labels found");
            }

            return recognizedTags;
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.singletonList("Error");
        }
    }





    public void submitPhoto(View view) {
        drawnImage = drawingArea.getBitmap();

        // Resize the drawn image to a smaller size
        int targetWidth = 200; // Set your desired width
        int targetHeight = 200; // Set your desired height
        Bitmap resizedImage = resizeBitmap(drawnImage, targetWidth, targetHeight);

        Date date = new Date();
        SimpleDateFormat date2 = new SimpleDateFormat("MMM dd, yyyy - ha", Locale.US);
        date3 = date2.format(date);

        // Check if the existing 'tags' EditText has text
        String manualTag = tags.getText().toString().trim();
        if (!manualTag.isEmpty()) {
            capturedTags = manualTag;
        } else {
            new VisionApiTask().execute(drawnImage); // Set the captured tags
        }

        // Save the drawn image and get its tags
        saveImageToDatabase(resizedImage, date3, capturedTags);
        // Show the latest images
        showLatestImages();
    }

    //Make the image smaller for drawing
    private Bitmap resizeBitmap(Bitmap source, int targetWidth, int targetHeight) {
        float scale = Math.min(
                (float) targetWidth / source.getWidth(),
                (float) targetHeight / source.getHeight()
        );
        int scaledWidth = Math.round(source.getWidth() * scale);
        int scaledHeight = Math.round(source.getHeight() * scale);
        return Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true);
    }


    // Modify the findPhoto method to retrieve the latest image with the specified tag
    public void findPhoto(View view) {
        String searchTags = findText.getText().toString().trim(); // Trim to handle empty and whitespace

        if (searchTags.isEmpty()) {
            // If the search tags are empty, show the last 3 images without a specific tag
            showLatestImages();
        } else {
            // If search tags are provided, split them by comma and search for images with those tags
            String[] searchTagArray = searchTags.split(",");
            List<String> searchTagList = new ArrayList<>(Arrays.asList(searchTagArray));

            // Remove leading/trailing spaces from each tag
            for (int i = 0; i < searchTagList.size(); i++) {
                searchTagList.set(i, searchTagList.get(i).trim());
            }

            // Now, search for images with any of the specified tags
            showLatestWithTags(searchTagList);
        }
    }



    // Create a new method to show the latest image with a specific tag
    private void showLatestWithTags(List<String> searchTags) {
        // Clear the current list of images
        mylist.setAdapter(null);

        // Reset TextViews and ImageViews

        // Create the SQL query to search for images with any of the specified tags
        StringBuilder queryBuilder = new StringBuilder("SELECT tags, date, image FROM DRAWINGS3 WHERE ");
        for (int i = 0; i < searchTags.size(); i++) {
            if (i > 0) {
                queryBuilder.append(" OR ");
            }
            queryBuilder.append("tags LIKE ?");
        }
        queryBuilder.append(" ORDER BY date DESC");

        String query = queryBuilder.toString();
        String[] selectionArgs = new String[searchTags.size()];
        for (int i = 0; i < searchTags.size(); i++) {
            selectionArgs[i] = "%" + searchTags.get(i) + "%";
        }
        Cursor cursor = mydb.rawQuery(query, selectionArgs);

        // Create a list to store search results
        List<ImageItem> searchResults = new ArrayList<>();

        // Process and add the retrieved images to the searchResults list
        while (cursor.moveToNext()) {
            searchResults.add(new ImageItem(cursor.getString(0), cursor.getString(1), BitmapFactory.decodeByteArray(cursor.getBlob(2), 0, cursor.getBlob(2).length)));
        }

        cursor.close();

        // Create a custom adapter for the search results and set it to the ListView
        ImageListAdapter adapter = new ImageListAdapter(this, R.layout.image_list_item, searchResults);
        mylist.setAdapter(adapter);
    }

    public void resetPhoto(View view) {
        drawingArea.reset();
    }

    public void getTags(View view) {
        // Fetch the tags for the current image being displayed
        drawnImage = drawingArea.getBitmap();

        new VisionApiTask().execute(drawnImage);
    }


    private int currentColor = Color.BLACK; // Default color
    public void showColorPickerDialog(View view) {
        final String[] colors = {"Black", "Red", "Green", "Blue"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Color");

        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        currentColor = Color.BLACK;
                        break;
                    case 1:
                        currentColor = Color.RED;
                        break;
                    case 2:
                        currentColor = Color.GREEN;
                        break;
                    case 3:
                        currentColor = Color.BLUE;
                        break;
                }
                // Set the selected color to your drawing area or paint object
                drawingArea.setPaintColor(currentColor);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }


    private float currentStrokeSize = 5f; // Default stroke size

    public void showStrokeSizePickerDialog(View view) {
        final String[] strokeSizes = {"1", "2", "5", "10"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Stroke Size");

        builder.setItems(strokeSizes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        currentStrokeSize = 1f;
                        break;
                    case 1:
                        currentStrokeSize = 2f;
                        break;
                    case 2:
                        currentStrokeSize = 5f;
                        break;
                    case 3:
                        currentStrokeSize = 10f;
                        break;
                }
                // Set the selected stroke size to your drawing area or paint object
                drawingArea.setPaintStrokeSize(currentStrokeSize);
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.show();
    }

    public void goBack(View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }
}