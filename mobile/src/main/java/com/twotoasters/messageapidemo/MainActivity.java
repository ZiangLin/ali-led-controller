/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.twotoasters.messageapidemo;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import bluetooth.DeviceListActivity;
import bluetooth.UartService;
import ui.ColorPicker;
import ui.LightnessSlider;
import ui.OnValueChangedListener;
import ui.SaveColorView;

public class MainActivity extends Activity implements RadioGroup.OnCheckedChangeListener, OnValueChangedListener {
    private static MainActivity singleton;
    public static final String TAG = "nRFUART";
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    private static final int UART_PROFILE_READY = 10;
    public static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private static final int STATE_OFF = 10;
    private static final String PREFS = "prefs";
    private static final String SAVE1 = "save1_color";
    private static final String SAVE2 = "save2_color";
    private static final String SAVE3 = "save3_color";
    private static final String SAVE4 = "save4_color";
    TextView mRemoteRssiVal;
    RadioGroup mRg;
    byte[] value = new byte[4];
    int count;
    boolean isRGBLayout;
    SharedPreferences mSharedPreferences;
    public boolean whiteOn = false;
    public boolean androidWearOnRGB = false;
    public boolean androidWearOnWhiteOnly = false;
    public boolean colorShowOn = false;
    int speed = 60;
    public int mState = UART_PROFILE_DISCONNECTED;
    public UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;
    private ListView messageListView;
    private ArrayAdapter<String> listAdapter;
    private Button btnConnectDisconnect;//,btnSend;
    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;
            //*********************//
            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        char check = mDevice.getName().charAt(3);
                        if (check >= '0' && check <= '9') {//it is rgb lamp
                            //isRGBLayout = true;
                            welcomeLayout.setVisibility(LinearLayout.GONE);
                            rgbLayout.setVisibility(LinearLayout.VISIBLE);
                            MobileWearableService.getInstance().sendMessage("rgb");
                        } else if(mDevice.getName().substring(3,mDevice.getName().length()).equals("PC12")) {//it is white only lamp
                            welcomeLayout.setVisibility(LinearLayout.GONE);
                            brightnessLayout.setVisibility(LinearLayout.VISIBLE);
                            MobileWearableService.getInstance().sendMessage("white");
                        } else{//upwash,downwash,readings
                            welcomeLayout.setVisibility(LinearLayout.GONE);
                            twoWhiteLayout.setVisibility(LinearLayout.VISIBLE);
                        }
//                         seekBar1.setEnabled(true);
//                         seekBar2.setEnabled(true);
//                         seekBar3.setEnabled(true);


                        //edtMessage.setEnabled(true);
                        //btnSend.setEnabled(true);
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - ready");
                        listAdapter.add("[" + currentDateTimeString + "] Connected to: " + mDevice.getName());
                        //messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            //*********************//
            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");

