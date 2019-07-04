package make.more.r2d2.round_corner;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by HeZX on 2019-06-19.
 */
public class RoundHelper {

    private float[] radii = new float[8];  // top-left, top-right, bottom-right, bottom-left
    private Paint paint;                   // 画笔
    private ColorStateList strokeColor;    // 描边颜色列表
    private int strokeWidth;               // 描边宽度
    private Drawable bg;                   // 背景
    private ColorStateList bg_color;       // 背景颜色列表
    private ColorStateList bg_tint;        // 背景tint颜色列表
    private PorterDuff.Mode bg_tint_mode;  // 背景tint模式

    private RectF layer = new RectF();                  // 画布图层大小
    private RectF tempRectF;                            // 临时矩形 onDraw中避免 new 新对象
    private Path clipPath = new Path();                 // 剪裁区域
    private Path tempPath = new Path();                 // 临时区域 onDraw中避免 new 新对象
    private PorterDuffXfermode mode_dst_out = new PorterDuffXfermode(PorterDuff.Mode.DST_OUT);
    private PorterDuffXfermode mode_src_over = new PorterDuffXfermode(PorterDuff.Mode.SRC_OVER);
    private PorterDuffXfermode mode_bg_tint;

    PaintFlagsDrawFilter filter = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);

    public void init(Context context, View view, AttributeSet attrs) {
        view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.RoundLayout);
        strokeColor = ta.getColorStateList(R.styleable.RoundLayout_round_stroke_color);
        strokeWidth = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_stroke_width, 0);
        int radius = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius, 0);
        try {
            bg = ta.getDrawable(R.styleable.RoundLayout_round_bg);
        } catch (Exception e) {
        }
        try {
            bg_color = ta.getColorStateList(R.styleable.RoundLayout_round_bg);
        } catch (Exception e) {
        }
        bg_tint = ta.getColorStateList(R.styleable.RoundLayout_round_bg_tint);
        bg_tint_mode = parseTintMode(ta.getInt(R.styleable.RoundLayout_round_bg_tint_mode, -1), PorterDuff.Mode.SRC_IN);

        int radiusLeft = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_left, radius);
        int radiusRight = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_right, radius);
        int radiusTop = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_top, radius);
        int radiusBottom = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_bottom, radius);

        int radiusTopLeft = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_top_left, radiusLeft > 0 ? radiusLeft : radiusTop);
        int radiusTopRight = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_top_right, radiusRight > 0 ? radiusRight : radiusTop);
        int radiusBottomRight = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_bottom_right, radiusRight > 0 ? radiusRight : radiusBottom);
        int radiusBottomLeft = ta.getDimensionPixelSize(R.styleable.RoundLayout_round_radius_bottom_left, radiusLeft > 0 ? radiusLeft : radiusBottom);
        ta.recycle();

        setRadius(radiusTopLeft, radiusTopRight, radiusBottomRight, radiusBottomLeft);
        if (bg != null && bg_tint != null) mode_bg_tint = new PorterDuffXfermode(bg_tint_mode);
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setAntiAlias(true);
        if (Build.VERSION.SDK_INT <= 18) tempRectF = new RectF();

    }

    @SuppressWarnings("unused")
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        layer.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addRoundRect(layer, radii, Path.Direction.CCW);
    }

    public void drawBG(Canvas canvas, int[] drawableState) {
        canvas.saveLayer(layer, null, Canvas.ALL_SAVE_FLAG);
        canvas.setDrawFilter(filter);
        if (bg_color != null) {
            canvas.drawColor(bg_color.getColorForState(drawableState, bg_color.getDefaultColor()));
        }
        if (bg != null) {
            bg.setState(drawableState);
            bg.setBounds(0, 0, (int) layer.width(), (int) layer.height());
            bg.draw(canvas);
            if (bg_tint != null) {
                paint.setColor(bg_tint.getColorForState(drawableState, bg_tint.getDefaultColor()));
                paint.setStyle(Paint.Style.FILL);
                paint.setXfermode(mode_bg_tint);
                canvas.drawRect(0, 0, (int) layer.width(), (int) layer.height(), paint);
            }
        }
    }

    public void drawableStateChanged(View view) {
        boolean refresh = false;
        int[] drawableState = view.getDrawableState();
        if (bg_color != null && bg_color.isStateful()) {
            refresh = true;
        }
        if (bg != null && bg.isStateful()) {
            if (bg.setState(drawableState)) {
                refresh = true;
            }
        }
        if (strokeColor != null && strokeColor.isStateful()) {
            refresh = true;
        }
        if (bg_tint != null && bg_tint.isStateful()) {
            refresh = true;
        }
        if (refresh) view.postInvalidate();
    }

    public void drawClip(Canvas canvas, int[] drawableState) {
        if (strokeWidth > 0 && strokeColor != null) {
            paint.setStrokeWidth(strokeWidth * 2);
            paint.setXfermode(mode_dst_out);
            paint.setColor(Color.WHITE);
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(clipPath, paint);
            paint.setXfermode(mode_src_over);
            paint.setColor(strokeColor.getColorForState(drawableState, strokeColor.getDefaultColor()));
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawPath(clipPath, paint);
        }
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        paint.setXfermode(mode_dst_out);

        if (Build.VERSION.SDK_INT > 18) {
            tempPath.reset();
            tempPath.addRect(0, 0, (int) layer.width(), (int) layer.height(), Path.Direction.CW);
            tempPath.op(clipPath, Path.Op.DIFFERENCE);
            canvas.drawPath(tempPath, paint);
        } else {
            clipUnder18(canvas);
        }
    }

    private void clipUnder18(Canvas canvas) {
        float topLeft = radii[0];
        float topRight = radii[2];
        float bottomRight = radii[4];
        float bottomLeft = radii[6];

        if (topLeft > 0) {
            tempPath.reset();
            tempPath.moveTo(0, topLeft);
            tempPath.lineTo(0, 0);
            tempPath.lineTo(topLeft, 0);
            tempRectF.set(0, 0, topLeft * 2, topLeft * 2);
            tempPath.arcTo(tempRectF, -90, -90);
            tempPath.close();
            canvas.drawPath(tempPath, paint);
        }
        if (topRight > 0) {
            float width = layer.width();
            tempPath.reset();
            tempPath.moveTo(width - topRight, 0);
            tempPath.lineTo(width, 0);
            tempPath.lineTo(width, topRight);
            tempRectF.set(width - 2 * topRight, 0, width, topRight * 2);
            tempPath.arcTo(tempRectF, 0, -90);
            tempPath.close();
            canvas.drawPath(tempPath, paint);
        }
        if (bottomRight > 0) {
            float height = layer.height();
            float width = layer.width();
            tempPath.reset();
            tempPath.moveTo(width - bottomRight, height);
            tempPath.lineTo(width, height);
            tempPath.lineTo(width, height - bottomRight);
            tempRectF.set(width - 2 * bottomRight, height - 2 * bottomRight, width, height);
            tempPath.arcTo(tempRectF, 0, 90);
            tempPath.close();
            canvas.drawPath(tempPath, paint);
        }
        if (bottomLeft > 0) {
            float height = layer.height();
            tempPath.reset();
            tempPath.moveTo(0, height - bottomLeft);
            tempPath.lineTo(0, height);
            tempPath.lineTo(bottomLeft, height);
            tempRectF.set(0, height - 2 * bottomLeft, bottomLeft * 2, height);
            tempPath.arcTo(tempRectF, 90, 90);
            tempPath.close();
            canvas.drawPath(tempPath, paint);
        }
    }

    public static PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return defaultMode;
        }
    }

    //==============设置角半径=============//
    public float[] getRadii() {
        return radii;
    }

    public void setRadius(int radius) {
        for (int i = 0; i < 8; i++) {
            radii[i] = radius;
        }
    }

    public void setRadius(int topLeft, int topRight, int bottomRight, int bottomLeft) {
        radii[0] = topLeft;
        radii[1] = topLeft;
        radii[2] = topRight;
        radii[3] = topRight;
        radii[4] = bottomRight;
        radii[5] = bottomRight;
        radii[6] = bottomLeft;
        radii[7] = bottomLeft;
    }

    //==============设置边框和背景=============//
    public ColorStateList getStrokeColor() {
        return strokeColor;
    }

    public void setStrokeColor(ColorStateList strokeColor) {
        this.strokeColor = strokeColor;
    }

    public int getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(int strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public ColorStateList getBg_color() {
        return bg_color;
    }

    public void setBg_color(ColorStateList bg_color) {
        this.bg_color = bg_color;
        this.bg = null;
    }

    public Drawable getBg() {
        return bg;
    }

    public void setBg(Drawable bg) {
        this.bg = bg;
        this.bg_color = null;
    }

    public ColorStateList getBg_tint() {
        return bg_tint;
    }

    public void setBg_tint(ColorStateList bg_tint) {
        this.bg_tint = bg_tint;
    }

    public PorterDuff.Mode getBg_tint_mode() {
        return bg_tint_mode;
    }

    public void setBg_tint_mode(PorterDuff.Mode bg_tint_mode) {
        this.bg_tint_mode = bg_tint_mode;
    }
}
