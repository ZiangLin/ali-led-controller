package com.twotoasters.messageapidemo;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingFormatArgumentException;
import java.util.concurrent.TimeUnit;

public class MobileWearableService extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    private boolean nodeConnected = false;
    private final int TIMEOUT_S = 1;
    //test for data api
    private static final String PATH_DATA = "/ALI/ColorData";
    public static final String TAG = "ALI";
    private static String nodeId;
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private static MobileWearableService singleton;
    public static String currentState="none";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"start Listener Service");
        singleton = this;
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
    public void onConnected(Bundle bundle) {
        if(!nodeConnected) {
            nodeConnected = true;
            Log.i(TAG, "node connected");
            Wearable.MessageApi.addListener(mGoogleApiClient, this);
            //test for data api
            Wearable.DataApi.addListener(mGoogleApiClient, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        nodeConnected = false;
        Log.i(TAG,"node connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        nodeConnected = false;
        Log.i(TAG,"node connection failed");
    }

    //test for data api
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Log.i(TAG,"on data changed");
        final ArrayList<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);

        for (DataEvent event : events) {
            PutDataMapRequest putDataMapRequest =
                    PutDataMapRequest.createFromDataMapItem(DataMapItem.fromDataItem(event.getDataItem()));

            String path = event.getDataItem().getUri().getPath();
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                if (PATH_DATA.equals(path)) {
                    DataMap dataMap = putDataMapRequest.getDataMap();
                    String data = dataMap.getString("data");
                    Log.i(TAG,"data:"+data);
                }


            } else if (event.getType() == DataEvent.TYPE_DELETED) {

            }
        }
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        String message = messageEvent.getPath();
        Log.i(TAG,message);
        if(message.equals("queryState")){//when query state send the current state
            sendMessage(currentState);
            return;
        }
        String v[] = message.split(" ");
        byte value[] =new  byte[4];
        for(int i=0;i<4;i++){
            value[i]=(byte)Integer.parseInt(v[i]);
        }
        //notify main activity
        MainActivity.getInstance().onWatchMessageRecevided(value);
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
                        Log.i(TAG, "node id=" + nodeId);
                    }
                    mGoogleApiClient.disconnect();
                }catch(Exception ex){
                    Log.i(TAG,ex.getStackTrace().toString());
                }
            }
        }).start();
    }

    /**
     * Sends a message to the connected mobile device, telling it to show a Toast.
     */
    public void sendMessage(final String message) {
        currentState = message;
        if (nodeId != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    mGoogleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, nodeId, message, null);
                    Log.i(TAG,message);
                    mGoogleApiClient.disconnect();
                }
            }).start();
        }else{
            Log.i(TAG,"nodeId is null");
        }
    }

}
