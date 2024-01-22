package com.example.assignment4;

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


public class PictureLayout extends AppCompatActivity {

    private ImageView imageView0, imageView1, imageView2, imageView3;
    ListView mylist;
    EditText tags;
    EditText findText;
    private Bitmap capturedImage;

    private SQLiteDatabase mydb;
    private final String API_KEY = "AIzaSyDLZB52P135JbAdAlsnpDMC137TZR6kRuU";


    public void dataBaseSetUp() {
        mydb = this.openOrCreateDatabase("mydb", Context.MODE_PRIVATE, null);
        mydb.execSQL("CREATE TABLE IF NOT EXISTS DRAWINGS3 (timestamp TEXT PRIMARY KEY, image BLOB, date TEXT, tags TEXT, image_type TEXT)");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.picture_layout);
        tags = findViewById(R.id.tags);
        findText = findViewById(R.id.findText);
        imageView0 = findViewById(R.id.imageView);
        mylist = findViewById(R.id.mylist);
        dataBaseSetUp();
        // Show the latest images when the app starts
        showLatestImages();
    }



    private List<String> performImageRecognitionForCapturedImage() {
        // Encode the captured image
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        capturedImage.compress(Bitmap.CompressFormat.JPEG, 90, bout);
        Image myimage = new Image();
        myimage.encodeContent(bout.toByteArray());

        try {
            return new AsyncTask<Void, Void, List<String>>() {
                @Override
                protected List<String> doInBackground(Void... params) {
                    try {
                        return performNetworkRequest(myimage);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return Collections.singletonList("Error");
                    }
                }

                @Override
                protected void onPostExecute(List<String> results) {
                    // Update the UI with the recognized tags
                    if (!results.isEmpty()) {
                        // Convert the list of tags to a comma-separated string
                        String tagsText = TextUtils.join(", ", results);
                        tags.setText(tagsText);
                    } else {
                        tags.setText("No tags found");
                    }
                }
            }.execute().get(); // .get() is used to get the result of the AsyncTask
        } catch (Exception e) {
            e.printStackTrace();
            return Collections.singletonList("Error");
        }
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
        values.put("tags", tags);
        values.put("image_type", "photo");  // Set the image_type value

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Date currentDate = new Date();
        String timestamp = dateFormat.format(currentDate);
        values.put("timestamp", timestamp);

        mydb.insert("DRAWINGS3", null, values);
    }

    public void submitPhoto(View view) {
        if (capturedImage != null) {
            Date date = new Date();
            SimpleDateFormat date2 = new SimpleDateFormat("MMM dd, yyyy - ha", Locale.US);
            String date3 = date2.format(date);
            String tags2 = tags.getText().toString();

            saveImageToDatabase(capturedImage, date3, tags2);
            showLatestImages(); // Refresh the displayed images after saving.
        }
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

            // Search for images with any of the specified tags
            showLatestWithTags(searchTagList);
        }
    }

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
        queryBuilder.append(" ORDER BY date DESC ");

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




    private void displayImageWithTag(String tags, String date, byte[] imageByteArray, View listViewItem) {
        ImageView imageView = listViewItem.findViewById(R.id.imageViewInListItem);
        TextView tagsTextView = listViewItem.findViewById(R.id.tagsTextViewInListItem);
        TextView dateTextView = listViewItem.findViewById(R.id.dateTextViewInListItem);

        String[] tagArray = tags.split(","); // Split tags by comma
        StringBuilder tagsText = new StringBuilder();
        for (String tag : tagArray) {
            tagsText.append(tag.trim()).append(", "); // Trim to remove leading/trailing spaces
        }

        // Remove the trailing comma and space
        if (tagsText.length() > 0) {
            tagsText.setLength(tagsText.length() - 2);
        }

        // Set the tags and date in the TextViews
        tagsTextView.setText("Tags: " + tagsText.toString());
        dateTextView.setText("Date: " + date);

        // Decode the image byte array and set it in the ImageView
        Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
        imageView.setImageBitmap(imageBitmap);
    }



    public void startCamera(View view) {
        Intent cam_intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cam_intent, 1);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            // Get the captured image from the data intent
            capturedImage = (Bitmap) data.getExtras().get("data");

            // Set the captured image in the ImageView
            imageView0.setImageBitmap(capturedImage);

            // Enable the "Submit Photo" button for saving to the database
            findViewById(R.id.submitButton).setEnabled(true);

            // Perform image recognition and populate the first EditText box with tags

            List<String> recognizedTagsList = performImageRecognitionForCapturedImage();
            String recognizedTags = TextUtils.join(", ", recognizedTagsList);
            tags.setText(recognizedTags);
        }
    }




    public void goBack(View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }
}