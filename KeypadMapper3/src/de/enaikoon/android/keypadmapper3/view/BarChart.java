/**************************************************************************
 * Copyright
 *
 * $Id$
 * $HeadURL$
 **************************************************************************/

package de.enaikoon.android.keypadmapper3.view;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.BlurMaskFilter.Blur;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.View;
import de.enaikoon.android.keypadmapper3.KeypadMapperApplication;
import de.enaikoon.android.library.resources.locale.Localizer;

/**
 * 
 */
public class BarChart extends View {

    private class BarInfo {
        int number;

        float snr;

        boolean isUsed;
    }

    private static final int HEIGHT = 535;

    private static final int DRAW_AREA_HEIGHT = 450;

    private static final int WIDTH = 480;

    private static final int CHART_WIDTH = 350;

    private static final int LEVELS_TEXT_SIZE = 13;

    private static final int COLOR_BLUE = Color.rgb(70, 180, 231);

    private static final int COLOR_YELLOW = Color.rgb(255, 191, 39);

    private static final int COLOR_GRAY = Color.rgb(122, 135, 141);

    private Localizer localizer = KeypadMapperApplication.getInstance().getLocalizer();

    private Paint imagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint lvlTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint satelliteTypeGlowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private Paint satelliteTypePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private List<BarInfo> infos = new ArrayList<BarChart.BarInfo>();

    public BarChart(Context context) {
        super(context);
        init();
    }

