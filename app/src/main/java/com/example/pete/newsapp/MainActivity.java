package com.example.pete.newsapp;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import static com.example.pete.newsapp.Story.currentPage;
import static com.example.pete.newsapp.Story.totalPages;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<Story>> {

    //region Constants and Instance Variables

    // Constants
    private static final int READ_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 15;
    private static final int MS_IN_SECOND = 1000;
    private static final String ENCODING = "UTF-8";
    private static final int LOADER_ID = 0;

    // API-related Constants
    private static final String QUERY_BASE_URL = "https://content.guardianapis.com/search?&show-tags=contributor";
    private static final byte[] E = {1, -71, -127, -55, -63, -61, 25, -103, -119, 105, -87, -101, 25, -127, 105, -93, 49, -95, -87, 107, 17, -103, -53, 25, 105, -119, -55, -63, -61, 17, -119, -71, -125, 35, 11, 25, -56};
    private static final int SHIFT_SEED = 3;

    // User Interface-related Constants
    private static final String HEADER = "The Guardian";
    public static final boolean USE_DARK_THEME_COLORS = false; // Affects section name colors
    private static final int VERTICAL_SPACING_RECYCLER_VIEW = 0;

    // Set these booleans to test response validation behaviors
    public static final boolean DEBUG_SIMULATE_SLOW_CONNECTION = false;
    public static final boolean DEBUG_SIMULATE_NO_CONNECTION = false;
    public static final boolean DEBUG_SIMULATE_NO_RESULTS = false;
    private static final boolean DEBUG_LOG_API_QUERY_URLS = true;

    // Query parameters (used by .getQueryUrl() to build queries based on user input)
    // (See also: Story.currentPage and Story.totalPages)
    String query_orderBy = "newest";
    String query_section = "commentisfree";

    // Pagination variables
    StoryAdapter adapter;
    private boolean isLoadingNextPage = false;

    // View References
    TextView emptyView;
    ProgressBar progressSpinner;
    RecyclerView storiesRecyclerView;
    View paginator;

    //endregion Constants and Instance Variables

    //region Project Notes
    /*
    todo: implement Navigation Drawer (see TourGuideApp) (fragments)
    todo: browse sections via  Navigation Drawer (discover & confirm sections via 'guardian sections.json')

    */
    //endregion Project Notes

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle(HEADER);

        // Get View references (MainActivity)
        getViewReferences_MainActivity();

        // Set the stories RecyclerView and empty TextView as "Gone" so that we only see the Progress Spinner
        setVisibilities(true, false, false);

        // Set the text of the empty view
        emptyView.setText(getResources().getString(R.string.empty_text));

        // Check network connectivity
        if (!isOnline()) {
            // Show only the empty view (no internet connection message)
            setVisibilities(false, false, true);

            emptyView.setText(getResources().getString(R.string.no_internet_text));

            return;
        }

        // Create an adapter and populate the ListView
        setRecyclerViewAdapter(this, new ArrayList<Story>());

        // Pass the URL in a Bundle
        Bundle bundle = new Bundle();
        bundle.putString("url", getQueryUrl());

        // Start the AsyncTaskLoader
        getLoaderManager().initLoader(LOADER_ID, bundle, this).forceLoad();
    }

    // Build the Query URL (using instance variables)
    private String getQueryUrl() {
        String query = QUERY_BASE_URL + "&order-by=" + query_orderBy + "&section=" + query_section + "&page=" + currentPage + "&api-key=" + d(E);

        if (DEBUG_LOG_API_QUERY_URLS) {
            Log.d("getQueryUrl", query);
        }

        return query;
    }

    //region encoding / decoding

    /*
    Encode a given String
    Returns the String representation of an encoded byte array
    This byte array is intended to be hard-coded as a constant
    */
    private String e(String key) {
        byte[] bytes = key.getBytes(Charset.forName(ENCODING));
        byte[] bytesE = new BigInteger(bytes).shiftLeft(SHIFT_SEED).toByteArray();

        return Arrays.toString(bytesE);
    }

    /*
    Decode a given byte array
    Undoes the encoding done by the e() method
    d() and e() are used to obfuscate the API Key
    */
    private String d(byte[] bytes) {
        byte[] bytesD = new BigInteger(bytes).shiftRight(SHIFT_SEED).toByteArray();
        return new String(bytesD, Charset.forName(ENCODING));
    }

    //endregion encoding / decoding

    //region UI-related methods

    // Update the Main Activity UI with a given ArrayList of Stories
    private void setRecyclerViewAdapter(MainActivity mainActivity, ArrayList<Story> stories) {
        if (storiesRecyclerView == null) {
            return;
        }

        // Create a new StoryAdapter of stories
        adapter = new StoryAdapter(this, 0, stories);

        // Could initialize an ItemAnimator here

        // Initialize LayoutManager
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);

        // Initialize ItemDecorators
        VerticalSpaceItemDecorator verticalSpaceItemDecorator =
                new VerticalSpaceItemDecorator(VERTICAL_SPACING_RECYCLER_VIEW);

        // For performance, tell OS that RecyclerView won't change size
        storiesRecyclerView.setHasFixedSize(true);

        // Set the LayoutManager
        storiesRecyclerView.setLayoutManager(layoutManager);

        // Set the ItemDecorators
        storiesRecyclerView.addItemDecoration(verticalSpaceItemDecorator);

        // Attach the adapter to the RecyclerView so that the list will be populated in the user interface
        storiesRecyclerView.setAdapter(adapter);

        // Set a scroll listener for the storiesRecyclerView
        // (Support pagination - infinite scrolling behavior)
        storiesRecyclerView.addOnScrollListener(new PaginationScrollListener(layoutManager) {
            @Override
            protected void loadMoreItems() {
                isLoadingNextPage = true;
                Story.currentPage += 1;

                // Pass the URL in a Bundle
                Bundle bundle = new Bundle();
                bundle.putString("url", getQueryUrl());

                // Restart the AsyncTaskLoader (load next page)
                getLoaderManager().restartLoader(LOADER_ID, bundle, mainActivity).forceLoad();
            }

            @Override
            public int getTotalPageCount() {
                return Story.totalPages;
            }

            @Override
            public boolean isLastPage() {
                return mainActivity.isLastPage();
            }

            @Override
            public boolean isLoading() {
                return isLoadingNextPage;
            }
        });
    }

    // Set visibility of the Main Activity UI components
    private void setVisibilities(boolean progressSpinnerVisible, boolean storiesRecyclerViewVisible, boolean emptyTextViewVisible) {
        // Use the conditional operator:
        progressSpinner.setVisibility(progressSpinnerVisible ? View.VISIBLE : View.GONE);
        storiesRecyclerView.setVisibility(storiesRecyclerViewVisible ? View.VISIBLE : View.GONE);
        emptyView.setVisibility(emptyTextViewVisible ? View.VISIBLE : View.GONE);
    }

    // Get references to Views in activity_main.xml, store them in instance variables of MainActivity
    private void getViewReferences_MainActivity() {
        // Main Activity Views
        storiesRecyclerView = findViewById(R.id.stories_recycler_view);
        emptyView = findViewById(R.id.empty_text_view);
        progressSpinner = findViewById(R.id.progress_spinner);
    }

    //endregion UI-related methods

    //region RecyclerView Decorator Inner Classes

    /*
    Adds an additional amount of vertical spacing to the items in RecyclerView
    Currently unused because I have CardViews which provide their own spacing
    */
    private class VerticalSpaceItemDecorator extends RecyclerView.ItemDecoration {
        // see: https://traversoft.com/2016/01/31/replace-listview-with-recyclerview/

        private final int verticalSpaceHeight;

        VerticalSpaceItemDecorator(int verticalSpaceHeight) {
            this.verticalSpaceHeight = verticalSpaceHeight;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {

            // 1. Determine if we want to add a spacing decorator
            if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {

                // 2. Set the bottom offset to the specified height
                outRect.bottom = verticalSpaceHeight;
            }
        }
    }

    //endregion RecyclerView Decorator Inner Classes

    //region Utility methods

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
        NetworkInfo networkInfo = null;
        if (connMgr != null) {
            networkInfo = connMgr.getActiveNetworkInfo();
        }
        return (networkInfo != null && networkInfo.isConnected());
    }

    private boolean isLastPage() {
        return Story.currentPage >= Story.totalPages;
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
            urlConnection.setReadTimeout(READ_TIMEOUT_SECONDS * MS_IN_SECOND /* milliseconds */);
            urlConnection.setConnectTimeout(RESPONSE_TIMEOUT_SECONDS * MS_IN_SECOND /* milliseconds */);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful (response code 200), then read the input stream and parse the response.
            if (urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK) {
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
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName(ENCODING));
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

        if (stories.size() > 0) {
            // Remove the list_item_paginator_loading from the end of RecyclerView
            adapter.removeLoadingFooter();
            isLoadingNextPage = false;
            adapter.addAll(stories);

            if (!isLastPage()) {
                adapter.addLoadingFooter();
            }

            setVisibilities(false, true, false);
        } else {
            // No results
            if (adapter.getItemCount() == 0) {
                setVisibilities(false, false, true);
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<ArrayList<Story>> loader) {
        loader.reset();
    }

    //endregion Loader callback methods

}