/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.datalayer;

import static com.example.android.wearable.datalayer.DataLayerListenerService.LOGD;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.List;

/**
 * Shows events and photo from the Wearable APIs.
 */
public class MainActivity extends Activity implements ConnectionCallbacks,
        OnConnectionFailedListener, DataApi.DataListener, MessageApi.MessageListener,
        NodeApi.NodeListener {

    private static final String TAG = "MainActivity";

    private GoogleApiClient mGoogleApiClient;
    private TextView mIntroText;
    private View mLayout;
    private Handler mHandler;
    private TextClock mClock;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        mHandler = new Handler();
        LOGD(TAG, "onCreate");
        setContentView(R.layout.main_activity);
        mIntroText = (TextView) findViewById(R.id.intro);
        mLayout = findViewById(R.id.layout);
        mClock = (TextClock) findViewById(R.id.watch_time);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        LOGD(TAG, "registerReceiver()");
        registerReceiver(mActionReceiver, localIntentFilter);

    }

    @Override
    protected void onResume() {
        super.onResume();
        LOGD(TAG, "onResume()");

        mGoogleApiClient.connect();

        mClock.setBackgroundColor(Color.argb(128, 0, 0, 0));
        final Bitmap bitmap = DataLayerListenerService.lastBitmap;
        if(bitmap == null) {
            mLayout.setBackground(null);
            mIntroText.setVisibility(View.VISIBLE);
        }else {
            mLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
            mIntroText.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LOGD(TAG, "onPause()");

        Wearable.DataApi.removeListener(mGoogleApiClient, this);
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();

        mClock.setBackgroundColor(Color.argb(0, 0, 0, 0));
        mLayout.setBackground(null);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LOGD(TAG, "onDestroy()");

        try {
            LOGD(TAG, "unregisterReceiver()");
            unregisterReceiver(mActionReceiver);
        } catch (Exception e) {
        }

    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
        Wearable.NodeApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        LOGD(TAG, "onDataChanged(): " + dataEvents);

        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
        dataEvents.close();
    }

    @Override
    public void onMessageReceived(MessageEvent event) {
        LOGD(TAG, "onMessageReceived: " + event);
    }

    @Override
    public void onPeerConnected(Node node) {
    }

    @Override
    public void onPeerDisconnected(Node node) {
    }

    private static class DataItemAdapter extends ArrayAdapter<Event> {

        private final Context mContext;

        public DataItemAdapter(Context context, int unusedResource) {
            super(context, unusedResource);
            mContext = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(android.R.layout.two_line_list_item, null);
                convertView.setTag(holder);
                holder.text1 = (TextView) convertView.findViewById(android.R.id.text1);
                holder.text2 = (TextView) convertView.findViewById(android.R.id.text2);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            Event event = getItem(position);
            holder.text1.setText(event.title);
            holder.text2.setText(event.text);
            return convertView;
        }

        private class ViewHolder {

            TextView text1;
            TextView text2;
        }
    }

    private class Event {

        String title;
        String text;

        public Event(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    IntentFilter localIntentFilter = new IntentFilter("com.google.android.clockwork.home.action.BACKGROUND_ACTION");
    private final BroadcastReceiver mActionReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent data) {
            LOGD(TAG, "onReceive: " + data);
            Bundle extras = data.getExtras();
            for(String key : extras.keySet()) {
                LOGD(TAG, key + " : " + extras.get(key));
            }

            if (data.hasExtra("ambient_mode")) {
                boolean isAmbient = data.getBooleanExtra("ambient_mode", false);
                LOGD(TAG, "Ambient: " + isAmbient);
            }
        }
    };

}
