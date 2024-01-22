package com.example.assignment4;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StoryLayout extends AppCompatActivity {

    String url = "https://api.textcortex.com/v1/texts/social-media-posts";
    String API_KEY = "gAAAAABlTULt5MbjYBSsOvnyMggy2hpE-Ds2yc-sbDwHwWHLYecOKX2WnCRjyJTsCCmEivAQJJ-VyxWj7K0ETEjuMehQbRUMdAHHKGwRch88PVNFrFGy7gjlZSGOCiIun9W50PzxY-4J";


    private TextView selectedView;
    private TextView storyView;

    EditText findText;
    ListView mylist;
    TextToSpeech tts;
    private SQLiteDatabase mydb;
    private List<ImageItem> imageItems;
    private ImageListAdapter imageListAdapter;
    private boolean includeSketches = false;
    private List<ImageItem> selectedItems;

    // Create a new method to update the selected items list
    private void updateSelectedItems() {
        selectedItems = imageListAdapter.getSelectedItems();
    }

    public void dataBaseSetUp() {
        mydb = this.openOrCreateDatabase("mydb", Context.MODE_PRIVATE, null);
        mydb.execSQL("CREATE TABLE IF NOT EXISTS DRAWINGS3 (timestamp TEXT PRIMARY KEY, image BLOB, date TEXT, tags TEXT, image_type TEXT)");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.story_layout);

        selectedView = findViewById(R.id.selectedView);
        storyView = findViewById(R.id.storyView);
        findText = findViewById(R.id.findText);
        mylist = findViewById(R.id.mylist);
        imageItems = new ArrayList<>();
        // Create ImageListAdapter and set the selectedView
        imageListAdapter = new ImageListAdapter(this, R.layout.image_list_item, imageItems, selectedView);
        mylist.setAdapter(imageListAdapter);

        dataBaseSetUp();

        // Show the latest images when the app starts
        showInitialImages();

        CheckBox checkboxSketches = findViewById(R.id.checkbox_sketches);
        checkboxSketches.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call findPhoto method when checkbox state changes
                findPhoto(v);
            }
        });

        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);

                    // Set pitch and rate here
                    float pitch = 1.0f; // Normal pitch
                    float rate = 1.0f; // Normal rate

                    tts.setPitch(pitch);
                    tts.setSpeechRate(rate);
                }
            }
        });
    }

    void makeHTTPRequest(List<String> tags) throws JSONException {
        JSONObject data = new JSONObject();

        // Set user input in JSON payload
        data.put("context", "Write something funny");

        // Process tags and set in JSON payload
        if (!tags.isEmpty()) {
            data.put("keywords", new JSONArray(tags));
        }

        data.put("max_tokens", 100);
        data.put("mode", "twitter");
        data.put("model", "chat-sophos-1");

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, data, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Log.d("success", response.toString());

                try {
                    // Extract the text from the JSON response
                    JSONArray outputs = response.getJSONObject("data").getJSONArray("outputs");
                    if (outputs.length() > 0) {
                        String text = outputs.getJSONObject(0).getString("text");

                        // Update UI or perform further actions with the extracted text
                        storyView.setText(text);
                        String textToRead = storyView.getText().toString();
                        tts.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, null);
                    } else {
                        storyView.setText("No outputs found");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    storyView.setText("Error parsing response");
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("error", new String(error.networkResponse.data));
                // Handle the error, update UI, etc.
                storyView.setText("Error: " + new String(error.networkResponse.data));
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", "Bearer " + API_KEY);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(request);
    }

    // Modify the findPhoto method to retrieve the latest image with the specified tag
    public void findPhoto(View view) {
        String searchTags = findText.getText().toString().trim();

        // Update includeSketches based on the checkbox state
        CheckBox checkboxSketches = findViewById(R.id.checkbox_sketches);
        includeSketches = checkboxSketches.isChecked();

        if (searchTags.isEmpty()) {
            // If the search box is empty, show all images based on the checkbox state
            if (includeSketches) {
                showLatestImages(); // Include sketches
            } else {
                showLatestImages(); // Exclude sketches
            }
            selectedView.setText("You selected: ");
        } else {
            // If search tags are provided, split them by comma and search for images with those tags
            String[] searchTagArray = searchTags.split(",");
            List<String> searchTagList = new ArrayList<>(Arrays.asList(searchTagArray));

            // Remove leading/trailing spaces from each tag
            for (int i = 0; i < searchTagList.size(); i++) {
                searchTagList.set(i, searchTagList.get(i).trim());
            }

            // Now, search for images with any of the specified tags and based on the checkbox state
            if (includeSketches) {
                showLatestWithTags(searchTagList); // Include sketches
            } else {
                showLatestPhotosWithTags(searchTagList); // Exclude sketches
            }
        }
    }

    // Create a new method to show the latest photos with a specific tag
    private void showLatestPhotosWithTags(List<String> searchTags) {
        // Clear the current list of images
        mylist.setAdapter(null);

        // Reset TextViews and ImageViews

        // Create the SQL query to search for images with any of the specified tags
        StringBuilder queryBuilder = new StringBuilder("SELECT tags, date, image FROM DRAWINGS3 WHERE image_type = 'photo' AND (");
        for (int i = 0; i < searchTags.size(); i++) {
            if (i > 0) {
                queryBuilder.append(" OR ");
            }
            queryBuilder.append("tags LIKE ?");
        }
        queryBuilder.append(") ORDER BY date DESC");

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
        ImageListAdapter adapter = new ImageListAdapter(this, R.layout.image_list_item, searchResults, selectedView);
        mylist.setAdapter(adapter);
    }

    private void showLatestImages() {
        // Query the database to retrieve the latest images
        String query;
        if (includeSketches == false) {
            // Show the last 3 images, including sketches
            query = "SELECT tags, date, image FROM DRAWINGS3 WHERE image_type = 'photo' ORDER BY timestamp DESC";
        } else {
            // Show the last 3 images, excluding sketches
            query = "SELECT tags, date, image FROM DRAWINGS3 ORDER BY timestamp DESC";
        }
        Cursor cursor = mydb.rawQuery(query, null);

        imageItems.clear(); // Clear the existing data in imageItems

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
        ImageListAdapter adapter = new ImageListAdapter(this, R.layout.image_list_item, imageItems, selectedView);
        mylist.setAdapter(adapter);

        imageListAdapter.notifyDataSetChanged();
    }

    private void showInitialImages() {
        // Query the database to retrieve the latest images
        String query;

        // Show the last 3 images, excluding sketches
        query = "SELECT tags, date, image FROM DRAWINGS3 ORDER BY timestamp DESC";
        Cursor cursor = mydb.rawQuery(query, null);

        imageItems.clear(); // Clear the existing data in imageItems

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
        ImageListAdapter adapter = new ImageListAdapter(this, R.layout.image_list_item, imageItems, selectedView);
        mylist.setAdapter(adapter);

        imageListAdapter.notifyDataSetChanged();
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
        ImageListAdapter adapter = new ImageListAdapter(this, R.layout.image_list_item, searchResults, selectedView);
        mylist.setAdapter(adapter);
    }

    public void CreateStory(View view) {
        // Get the text from the selectedText TextView
        String selectedText = selectedView.getText().toString();

        // Remove the "You selected:" prefix
        String selectedTags = selectedText.replace("You selected:", "").trim();

        // Check if there are any tags after removing the prefix
        if (selectedTags.isEmpty()) {
            Toast.makeText(this, "No images selected for the story", Toast.LENGTH_SHORT).show();
            return;
        }

        // Display or use the selected tags in the storyView TextView
        storyView.setText(selectedTags);

        // Split the tags by comma to get individual words if needed
        String[] tagArray = selectedTags.split(",\\s*");

        // Make a request for a story based on the selected tags
        try {
            List<String> selectedTagList = Arrays.asList(tagArray);
            makeHTTPRequest(selectedTagList);
        } catch (JSONException e) {
            Log.e("error", e.toString());
        }

    }



    public void goBack(View view){
        Intent intent = new Intent (this, MainActivity.class);
        startActivity(intent);
    }
}