                        //show welcome screen when disconnected
                        welcomeLayout.setVisibility(LinearLayout.VISIBLE);
                        rgbLayout.setVisibility(LinearLayout.GONE);
                        brightnessLayout.setVisibility(LinearLayout.GONE);
                        twoWhiteLayout.setVisibility(LinearLayout.GONE);

                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        listAdapter.add("[" + currentDateTimeString + "] Disconnected to: " + mDevice.getName());
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.disconnect();
                        mService.close();
                        //setUiState();

                    }
                });
            }


            //*********************//
            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }
            //*********************//
            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            listAdapter.add("[" + currentDateTimeString + "] RX: " + text);
                            messageListView.smoothScrollToPosition(listAdapter.getCount() - 1);

                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            //*********************//
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                //showMessage("Connecting");
                //mService.disconnect();
            }


        }
    };
    // private EditText edtMessage;
    private LinearLayout rgbLayout;
    private LinearLayout welcomeLayout;
    private RelativeLayout brightnessLayout;
    private RelativeLayout twoWhiteLayout;
    private LightnessSlider brightnessSlider;
    private SeekBar seekBarColorSpeed;
    private SeekBar seekBarWhite;
    private SeekBar seekBar3;
    private ColorPicker colorPicker;
    private SaveColorView save1;
    private SaveColorView save2;
    private SaveColorView save3;
    private SaveColorView save4;
    private Button delete;
    private Button white;
    private Button androidWearRGB;
    private Button androidWearWhiteOnly;
    private Button colorShow;
    private Button stopColorShow;
    private Toast address;
    //UART service connected/disconnected
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

        }

        public void onServiceDisconnected(ComponentName classname) {
            ////     mService.disconnect(mDevice);
            mService = null;
        }
    };
    private Handler mHandler = new Handler() {
        @Override

        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }

    public static MainActivity getInstance() {
        return singleton;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        singleton = this;
        setContentView(R.layout.main);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        //messageListView = (ListView) findViewById(R.id.listMessage);
        listAdapter = new ArrayAdapter<String>(this, R.layout.message_detail);
        // messageListView.setAdapter(listAdapter);
        //messageListView.setDivider(null);
        rgbLayout = (LinearLayout) findViewById(R.id.rgbLayout);
        welcomeLayout = (LinearLayout) findViewById(R.id.welcomeLayout);
        brightnessLayout = (RelativeLayout) findViewById(R.id.brightnessLayout);
        twoWhiteLayout = (RelativeLayout) findViewById(R.id.twoWhiteLayout);
        rgbLayout.setVisibility(LinearLayout.GONE);
        brightnessLayout.setVisibility(LinearLayout.GONE);
        twoWhiteLayout.setVisibility(LinearLayout.GONE);

        //create Lightness slider and register listener to it
        brightnessSlider = (LightnessSlider) findViewById(R.id.brightnessSlider);
        brightnessSlider.setOnValueChangedListener(this);

        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        //btnSend=(Button) findViewById(R.id.sendButton);
        //edtMessage = (EditText) findViewById(R.id.sendText);


        service_init();

        //init the control for two white panel
        TwoWhiteActivity ac = new TwoWhiteActivity();


        // Handler Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices

                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                            //change the watch to welcome screen
                            MobileWearableService.getInstance().sendMessage("none");
                        }
                    }
                }
            }
        });

        colorPicker = (ColorPicker) findViewById(R.id.colorPicker);
        colorPicker.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int color = colorPicker.getColor();
                value[0] = (byte) Color.red(color);
                value[1] = (byte) Color.green(color);
                value[2] = (byte) Color.blue(color);
                value[3] = 0;
                //decrease the amount of data send through the bluetooth
                // to increase reaction speed
                if (count == 0) {
                    if (whiteOn == false && colorShowOn == false && androidWearOnRGB == false) {
                        sendValue(value);
                    } else if (whiteOn == true) {//cancel the old toast if exist and show a new toast
                        handleToast("Turn the white off to change color");
                    } else if (colorShowOn == true) {
                        handleToast("Turn the color show off to change color");
                    } else if (androidWearOnRGB == true) {
                        handleToast("Turn the android wear off to change color");
                    }
                    count++;
                } else if (count == 1) {
                    count = 0;
                }
                //unselect all the saves if exist
                if (save1 != null)
                    save1.unselect();
                if (save2 != null)
                    save2.unselect();
                if (save3 != null)
                    save3.unselect();
                if (save4 != null)
                    save4.unselect();
                return false;
            }
        });

        save1 = (SaveColorView) findViewById(R.id.saveColor1);
        save2 = (SaveColorView) findViewById(R.id.saveColor2);
        save3 = (SaveColorView) findViewById(R.id.saveColor3);
        save4 = (SaveColorView) findViewById(R.id.saveColor4);
        //get the save color information from  SharedPreferences
        mSharedPreferences = getSharedPreferences(PREFS, MODE_PRIVATE);
        String color1 = mSharedPreferences.getString(SAVE1, "");
        if (color1.length() > 0) {//if color exist
            save1.setColor(Integer.parseInt(color1));
            save1.state = "save";
            save1.invalidate();
        }
        String color2 = mSharedPreferences.getString(SAVE2, "");
        if (color2.length() > 0) {//if color exist
            save2.setColor(Integer.parseInt(color2));
            save2.state = "save";
            save2.invalidate();
        }
        String color3 = mSharedPreferences.getString(SAVE3, "");
        if (color3.length() > 0) {//if color exist
            save3.setColor(Integer.parseInt(color3));
            save3.state = "save";
            save3.invalidate();
        }
        String color4 = mSharedPreferences.getString(SAVE4, "");
        if (color4.length() > 0) {//if color exist
            save4.setColor(Integer.parseInt(color4));
            save4.state = "save";
            save4.invalidate();
        }
        save1.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (save1.state.equals("empty")) {
                    save1.setColor(colorPicker.getColor());
                    save1.state = "save";
                    save1.invalidate();
                }
                if (save1.state.equals("save")) {
                    save1.state = "select";
                    save1.invalidate();
                    save2.unselect();
                    save3.unselect();
                    save4.unselect();
                    int color = save1.getColor();
                    //change color to the selected color on the color picker
                    colorPicker.setColor(color);
                    colorPicker.invalidate();
                    //send the color through bluetooth
                    value[0] = (byte) Color.red(color);
                    value[1] = (byte) Color.green(color);
                    value[2] = (byte) Color.blue(color);
                    value[3] = 0;
                    if (whiteOn == false && colorShowOn == false && androidWearOnRGB == false) {
                        sendValue(value);

                    } else if (whiteOn == true) {//cancel the old toast if exist and show a new toast
                        handleToast("Turn the white off to change color");
                    } else if (colorShowOn == true) {
                        handleToast("Turn the color show off to change color");
                    } else if (androidWearOnRGB == true) {
                        handleToast("Turn the android wear off to change color");
                    }
                }

                return false;
            }
        });
        save2.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (save2.state.equals("empty")) {
                    save2.setColor(colorPicker.getColor());
                    save2.state = "save";
                    save2.invalidate();
                }
                if (save2.state.equals("save")) {
                    save2.state = "select";
                    save2.invalidate();
                    save1.unselect();
                    save3.unselect();
                    save4.unselect();
                    int color = save2.getColor();
                    //change color to the selected color on the color picker
                    colorPicker.setColor(color);
                    colorPicker.invalidate();
                    value[0] = (byte) Color.red(color);
                    value[1] = (byte) Color.green(color);
                    value[2] = (byte) Color.blue(color);
                    value[3] = 0;
                    if (whiteOn == false && colorShowOn == false && androidWearOnRGB == false) {
                        sendValue(value);
                    } else if (whiteOn == true) {//cancel the old toast if exist and show a new toast
                        handleToast("Turn the white off to change color");
                    } else if (colorShowOn == true) {
                        handleToast("Turn the color show off to change color");
                    } else if (androidWearOnRGB == true) {
                        handleToast("Turn the android wear off to change color");
                    }
                }
                return false;
            }
        });
        save3.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (save3.state.equals("empty")) {
                    save3.setColor(colorPicker.getColor());
                    save3.state = "save";
                    save3.invalidate();
                }
                if (save3.state.equals("save")) {
                    save3.state = "select";
                    save3.invalidate();
                    save2.unselect();
                    save1.unselect();
                    save4.unselect();
                    int color = save3.getColor();
                    //change color to the selected color on the color picker
                    colorPicker.setColor(color);
                    colorPicker.invalidate();
                    value[0] = (byte) Color.red(color);
                    value[1] = (byte) Color.green(color);
                    value[2] = (byte) Color.blue(color);
                    value[3] = 0;
                    if (whiteOn == false && colorShowOn == false && androidWearOnRGB == false) {
                        sendValue(value);
                    } else if (whiteOn == true) {//cancel the old toast if exist and show a new toast
                        handleToast("Turn the white off to change color");
                    } else if (colorShowOn == true) {
                        handleToast("Turn the color show off to change color");
                    } else if (androidWearOnRGB == true) {
                        handleToast("Turn the android wear off to change color");
                    }
                }
                return false;
            }
        });
        save4.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (save4.state.equals("empty")) {
                    save4.setColor(colorPicker.getColor());
                    save4.state = "save";
                    save4.invalidate();
                }
                if (save4.state.equals("save")) {
                    save4.state = "select";
                    save4.invalidate();
                    save2.unselect();
                    save3.unselect();
                    save1.unselect();
                    int color = save4.getColor();
                    //change color to the selected color on the color picker
                    colorPicker.setColor(color);
                    colorPicker.invalidate();
                    value[0] = (byte) Color.red(color);
                    value[1] = (byte) Color.green(color);
                    value[2] = (byte) Color.blue(color);
                    value[3] = 0;
                    if (whiteOn == false && colorShowOn == false && androidWearOnRGB == false) {
                        sendValue(value);
                    } else if (whiteOn == true) {//cancel the old toast if exist and show a new toast
                        handleToast("Turn the white off to change color");
                    } else if (colorShowOn == true) {
                        handleToast("Turn the color show off to change color");
                    } else if (androidWearOnRGB == true) {
                        handleToast("Turn the android wear off to change color");
                    }
                }
                return false;
            }
        });
        delete = (Button) findViewById(R.id.delete);
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (save1.state.equals("select")) {
                    save1.state = "empty";
                    save1.invalidate();
                } else if (save2.state.equals("select")) {
                    save2.state = "empty";
                    save2.invalidate();
                } else if (save3.state.equals("select")) {
                    save3.state = "empty";
                    save3.invalidate();
                } else if (save4.state.equals("select")) {
                    save4.state = "empty";
                    save4.invalidate();
                }
            }
        });
        //turn the white on and off
        white = (Button) findViewById(R.id.white);
        white.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whiteOn) {
                    whiteOn = false;
                    white.setText("white off");
                    updateState("normal");
                    seekBarWhite.setVisibility(View.GONE);
                    //toast disappear
                    if (address != null) {
                        address.cancel();
                        address = null;
                    }
                } else {
                    whiteOn = true;
                    white.setText("white on");
                    updateState("white");
                    seekBarWhite.setVisibility(View.VISIBLE);
                }
            }
        });
        //turn the android wear on and off
        androidWearRGB = (Button) findViewById(R.id.watchForRGB);
        androidWearRGB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (androidWearOnRGB) {
                    androidWearOnRGB = false;
                    androidWearRGB.setText("android wear off");
                    updateState("normal");
                    //toast disappear
                    if (address != null) {
                        address.cancel();
                        address = null;
                    }
                } else {
                    androidWearOnRGB = true;
                    androidWearRGB.setText("android wear on");
                    updateState("wear");
                }
            }
        });
        androidWearWhiteOnly = (Button) findViewById(R.id.watchForWhiteOnly);
        androidWearWhiteOnly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (androidWearOnWhiteOnly) {
                    androidWearOnWhiteOnly = false;
                    androidWearWhiteOnly.setText("android wear off");
                    //update to current brightness level
                    updateBrightness();
                    //toast disappear
                    if (address != null) {
                        address.cancel();
                        address = null;
                    }
                } else {
                    androidWearOnWhiteOnly = true;
                    androidWearWhiteOnly.setText("android wear on");
                }
            }
        });
        //turn the color show on and off
        stopColorShow = (Button) findViewById(R.id.color_show1);
        stopColorShow.setVisibility(View.GONE);
        stopColorShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                colorShowOn = false;
                seekBarColorSpeed.setVisibility(View.GONE);
                stopColorShow.setVisibility(View.GONE);
                colorShow.setVisibility(View.VISIBLE);
                updateState("normal");
                //toast disappear
                if (address != null) {
                    address.cancel();
                    address = null;
                }
            }
        });

        colorShow = (Button) findViewById(R.id.color_show);
        colorShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                //enable seekBar
                seekBarColorSpeed.setVisibility(View.VISIBLE);
                colorShowOn = true;
                colorShow.setVisibility(View.GONE);
                stopColorShow.setVisibility(View.VISIBLE);
                updateState("show");
                value[3] = (byte) (100 - seekBarColorSpeed.getProgress());
                sendValue(value);
            }
        });
        //White color control
        seekBarWhite = (SeekBar) findViewById(R.id.seekWhiteBar);
        seekBarWhite.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (count == 0) {
                    value[0] = (byte) (progress);
                    value[1] = (byte) (progress);
                    value[2] = (byte) (progress);
                    value[3] = 0;
                    sendValue(value);
                    count++;
                } else if (count == 1) {
                    count = 0;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //Speed of color show
        seekBarColorSpeed = (SeekBar) findViewById(R.id.seekColorShowBar);
        seekBarColorSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                value[3] = (byte) (100 - progressValue);
                sendValue(value);
                //toast disappear
                if (address != null) {
                    address.cancel();
                    address = null;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBarColorSpeed.setVisibility(View.GONE);
        seekBarWhite.setVisibility(View.GONE);

    }

    public void sendValue(byte[] value) {
        if (mState == UART_PROFILE_CONNECTED)
            mService.writeRXCharacteristic(value);
    }

    private void showColor(int r, int g, int b) {
        value[0] = (byte) r;
        value[1] = (byte) g;
        value[2] = (byte) b;
        sendValue(value);
    }

    private void updateState(String state) {
        if (state.equals("normal")) {//color picker or saved color can be sent
            //get the current color and sent it
            int color;
            if (save1.state.equals("select"))
                color = save1.getColor();
            else if (save2.state.equals("select"))
                color = save2.getColor();
            else if (save3.state.equals("select"))
                color = save3.getColor();
            else if (save4.state.equals("select"))
                color = save4.getColor();
            else
                color = colorPicker.getColor();
            value[0] = (byte) Color.red(color);
            value[1] = (byte) Color.green(color);
            value[2] = (byte) Color.blue(color);
            value[3] = 0;
            sendValue(value);
            //normal mode can turn on either color show or white
            colorShow.setEnabled(true);
            white.setEnabled(true);
            androidWearRGB.setEnabled(true);
        } else if (state.equals("white")) {
            value[0] = (byte) seekBarWhite.getProgress();
            value[1] = (byte) seekBarWhite.getProgress();
            value[2] = (byte) seekBarWhite.getProgress();
            value[3] = 0;

            sendValue(value);
            //cannnot start color show
            colorShow.setEnabled(false);
            androidWearRGB.setEnabled(false);
        } else if (state.equals("show")) {
            //cannot turn white on
            white.setEnabled(false);
            androidWearRGB.setEnabled(false);
        } else if (state.equals("wear")) {
            //cannot turn white on
            white.setEnabled(false);
            colorShow.setEnabled(false);
        }
    }

    private void updateBrightness() {
        count = 0;
        onValueChanged(brightnessSlider.getBrightness());
    }

    private void service_init() {
        //start listener service
        Intent intent = new Intent(this, MobileWearableService.class);
        startService(intent);

        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        saveData();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);


                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            saveData();
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }

    private void handleToast(String message) {
        if (address != null) {
            address.cancel();
            address = null;
        }
        if (address == null) {
            address = Toast.makeText(getBaseContext(), message, Toast.LENGTH_SHORT);
            address.setGravity(Gravity.CENTER, 0, 0);
            address.show();
        }
    }

    private void saveData() {//save the 4 colors
        SharedPreferences.Editor e = mSharedPreferences.edit();
        if (!save1.state.equals("empty")) {//not empty, save the data
            e.putString(SAVE1, String.valueOf(save1.getColor()));
            e.commit();
        } else {//if it's empty, clear the data
            e.putString(SAVE1, "");
            e.commit();
        }
        if (!save2.state.equals("empty")) {//not empty, save the data
            e.putString(SAVE2, String.valueOf(save2.getColor()));
            e.commit();
        } else {//if it's empty, clear the data
            e.putString(SAVE2, "");
            e.commit();
        }
        if (!save3.state.equals("empty")) {//not empty, save the data
            e.putString(SAVE3, String.valueOf(save3.getColor()));
            e.commit();
        } else {//if it's empty, clear the data
            e.putString(SAVE3, "");
            e.commit();
        }
        if (!save4.state.equals("empty")) {//not empty, save the data
            e.putString(SAVE4, String.valueOf(save4.getColor()));
            e.commit();
        } else {//if it's empty, clear the data
            e.putString(SAVE4, "");
            e.commit();
        }
    }

    /**
     * Notify when slider change value
     * need to send brightness through bluetooth
     *
     * @param value
     */
    @Override
    public void onValueChanged(int value) {
        if(!androidWearOnWhiteOnly) {
            byte[] values = new byte[4];
            values[0] = (byte) value;
            values[1] = (byte) value;
            values[2] = (byte) value;
            if (count == 0) {
                if (mState == UART_PROFILE_CONNECTED)
                    mService.writeRXCharacteristic(values);
            } else if (count == 1) {
                count = 0;
            }
        }else{//give control to the watch
            handleToast("Turn the android wear off to change brightness");
        }
    }

    public void onWatchMessageRecevided(byte[] value) {
        if (mState == MainActivity.UART_PROFILE_CONNECTED) {
            if ((MobileWearableService.getInstance().currentState.equals("rgb") && androidWearOnRGB)
                    || MobileWearableService.getInstance().currentState.equals("white") && androidWearOnWhiteOnly)
                mService.writeRXCharacteristic(value);
        }
    }
}
