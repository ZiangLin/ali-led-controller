package com.twotoasters.messageapidemo;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Text;

import ui.ColorPicker;
import ui.LightnessSlider;
import ui.OnValueChangedListener;

public class MyActivity extends Activity implements OnValueChangedListener {

    private static MyActivity singleton;
    private static GoogleApiClient client;

    static ColorPicker colorPicker;
    static TextView welcomeLabel;
    static LightnessSlider lightnessSlider;
    static int[] value = new int[4];
    int count =0;
    public static boolean doubleFinger = false;
    public static final String TAG = "ALI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        singleton = this;

        //start the connection to tablet
        Intent intent = new Intent(this, MobileWearableService.class);
        startService(intent);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                setupWidgets();
            }
        });
    }

    public static MyActivity getInstance(){
        return singleton;
    }


    /**
     * Sets up the button for handling click events.
     */
    private void setupWidgets() {
        colorPicker = (ColorPicker) findViewById(R.id.colorPicker);
        colorPicker.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                //Log.i(TAG,"color picker on touch");
                if(doubleFinger)//disable any touch after double finger tap
                    return true;
                if(event.getPointerCount()==2){
                    Intent intent = new Intent(MyActivity.this, ModeActivity.class);
                    startActivity(intent);
                    doubleFinger= true;
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    //System.out.println("moving");
                    if (count == 3) {
                        getColorAndSend();
                        count = 0;
                    } else
                        count++;
                } else if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        getColorAndSend();

                } else if(event.getAction() == MotionEvent.ACTION_UP) {
                    getColorAndSend();
                }
                return false;
            }
        });
        colorPicker.setVisibility(LinearLayout.GONE);

        lightnessSlider = (LightnessSlider) findViewById(R.id.brightnessSlider);
        lightnessSlider.setOnValueChangedListener(this);
        lightnessSlider.setVisibility(LinearLayout.GONE);

        welcomeLabel = (TextView) findViewById(R.id.welcomeLabel);
        welcomeLabel.setVisibility(LinearLayout.VISIBLE);
        welcomeLabel.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                queryState();
                return true;
            }
        });
    }

    /**
     * Returns a GoogleApiClient that can access the Wear API.
     * @param context
     * @return A GoogleApiClient that can make calls to the Wear API
     */
    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .build();
    }

    public static void  getColorAndSend(){
        String message = "";
            //get color first
            int color = colorPicker.getColor();
            value[0] = Color.red(color);
            value[1] = Color.green(color);
            value[2] = Color.blue(color);
            value[3] = 0;
            message = "" + value[0] + " " + value[1] + " " + value[2] + " " + value[3];
        //send to mobile device
       MobileWearableService.getInstance().sendMessage(message);
    }

    public void onTabletMessageReceived(final String message){
        //Log.i(TAG,message);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(message.equals("rgb")){
                    welcomeLabel.setVisibility(LinearLayout.GONE);
                    lightnessSlider.setVisibility(LinearLayout.GONE);
                    colorPicker.setVisibility(LinearLayout.VISIBLE);
                }else if(message.equals("white")){
                    welcomeLabel.setVisibility(LinearLayout.GONE);
                    colorPicker.setVisibility(LinearLayout.GONE);
                    lightnessSlider.setVisibility(LinearLayout.VISIBLE);
                }else if(message.equals("none")) {
                    welcomeLabel.setVisibility(LinearLayout.VISIBLE);
                    colorPicker.setVisibility(LinearLayout.GONE);
                    lightnessSlider.setVisibility(LinearLayout.GONE);
                }
            }
        });

    }

    @Override
    public void onValueChanged(int value, int action) {
        //send to mobile device
        if(action==MotionEvent.ACTION_DOWN){
            MobileWearableService.getInstance().sendMessage(String.format("%d %d %d 0", value, value, value));
        }else if(action==MotionEvent.ACTION_MOVE) {
            if (count == 3) {
                MobileWearableService.getInstance().sendMessage(String.format("%d %d %d 0", value, value, value));
                count = 0;
            } else
                count++;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //Log.d(TAG, "onResume");
    }

    private void queryState(){
        MobileWearableService.getInstance().sendMessage("queryState");
    }
}
