package com.example.pete.newsapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.example.pete.newsapp.MainActivity.createUrl;

/*
Represents a Guardian news article
*/
class Story implements Parcelable {

    //region Constants and Instance Variables

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

    private String pillarName; // Stories are organized like Pillar > Section
    private String sectionName;
    private String title; // The article title
    private String date; // Publication Date
    private String author; // Author = first contributor
    private String url;

    //endregion Constants and Instance Variables

    public Story(String pillarName, String sectionName, String title, String date, String author, String url) {
        setPillarName(pillarName);
        setSectionName(sectionName);
        setTitle(title);
        setDate(date);
        setAuthor(author);
        setUrl(url);
    }

    //region Parcelable methods

    // Restore from saved parcel
    private Story(Parcel parcel) {
        String[] data = new String[6];

        parcel.readStringArray(data);

        setPillarName(data[0]);
        setSectionName(data[1]);
        setTitle(data[2]);
        setDate(data[3]);
        setAuthor(data[4]);
        setUrl(data[5]);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] {this.pillarName,
                                            this.sectionName,
                                            this.title,
                                            this.date,
                                            this.author,
                                            this.url});
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Story createFromParcel(Parcel in) {
            return new Story(in);
        }

        public Story[] newArray(int size) {
            return new Story[size];
        }
    };

    //endregion Parcelable methods

    //region Getters and Setters

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

    public String getPillarName() {
        return pillarName;
    }

    private void setPillarName(String pillarName) {
        this.pillarName = pillarName;
    }

    //endregion Getters and Setters

}