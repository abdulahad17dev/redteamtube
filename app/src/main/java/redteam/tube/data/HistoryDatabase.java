package redteam.tube.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class HistoryDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "youtube_history.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_HISTORY = "history";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_URL = "url";
    private static final String COLUMN_TITLE = "title";
    private static final String COLUMN_TIMESTAMP = "timestamp";

    private static HistoryDatabase instance;

    public static synchronized HistoryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new HistoryDatabase(context.getApplicationContext());
        }
        return instance;
    }

    private HistoryDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE " + TABLE_HISTORY + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_URL + " TEXT NOT NULL, " +
                COLUMN_TITLE + " TEXT, " +
                COLUMN_TIMESTAMP + " INTEGER NOT NULL)";
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
    }

    public void addHistory(String url, String title) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_URL, url);
        values.put(COLUMN_TITLE, title);
        values.put(COLUMN_TIMESTAMP, System.currentTimeMillis());
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    public List<SearchHistory> getAllHistory() {
        List<SearchHistory> historyList = new ArrayList<>();
        String query = "SELECT * FROM " + TABLE_HISTORY + " ORDER BY " + COLUMN_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(query, null);

        if (cursor.moveToFirst()) {
            do {
                int idIndex = cursor.getColumnIndex(COLUMN_ID);
                int urlIndex = cursor.getColumnIndex(COLUMN_URL);
                int titleIndex = cursor.getColumnIndex(COLUMN_TITLE);
                int timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP);

                long id = idIndex != -1 ? cursor.getLong(idIndex) : 0;
                String url = urlIndex != -1 ? cursor.getString(urlIndex) : "";
                String title = titleIndex != -1 ? cursor.getString(titleIndex) : "";
                long timestamp = timestampIndex != -1 ? cursor.getLong(timestampIndex) : 0;

                historyList.add(new SearchHistory(id, url, title, timestamp));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyList;
    }

    public void clearHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }

    public void deleteHistoryItem(long id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }
}