    public BarChart(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BarChart(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    synchronized public void updateData(GpsStatus gpsStatus) {
        infos.clear();
        int maxSats = 0;
        int usedSats = 0;
        Iterable<GpsSatellite> gpsSatellites = gpsStatus.getSatellites();
        for (GpsSatellite sat : gpsSatellites) {
            BarInfo info = new BarInfo();
            info.number = sat.getPrn();
            info.snr = sat.getSnr();
            maxSats++;
            if (sat.usedInFix()) {
                usedSats++;
                info.isUsed = true;
            }
            infos.add(info);
        }
        invalidate();
    }

    @Override
    synchronized protected void onDraw(Canvas canvas) {
        drawYLevels(canvas);

        // BarInfo info = null;
        //
        // info = new BarInfo();
        // info.isUsed = true;
        // info.number = 1;
        // info.snr = 10;
        // infos.add(info);
        //
        // info = new BarInfo();
        // info.isUsed = true;
        // info.number = 2;
        // info.snr = 20;
        // infos.add(info);
        //
        // info = new BarInfo();
        // info.isUsed = true;
        // info.number = 3;
        // info.snr = 30;
        // infos.add(info);
        //
        // info = new BarInfo();
        // info.isUsed = true;
        // info.number = 4;
        // info.snr = 40;
        // infos.add(info);
        //
        // info = new BarInfo();
        // info.isUsed = false;
        // info.number = 5;
        // info.snr = 0;
        // infos.add(info);
        //
        // info = new BarInfo();
        // info.isUsed = false;
        // info.number = 16;
        // info.snr = 15;
        // infos.add(info);
        //
        // info = new BarInfo();
        // info.isUsed = false;
        // info.number = 17;
        // info.snr = 100;
        // infos.add(info);
        //
        // // /
        // info = new BarInfo();
        // info.isUsed = true;
        // info.number = 65;
        // info.snr = 10;
        // infos.add(info);

        drawBars(canvas, infos);
        drawBelowBaselevel(canvas);
    }

    private int convertDpToPx(float dp) {
        DisplayMetrics displayMetrics = getContext().getResources().getDisplayMetrics();
        return (int) ((dp * displayMetrics.density) + 0.5);
    }

    private void drawBars(Canvas canvas, List<BarInfo> infos) {
        int size = infos.size();
        if (size == 0) {
            return;
        }
        int barWidth = 28 * getWidth() / WIDTH;
        int barPadding = 15 * getWidth() / WIDTH;
        if (size > 8) {
            barWidth = (int) (((CHART_WIDTH / size) * 0.65) * getWidth() / WIDTH);
            barPadding = (int) (((CHART_WIDTH / size) * 0.35) * getWidth() / WIDTH);
        }
        int leftPadding = 100 * getWidth() / WIDTH;
        int gpsStart = -1;
        int glonassStart = -1;
        for (int i = 0; i < size; i++) {
            // The satellites numbered 65 to 88 are GLONASS satellites that are
            // being tracked.
            if (infos.get(i).number < 65 && gpsStart == -1) {
                gpsStart = i;
            } else if (infos.get(i).number >= 65 && glonassStart == -1) {
                glonassStart = i;
            }

            int barLeftPadding = leftPadding + i * (barWidth + barPadding);
            Rect rect = new Rect();

            rect.top =
                    getBaseLevel()
                            - (int) (infos.get(i).snr
                                    * (getHeight() - (getHeight() - getBaseLevel())) * 108
                                    / DRAW_AREA_HEIGHT / 10);
            rect.bottom = getBaseLevel();
            rect.left = barLeftPadding;
            rect.right = barLeftPadding + barWidth;

            if (infos.get(i).isUsed) {
                if (infos.get(i).snr >= 30) {
                    barPaint.setColor(COLOR_BLUE);
                } else {
                    barPaint.setColor(COLOR_YELLOW);
                }
            } else {
                barPaint.setColor(COLOR_GRAY);
            }

            canvas.drawRect(rect, barPaint);

            canvas.drawText(infos.get(i).number + "", barLeftPadding + barWidth / 2, getBaseLevel()
                    + convertDpToPx(15), barPaint);
        }

        if (gpsStart != -1) {
            int left = leftPadding;
            int right = leftPadding;
            if (glonassStart != -1) {
                right +=
                        glonassStart * barWidth + (int) ((glonassStart - 0.5d) * barPadding) + 2
                                * getHeight() / HEIGHT;
            } else {
                right += size * barWidth + (size - 1) * barPadding;
            }
            // canvas.drawRect(new Rect(left, getBaseLevel() + 23 * getHeight()
            // / HEIGHT, right,
            // getBaseLevel() + (23 + 19) * getHeight() / HEIGHT), barPaint);
            drawBrackets(canvas, new Rect(left, getBaseLevel() + convertDpToPx(16), right,
                    getBaseLevel() + convertDpToPx(28)));
            canvas.drawText(localizer.getString("satellite_gps"), (left + right) / 2,
                    getBaseLevel() + convertDpToPx(43), satelliteTypeGlowPaint);
            canvas.drawText(localizer.getString("satellite_gps"), (left + right) / 2,
                    getBaseLevel() + convertDpToPx(43), satelliteTypePaint);
        }
        if (glonassStart != -1) {
            int left =
                    leftPadding + glonassStart * barWidth
                            + (int) ((glonassStart - 0.5d) * barPadding) - 2 * getHeight() / HEIGHT;
            int right = leftPadding + size * barWidth + (size - 1) * barPadding;
            drawBrackets(canvas, new Rect(left, getBaseLevel() + convertDpToPx(16), right,
                    getBaseLevel() + convertDpToPx(28)));
            canvas.drawText(localizer.getString("satellite_glonass"), (left + right) / 2,
                    getBaseLevel() + convertDpToPx(43), satelliteTypeGlowPaint);
            canvas.drawText(localizer.getString("satellite_glonass"), (left + right) / 2,
                    getBaseLevel() + convertDpToPx(43), satelliteTypePaint);
        }
    }

    private void drawBelowBaselevel(Canvas canvas) {
        Bitmap bitmap = ((BitmapDrawable) localizer.getDrawable("barchart_axis")).getBitmap();
        int lineHeight = getHeight() * 3 / HEIGHT;
        if (lineHeight == 0) {
            lineHeight = 1;
        }
        Rect rect = new Rect(0, getBaseLevel(), getWidth(), getBaseLevel() + lineHeight);
        canvas.drawBitmap(bitmap, null, rect, imagePaint);
    }

    private void drawBrackets(Canvas canvas, Rect rect) {
        Bitmap leftBitmap =
                ((BitmapDrawable) localizer.getDrawable("bracket_left_part")).getBitmap();
        int width = leftBitmap.getWidth();
        int height = leftBitmap.getHeight();
        int targetHeight = rect.bottom - rect.top;
        int targetWidth = width * targetHeight / height;

        // left
        canvas.drawBitmap(leftBitmap, null, new Rect(rect.left, rect.top, rect.left + targetWidth,
                rect.top + targetHeight), imagePaint);
        // right
        Bitmap rightBitmap =
                ((BitmapDrawable) localizer.getDrawable("bracket_right_part")).getBitmap();
        canvas.drawBitmap(rightBitmap, null, new Rect(rect.right - targetWidth, rect.top,
                rect.right, rect.top + targetHeight), imagePaint);
        // middle
        Bitmap middleBitmap =
                ((BitmapDrawable) localizer.getDrawable("bracket_middle_part")).getBitmap();
        canvas.drawBitmap(middleBitmap, null, new Rect(rect.left + targetWidth, rect.top,
                rect.right - targetWidth, rect.top + targetHeight), imagePaint);
    }

    private void drawYLevels(Canvas canvas) {

        int padding = getWidth() * 30 / WIDTH;

        int levelHeight = (getHeight() - (getHeight() - getBaseLevel())) * 108 / DRAW_AREA_HEIGHT;

        int lineHeight = getHeight() * 2 / HEIGHT;

        if (lineHeight == 0) {
            lineHeight = 1;
        }

        for (int i = 0; i < 4; i++) {
            Bitmap bitmap = ((BitmapDrawable) localizer.getDrawable("line_grey")).getBitmap();
            Rect rect =
                    new Rect(padding, getBaseLevel() - (i + 1) * levelHeight, getWidth() - padding,
                            getBaseLevel() - (i + 1) * levelHeight + lineHeight);
            canvas.drawBitmap(bitmap, null, rect, imagePaint);

            canvas.drawText(((i + 1) * 10) + "", padding, getBaseLevel() - (i + 1) * levelHeight
                    - ((lvlTextPaint.descent() + lvlTextPaint.ascent()) / 2), lvlTextPaint);
        }

    }

    private int getBaseLevel() {
        // int baseLvl = getHeight() - getHeight() * 82 / HEIGHT;
        int baseLvl = getHeight() - convertDpToPx(55);
        return baseLvl;
    }

    private void init() {
        lvlTextPaint.setColor(Color.WHITE);
        lvlTextPaint.setShadowLayer(convertDpToPx(LEVELS_TEXT_SIZE / 2), 0, 0, COLOR_BLUE);
        lvlTextPaint.setTextSize(convertDpToPx(LEVELS_TEXT_SIZE));
        lvlTextPaint.setTextAlign(Align.CENTER);

        barPaint.setTextSize(convertDpToPx(LEVELS_TEXT_SIZE));
        barPaint.setTextAlign(Align.CENTER);

        satelliteTypeGlowPaint.setColor(COLOR_YELLOW);
        satelliteTypeGlowPaint.setTextSize(convertDpToPx(LEVELS_TEXT_SIZE));
        satelliteTypeGlowPaint.setTextAlign(Align.CENTER);
        satelliteTypeGlowPaint.setMaskFilter(new BlurMaskFilter(convertDpToPx(5), Blur.SOLID));

        satelliteTypePaint.setColor(Color.WHITE);
        satelliteTypePaint.setTextSize(convertDpToPx(LEVELS_TEXT_SIZE));
        satelliteTypePaint.setTextAlign(Align.CENTER);
    }
}
