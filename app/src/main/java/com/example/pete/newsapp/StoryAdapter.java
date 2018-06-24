package com.example.pete.newsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class StoryAdapter extends ArrayAdapter<Story> {

    private Context context;

    StoryAdapter(@NonNull Context context, int resource, ArrayList<Story> stories) {
        super(context, resource, stories);

        this.context = context;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
//        return super.getView(position, convertView, parent);

        // todo: use view holder pattern
        // see: https://developer.android.com/training/improving-layouts/smooth-scrolling#ViewHolder

        // Either recycle the view or inflate a new one if necessary
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_story, parent, false);
        }

        final Story thisStory = getItem(position);

        if (thisStory == null) {
            return convertView;
        }

        // Set view properties for list_item_story layout:

        // Section Name
        TextView sectionNameTextView = convertView.findViewById(R.id.list_item_story_section_name);
        sectionNameTextView.setText(thisStory.getSectionName());

        // Story Title
        TextView storyTitleTextView = convertView.findViewById(R.id.list_item_story_title);
        storyTitleTextView.setText(thisStory.getTitle());

        // Publication Date
        TextView dateTextView = convertView.findViewById(R.id.list_item_story_date);
        dateTextView.setText(thisStory.getDate());

        // Author
        // (Leave TextView blank if there is no author)
        TextView authorTextView = convertView.findViewById(R.id.list_item_story_author);
        String author_text = "";
        if (!thisStory.getAuthor().equals("")) {
            author_text = String.format(context.getString(R.string.by_string), thisStory.getAuthor());
        }
        authorTextView.setText(author_text);

        // Clicking layout opens website URL
        LinearLayout earthquakeLinearLayout = (LinearLayout) convertView.findViewById(R.id.list_item_story_linear_layout);

        earthquakeLinearLayout.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thisStory.getUrl_asString()));
            context.startActivity(intent, null);
        });

        return convertView;
    }
}
