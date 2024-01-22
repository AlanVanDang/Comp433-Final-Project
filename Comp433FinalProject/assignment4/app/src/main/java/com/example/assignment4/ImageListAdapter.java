package com.example.assignment4;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class ImageListAdapter extends ArrayAdapter<ImageItem> {
    private final Context context;
    private final int layoutResourceId;
    private final List<ImageItem> data;
    private TextView selectedView;

    public ImageListAdapter(Context context, int layoutResourceId, List<ImageItem> data, TextView selectedView) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
        this.selectedView = selectedView;
    }

    // Constructor for DrawingLayout and PictureLayout
    public ImageListAdapter(Context context, int layoutResourceId, List<ImageItem> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
        this.selectedView = null; // Set selectedView to null for DrawingLayout and PictureLayout
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ImageItemHolder holder;

        if (row == null) {
            row = ((Activity) context).getLayoutInflater().inflate(layoutResourceId, parent, false);

            holder = new ImageItemHolder();
            holder.image = row.findViewById(R.id.imageViewInListItem);
            holder.tags = row.findViewById(R.id.tagsTextViewInListItem);
            holder.date = row.findViewById(R.id.dateTextViewInListItem);
            holder.checkbox = row.findViewById(R.id.checkboxInListItem); // Make sure to add this line

            row.setTag(holder);
        } else {
            holder = (ImageItemHolder) row.getTag();
        }

        ImageItem item = data.get(position);

        holder.tags.setText("Tags: " + item.getTags());
        holder.date.setText("Date: " + item.getDate());
        holder.image.setImageBitmap(item.getImage());

        // Always set checkbox to visible
        holder.checkbox.setVisibility(View.VISIBLE);

        // Handle checkbox state
        holder.checkbox.setChecked(item.isSelected());

        // Handle checkbox click events
        holder.checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setSelected(isChecked);
            notifyDataSetChanged(); // Notify adapter that data has changed

            // Update selectedView based on the checked items
            updateSelectedView();

            // Count the number of selected checkboxes
            int selectedCount = getSelectedItems().size();

            // Handle the case where the user tries to check more than three checkboxes
            if (selectedCount > 3) {
                Toast.makeText(getContext(), "You can only select up to 3 checkboxes", Toast.LENGTH_SHORT).show();
                item.setSelected(false); // Uncheck the checkbox
                // Update selectedView based on the checked items
                updateSelectedView();
            } else {
                notifyDataSetChanged(); // Notify adapter that data has changed
            }
        });

        return row;
    }

    // Add this method to update selectedView
    // Add this method to update selectedView
    private void updateSelectedView() {
        if (selectedView != null) {
            StringBuilder selectedTags = new StringBuilder("You selected: ");
            boolean atLeastOneSelected = false;

            for (ImageItem item : data) {
                if (item.isSelected()) {
                    atLeastOneSelected = true;
                    selectedTags.append(item.getTags()).append(", ");
                }
            }

            if (atLeastOneSelected) {
                // Remove the trailing comma and space
                selectedTags.setLength(selectedTags.length() - 2);
            } else {
                // No items selected
                selectedTags.append("No images selected");
            }

            selectedView.setText(selectedTags.toString());
        }
    }

    public List<ImageItem> getSelectedItems() {
        List<ImageItem> selectedItems = new ArrayList<>();
        for (ImageItem item : data) {
            if (item.isSelected()) {
                selectedItems.add(item);
            }
        }
        return selectedItems;
    }


    static class ImageItemHolder {
        ImageView image;
        TextView tags;
        TextView date;
        CheckBox checkbox;
    }
}
