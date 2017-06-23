package com.twotoasters.messageapidemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by ziangl on 6/20/2017.
 */

public class MobileWearableService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private GoogleApiClient mGoogleApiClient;
    private boolean nodeConnected = false;
    private final int TIMEOUT_S = 1;
    private static final String PATH_DATA = "/ALI/ColorData";
    public static final String TAG = "ALI";
    public static int count = 0;
    private static String nodeId;
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static MobileWearableService singleton;
    @Override
    public void onCreate()
    {
        super.onCreate();
        singleton = this;
        //Log.i(TAG,"start Mobile Wearable Services");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        //for sending message
        retrieveDeviceNode();
    }

    public static MobileWearableService getInstance(){
        return singleton;
    }
    @Override
    public void onConnected(Bundle bundle)
    {
        if(!nodeConnected) {
            nodeConnected = true;
            //Log.i(TAG, "node connected");
            //for receiving message
            Wearable.MessageApi.addListener(mGoogleApiClient, this);
            //test for data api
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    while(true) {
//                        if (nodeConnected) {
//                            Log.i(TAG,"send data");
//                            sendData("test connection"+count++);
//                            Thread.sleep(5000);
//                        }
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
        }
    }

    @Override
    public void onConnectionSuspended(int i)
    {
        nodeConnected = false;
        //Log.i(TAG,"node connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        nodeConnected = false;
        //Log.i(TAG,"node connection failed");
    }

    //test for data api
    private void sendData(final String dataToSend)
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                if (!nodeConnected)
                {
                    mGoogleApiClient.blockingConnect(TIMEOUT_S, TimeUnit.SECONDS);
                }
                if (!nodeConnected)
                {
                    //Log.e(TAG, "Failed to connect to mGoogleApiClient within " + TIMEOUT_S + " seconds");
                    return;
                }

                if (mGoogleApiClient.isConnected())
                {
                    PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_DATA);
                    putDataMapRequest.getDataMap().putString("data", dataToSend);
                    PutDataRequest request = putDataMapRequest.asPutDataRequest();

                    PendingResult<DataApi.DataItemResult> pendingResult =
                            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                    //Log.i(TAG,"put data item");
                    pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>()
                    {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult)
                        {
                            if(dataItemResult.getStatus().isSuccess()) {
                                //Log.i(TAG,"successfully put data item");
                                //Log.d(TAG, "Data item set: " + dataItemResult.getDataItem().getUri());
                            }else{
                                //Log.i(TAG,dataItemResult.getStatus().toString());
                            }
                        }
                    });

                } else {
                    //Log.e(TAG, "No Google API Client connection");
                }
            }
        }).start();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String message = messageEvent.getPath();
        MyActivity.getInstance().onTabletMessageReceived(message);
    }

    /**
     * Connects to the GoogleApiClient and retrieves the connected device's Node ID. If there are
     * multiple connected devices, the first Node ID is returned.
     */
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    NodeApi.GetConnectedNodesResult result =
                            Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
                    List<Node> nodes = result.getNodes();
                    if (nodes.size() > 0) {
                        nodeId = nodes.get(0).getId();
                        //Log.i(TAG, "node id=" + nodeId);
                    }
                    mGoogleApiClient.disconnect();
                }catch(Exception ex){
                    //Log.i(TAG,ex.getStackTrace().toString());
                }
            }
        }).start();
    }

    /**
     * Sends a message to the connected mobile device, telling it to show a Toast.
     */
    public void sendMessage(final String message) {
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, message, null);
                    //Log.i(TAG,message);
                    mGoogleApiClient.disconnect();
                }
            }).start();
        }else{
            //Log.i(TAG,"nodeId is null");
        }
    }
}
