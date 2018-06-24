package com.example.pete.newsapp;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import static com.example.pete.newsapp.Story.currentPage;
import static com.example.pete.newsapp.Story.totalPages;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<Story>> {

    //region Constants and Instance Variables

    // Constants
    private static final String API_KEY = "70988c31-53c0-4f45-b39c-1988b170dac9";
    private static final String QUERY_BASE_URL = "https://content.guardianapis.com/search?&show-tags=contributor";
    private static final String HEADER = "US News";

    // Set these booleans to test response validation behaviors
    public static final boolean DEBUG_SIMULATE_SLOW_CONNECTION = false;
    public static final boolean DEBUG_SIMULATE_NO_CONNECTION = false;
    public static final boolean DEBUG_SIMULATE_NO_RESULTS = false;

    // Query parameters
    String query_orderBy = "newest";
    String query_section = "us-news";

    // View References
    TextView emptyView;
    ProgressBar progressSpinner;
    ListView storiesListView;
    View paginator;
    TextView paginator_text;
    ImageView paginator_back_button;
    ImageView paginator_forward_button;

    //endregion Constants and Instance Variables

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(HEADER);

        // Get View references (MainActivity)
        getViewReferences_MainActivity();

        // Set the ListView's empty view
        storiesListView.setEmptyView(emptyView);

        // Set the ListView's footer view (paginator)
        paginator = getLayoutInflater().inflate(R.layout.list_item_paginator, storiesListView, false);
        storiesListView.addFooterView(paginator);

        // Get View references (Paginator)
        getViewReferences_Paginator();

        // Initialize the paginator (set onClickListeners for forward and back buttons)
        initializePaginator(this);

        // Set the ListView and TextView as "Gone" so that we only see the Progress Spinner
        setVisibilities(true, false, false);

        // Check network connectivity
        if (!isOnline()) {
            // Change View visibility
            setVisibilities(false, false, true);

            emptyView.setText(getResources().getString(R.string.no_internet_text));

            return;
        }

        // Pass the URL in a Bundle
        Bundle bundle = new Bundle();
        bundle.putString("url", getQueryUrl());

        // Start the AsyncTaskLoader
        getLoaderManager().initLoader(0, bundle, this).forceLoad();
    }

    // Build the Query URL (using instance variables)
    private String getQueryUrl() {
        return QUERY_BASE_URL + "&order-by=" + query_orderBy + "&section=" + query_section + "&page=" + currentPage + "&api-key=" + API_KEY;
    }

    //region Utility methods

    // Update the Main Activity UI with a given ArrayList of Stories
    private void setListViewAdapter(ArrayList<Story> stories) {
        if (storiesListView == null) {
            return;
        }

        // todo: should probably use Adapter.notifyDataSetChanged() instead of creating a new Adapter every time
        // see: https://stackoverflow.com/questions/16401025/adding-items-to-the-end-of-a-listview

        // Create a new {@link ArrayAdapter} of earthquakes
        ArrayAdapter<Story> adapter = new StoryAdapter(this, 0, stories);

        // Set the adapter on the ListView so the list can be populated in the user interface
        storiesListView.setAdapter(adapter);
    }

    /*
    Initialize the paginator (list_item_paginator)
    This sets the onClickListeners for the buttons
    */
    private void initializePaginator(MainActivity mainActivity) {
        // (Modify the API query URL to request the next or previous set of results)
        // https://open-platform.theguardian.com/documentation/search

        paginator_back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage > 1) {
                    currentPage -= 1;

                    // Pass the URL in a Bundle
                    Bundle bundle = new Bundle();
                    bundle.putString("url", getQueryUrl());

                    // Start the AsyncTaskLoader
                    getLoaderManager().restartLoader(0, bundle, mainActivity).forceLoad();
                }
            }
        });

        paginator_forward_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage < totalPages) {
                    currentPage += 1;

                    // Pass the URL in a Bundle
                    Bundle bundle = new Bundle();
                    bundle.putString("url", getQueryUrl());

                    // Start the AsyncTaskLoader
                    getLoaderManager().restartLoader(0, bundle, mainActivity).forceLoad();
                }
            }
        });
    }

    // Update the paginator (at the bottom of the Stories ListView)
    private void updatePaginator() {
        // Update the paginator text
        String paginator_text_resource = getResources().getString(R.string.paginator_text);
        paginator_text.setText(String.format(paginator_text_resource, String.valueOf(currentPage),
                String.valueOf(totalPages)));

        if (currentPage <= 1) {
            // Disable the back button if we are on the first page
            paginator_back_button.setEnabled(false);
            paginator_back_button.setColorFilter(getResources().getColor(R.color.textColorDeemphasized));
        } else {
            // Enable the back button
            paginator_back_button.setEnabled(true);
            paginator_back_button.setColorFilter(getResources().getColor(R.color.textColorEmphasized));
        }

        if (currentPage >= totalPages) {
            // Disable the forward button if we are on the last page
            paginator_forward_button.setEnabled(false);
            paginator_forward_button.setColorFilter(getResources().getColor(R.color.textColorDeemphasized));
        } else {
            // Enable the forward button
            paginator_forward_button.setEnabled(true);
            paginator_forward_button.setColorFilter(getResources().getColor(R.color.textColorEmphasized));
        }
    }

    // Set visibility of the Main Activity UI components
    private void setVisibilities(boolean progressSpinnerVisible, boolean storiesListViewVisible, boolean emptyTextViewVisible) {
        // Use the conditional operator:
        progressSpinner.setVisibility(progressSpinnerVisible ? View.VISIBLE : View.GONE);
        storiesListView.setVisibility(storiesListViewVisible ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(emptyTextViewVisible ? View.VISIBLE : View.GONE);
    }

    // Get references to Views in activity_main.xml, store them in instance variables of MainActivity
    private void getViewReferences_MainActivity() {
        // Main Activity Views
        storiesListView = findViewById(R.id.stories_list_view);
        emptyView = findViewById(R.id.empty_text_view);
        progressSpinner = findViewById(R.id.progress_spinner);
    }

    // Get references to Views in list_item_paginator.xml (Paginator must have been inflated before calling this method)
    private void getViewReferences_Paginator() {
        // Paginator Views
        paginator_text = paginator.findViewById(R.id.paginator_text);
        paginator_back_button = paginator.findViewById(R.id.paginator_back_button);
        paginator_forward_button = paginator.findViewById(R.id.paginator_forward_button);
    }

    // Returns new URL object from the given string URL.
    public static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e("createUrl", "Error creating URL", exception);
            return null;
        }
        return url;
    }

    public boolean isOnline() {
        if (DEBUG_SIMULATE_NO_CONNECTION) {
            return false;
        }

        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    // Make HTTP request to the given URL and return a String as the response.
    public static String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (url == null) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(10000 /* milliseconds */);
            urlConnection.setConnectTimeout(15000 /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200), then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == 200) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Log.e("makeHttpRequest", "Error response code: " + urlConnection.getResponseCode());
            }
        } catch (IOException e) {
            Log.e("makeHttpRequest", "Problem retrieving the JSON results.", e);
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    // Convert the {@link InputStream} into a String which contains the whole JSON response from the server.
    public static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    //endregion Utility methods

    //region Loader callback methods

    @Override
    public Loader<ArrayList<Story>> onCreateLoader(int id, Bundle args) {
        return new StoryLoader(this, createUrl(args.getString("url")));
    }

    @Override
    public void onLoadFinished(Loader<ArrayList<Story>> loader, ArrayList<Story> stories) {
        // Hide the progress bar now that the Loader has finished
        setVisibilities(false, true, false);

        // Create an adapter and populate the ListView
        setListViewAdapter(stories);

        // Update the paginator
        updatePaginator();

        // Set the text of the empty view
        // (done here to prevent empty text showing when the app first loads)
        emptyView.setText(getResources().getString(R.string.empty_text));
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Story>> loader) {
        loader.reset();
    }

    //endregion Loader callback methods

}