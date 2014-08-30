package com.example.android.wearable.datalayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import twitter4j.MediaEntity;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * Created by morodomi on 2014/08/30.
 */
public class TwitterImage {

    public interface OnReceiveTwitterImageListener {
        /**
         * called when received image bitmap
         * @param bitmap
         */
        public void onReceiveTwitterImage(Bitmap bitmap);

        /**
         * called
         * @param message
         */
        public void onFailedTwitterImage(String message);
    }

    private OnReceiveTwitterImageListener listener = null;

    private String queryString = "%23%E3%82%B0%E3%83%A9%E3%83%89%E3%83%AB%E8%87%AA%E6%92%AE%E3%82%8A%E9%83%A8";
    private String extension = ".jpg";

    /**
     * Constructor
     * @param listener
     */
    public TwitterImage(OnReceiveTwitterImageListener listener) {
        this.listener = listener;
    }

    private void setQuery(String query) {
        this.queryString = query;
    }


    public void getImage() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int width = 0, height = 0;
                    String imageUrl = "";
                    while(width < 600 || height < 600) {
                        imageUrl = getImageUrl();
                        HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                        connection.setDoInput(true);
                        connection.connect();
                        InputStream is = connection.getInputStream();
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeStream(is, null, options);
                        width = options.outWidth;
                        height = options.outHeight;
                    }
                    Log.d("JIDORIBU", "URL:" + imageUrl + " 画像サイズ h:" + height + "  w:" + width);
                    HttpURLConnection connection = (HttpURLConnection) new URL(imageUrl).openConnection();
                    connection.setDoInput(true);
                    connection.connect();
                    InputStream is = connection.getInputStream();
                    BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(is, true);
                    Rect rect = new Rect(0, 0, 600, 600);
                    Bitmap bitmap = decoder.decodeRegion(rect, new BitmapFactory.Options());
                    listener.onReceiveTwitterImage(bitmap);
                } catch (TwitterException e) {
                    Log.d("JIDORIBU", e.getMessage(), e);
                } catch (MalformedURLException e) {
                    Log.d("JIDORIBU", e.getMessage(), e);
                } catch (IOException e) {
                    Log.d("JIDORIBU", e.getMessage(), e);
                }
            }
        }).start();
    }
        /**
         * @return Most Favorite Image URL
         * @throws twitter4j.TwitterException
         * @throws java.net.MalformedURLException
         * @throws java.io.IOException
         */
    private String getImageUrl() throws TwitterException, MalformedURLException, IOException {
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
                .setOAuthConsumerKey("dlqiziQgwWOXqRlhCiXyPktwc")
                .setOAuthConsumerSecret("kmgwbGddH6BNMWS5qww8WBDKPJVYQlqeFpSYxk4hMmZaYSdzzB")
                .setOAuthAccessToken("386623809-wBzPmMYLMfsaotTEcwLzu06IJE4GbrLpYjA3kSBL")
                .setOAuthAccessTokenSecret("2gpqiXfSQ8gNrYvkRArMS3iZj9dmWsZWKUYGvRD1LwtFo");
        Twitter twitter = new TwitterFactory(cb.build()).getInstance();
        Query query = new Query();
        query.setQuery(queryString);
        query.setLang("ja");
        query.setCount(100);
        QueryResult result = twitter.search(query);
        List<String> urls = new ArrayList<String>();
        for(Status sts : result.getTweets()) {
            MediaEntity[] arrayMedia = sts.getMediaEntities();
            for (MediaEntity media : arrayMedia) {
                if (media.getMediaURL().endsWith(extension)) {
                    urls.add(media.getMediaURL());
                }
            }
        }
        Log.d("JIDORIBU", "SINCE:" + result.getSinceId() + " MAX:" + result.getMaxId());
        // ランダム
        return urls.get(new Random().nextInt(urls.size()));
    }
}
