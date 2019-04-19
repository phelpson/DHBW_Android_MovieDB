package com.example.moviedatabase;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    //########create empty container
    //json Obect for movie search
    JSONObject jsonObject;
    //URL for building api query
    URL Url = null;
    // user input - movie title to search for
    EditText titleSearch;
    // generated param for URL
    String replacedURL;
    //search button
    Button searchbutton;

    //emptydb-manager container
    protected DatenbankManager _datenbankManager = null;
    //empty listview
    private ListView ResultsList;
    private ListView FavList;
    //array adapter for result and favlist
    private ArrayAdapter<String> ResultsListAdapter;
    private ArrayAdapter<String> FavListAdapter;
    //pagecounter for switching between pages
    public int pageCounter = 1;
    //########

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //db manager
        _datenbankManager = new DatenbankManager(this);

        //create Host Layout
        final TabHost host = findViewById(R.id.tabHost);
        host.setup();

        //Tab 1
        TabHost.TabSpec spec = host.newTabSpec("Movie Search");
        spec.setContent(R.id.tab1);
        spec.setIndicator("Movie Search");
        host.addTab(spec);

        //Tab 2
        spec = host.newTabSpec("Favourites");
        spec.setContent(R.id.tab2);
        spec.setIndicator("Favourites");
        host.addTab(spec);

        host.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            //check if tab changes
            @Override
            public void onTabChanged(String tabId) {

                int i = host.getCurrentTab();

                if (i == 0) {
                } else if (i == 1) {
                    createFavList();
                }

            }
        });

        //get Button for starting search and set listener
        searchbutton = findViewById(R.id.buttonSearch);
        searchbutton.setOnClickListener(this);

    }

    //creates the list with all movies set to favourite
    public void createFavList() {

        //create fav list from db
        try {
            //create movie favourite List to store
            String[] movieFavs = _datenbankManager.getFavMovieList();
            //get favList List
            FavList = findViewById(R.id.FavList);
            //create new favlistadapter
            FavListAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.simple_list_row, movieFavs);
            //set Items to ListView
            FavList.setAdapter(FavListAdapter);
            //set listener on each list object for getting into detail view of movie
            FavList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                //getApplicationContext <=> this
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position
                        , long id) {
                    //get selected movie title
                    String selectedMovieTitle = (String) FavList.getItemAtPosition(position);
                    //create new intent to change from mainactivity to detailview activity
                    Intent detailview = new Intent(MainActivity.this, detail_view.class);
                    //put movie title as extra for creating the detail view
                    detailview.putExtra("Title", selectedMovieTitle);
                    //start detail view activity
                    startActivity(detailview);
                }
            });

        } catch (Exception e) {
            Log.d("db Exception", "keine DB vorhanden");
        }
    }

    /**
     * onClick method for starting searching for movies in database
     */
    @Override
    public void onClick(View v) {

        //create connection manager for checking the internet connection
        ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        // getting network info about wifi status
        NetworkInfo mWifi = connManager.getActiveNetworkInfo();
        //check if wifi is connected
        if (mWifi != null) {

            if( v == searchbutton ){
               Log.d("searchButton","search button clicked");
               pageCounter = 1;
            }
            //build URL for API query
            buildURL();

            //start thread with build url
            ThreadClass tc = new ThreadClass();
            tc.execute("ThreadRun");

            //hide keyboard after entering search
            InputMethodManager inputManager = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        } else {
            //if there is no connection create Toast with information for the user
            Toast.makeText(MainActivity.this, "Keine Internetverbindung! Bitte Wifi oder mobile Daten aktivieren!", Toast.LENGTH_LONG).show();
        }
    }

    //building the url with parameter for api request
    public void buildURL() {

        //get User Input
        titleSearch = findViewById(R.id.text_input);
        String userInput = titleSearch.getText().toString();
        //build URl
        replacedURL = "s=" + userInput;
        //replace space with +
        replacedURL = replacedURL.replace(" ", "+");

        //try to run api query
        try {
            //put url together
            Url = new URL("https://www.omdbapi.com/?" + replacedURL + "&page=" + pageCounter + "&apikey=342e82b5");
        } catch (Exception e) {
            Log.d("urlBuild", "Url build failed");
        }
    }

    //count pagecounter up to get to next page
    public void countPageUp(View v) {
        //count up
        pageCounter++;
        //start onClick method to get listitems from next page
        onClick(v);
    }

    //count pagecounter down to get to previous page
    public void countPageDown(View v) {
        //count down
        if(pageCounter >1) {
            pageCounter--;
        }else{
            Toast.makeText(MainActivity.this, "Sie befinden sich bereits auf der ersten Seite!", Toast.LENGTH_LONG).show();
        }
        //start onClick method to get listitems from next page
        onClick(v);
    }

    //method to get data from WebAPI
    public static String holeDatenVonWebAPI(URL url) throws Exception {

        //create params
        HttpURLConnection conn = null;
        String httpErgebnisDokument = "";

        //open connection to api
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int st = conn.getResponseCode();

        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            Log.d("arrayNull", "json response null");
            String errorMessage = "HTTP-Fehler: " + conn.getResponseMessage();
            throw new Exception(errorMessage);
        } else {
            //open stream and read json
            InputStream is = conn.getInputStream();
            InputStreamReader ris = new InputStreamReader(is);
            BufferedReader reader = new BufferedReader(ris);

            String zeile = "";
            while ((zeile = reader.readLine()) != null) {
                httpErgebnisDokument += zeile;
            }
        }
        //Log.d("httpDok", httpErgebnisDokument);
        return httpErgebnisDokument;

    }

    public class ThreadClass extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... backgroundString) {

            //empty container for search result string array
            String jsonDaten = "";

            try {
                //get Data from method
                jsonDaten = holeDatenVonWebAPI(Url);
            } catch (Exception e) {
                Log.d("ErrorGetData", "ERROR: " + e.getMessage());
            }
            return jsonDaten;
        }

        protected void onPostExecute(String searchResults) {

            try {
                //check if there are movies in searchResult
                if (searchResults.contains("Error")) {
                    Toast.makeText(MainActivity.this, "Keine Filme gefunden! Bitte erneut eingeben!", Toast.LENGTH_LONG).show();
                }
                //parsee search results to jsObject
                jsonObject = new JSONObject(searchResults);
                //get resultlist ui element
                ResultsList = findViewById(R.id.ResultsList);
                List<String> movietitles = new ArrayList<String>();

                //get root object
                JSONArray arrayResults = jsonObject.getJSONArray("Search");

                //iterate throug json to get all movie titles
                for (int i = 0; i < arrayResults.length(); i++) {
                    JSONObject resultObject = (JSONObject) arrayResults.get(i);
                    movietitles.add(resultObject.getString("Title"));
                }
                //create new movie arraylist and add results from searchresult
                ArrayList<String> movieArrayList = new ArrayList<String>();
                movieArrayList.addAll(movietitles);

                ResultsListAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.simple_list_row, movieArrayList);

                //set Items to ListView
                ResultsList.setAdapter(ResultsListAdapter);

                //Clickable Items in a ListView
                ResultsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                    //getApplicationContext <=> this
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position
                            , long id) {

                        //get movietitle of clickable listitem
                        String selectedMovieTitle = (String) ResultsList.getItemAtPosition(position);

                        Intent detailview = new Intent(MainActivity.this, detail_view.class);
                        detailview.putExtra("Title", selectedMovieTitle);
                        startActivity(detailview);
                    }
                });

            } catch (Exception e) {

            }

        }

    }
}
