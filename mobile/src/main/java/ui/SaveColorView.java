package ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Ziang on 8/7/2015.
 */
public class SaveColorView extends View {
    int radius;
    public String state = "empty";
    Paint paint;
    Canvas canvas;
    int x;
    int y;
    int color = 0;

    public SaveColorView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public SaveColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SaveColorView(Context context) {
        super(context);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        this.canvas = canvas;
        x = getWidth();
        y = getHeight();
        paint = new Paint();
        if (state.equals("empty")) {
            //draw the outside rect
            paint.setColor(Color.GRAY);
            canvas.drawRect(x / 2 - radius, y / 2 - radius, x / 2 + radius, y / 2 + radius, paint);
            // draw the inside rect
            paint.setColor(Color.WHITE);
            canvas.drawRect(x / 2 - (float) 0.85 * radius, y / 2 - (float) 0.85 * radius, x / 2 + (float) 0.85 * radius, y / 2 + (float) 0.85 * radius, paint);
            //draw the '+'
            paint.setColor(Color.GRAY);
            canvas.drawRect(x / 2 - (float) 0.3 * radius, y / 2 - (float) 0.1 * radius, x / 2 + (float) 0.3 * radius, y / 2 + (float) 0.1 * radius, paint);
            canvas.drawRect(x / 2 - (float) 0.1 * radius, y / 2 - (float) 0.3 * radius, x / 2 + (float) 0.1 * radius, y / 2 + (float) 0.3 * radius, paint);
        } else if (state.equals("save")) {
            paint.setColor(Color.GRAY);
            canvas.drawRect(x / 2 - radius, y / 2 - radius, x / 2 + radius, y / 2 + radius, paint);
            paint.setColor(color);
            canvas.drawRect(x / 2 - (float) 0.85 * radius, y / 2 - (float) 0.85 * radius, x / 2 + (float) 0.85 * radius, y / 2 + (float) 0.85 * radius, paint);
        } else if (state.equals("select")) {
            paint.setColor(Color.GRAY);
            canvas.drawCircle(x / 2, y / 2, radius, paint);
            paint.setColor(color);
            canvas.drawCircle(x / 2, y / 2, (float) 0.85 * radius, paint);
        }

    }

    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        radius = width / 5;
    }

    public void unselect() {
        if (state.equals("select")) {
            state = "save";
            invalidate();
        }
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }
}
