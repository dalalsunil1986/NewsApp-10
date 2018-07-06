package com.example.pete.newsapp;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static com.example.pete.newsapp.MainActivity.DEBUG_SIMULATE_NO_RESULTS;
import static com.example.pete.newsapp.MainActivity.DEBUG_SIMULATE_SLOW_CONNECTION;
import static com.example.pete.newsapp.MainActivity.makeHttpRequest;

class StoryLoader extends AsyncTaskLoader<ArrayList<Story>> {

    private static final int SLOW_CONNECTION_DELAY_MILLISECONDS = 3000;

    private final URL url;

    StoryLoader(Context context, URL url) {
        super(context);
        this.url = url;
    }

    @Override
    // Perform the HTTP request, get JSON String, pass String to extractStories
    public ArrayList<Story> loadInBackground() {
        // Simulate a slow internet connection (test the progress spinner animation)
        if (DEBUG_SIMULATE_SLOW_CONNECTION) {
            try {
                Thread.sleep(SLOW_CONNECTION_DELAY_MILLISECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        if (url == null) {
            return null;
        }

        // Perform HTTP request to the URL and receive a JSON response back
        String JSONString = null;
        try {
            JSONString = makeHttpRequest(url);
        } catch (IOException e) {
            Log.e("loadInBackground", "Error closing input stream", e);
        }

        return extractStories(JSONString);
    }

    // Extract Stories from the given JSON String (parse the JSON String)
    private static ArrayList<Story> extractStories(String JSONString) {
        // Create an empty ArrayList that we can start adding stories to
        ArrayList<Story> stories = new ArrayList<>();

        if (DEBUG_SIMULATE_NO_RESULTS) {
            return stories;
        }

        try {
            // Get the root object from the JSONString
            JSONObject rootObject = new JSONObject(JSONString);

            // Get the response object from the root object
            JSONObject responseObject = rootObject.getJSONObject("response");

            // Get the currentPage and total pages from the responseObject
            // (These are static variables of Story because they're common to all Stories)
            Story.currentPage = responseObject.optInt("currentPage");
            Story.totalPages = responseObject.optInt("pages");

            // Get the resultsArray Array
            JSONArray resultsArray = responseObject.getJSONArray("results");

            // Iterate over resultsArray (can't use enhanced for loop on JSONArrays)
            for (int e = 0; e < resultsArray.length(); e++) {
                JSONObject resultObject = resultsArray.getJSONObject(e);

                String pillarName = resultObject.optString("pillarName");
                String sectionName = resultObject.optString("sectionName");
                String title = resultObject.optString("webTitle");
                String date = resultObject.optString("webPublicationDate");
                String url = resultObject.optString("webUrl");

                // Author:
                // The rubric mentions getting an author for the story
                // There's a property at show-references=author but I've never seen this used
                // There's a much more often used property at show-tags=contributor
                JSONArray tagsArray = resultObject.getJSONArray("tags");
                String author = "";
                if (tagsArray.length() >= 1) {
                    // This object represents the first contributor to the story (the author)
                    JSONObject firstTagObject = tagsArray.getJSONObject(0);
                    // webTitle is the name of the contributor (author)
                    author = firstTagObject.optString("webTitle");
                }

                Story thisStory = new Story(pillarName, sectionName, title, date, author, url);

                stories.add(thisStory);
            }
        } catch (JSONException e) {
            Log.e("extractStories", "Problem parsing the JSON results", e);
        }

        // Return the list of stories
        return stories;
    }

}