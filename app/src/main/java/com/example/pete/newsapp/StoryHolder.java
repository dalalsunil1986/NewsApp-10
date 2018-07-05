package com.example.pete.newsapp;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import static com.example.pete.newsapp.MainActivity.sharedPreferences;

class StoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private final Context context;
    private Story thisStory;

    // View references
    private TextView pillarNameTextView;
    private TextView dividerPillarSectionTextView;
    private TextView sectionNameTextView;
    private TextView storyTitleTextView;
    private TextView dateTextView;
    private TextView authorTextView;

    StoryHolder(Context context, View itemView) {
        super(itemView);

        this.context = context;

        getViewReferences(itemView);

        itemView.setOnClickListener(this);
    }

    // Bind the given Story data to this ViewHolder
    public void bindStory(Story story) {
        this.thisStory = story;

        this.pillarNameTextView.setText(thisStory.getPillarName());
        this.sectionNameTextView.setText(thisStory.getSectionName());
        setPillarAndSectionColors(thisStory.getPillarName());

        this.dividerPillarSectionTextView.setText(getStringResource(R.string.divider_text));
        this.dividerPillarSectionTextView.setTextColor(getColorResource(R.color.color_pillar_section_divider));

        this.storyTitleTextView.setText(thisStory.getTitle());
        this.dateTextView.setText(thisStory.getDate());

        String author_text;
        if (!thisStory.getAuthor().equals("")) {
            author_text = String.format(context.getString(R.string.by_string), thisStory.getAuthor());
            this.authorTextView.setText(author_text);
            this.authorTextView.setVisibility(View.VISIBLE);
        } else {
            this.authorTextView.setVisibility(View.GONE);
        }

        // Show or hide UI elements based on user preferences
        setUiVisibilities();
    }

    /*
    Changes the text color of the sectionNameTextView
    Section colors are based on Guardian theme colors in colors.xml resource
    */
    private void setPillarAndSectionColors(String pillarName) {
        int color;

        // Get the color scheme from preferences
        String setting_color_scheme =
                sharedPreferences.getString(
                        getStringResource(R.string.color_scheme_key_name),
                        getStringResource(R.string.color_scheme_default_value));

        // Interpret the color scheme as a boolean
        Boolean USE_DARK_THEME_COLORS = setting_color_scheme.equals(getStringResource(R.string.internal_color_scheme_dark));

        // Use dark or regular variations of theme colors. See conditional operator in .getColor statements
        // Can't use switch statement due to "non-constant" String resources
        if (pillarName.equals(getStringResource(R.string.api_pillar_news))) {
            color = getColorResource(USE_DARK_THEME_COLORS ? R.color.theme_news_dark : R.color.theme_news);
        } else if (pillarName.equals(getStringResource(R.string.api_pillar_opinion))) {
            color = getColorResource(USE_DARK_THEME_COLORS ? R.color.theme_opinion_dark : R.color.theme_opinion);
        } else if (pillarName.equals(getStringResource(R.string.api_pillar_sport))) {
            color = getColorResource(USE_DARK_THEME_COLORS ? R.color.theme_sport_dark : R.color.theme_sport);
        } else if (pillarName.equals(getStringResource(R.string.api_pillar_culture))) {
            color = getColorResource(USE_DARK_THEME_COLORS ? R.color.theme_culture_dark : R.color.theme_culture);
        } else if (pillarName.equals(getStringResource(R.string.api_pillar_lifestyle))) {
            color = getColorResource(USE_DARK_THEME_COLORS ? R.color.theme_lifestyle_dark : R.color.theme_lifestyle);
        } else if (pillarName.equals("")) {
            // More Pillar
            color = getColorResource(USE_DARK_THEME_COLORS ? R.color.theme_background_medium : R.color.theme_background_dark);
        } else {
            color = getColorResource(R.color.textColorDeemphasized);
        }

        this.pillarNameTextView.setTextColor(color);
        this.sectionNameTextView.setTextColor(color);
    }

    // Show or hide UI elements based on user preferences
    private void setUiVisibilities() {
        // Get preferences
        boolean setting_show_pillar = sharedPreferences.getBoolean(
                getStringResource(R.string.internal_show_pillar),
                getBooleanResource(R.bool.show_pillar_default_value));
        boolean setting_show_section = sharedPreferences.getBoolean(
                getStringResource(R.string.internal_show_section),
                getBooleanResource(R.bool.show_section_default_value));
        boolean setting_show_date = sharedPreferences.getBoolean(
                getStringResource(R.string.internal_show_date),
                getBooleanResource(R.bool.show_date_default_value));
        boolean setting_show_author = sharedPreferences.getBoolean(
                getStringResource(R.string.internal_show_author),
                getBooleanResource(R.bool.show_author_default_value));

        // If the Pillar name is blank, we need to hide the Pillar TextView (happens in the "More" Pillar)
        if (pillarNameTextView.getText().equals("")) {
            setting_show_pillar = false;
        }

        // If the Pillar name and Section name are the same, hide the Pillar TextView
        if (pillarNameTextView.getText().equals(sectionNameTextView.getText())) {
            if (!setting_show_section && setting_show_pillar) {
                // If the section name is hidden but the pillar name isn't
                // show the section name instead of the pillar name
                setting_show_section = true;
            }
            setting_show_pillar = false;
        }

        // If the author text is blank, hide the Author TextView
        if (authorTextView.getText().equals("")) {
            setting_show_author = false;
        }

        // Set visibilities
        pillarNameTextView.setVisibility(setting_show_pillar ? View.VISIBLE : View.GONE);
        sectionNameTextView.setVisibility(setting_show_section ? View.VISIBLE : View.GONE);
        dateTextView.setVisibility(setting_show_date ? View.VISIBLE : View.GONE);
        authorTextView.setVisibility(setting_show_author ? View.VISIBLE : View.GONE);

        // If we have Pillar but not Section showing, Pillar needs to have layout_weight="1" to push the Date to the right
        if (setting_show_pillar && !setting_show_section) {
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);

            pillarNameTextView.setLayoutParams(params);
        }

        // Hide the divider if either Pillar Name or Section Name are hidden
        dividerPillarSectionTextView.setVisibility(setting_show_pillar && setting_show_section ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onClick(View v) {
        if (this.thisStory != null) {
            // Open the Story (Guardian website URL) in a web browser
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thisStory.getUrl_asString()));
            context.startActivity(intent, null);
        }
    }

    private void getViewReferences(View itemView) {
        pillarNameTextView = itemView.findViewById(R.id.list_item_story_pillar_name);
        dividerPillarSectionTextView = itemView.findViewById(R.id.list_item_divider_pillar_section);
        sectionNameTextView = itemView.findViewById(R.id.list_item_story_section_name);
        storyTitleTextView = itemView.findViewById(R.id.list_item_story_title);
        dateTextView = itemView.findViewById(R.id.list_item_story_date);
        authorTextView = itemView.findViewById(R.id.list_item_story_author);
    }

    private String getStringResource(int resourceID) {
        Resources resources = context.getResources();

        return resources.getString(resourceID);
    }

    private boolean getBooleanResource(int resourceID) {
        Resources resources = context.getResources();

        return resources.getBoolean(resourceID);
    }

    private int getColorResource(int resourceID) {
        Resources resources = context.getResources();

        return resources.getColor(resourceID);
    }

}