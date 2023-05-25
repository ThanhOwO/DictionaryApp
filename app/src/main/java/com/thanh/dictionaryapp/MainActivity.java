package com.thanh.dictionaryapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private ListView resultListView;
    private Button saveInternalButton, saveExternalButton, copyExternalButton;

    private SQLiteDatabase database;

    private SharedPreferences sharedPreferences;

    private static final String PREF_NAME = "dictionary_prefs";
    private static final String KEY_LAST_SEARCH = "last_search";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.edit_text_search);
        resultListView = findViewById(R.id.list_view_results);
        saveInternalButton = findViewById(R.id.button_save_internal);
        saveExternalButton = findViewById(R.id.button_save_external);
        copyExternalButton = findViewById(R.id.button_copy_external);

        // Initialize the SQLite database
        SQLiteOpenHelper dbHelper = new DictionaryDBHelper(this);
        database = dbHelper.getReadableDatabase();

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        // Restore the last search text from shared preferences
        String lastSearch = sharedPreferences.getString(KEY_LAST_SEARCH, "");
        searchEditText.setText(lastSearch);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Not used
            }

            @Override
            public void afterTextChanged(Editable s) {
                String searchText = s.toString();
                searchDictionary(searchText);
            }
        });

        resultListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedItem = (String) parent.getItemAtPosition(position);
                searchEditText.setText(selectedItem);
            }
        });

        saveInternalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDictionaryToInternalStorage();
            }
        });

        saveExternalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveDictionaryToExternalStorage();
            }
        });

        copyExternalButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyDictionaryToExternalStorage();
            }
        });

    }

    private void searchDictionary(String searchText) {
        // Save the current search text to shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_LAST_SEARCH, searchText);
        editor.apply();

        List<String> results = new ArrayList<>();

        // Search for the exact word in the dictionary
        Cursor cursor = database.rawQuery("SELECT definition FROM dictionary WHERE word = ?", new String[]{searchText});
        if (cursor.moveToFirst()) {
            // Exact match found
            String definition = cursor.getString(0);
            results.add(definition);
        } else {
            // No exact match found, search for words containing the search text
            cursor = database.rawQuery("SELECT word FROM dictionary WHERE word LIKE ?", new String[]{"%" + searchText + "%"});
            if (cursor.moveToFirst()) {
                do {
                    String word = cursor.getString(0);
                    results.add(word);
                } while (cursor.moveToNext());
            }
        }

        cursor.close();

        // Display the search results in the ListView
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, results);
        resultListView.setAdapter(adapter);
    }

    // Database helper class
    private static class DictionaryDBHelper extends SQLiteOpenHelper {

        private static final String DB_NAME = "dictionary.db";
        private static final int DB_VERSION = 1;

        private final Context context;

        public DictionaryDBHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
            this.context = context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            // Create the dictionary table
            db.execSQL("CREATE TABLE dictionary (word TEXT, definition TEXT)");

            // Populate the dictionary with sample data
            db.execSQL("INSERT INTO dictionary VALUES ('apple', 'A round fruit with red or green skin and crisp flesh')");
            db.execSQL("INSERT INTO dictionary VALUES ('banana', 'A long curved fruit with a yellow skin')");
            db.execSQL("INSERT INTO dictionary VALUES ('cat', 'A small domesticated carnivorous mammal')");
            db.execSQL("INSERT INTO dictionary VALUES ('dog', 'A domesticated carnivorous mammal')");
            db.execSQL("INSERT INTO dictionary VALUES ('elephant', 'A large mammal with a long trunk and tusks')");
            db.execSQL("INSERT INTO dictionary VALUES ('flower', 'The reproductive structure of a plant')");
            db.execSQL("INSERT INTO dictionary VALUES ('guitar', 'A stringed musical instrument')");
            db.execSQL("INSERT INTO dictionary VALUES ('hamburger', 'A sandwich consisting of a cooked patty of ground meat')");
            db.execSQL("INSERT INTO dictionary VALUES ('island', 'A piece of land surrounded by water')");
            db.execSQL("INSERT INTO dictionary VALUES ('jungle', 'A dense forest in a tropical region')");
            db.execSQL("INSERT INTO dictionary VALUES ('koala', 'A small herbivorous marsupial native to Australia')");
            db.execSQL("INSERT INTO dictionary VALUES ('lion', 'A large carnivorous feline')");
            db.execSQL("INSERT INTO dictionary VALUES ('mountain', 'A large natural elevation of the earth')");
            db.execSQL("INSERT INTO dictionary VALUES ('night', 'The period of darkness between sunset and sunrise')");
            db.execSQL("INSERT INTO dictionary VALUES ('ocean', 'A vast body of saltwater that covers most of the Earth')");
            db.execSQL("INSERT INTO dictionary VALUES ('piano', 'A musical instrument with a keyboard')");
            db.execSQL("INSERT INTO dictionary VALUES ('quartz', 'A hard mineral consisting of silicon dioxide')");
            db.execSQL("INSERT INTO dictionary VALUES ('rainbow', 'A meteorological phenomenon that is caused by reflection')");
            db.execSQL("INSERT INTO dictionary VALUES ('sun', 'The star that is the central body of the solar system')");
            db.execSQL("INSERT INTO dictionary VALUES ('tree', 'A woody perennial plant')");


        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // Not used
        }
    }

    // Save dictionary data to internal storage
    private void saveDictionaryToInternalStorage() {
        try {
            // Create a file in internal storage
            File file = new File(getFilesDir(), "dictionary.txt");
            FileOutputStream outputStream = new FileOutputStream(file);

            // Retrieve dictionary data from the SQLite database
            Cursor cursor = database.rawQuery("SELECT word, definition FROM dictionary", null);
            if (cursor.moveToFirst()) {
                do {
                    String word = cursor.getString(0);
                    String definition = cursor.getString(1);

                    // Write the word and definition to the file
                    outputStream.write((word + ": " + definition + "\n").getBytes());
                } while (cursor.moveToNext());
            }

            cursor.close();
            outputStream.close();

            Toast.makeText(this, "Dictionary data saved to internal storage", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save dictionary data", Toast.LENGTH_SHORT).show();
        }
    }

    // Save dictionary data to external storage
    private void saveDictionaryToExternalStorage() {
        try {
            if (isExternalStorageWritable()) {
                // Create a file in external storage
                File file = new File(getExternalFilesDir(null), "dictionary.txt");
                FileOutputStream outputStream = new FileOutputStream(file);

                // Retrieve dictionary data from the SQLite database
                Cursor cursor = database.rawQuery("SELECT word, definition FROM dictionary", null);
                if (cursor.moveToFirst()) {
                    do {
                        String word = cursor.getString(0);
                        String definition = cursor.getString(1);

                        // Write the word and definition to the file
                        outputStream.write((word + ": " + definition + "\n").getBytes());
                    } while (cursor.moveToNext());
                }

                cursor.close();
                outputStream.close();

                Toast.makeText(this, "Dictionary data saved to external storage", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "External storage is not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save dictionary data", Toast.LENGTH_SHORT).show();
        }
    }

    // Check if external storage is writable
    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    // Copy dictionary data from internal storage to external storage
    private void copyDictionaryToExternalStorage() {
        try {
            if (isExternalStorageWritable()) {
                // Create a file in external storage
                File internalFile = new File(getFilesDir(), "dictionary.txt");
                File externalFile = new File(getExternalFilesDir(null), "dictionary_copy.txt");

                FileInputStream inputStream = new FileInputStream(internalFile);
                FileOutputStream outputStream = new FileOutputStream(externalFile);

                // Copy the contents of the internal file to the external file
                FileChannel inChannel = inputStream.getChannel();
                FileChannel outChannel = outputStream.getChannel();
                inChannel.transferTo(0, inChannel.size(), outChannel);

                inputStream.close();
                outputStream.close();

                Toast.makeText(this, "Dictionary data copied to external storage", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "External storage is not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to copy dictionary data", Toast.LENGTH_SHORT).show();
        }
    }


}