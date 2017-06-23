package ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.AttributeSet;


public class LightnessSlider extends AbsCustomSlider {
    private int color;
    private Paint barPaint = PaintBuilder.newPaint().build();
    private Paint solid = PaintBuilder.newPaint().build();
    private Paint clearingStroke = PaintBuilder.newPaint().color(0xffffffff).xPerMode(PorterDuff.Mode.CLEAR).build();


    public LightnessSlider(Context context) {
        super(context);
    }

    public LightnessSlider(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public LightnessSlider(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void drawBar(Canvas barCanvas) {
        int width = barCanvas.getWidth();
        int height = barCanvas.getHeight();

        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        int l = Math.max(2, width / 256);
        for (int x = 0; x <= width; x += l) {
            hsv[2] = (float) x / (width - 1);
            barPaint.setColor(Color.HSVToColor(hsv));
            barCanvas.drawRect(x, 0, x + l, height, barPaint);
        }
    }

    @Override
    protected void drawHandle(Canvas canvas, float x, float y) {
        solid.setColor(Color.LTGRAY);
        canvas.drawCircle(x, y, handleRadius, solid);
        solid.setColor(colorAtLightness(color, value));
        canvas.drawCircle(x, y, handleRadius * 0.95f, clearingStroke);
        canvas.drawCircle(x, y, handleRadius * 0.75f, solid);
    }

//    public void setColorPicker(ColorPickerView colorPicker) {
//        this.colorPicker = colorPicker;
//    }

//    public void setColor(int color) {
//        this.color = color;
//        this.value = lightnessOfColor(color);
//        if (bar != null) {
//            updateBar();
//            invalidate();
//        }
//    }

    public static int colorAtLightness(int color, float lightness) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        hsv[2] = lightness;
        return Color.HSVToColor(hsv);
    }

    public static int brightnessAtLightness(float lightness){
        int color = colorAtLightness(0,lightness);
        //because we only have grey color
        //can use any color from r,g,b
        return Color.red(color);
    }

    public static float lightnessOfColor(int color) {
        float[] hsv = new float[3];
        Color.colorToHSV(color, hsv);
        return hsv[2];
    }
}
