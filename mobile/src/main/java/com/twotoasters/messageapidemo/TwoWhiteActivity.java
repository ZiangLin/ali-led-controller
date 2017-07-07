package com.twotoasters.messageapidemo;

import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;

import bluetooth.UartService;
import ui.LightnessSlider;
import ui.OnValueChangedListener;

/**
 * Created by ziangl on 6/29/2017.
 */

public class TwoWhiteActivity {
    private RelativeLayout twoWhiteLayout;
    private LightnessSlider upwashSlider;
    private LightnessSlider downwashSlider;
    private Button allOnBtn;
    private Button allOffBtn;
    private Switch[] readingSwiched = new Switch[8];
    private int state;
    private static final int ENABLED = 3;
    private static final int DISABLED = 4;
    private Switch bluetoothEnabledSwitch;
    public TwoWhiteActivity(){
        twoWhiteLayout = (RelativeLayout) MainActivity.getInstance().findViewById(R.id.twoWhiteLayout);
        bluetoothEnabledSwitch = (Switch) MainActivity.getInstance().findViewById(R.id.bluetoothEnabled);
        bluetoothEnabledSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                byte[] values = new byte[4];
                if(bluetoothEnabledSwitch.isChecked()){
                    state = ENABLED;
                    values[3] = (byte) ENABLED;
                }else{
                    state = DISABLED;
                    values[3] = (byte) DISABLED;
                }
                MainActivity.getInstance().sendValue(values);
            }
        });

        upwashSlider = (LightnessSlider)MainActivity.getInstance().findViewById(R.id.upwashSlider);
        upwashSlider.setOnValueChangedListener(new OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                if(state==ENABLED) {
                    byte[] values = new byte[4];
                    values[0] = (byte) value;
                    values[3] = (byte) 0;
                    MainActivity.getInstance().sendValue(values);
                }else{
                    MainActivity.getInstance().handleToast("Bluetooth is disabled");
                }
            }
        });

        downwashSlider = (LightnessSlider)MainActivity.getInstance().findViewById(R.id.downwashSlider);
        downwashSlider.setOnValueChangedListener(new OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                if(state==ENABLED) {
                    byte[] values = new byte[4];
                    values[1] = (byte) value;
                    values[3] = (byte) 1;
                    MainActivity.getInstance().sendValue(values);
                }else{
                    MainActivity.getInstance().handleToast("Bluetooth is disabled");
                }
            }
        });
        for(int i=0;i<8;i++){
            int temp = MainActivity.getInstance().getResources().getIdentifier("reading"+i, "id", MainActivity.getInstance().getPackageName());
            readingSwiched[i] = (Switch)MainActivity.getInstance().findViewById(temp);
            readingSwiched[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(state==ENABLED) {
                        byte[] values = new byte[4];
                        int value = 0;
                        for (int i = 0; i < 8; i++) {
                            if (readingSwiched[i].isChecked()) {
                                value++;
                            }
                            value *= 2;
                        }
                        value /= 2;
                        values[2] = (byte) value;
                        values[3] = (byte) 2;
                        MainActivity.getInstance().sendValue(values);
                    }else{
                        MainActivity.getInstance().handleToast("Bluetooth is disabled");
                    }
                }
            });
        }
        allOnBtn = (Button)MainActivity.getInstance().findViewById(R.id.allon);
        allOnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i=0;i<8;i++){
                    readingSwiched[i].setChecked(true);
                }
                if(state==ENABLED) {
                    byte[] values = new byte[4];
                    values[2] = (byte) 255;
                    values[3] = (byte) 2;
                    MainActivity.getInstance().sendValue(values);
                }else{
                    MainActivity.getInstance().handleToast("Bluetooth is disabled");
                }
            }
        });
        allOffBtn = (Button)MainActivity.getInstance().findViewById(R.id.alloff);
        allOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for(int i=0;i<8;i++){
                    readingSwiched[i].setChecked(false);
                }
                if(state==ENABLED) {
                    byte[] values = new byte[4];
                    values[2] = (byte) 0;
                    values[3] = (byte) 2;
                    MainActivity.getInstance().sendValue(values);
                }else{
                    MainActivity.getInstance().handleToast("Bluetooth is disabled");
                }
            }
        });
    }
    public void setVisibility(int visibility){
        twoWhiteLayout.setVisibility(visibility);
    }

    public void setValues(int up, int down, int readings, int state){
        upwashSlider.setValue(up);
        downwashSlider.setValue(down);
        for(int i=7;i>=0;i--){
            readingSwiched[i].setChecked(readings%2==1);
            readings/=2;
        }
        //change state
        this.state = state;
        //update the enable switch
        if(state==ENABLED){
            bluetoothEnabledSwitch.setChecked(true);
        }else{
            bluetoothEnabledSwitch.setChecked(false);
        }
    }
}
