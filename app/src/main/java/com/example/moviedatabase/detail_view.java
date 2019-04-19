package com.example.moviedatabase;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class detail_view extends AppCompatActivity {

    //ui elements
    TextView MovieTitle;
    WebView poster;
    JSONObject jsonObject;
    URL Url = null;
    EditText titleSearch;
    TextView IMDBRating;
    TextView Released;
    TextView Plot;
    DatenbankManager _dbmanager;
    ToggleButton buttonFavorite;
    String movieSelection;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_view);

        //get support action bar for enabling back arrow
        this.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //get all UI Elements
        MovieTitle = findViewById(R.id.Title);
        poster = findViewById(R.id.webViewPoster);
        titleSearch = findViewById(R.id.text_input);
        IMDBRating = findViewById(R.id.IMDBRating);
        Released = findViewById(R.id.Released);
        Plot = findViewById(R.id.Plot);
        //create new intent
        Intent previousMainActivityIntent = getIntent();
        //get movietitle from previous intent
        movieSelection = previousMainActivityIntent.getStringExtra("Title");
        //create new dbmanager
        _dbmanager = new DatenbankManager(this);

        try {
            //remove illegal characters for building the url
            movieSelection = movieSelection.replace(" ", "+");
            movieSelection = movieSelection.replace(":", "%3A");

            //create url with movietitle as parameter
            Url = new URL("https://www.omdbapi.com/?t=" + movieSelection + "&apikey=342e82b5");
        } catch (Exception e) {
            Log.d("urlBuild", "Url Build failed");
        }
        //start thread
        ThreadClass tc = new ThreadClass();
        tc.execute("test");
    }

    //initalise favourite Button icon in detail view
    public void favButtonInit(View v) {

        buttonFavorite = findViewById(R.id.button_favheart);

        //Check, is the toggle is on
        boolean on = ((ToggleButton) v).isChecked();

        if (on) {
            //if button is toggled and its on, save movie as new favourite in db
            saveMovieAsFav(buttonFavorite);
        } else {
            //if button is toggled and its off, delete movie from favourite movie table in db
            deleteMovieAsFav(buttonFavorite);
        }
    }

    //save title as new favourite movie in db
    public void saveMovieAsFav(View view) {
        //get movietitle
        String movieTitle = MovieTitle.getText().toString();
        DatenbankManager dm = new DatenbankManager(this);
        //save title
        dm.saveTitle(movieTitle);
        //output for user
        Toast.makeText(this, "Film als Favourit gespeichert!",
                Toast.LENGTH_LONG).show();
    }

    //delete title from favourite movie table in db
    public void deleteMovieAsFav(View view) {
        //get movietitle
        String movieTitle = MovieTitle.getText().toString();
        movieTitle = movieTitle.replace("Title", "");
        DatenbankManager dm = new DatenbankManager(this);
        //delete title
        dm.deleteTitle(movieTitle);
        //output for user
        Toast.makeText(this, "Film aus Favouriten entfernt!",
                Toast.LENGTH_LONG).show();
    }

    // get data from webapi with previous build url
    public static String holeDatenVonWebAPI(URL url) throws Exception {

        //create params
        HttpURLConnection conn = null;
        String httpErgebnisDokument = "";

        //open connection to api
        conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        int st = conn.getResponseCode();
        //if connection doesnt return OK code, create error message
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
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
        return httpErgebnisDokument;

    }

    public class ThreadClass extends AsyncTask<String, Void, String> {


        protected String doInBackground(String... backgroundString) {
            //create empty string
            String jsonDaten = "";

            try {
                //get Data from method
                jsonDaten = holeDatenVonWebAPI(Url);

            } catch (Exception e) {
                Log.d("ErrorGetData", "ERROR: " + e.getMessage());
            }
            return jsonDaten;
        }

        protected void onPostExecute(String parsedString) {

            //check if movie already in db
            Boolean inDB = _dbmanager.checkTitle(movieSelection);
            buttonFavorite = findViewById(R.id.button_favheart);
            if (inDB == true) {
                buttonFavorite.setChecked(true);
            } else {
                buttonFavorite.setChecked(false);
            }

            //set different types of UI Elements
            try {
                //parse json object
                jsonObject = new JSONObject(parsedString);
                //set movietitle
                MovieTitle.setText(jsonObject.getString("Title"));
                //set image of movie poster
                poster.loadUrl(jsonObject.getString("Poster"));
                //set imdb rating
                IMDBRating.setText("IMDB Rating: " + jsonObject.getString("imdbRating") + "/10");
                //set release date
                Released.setText("Released: " + jsonObject.getString("Released"));
                //set plot text
                Plot.setText("Plot: " + jsonObject.getString("Plot"));
            } catch (Exception e) {

            }

        }


    }
}
