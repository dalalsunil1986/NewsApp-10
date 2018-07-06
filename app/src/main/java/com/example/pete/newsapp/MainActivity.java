package com.example.pete.newsapp;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Rect;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

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

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<ArrayList<Story>>, SwipeRefreshLayout.OnRefreshListener {

    //region Constants and Instance Variables

    // Constants
    private static final int READ_TIMEOUT_SECONDS = 10;
    private static final int RESPONSE_TIMEOUT_SECONDS = 15;
    private static final int MS_IN_SECOND = 1000;
    private static final String ENCODING = "UTF-8";
    private static final int LOADER_ID = 0;
    private static final String SAVED_PARCELABLE_KEY = "savedStoriesArrayKey";

    // API-related Constants
    private static final String QUERY_BASE_URL = "https://content.guardianapis.com/search?&show-tags=contributor";
    private static final byte[] E = {1, -71, -127, -55, -63, -61, 25, -103, -119, 105, -87, -101, 25, -127, 105, -93, 49, -95, -87, 107, 17, -103, -53, 25, 105, -119, -55, -63, -61, 17, -119, -71, -125, 35, 11, 25, -56};
    private static final int SHIFT_SEED = 3;

    // User Interface-related Constants
    private static final int VERTICAL_SPACING_RECYCLER_VIEW = 0;

    // Set these booleans to test response validation behaviors
    public static final boolean DEBUG_SIMULATE_SLOW_CONNECTION = false;
    private static final boolean DEBUG_SIMULATE_NO_CONNECTION = false;
    public static final boolean DEBUG_SIMULATE_NO_RESULTS = false;
    private static final boolean DEBUG_LOG_API_QUERY_URLS = false;

    // Settings / Preferences
    public static SharedPreferences sharedPreferences;
    private static String currentPillar = "Latest Stories";

    // Query parameters (used by .getQueryUrl() to build queries based on user input)
    // (See also: Story.currentPage and Story.totalPages)
    private String query_orderBy = "newest"; // This is used instead of the settings.order_by in certain contexts
    private String query_sections = ""; // Since the API doesn't support Pillars, this is a list of sections known to be in the selected Pillar
    private int query_pageSize = 10;
    private String query_edition = "";

    // Pagination / storyAdapter variables
    private StoryAdapter storyAdapter;
    private LinearLayoutManager layoutManager;
    private boolean isLoadingNextPage = false;

    // View References
    private TextView emptyView;
    private ProgressBar progressSpinner;
    private SwipeRefreshLayout swipe_refresh;
    private RecyclerView storiesRecyclerView;
    private android.support.design.widget.AppBarLayout appBarLayout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private MainActivity mainActivity;

    //endregion Constants and Instance Variables

    //region Project Notes
    /*
    +: implement Navigation Drawer (see TourGuideApp) (fragments)
    +: browse sections via  Navigation Drawer (discover & confirm sections via 'guardian sections.json')
    +: implement Settings screen
    +: Handle screen rotation (don't forget which Pillar was last selected)
        See Udacity Lesson 5.7.6: URL Building
        (This should also solve a problem with returning to MainActivity from the SettingsActivity)
        (Be wary of setting the page number in MainActivity.onCreate.
         Maybe it shouldn't be set in onCreate once the above issue has been solved.)

    +: onSave / onRestore Instance States and screen rotation
        for some reason navDrawer_selectPillar() still has to be called in onCreate
        the results don't interfere with restoring the storyAdapter contents and its scroll position
        they're just discarded, I guess
        + (fixed) figure out why _selectPillar() must be called. Does it have to do with the setVisibilities() stuff I'm doing in onCreate()?

    +: Pull down RecyclerView to refresh it
        use SwipeRefreshLayout

    +: If sortBy = oldest, change Latest Stories to Oldest Stories
        (Can cause user confusion if they're on the Latest Stories pillar
         and change setting to "oldest" but nothing happens)

    todo: Add search feature
        Should be relatively simple
        - Icon in toolbar
        - Clicking icon gives InputText box
        - Pressing OK in InputText sets a query_search variable
        - getQueryURL gives the API that search term
        - getQueryURL also sets query_orderBy to "relevance"
        - how should rotating screen or leaving MainActivity affect search results?
          should the user have to navigate away from search results to clear them?
          should search results have their own back button in the toolbar?

    todo: bug: Set background of appBarLayout when changing themes doesn't work on Nexus 5X device until the app is restarted
        + Works in Android Emulator

    +: customize app icon
        See TourGuide app for an example
    */
    //endregion Project Notes

    //region Activity Overrides

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get View references (MainActivity)
        getViewReferences_MainActivity();

        // Get Preferences (store as as instance variable of MainActivity)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        // Set the default Preference values (doesn't happen automatically for some reason)
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        // Make sure we're on the first page of results when entering MainActivity
        Story.currentPage = 1;

        // Set UI colors based on user preference
        setUIColors();

        // Set up the Toolbar / Action Bar (show nav drawer icon)
        navDrawer_setUpToolbar();

        // Define navigation view on click behaviors
        navDrawer_setItemSelectedBehaviors();

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
        setRecyclerViewAdapter(this, new ArrayList<>());

        // Initialize the SwipeRefreshLayout (pull down at the top of the list to refresh)
        initialize_SwipeRefreshLayout();

        // Change UI, reset Loader, load content for selected Pillar
        if (savedInstanceState == null) {
            navDrawer_selectPillar(currentPillar, true);
        } else {
            // Don't reload the Pillar, just set RecyclerView visible if restoring from saved state
            setVisibilities(false, true, false);
        }

        // Select the current Nav Drawer item (otherwise the first item will be selected no matter what Pillar we're in)
        navDrawer_setSelectedItem();

        Log.d("MainActivity.onCreate", "onCreate was called");
    }

    @Override
    // Called when leaving Activity (rotating, going to Settings Activity)
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("onSaveInstanceState", "called");

        // Make sure the Loading Footer isn't in the Stories array (it would interfere with Parceling)
        storyAdapter.removeLoadingFooter();

        // Save the ArrayList of Stories
        outState.putParcelableArrayList(SAVED_PARCELABLE_KEY, storyAdapter.getStories());

        Log.d("onSaveInstanceState", storyAdapter.getItemCount() + " items in storyAdapter.");
    }

    @Override
    // Called when restoring Activity (rotating)
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("onRestoreInstanceState", "called");

        // Make sure the storyAdapter is empty
        storyAdapter.removeAll();

        // Restore the saved data set (ArrayList of Stories)
        storyAdapter.addAll(savedInstanceState.getParcelableArrayList(SAVED_PARCELABLE_KEY));

        // Add the Loading Footer
        storyAdapter.addLoadingFooter();

        Log.d("onRestoreInstanceState", storyAdapter.getItemCount() + " items in storyAdapter.");
    }

    @Override
    // Called when returning from Settings by pressing Back button
    protected void onResume() {
        super.onResume();

        // Refresh the RecyclerView. This happens automatically if the "Home" button is pressed but not the Back button

        Story.currentPage = 1;
        storyAdapter.removeAll();

        navDrawer_selectPillar(currentPillar, false);

        Log.d("onResume", "called");
    }

    //endregion Activity Overrides

    // Build the Query URL (using instance variables)
    private String getQueryUrl() {
        // Get settings
        String setting_edition = sharedPreferences.getString(
                getString(R.string.edition_key_name),
                getString(R.string.edition_default_value));
        String setting_orderBy = sharedPreferences.getString(
                getString(R.string.order_by_key_name),
                getString(R.string.order_by_default_value));
        String setting_pageSize = sharedPreferences.getString(
                getString(R.string.page_size_key_name),
                getString(R.string.page_size_default_value));

        // Set query_ variables according to setting_ variables: (sometimes query_ overrides setting_)

        // Override edition if this is "all" (in which case it needs to be blank)
        if (setting_edition.equals(getString(R.string.internal_edition_all_value))) {
            query_edition = "";
        } else {
            query_edition = setting_edition;
        }

        query_orderBy = setting_orderBy;

        // todo: Ignore orderBy setting if a search is being done (order_by = relevance)

        query_pageSize = Integer.parseInt(setting_pageSize);

        // todo: Use Uri.Builder for the next section of code:

        // Build individual parameter arguments ("" if no argument provided)
        String param_orderBy = query_orderBy.equals("") ? "" : "&order-by=" + query_orderBy;
        String param_section = query_sections.equals("") ? "" : "&section=" + query_sections;
        String param_page_size = "&page-size=" + query_pageSize;
        String param_edition = query_edition.equals("") ? "" : "&production-office=" + query_edition;

        String query = QUERY_BASE_URL + param_orderBy + param_section + param_edition + param_page_size + "&page=" + currentPage + "&api-key=" + d(E);

        if (DEBUG_LOG_API_QUERY_URLS) {
            Log.d("getQueryUrl", query);
        }

        return query;
    }

    //region UI-related methods

    // Update the Main Activity UI with a given ArrayList of Stories
    private void setRecyclerViewAdapter(MainActivity mainActivity, ArrayList<Story> stories) {
        if (storiesRecyclerView == null) {
            return;
        }

        // Create a new StoryAdapter of stories
        storyAdapter = new StoryAdapter(this, 0, stories);

        // Could initialize an ItemAnimator here

        // Initialize LayoutManager
        layoutManager = new LinearLayoutManager(this);

        // Initialize ItemDecorators
        VerticalSpaceItemDecorator verticalSpaceItemDecorator =
                new VerticalSpaceItemDecorator();

        // For performance, tell OS that RecyclerView won't change size
        storiesRecyclerView.setHasFixedSize(true);

        // Set the LayoutManager
        storiesRecyclerView.setLayoutManager(layoutManager);

        // Set the ItemDecorators
        storiesRecyclerView.addItemDecoration(verticalSpaceItemDecorator);

        // Attach the storyAdapter to the RecyclerView so that the list will be populated in the user interface
        storiesRecyclerView.setAdapter(storyAdapter);

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
        swipe_refresh = findViewById(R.id.swipe_refresh);
        storiesRecyclerView = findViewById(R.id.stories_recycler_view);
        emptyView = findViewById(R.id.empty_text_view);
        progressSpinner = findViewById(R.id.progress_spinner);
        appBarLayout = findViewById(R.id.main_app_bar_layout);
        drawerLayout = findViewById(R.id.main_drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        mainActivity = this;
    }

    // Set theme colors based on user preference (list items' colors are set in StoryHolder.setPillarAndSectionColors)
    private void setUIColors() {
        // Get the color scheme from preferences
        String setting_color_scheme =
                sharedPreferences.getString(
                        getString(R.string.color_scheme_key_name),
                        getString(R.string.color_scheme_default_value));

        // Interpret the color scheme as a boolean
        Boolean USE_DARK_THEME_COLORS = setting_color_scheme.equals(getString(R.string.internal_color_scheme_dark));

        if (USE_DARK_THEME_COLORS) {
            appBarLayout.setBackgroundColor(getColorResource(R.color.theme_background_medium));
        } else {
            appBarLayout.setBackgroundColor(getColorResource(R.color.theme_background_light));
        }
    }

    // Display Toast notification
    public static void displayToast(Context context, String textToShow) {
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, textToShow, duration);
        toast.show();
    }

    //endregion UI-related methods

    //region Nav Drawer, ActionBar methods

    @Override
    // Open Nav Drawer or Settings (ActionBar buttons)
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Nav Drawer
                drawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.action_settings:
                // Settings
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    // Inflate menu_main.xml onto the toolbar (settings icon)
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return super.onCreateOptionsMenu(menu);
    }

    // Handle Nav Drawer item clicks (Latest Stories, News, etc)
    private void navDrawer_setItemSelectedBehaviors() {
        navigationView.setNavigationItemSelectedListener(item -> {
            // Set item as selected to persist highlight
            item.setChecked(true);

            // Close Nav Drawer after item selected
            drawerLayout.closeDrawers();

            // The title of the Nav Drawer item is the title of the Pillar
            String itemTitle = item.getTitle().toString();

            // Change UI, reset Loader, load content for selected Pillar
            navDrawer_selectPillar(itemTitle, false);

            return true;
        });
    }

    // Set which Nav Drawer item is selected (used when restoring Main Activity's state)
    private void navDrawer_setSelectedItem() {
        if (currentPillar.equals(getString(R.string.nav_latest_stories_title)) ||
                currentPillar.equals(getString(R.string.nav_oldest_stories_title))) {
            navigationView.getMenu().findItem(R.id.menu_latest).setChecked(true);
        } else if (currentPillar.equals(getString(R.string.pillar_news))) {
            navigationView.getMenu().findItem(R.id.menu_pillar_news).setChecked(true);
        } else if (currentPillar.equals(getString(R.string.pillar_opinion))) {
            navigationView.getMenu().findItem(R.id.menu_pillar_opinion).setChecked(true);
        } else if (currentPillar.equals(getString(R.string.pillar_sport))) {
            navigationView.getMenu().findItem(R.id.menu_pillar_sport).setChecked(true);
        } else if (currentPillar.equals(getString(R.string.pillar_culture))) {
            navigationView.getMenu().findItem(R.id.menu_pillar_culture).setChecked(true);
        } else if (currentPillar.equals(getString(R.string.pillar_lifestyle))) {
            navigationView.getMenu().findItem(R.id.menu_pillar_lifestyle).setChecked(true);
        } else if (currentPillar.equals(getString(R.string.pillar_more))) {
            navigationView.getMenu().findItem(R.id.menu_pillar_more).setChecked(true);
        } else {
            Log.d("navDrawer_setSelectedIt", "Unknown pillar title: " + currentPillar);
        }
    }

    // Change UI, reset Loader, load content for selected Pillar
    private void navDrawer_selectPillar(String pillarTitle, boolean initLoader) {
        // Show the progress spinner
        setVisibilities(true, false, false);

        Resources resources = getResources();

        // Change between "Latest Stories" or "Oldest Stories" based on setting Order By
        String allStoriesTitle = navDrawer_latestOrOldestStoriesName();

        if (pillarTitle.equals(resources.getString(R.string.nav_latest_stories_title)) || pillarTitle.equals(resources.getString(R.string.nav_oldest_stories_title))) {
            // Don't need to filter by Sections (just show everything)
            setTitle(allStoriesTitle);
            query_sections = "";
        } else if (pillarTitle.equals(resources.getString(R.string.pillar_news))) {
            setTitle(resources.getString(R.string.pillar_news));
            query_sections = resources.getString(R.string.pillar_news_section_ids);
        } else if (pillarTitle.equals(resources.getString(R.string.pillar_opinion))) {
            setTitle(resources.getString(R.string.pillar_opinion));
            query_sections = resources.getString(R.string.pillar_opinion_section_ids);
        } else if (pillarTitle.equals(resources.getString(R.string.pillar_sport))) {
            setTitle(resources.getString(R.string.pillar_sport));
            query_sections = resources.getString(R.string.pillar_sport_section_ids);
        } else if (pillarTitle.equals(resources.getString(R.string.pillar_culture))) {
            setTitle(resources.getString(R.string.pillar_culture));
            query_sections = resources.getString(R.string.pillar_culture_section_ids);
        } else if (pillarTitle.equals(resources.getString(R.string.pillar_lifestyle))) {
            setTitle(resources.getString(R.string.pillar_lifestyle));
            query_sections = resources.getString(R.string.pillar_lifestyle_section_ids);
        } else if (pillarTitle.equals(resources.getString(R.string.pillar_more))) {
            setTitle(resources.getString(R.string.pillar_more));
            query_sections = resources.getString(R.string.pillar_more_section_ids);
        }

        // Store the last selected Pillar so that it can be restored (leaving MainActivity, rotating screen)
        currentPillar = pillarTitle;

        // Reset and restart the Loading process
        storyAdapter.removeAll();

        Story.currentPage = 1;

        // Pass the URL in a Bundle
        Bundle bundle = new Bundle();
        bundle.putString("url", getQueryUrl());

        if (initLoader) {
            // Start the AsyncTaskLoader
            getLoaderManager().initLoader(LOADER_ID, bundle, this).forceLoad();
        } else {
            // Restart the AsyncTaskLoader
            getLoaderManager().restartLoader(LOADER_ID, bundle, mainActivity).forceLoad();
        }
    }

    /*
    The Latest Stories menu item can also be Oldest Stories, depending on user preferences
    This method sets the title of the menu item
    It returns the title so that navDrawer_selectPillar can set the ActionBar title if necessary
    */
    private String navDrawer_latestOrOldestStoriesName() {
        Resources resources = getResources();
        String result;

        // Get the orderBy setting from sharedPreferences
        String setting_orderBy = sharedPreferences.getString(
                getString(R.string.order_by_key_name),
                getString(R.string.order_by_default_value));

        if (setting_orderBy.equals(getString(R.string.api_order_by_oldest))) {
            // User chose "Latest Stories" and "Sort By Oldest" at the same time
            result = resources.getString(R.string.nav_oldest_stories_title);

            // Change the title of the Nav Drawer item
            navigationView.getMenu().findItem(R.id.menu_latest).setTitle(getString(R.string.nav_oldest_stories_title));
        } else {
            // User chose "Latest Stories" and "Sort By Newest"
            result = resources.getString(R.string.nav_latest_stories_title);

            // Change the title of the Nav Drawer item
            navigationView.getMenu().findItem(R.id.menu_latest).setTitle(getString(R.string.nav_latest_stories_title));
        }

        return result;
    }

    // Set up the Toolbar / Action Bar (show nav drawer icon)
    private void navDrawer_setUpToolbar() {
        android.support.v7.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        if (actionbar != null) {
            actionbar.setDisplayHomeAsUpEnabled(true);
            actionbar.setHomeAsUpIndicator(R.drawable.ic_menu);
        }
    }

    //endregion Nav Drawer, ActionBar methods

    //region SwipeRefreshLayout implementation

    // SwipeRefreshLayout: set onRefreshListener, set colors
    private void initialize_SwipeRefreshLayout() {
        // Set onRefresh Listener
        swipe_refresh.setOnRefreshListener(mainActivity);

        // Set colors
        swipe_refresh.setColorSchemeColors(getColorResource(R.color.theme_accent));
    }

    @Override
    public void onRefresh() {
        Story.currentPage = 1;
        storyAdapter.removeAll();

        navDrawer_selectPillar(currentPillar, false);

        swipe_refresh.setRefreshing(false);
    }

    //endregion SwipeRefreshLayout implementation

    //region RecyclerView Decorator Inner Classes

    /*
    Adds an additional amount of vertical spacing to the items in RecyclerView
    Currently unused because I have CardViews which provide their own spacing
    */
    private class VerticalSpaceItemDecorator extends RecyclerView.ItemDecoration {
        // see: https://traversoft.com/2016/01/31/replace-listview-with-recyclerview/

        private final int verticalSpaceHeight;

        VerticalSpaceItemDecorator() {
            this.verticalSpaceHeight = VERTICAL_SPACING_RECYCLER_VIEW;
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

    // Set the value of a String Shared Preference
    private void setSharedPreferenceValue(String settingKey, String settingValue) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(settingKey, settingValue);
        editor.apply();
    }

    // Returns new URL object from the given string URL.
    public static URL createUrl(String stringUrl) {
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e("createUrl", "Error creating URL", exception);
            return null;
        }
        return url;
    }

    private boolean isOnline() {
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
    private static String readFromStream(InputStream inputStream) throws IOException {
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

    // Get a color from resources
    private int getColorResource(int resourceID) {
        return getResources().getColor(resourceID);
    }

    //endregion Utility methods

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

    //region Loader callback methods

    @Override
    public Loader<ArrayList<Story>> onCreateLoader(int id, Bundle args) {
        return new StoryLoader(this, createUrl(args.getString("url")));
    }

    @Override
    // Update the RecyclerView with the newly retrieved list of Stories
    public void onLoadFinished(Loader<ArrayList<Story>> loader, ArrayList<Story> stories) {
        // Hide the progress bar now that the Loader has finished
        setVisibilities(false, true, false);

        if (stories.size() > 0) {
            // Remove the list_item_paginator_loading from the end of RecyclerView
            storyAdapter.removeLoadingFooter();
            isLoadingNextPage = false;

            // Add all the results to the storyAdapter
            storyAdapter.addAll(stories);

            // Add list_item_paginator_loading to the end of RecyclerView
            if (!isLastPage()) {
                storyAdapter.addLoadingFooter();
            }

            // Show only the RecyclerView
            setVisibilities(false, true, false);
        } else {
            // No results. Show only the empty TextView
            if (storyAdapter.getItemCount() == 0) {
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