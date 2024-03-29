package com.plusultra.csci448.journalshare;

/**
 * JournalDbSchema is the schema definition of the local database.
 *
 * Created by ndeibert on 5/7/2018.
 */

public class JournalDbSchema {
    public static final class JournalTable {
        public static final String NAME = "journals";
        public static final class Cols {
            public static final String UUID = "uuid";
            public static final String TITLE = "title";
            public static final String TEXT = "text";
            public static final String DATE = "date";
            public static final String LAT = "lat";
            public static final String LON = "lon";
        }
    }
}
