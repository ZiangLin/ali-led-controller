package com.twotoasters.messageapidemo;

import android.view.View;
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
    private Switch[] readingSwiched = new Switch[8];
    public TwoWhiteActivity(){
        twoWhiteLayout = (RelativeLayout) MainActivity.getInstance().findViewById(R.id.twoWhiteLayout);
        upwashSlider = (LightnessSlider)MainActivity.getInstance().findViewById(R.id.upwashSlider);
        upwashSlider.setOnValueChangedListener(new OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                byte[] values = new byte[4];
                values[0]= (byte)value;
                values[3]=(byte)0;
                MainActivity.getInstance().sendValue(values);
            }
        });

        downwashSlider = (LightnessSlider)MainActivity.getInstance().findViewById(R.id.downwashSlider);
        downwashSlider.setOnValueChangedListener(new OnValueChangedListener() {
            @Override
            public void onValueChanged(int value) {
                byte[] values = new byte[4];
                values[1]= (byte)value;
                values[3]=(byte)1;
                MainActivity.getInstance().sendValue(values);
            }
        });
        for(int i=0;i<8;i++){
            int temp = MainActivity.getInstance().getResources().getIdentifier("reading"+i, "id", MainActivity.getInstance().getPackageName());
            readingSwiched[i] = (Switch)MainActivity.getInstance().findViewById(temp);
            readingSwiched[i].setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    byte[] values = new byte[4];
                    int value = 0;
                    for(int i=0;i<8;i++){
                        if(readingSwiched[i].isChecked()){
                            value++;
                        }
                        value*=2;
                    }
                    value/=2;
                    values[2]= (byte)value;
                    values[3]=(byte)2;
                      MainActivity.getInstance().sendValue(values);
                }
            });
        }
    }
    public void setVisibility(int visibility){
        twoWhiteLayout.setVisibility(visibility);
    }

    public void setValues(int up, int down, int readings){
        upwashSlider.setValue(up);
        downwashSlider.setValue(down);
        for(int i=7;i>=0;i--){
            readingSwiched[i].setChecked(readings%2==1);
            readings/=2;
        }
    }
}
