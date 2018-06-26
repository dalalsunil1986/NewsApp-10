package com.example.pete.newsapp;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import java.util.Arrays;

import static com.example.pete.newsapp.MainActivity.USE_DARK_THEME_COLORS;

public class StoryHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private Context context;
    private Story thisStory;

    // View references
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

        this.sectionNameTextView.setText(thisStory.getSectionName());
        setSectionColor(thisStory.getSectionName());

        this.storyTitleTextView.setText(thisStory.getTitle());
        this.dateTextView.setText(thisStory.getDate());

        String author_text = "";
        if (!thisStory.getAuthor().equals("")) {
            author_text = String.format(context.getString(R.string.by_string), thisStory.getAuthor());
            this.authorTextView.setText(author_text);
            this.authorTextView.setVisibility(View.VISIBLE);
        } else {
            this.authorTextView.setVisibility(View.GONE);
        }
    }

    /*
    Changes the text color of the sectionNameTextView
    Section colors are based on Guardian theme colors in colors.xml resource
    */
    private void setSectionColor(String sectionName) {
        int color = 0;

        if (USE_DARK_THEME_COLORS) {
            // Use dark variations of theme colors
            switch (getSectionName(sectionName)) {
                case "news":
                    color = context.getResources().getColor(R.color.theme_news_dark);
                    break;
                case "opinion":
                    color = context.getResources().getColor(R.color.theme_opinion_dark);
                    break;
                case "sport":
                    color = context.getResources().getColor(R.color.theme_sport_dark);
                    break;
                case "culture":
                    color = context.getResources().getColor(R.color.theme_culture_dark);
                    break;
                case "lifestyle":
                    color = context.getResources().getColor(R.color.theme_lifestyle_dark);
                    break;
                default:
                    color = context.getResources().getColor(R.color.textColorDeemphasized);
                    break;
            }
        } else {
            // Use regular variations of theme colors
            switch (getSectionName(sectionName)) {
                case "news":
                    color = context.getResources().getColor(R.color.theme_news);
                    break;
                case "opinion":
                    color = context.getResources().getColor(R.color.theme_opinion);
                    break;
                case "sport":
                    color = context.getResources().getColor(R.color.theme_sport);
                    break;
                case "culture":
                    color = context.getResources().getColor(R.color.theme_culture);
                    break;
                case "lifestyle":
                    color = context.getResources().getColor(R.color.theme_lifestyle);
                    break;
                default:
                    color = context.getResources().getColor(R.color.textColorDeemphasized);
                    break;
            }
        }

        this.sectionNameTextView.setTextColor(color);
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
        this.sectionNameTextView = itemView.findViewById(R.id.list_item_story_section_name);
        this.storyTitleTextView = itemView.findViewById(R.id.list_item_story_title);
        this.dateTextView = itemView.findViewById(R.id.list_item_story_date);
        this.authorTextView = itemView.findViewById(R.id.list_item_story_author);
    }

    //region Section identification methods

    // Return a String identifying the section header (which helps set colors)
    private String getSectionName(String sectionName) {
        if (isNewsSection(sectionName)) {
            sectionName = "news";
        } else if (isOpinionSection(sectionName)) {
            sectionName = "opinion";
        } else if (isSportSection(sectionName)) {
            sectionName = "sport";
        } else if (isCultureSection(sectionName)) {
            sectionName = "culture";
        } else if (isLifestyleSection(sectionName)) {
            sectionName = "lifestyle";
        } else {
            sectionName = "";
        }

        return sectionName;
    }

    private boolean isNewsSection(String sectionName) {
        String[] news_sections = context.getResources().getStringArray(R.array.news_sections);

        return Arrays.asList(news_sections).contains(sectionName);
    }

    private boolean isOpinionSection(String sectionName) {
        String[] opinion_sections = context.getResources().getStringArray(R.array.opinion_sections);

        return Arrays.asList(opinion_sections).contains(sectionName);
    }

    private boolean isSportSection(String sectionName) {
        String[] sport_sections = context.getResources().getStringArray(R.array.sport_sections);

        return Arrays.asList(sport_sections).contains(sectionName);
    }

    private boolean isCultureSection(String sectionName) {
        String[] culture_sections = context.getResources().getStringArray(R.array.culture_sections);

        return Arrays.asList(culture_sections).contains(sectionName);
    }

    private boolean isLifestyleSection(String sectionName) {
        String[] lifestyle_sections = context.getResources().getStringArray(R.array.lifestyle_sections);

        return Arrays.asList(lifestyle_sections).contains(sectionName);
    }

    //endregion

}