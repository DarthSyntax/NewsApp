package stael.app.newsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;


public class MainActivity extends AppCompatActivity {
    ListView listView;
    ArrayList<String> titles;
    ArrayList<String> content;

    ArrayAdapter adapter;
    SQLiteDatabase articlesDB;

    public class DownloadJSON extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                InputStream in = connection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                int data = reader.read();
                while(data != -1) {
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;

                if(jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length();
                }

                //Delete the table upon startup so we get new articles
                articlesDB.execSQL("DELETE FROM articles");

                for(int i = 0; i < numberOfItems; i++) {
                    String articleID = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + articleID + ".json?print=pretty");

                    connection = (HttpURLConnection) url.openConnection();
                    in = connection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();

                    String articleResult = "";

                    while(data != -1) {
                        char current = (char) data;
                        articleResult += current;
                        data = reader.read();
                    }
                    //Log.i("Article Info", articleResult);

                    JSONObject jsonObject = new JSONObject(articleResult);

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleURL = jsonObject.getString("url");

                        url = new URL(articleURL);
                        connection = (HttpURLConnection) url.openConnection();
                        in = connection.getInputStream();
                        reader = new InputStreamReader(in);
                        data = reader.read();
                        String articleContent = "";

                        while(data != -1) {
                            char current = (char) data;
                            articleContent += current;
                            data = reader.read();
                        }

                        Log.i("Html", articleContent);

                        //Very Important
                        String sql = "INSERT INTO articles (articleID, title, content) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1, articleID);
                        statement.bindString(2, articleTitle);
                        statement.bindString(3, articleContent);

                        statement.execute();

                    }

                }

                //Log.i("URL Content", result);
                return result;

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            updateListView();
        }
    }

    public void updateListView() {
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);
        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()) {
            titles.clear();
            content.clear();

            do{
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while (c.moveToNext());
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID INTEGER, title VARCHAR, content VARCHAR)");

        DownloadJSON task = new DownloadJSON();
        try{
            //task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
        titles = new ArrayList<String>();
        content = new ArrayList<String>();
        listView = (ListView) findViewById(R.id.listView);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(), ArticleActivity.class);
                intent.putExtra("content", content.get(position));
                startActivity(intent);
            }
        });

        updateListView();


    }
}
