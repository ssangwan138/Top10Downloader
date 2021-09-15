package com.example.top10downloader;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView listApps;
    private  String feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
    private int feedLimit = 10;
    private String cachedUrl = "Invalid";
    public static String currentUrl = "def";
    public static String currentLimit = "abc";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        listApps = (ListView)findViewById(R.id.xmlListView);
        if(savedInstanceState != null){
            feedUrl = savedInstanceState.getString(currentUrl);
            feedLimit = savedInstanceState.getInt(currentLimit);
        }

        downloadUrl(String.format(feedUrl,feedLimit));

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.feeds_menu, menu);
        if(feedLimit == 10){
            menu.findItem(R.id.mnu10).setChecked(true);
        }
        else if(feedLimit == 25){
            menu.findItem(R.id.mnu25).setChecked(true);
        }
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        switch (id){
            case R.id.mnuFree:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml";
                break;
            case R.id.mnuPaid:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/toppaidapplications/limit=%d/xml";
                break;
            case R.id.mnuSongs:
                feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml";
                break;
            case R.id.mnu10:
            case R.id.mnu25:
                if(!item.isChecked()){
                    item.setChecked(true);
                    feedLimit = 35 - feedLimit;
                    Log.d(TAG, "onOptionsItemSelected" + item.getTitle() + "feedlimit changed to" + feedLimit);
                }
                else{
                    Log.d(TAG, "onOptionsItemSelected" + item.getTitle() + "feedlimit unchanged");
                }break;
            case R.id.mnuRefresh:
                cachedUrl = "Invalid";
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        downloadUrl(String.format(feedUrl,feedLimit));
        return true;

    }
    private void downloadUrl(String feedUrl){
        if(!feedUrl.equalsIgnoreCase(cachedUrl)) {
            Log.d(TAG, "downloadUrl: Starting downloadUrl");
            DownloadData downloadData = new DownloadData();

            downloadData.execute(feedUrl);
            cachedUrl = feedUrl;
            Log.d(TAG, "downloadUrl: finished");
        }
        else
            Log.d(TAG, "downloadUrl: URL not changed");
    }


    private class DownloadData extends Thread{
        private static final String TAG = "DownloadData";
        protected void execute(final String url) {
            Log.d(TAG, "doInBackground: starts with " + url);

            new Thread(() -> {
                final String rssFeed;
                rssFeed = downloadXML(url);
                runOnUiThread(() -> {
                    // send the data back
                    onPostExecute(rssFeed);
                });
            }).start();

        }
        protected void onPostExecute(String s){
            if(s==null)
                Log.d(TAG, "onPostExecute: Error");
            else
            {
                Log.d(TAG, "onPostExecute: " + s);
                ParsingApplications parseApplications = new ParsingApplications();
                parseApplications.parse(s);
                FeedAdapter feedAdapter = new FeedAdapter( MainActivity.this,R.layout.list_record,parseApplications.getApplications());

                listApps.setAdapter(feedAdapter);

            }


        }
        private String downloadXML(String urlPath){
            StringBuilder xmlResult = new StringBuilder();
            try{
                URL url = new URL(urlPath);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int response = connection.getResponseCode();
                Log.d(TAG, "downloadXML: Response code was" + response);
                InputStream inputStream = connection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader reader = new BufferedReader(inputStreamReader);

                int charsRead;
                char[] inputBuffer =  new char[500];
                while(true){
                    charsRead = reader.read(inputBuffer);
                    if(charsRead<0)
                        break;
                    if(charsRead>0){
                        xmlResult.append(String.copyValueOf(inputBuffer,0,charsRead));
                    }

                }reader.close();
                return xmlResult.toString();
            }
            catch(MalformedURLException e){
                Log.e(TAG, "downloadXML: Invalid URL" + e.getMessage());
            }
            catch(IOException e){
                Log.e(TAG, "downloadXML: IO exception reading data" + e.getMessage());
            }
            catch (SecurityException e){
                Log.e(TAG, "downloadXML: Security Exception" + e.getMessage());
            }
            return null;

        }

    }

    @Override
    protected void onSaveInstanceState( Bundle outState) {
        outState.putString(currentUrl , feedUrl );
        outState.putInt(currentLimit , feedLimit);
        super.onSaveInstanceState(outState);
    }


};