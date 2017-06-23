package com.twotoasters.messageapidemo;

import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;

import java.nio.channels.SelectionKey;

public class ModeActivity extends Activity {

    boolean whiteOn = false;
    Button white;
    boolean colorShowOn = false;
    Button colorShow;
    SeekBar colorShowSeekBar;
    SeekBar whiteSeekBar;
    static int speed = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);
        //Back button
        Button back = (Button) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //re-enable double finger tap
                MyActivity.doubleFinger=false;
                finish();
            }
        });

        //White on/off button
        white = (Button) findViewById(R.id.white);
        white.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (whiteOn) {//white is off
                    whiteOn = false;
                    white.setText("white off");
                    colorShow.setEnabled(true);
                    whiteSeekBar.setVisibility(SeekBar.GONE);
                    MyActivity.getColorAndSend();
                } else {//white is on
                    whiteOn = true;
                    white.setText("white on");
                    //disable color show
                    colorShow.setEnabled(false);
                    whiteSeekBar.setVisibility(SeekBar.VISIBLE);
                    sendWhiteProgress(whiteSeekBar.getProgress());
                }
            }
        });

        //turn the color show on and off
        colorShow = (Button) findViewById(R.id.colorShow);
        colorShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(colorShowOn) {//color show is off
                    colorShowOn = false;
                    colorShow.setText("color show off");
                    colorShowSeekBar.setVisibility(SeekBar.GONE);
                    //disable white
                    white.setEnabled(true);
                    MyActivity.getColorAndSend();
                }else {//color show is on
                    colorShowOn = true;
                    colorShow.setText("color show on");
                    colorShowSeekBar.setVisibility(SeekBar.VISIBLE);
                    //disable white
                    white.setEnabled(false);
                    sendColorShowProgress(colorShowSeekBar.getProgress());
                }
            }
        });

        //Speed of color show
        colorShowSeekBar = (SeekBar) findViewById(R.id.seekColorShowBar);
        colorShowSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                sendColorShowProgress(progressValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        colorShowSeekBar.setVisibility(SeekBar.GONE);

        whiteSeekBar = (SeekBar) findViewById(R.id.seekWhiteBar);
        whiteSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                sendWhiteProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        whiteSeekBar.setVisibility(SeekBar.GONE);

    }

    private void sendWhiteProgress(int progress){
        MobileWearableService.getInstance().sendMessage(String.format("%d %d %d 0",progress*8,progress*8,progress*8));
    }

    private void sendColorShowProgress(int progress){
        speed = 100-progress*3;
        MobileWearableService.getInstance().sendMessage(String.format("0 0 0 %d",speed));
    }

    @Override
    protected void onPause() {
        super.onPause();
        MyActivity.doubleFinger=false;
    }

}
