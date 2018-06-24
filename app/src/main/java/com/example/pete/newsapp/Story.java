package com.example.pete.newsapp;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;

import static com.example.pete.newsapp.MainActivity.createUrl;

/*
Represents a Guardian news article
*/
public class Story {

    // Constants
    // https://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
    private static final String ENCODED_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String DATE_FORMAT = "MMM dd, yyyy";
    private static final String TIME_FORMAT = "hh:mm a";
    private static final Locale LOCALE = Locale.US;

    // Static variables
    // (properties of the response JSON Object; the same for every Story)
    static int currentPage = 1;
    static int totalPages = 1;

    private String sectionName;
    private String title;
    // Publication Date
    private String date;
    // Author = first contributor
    private String author;
    private String url;

    public Story(String sectionName, String title, String date, String author, String url) {
        setSectionName(sectionName);
        setTitle(title);
        setDate(date);
        setAuthor(author);
        setUrl(url);
    }

    public String getSectionName() {
        return sectionName;
    }

    private void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getTitle() {
        return title;
    }

    private void setTitle(String title) {
        this.title = title;
    }

    public String getDate_encodedString() {
        return date;
    }

    // Get the article publication date in a printable format
    public String getDate() {
        // parse the Date format to be more readable
        Date thisDate = getInterpretedDate();

        // Create a SimpleDateFormat which defines the output format
        SimpleDateFormat sdf_output = new SimpleDateFormat(DATE_FORMAT, LOCALE);

        return sdf_output.format(thisDate);
    }

    // Get the article publication time in a printable format
    public String getTime() {
        // parse the Date format to be more readable
        Date thisDate = getInterpretedDate();

        // Create a SimpleDateFormat which defines the output format
        SimpleDateFormat sdf_output = new SimpleDateFormat(TIME_FORMAT, LOCALE);

        return sdf_output.format(thisDate);
    }

    // Convert the encoded date String to a Date object
    private Date getInterpretedDate() {
        // Create a SimpleDateFormat to interpret the encoded date format
        SimpleDateFormat sdf_input = new SimpleDateFormat(ENCODED_DATE_FORMAT, LOCALE);
        Date thisDate = null;
        try {
            thisDate = sdf_input.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return thisDate;
    }

    private void setDate(String date) {
        this.date = date;
    }

    public String getAuthor() {
        return author;
    }

    private void setAuthor(String author) {
        this.author = author;
    }

    public String getUrl_asString() {
        return url;
    }

    public URL getUrl_asURL() {
        return createUrl(url);
    }

    private void setUrl(String url) {
        this.url = url;
    }

}
