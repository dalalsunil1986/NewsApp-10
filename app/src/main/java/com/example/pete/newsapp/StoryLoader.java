package com.example.pete.newsapp;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

import com.example.pete.newsapp.Story;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import static com.example.pete.newsapp.MainActivity.DEBUG_SIMULATE_NO_RESULTS;
import static com.example.pete.newsapp.MainActivity.DEBUG_SIMULATE_SLOW_CONNECTION;
import static com.example.pete.newsapp.MainActivity.makeHttpRequest;

public class StoryLoader extends AsyncTaskLoader<ArrayList<Story>> {
    private URL url;

    public StoryLoader(Context context, URL url) {
        super(context);
        this.url = url;
    }

    @Override
    public ArrayList<Story> loadInBackground() {
        // Simulate a slow internet connection (test the progress spinner animation)
        if (DEBUG_SIMULATE_SLOW_CONNECTION) {
            try {
                Thread.sleep(3000);
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
    public static ArrayList<Story> extractStories(String JSONString) {
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
            Story.currentPage = responseObject.getInt("currentPage");
            Story.totalPages = responseObject.getInt("pages");

            // Get the resultsArray Array
            JSONArray resultsArray = responseObject.getJSONArray("results");

            // Iterate over resultsArray (can't use enhanced for loop on JSONArrays)
            for (int e = 0; e < resultsArray.length(); e++) {
                JSONObject resultObject = resultsArray.getJSONObject(e);

                String sectionName = resultObject.getString("sectionName");
                String title = resultObject.getString("webTitle");
                String date = resultObject.getString("webPublicationDate");
                String url = resultObject.getString("webUrl");

                // Author:
                // The rubric mentions getting an author for the story
                // There's a property at show-references=author but I've never seen this used
                // There's a much more often used property at show-tags=contributor
                JSONArray tagsArray = resultObject.getJSONArray("tags");
                // This object represents the first contributor to the story (the author)
                JSONObject firstTagObject = tagsArray.getJSONObject(0);
                // webTitle is the name of the contributor (author)
                String author = firstTagObject.getString("webTitle");

                Story thisStory = new Story(sectionName, title, date, author, url);

                stories.add(thisStory);
            }
        } catch (JSONException e) {
            Log.e("extractStories", "Problem parsing the JSON results", e);
        }

        // Return the list of stories
        return stories;
    }
}
