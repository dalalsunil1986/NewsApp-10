package com.example.pete.newsapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class StoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Constants: View types
    public enum recyclerViewTypes {
        STORY, FOOTER
    }

    private boolean isLoadingAdded = false;

    private ArrayList<Story> stories;
    private Context context;

    StoryAdapter(Context context, int itemResource, ArrayList<Story> stories) {
        this.context = context;
        this.stories = stories;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder viewHolder = null;
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        // Return different viewHolders depending on the supplied viewType
        if (viewType == recyclerViewTypes.STORY.ordinal()) {
            // Inflate the view and return the new ViewHolder
            View view = inflater.inflate(R.layout.list_item_story, parent, false);
            viewHolder = new StoryHolder(this.context, view);
        } else if (viewType == recyclerViewTypes.FOOTER.ordinal()) {
            View view = inflater.inflate(R.layout.list_item_paginator_loading, parent, false);
            viewHolder = new PaginatorHolder(this.context, view);
        } else {
            // Provide a default response so that the return value is never null
            View view = inflater.inflate(R.layout.list_item_story, parent, false);
            viewHolder = new StoryHolder(this.context, view);

            Log.d("onCreateViewHolder", "Unknown view type: " + String.valueOf(viewType) +
                    ". Should correspond to the recyclerViewTypes enum in StoryAdapter.");
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // Use position to get the correct Story object
        Story story = this.stories.get(position);

        // Identify the recyclerViewType of the current item
        int thisViewType = getItemViewType(position);

        if (thisViewType == recyclerViewTypes.STORY.ordinal()) {
            StoryHolder storyHolder = (StoryHolder) holder;

            // Bind the story object to the holder
            storyHolder.bindStory(story);
        } else if (thisViewType == recyclerViewTypes.FOOTER.ordinal()) {
            PaginatorHolder paginatorHolder = (PaginatorHolder) holder;

            // Handle View visibilities once paginator list item supports error messages
        } else {
            Log.d("onBindViewHolder", "Unknown view type: " + String.valueOf(thisViewType) +
                    ". Should correspond to the recyclerViewTypes enum in StoryAdapter.");
        }
    }

    @Override
    public int getItemCount() {
        return this.stories.size();
    }

    // Identify which recyclerViewType the item at the given position is
    @Override
    public int getItemViewType(int position) {
        if (position == stories.size() - 1) {
            return recyclerViewTypes.FOOTER.ordinal();
        } else {
            return recyclerViewTypes.STORY.ordinal();
        }
    }

    public void add(Story story) {
        stories.add(story);
        notifyItemInserted(stories.size() - 1);
    }

    public void addAll(ArrayList<Story> addStories) {
        for (Story story : addStories) {
            add(story);
        }
    }

    public void addLoadingFooter() {
        isLoadingAdded = true;
        add(new Story("", "", "", "", ""));
    }

    public void removeLoadingFooter() {
        if (isLoadingAdded) {
            isLoadingAdded = false;

            Story storyFooter = stories.get(stories.size() - 1);

            if (storyFooter != null) {
                stories.remove(storyFooter);
                notifyItemRemoved(stories.size() - 1);
            }
        }
    }

}