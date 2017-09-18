package org.awesomeapp.messenger.ui.widgets;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;

import org.ironrabbit.type.CustomTypefaceManager;

public class GroupAvatar extends ColorDrawable {
    private String groupId;
    private boolean rounded;
    private RectF rectBounds = new RectF();
    private Paint paint;
    private Path clip;
    private Path topPath;
    private Path bottomPath;
    private int colorTop;
    private int colorMiddle;
    private int colorBottom;

    public GroupAvatar(String groupId) {
        super();
        this.groupId = groupId;
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        rounded = true; // default rounded
    }

    public boolean isRounded() {
        return this.rounded;
    }

    public void setRounded(boolean rounded) {
        this.rounded = rounded;
    }

    @Override
    public void draw(Canvas canvas) {
        if (clip != null && isRounded()) {
            canvas.clipPath(clip);
        }
        super.draw(canvas);
        if (topPath != null) {
            paint.setColor(colorTop);
            canvas.drawPath(topPath, paint);
        }
        if (bottomPath != null) {
            paint.setColor(colorBottom);
            canvas.drawPath(bottomPath, paint);
        }
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        rectBounds.set(bounds);
        if (clip == null) {
            clip = new Path();
        } else {
            clip.reset();
        }
        clip.addOval(rectBounds, Path.Direction.CW);

        LinearCongruentialGenerator lcg = new LinearCongruentialGenerator(this.groupId.hashCode());
        double range = 0.3 * (double)bounds.height();
        int halfrange = (int)(range / 2);
        int a = (int)(lcg.random() * range) - halfrange;
        int b = (int)(lcg.random() * range) - halfrange;
        int c = (int)(lcg.random() * range) - halfrange;
        int d = (int)(lcg.random() * range) - halfrange;

        int height = bounds.height();
        int width = bounds.width();

        int aThird = (int)(0.33f * (float)height);
        int twoThirds = (int)(0.66f * (float)height);

        if (topPath == null) {
            topPath = new Path();
        }
        topPath.reset();
        topPath.moveTo(0, 0);
        topPath.lineTo(0, aThird + a);
        topPath.lineTo(width, aThird + c);
        topPath.lineTo(width, 0);
        topPath.close();

        if (bottomPath == null) {
            bottomPath = new Path();
        }
        bottomPath.reset();
        bottomPath.moveTo(0, twoThirds + b);
        bottomPath.lineTo(0, height);
        bottomPath.lineTo(width, height);
        bottomPath.lineTo(width, twoThirds + d);
        bottomPath.close();

        int[] arrayOfColors = new int[] {
                0xfff73d54,
                0xfffff74f,
                0xffb2142f,
                0xff4fcaff,
                0xff86ff76,
                0xffcc4317,
                0xff8376ff
        };

        colorTop = arrayOfColors[(int)(lcg.random() * arrayOfColors.length)];
        colorMiddle = arrayOfColors[(int)(lcg.random() * arrayOfColors.length)];
        colorBottom = arrayOfColors[(int)(lcg.random() * arrayOfColors.length)];

        setColor(colorMiddle);
    }

    private class LinearCongruentialGenerator {
        double lastRandom = 0.0;
        double m = 139968.0;
        double a = 3877.0;
        double c = 29573.0;

        LinearCongruentialGenerator(int seed) {
            double doubleSeed = Math.abs((double)seed);
            lastRandom = (doubleSeed * a + c) % m;
        }

        public double random() {
            lastRandom = (lastRandom * a + c) % m;
            return lastRandom / m;
        }
    }
}