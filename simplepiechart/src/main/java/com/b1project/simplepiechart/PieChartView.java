package com.b1project.simplepiechart;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class PieChartView extends View {

    public static final int CHART_TYPE_PIE = 0x00;
    public static final int CHART_TYPE_DONUT = 0x01;

    private static final String TAG = PieChartView.class.getSimpleName();
    private static final int BG_COLOR = 0x00000000;
	private static final int WAIT = 0;
	private static final int IS_READY_TO_DRAW = 1;
	private static final int IS_DRAWING = 2;

	private Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mLinePaints = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mShadowPaint = new Paint(0);

    private RectF mOvals = new RectF();
    private RectF mOutline = new RectF();

    private Path mOuterPath = new Path();
    private Path mCenterPath = new Path();
    private Path mBgCenterPath = new Path();
    private Path mDepthPath = new Path();
    private Path mShadowPath = new Path();

    private LightingColorFilter mClearLightingColorFilter = new LightingColorFilter(0xFFDDDDDD, 0xFF000000);

    private Matrix mScaleMatrix = new Matrix();

	private float   mWidth = 64;
	private float   mHeight = 64;
    private float   mDepth = 10.0f;
	private float   mGapLeft = 8;
	private float   mGapRight = 8;
	private float   mGapTop = 8;
	private float   mGapBottom = 16;
    private float mStartRadius = 0;
	private int     mState = WAIT;
    private float   mMaxConnection;
	private List<PieItem> mDataArray;
    private int mChartType = CHART_TYPE_PIE;
    private boolean mHasShadow = true;
	private int     mShadowRadius = 8;
	private int     mShadowColor = 0xFF101010;
	private int     mBorderColor = 0xFF101010;

	public PieChartView (Context context){
		super(context);
        init();
	}

	public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PieChartView);
        mChartType = a.getInt(R.styleable.PieChartView_chart_type, CHART_TYPE_PIE);
        mWidth = a.getDimensionPixelSize(R.styleable.PieChartView_chart_width, 64);
        mHeight = a.getDimensionPixelSize(R.styleable.PieChartView_chart_height, 64);
        mDepth = a.getDimensionPixelSize(R.styleable.PieChartView_chart_depth, 10);
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        mGapLeft = a.getDimensionPixelSize(R.styleable.PieChartView_chart_gap_left, 0) + padding;
        mGapRight = a.getDimensionPixelSize(R.styleable.PieChartView_chart_gap_right, 0) + padding;
        mGapTop = a.getDimensionPixelSize(R.styleable.PieChartView_chart_gap_top, 0) + padding;
        mGapBottom = a.getDimensionPixelSize(R.styleable.PieChartView_chart_gap_bottom, 0) + mDepth;
        setShadowRadius(a.getDimensionPixelSize(R.styleable.PieChartView_chart_shadow_radius, 8));
        a.recycle();
        init();
        setData(PieItem.Companion.getDummyData());
	}

    private void init(){

        mBgPaint.setAntiAlias(true);
        mBgPaint.setStyle(Paint.Style.FILL);
        mBgPaint.setColor(Color.RED);
        mBgPaint.setStrokeWidth(1.0f);

        mLinePaints.setAntiAlias(true);
        mLinePaints.setStyle(Paint.Style.STROKE);
        mLinePaints.setColor(0xFF000000);
        mLinePaints.setStrokeWidth(1.0f);

        mClearPaint.setStyle(Paint.Style.FILL);
        mClearPaint.setColor(Color.BLACK);

        mShadowPaint.setColor(mShadowColor);
        mShadowPaint.setMaskFilter(new BlurMaskFilter(mShadowRadius, BlurMaskFilter.Blur.NORMAL));
        setLayerType(LAYER_TYPE_SOFTWARE, mShadowPaint);
    }

	@SuppressLint("DrawAllocation")
	@Override
	protected void onDraw(Canvas canvas) {

        if (mState == IS_READY_TO_DRAW && mWidth > 0 && mHeight > 0) {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            Log.d(TAG, "onDraw");
            Log.d(TAG, "Items: " + mDataArray.size());
            Log.d(TAG, "Chart type: " + mChartType);
            Log.d(TAG, "Chart width: " + mWidth);
            Log.d(TAG, "Chart height: " + mHeight);
            Log.d(TAG, "Chart depth: " + mDepth);
            Log.d(TAG, "Has shadow: " + mHasShadow);
            setState(IS_DRAWING);

            Bitmap pie = Bitmap.createBitmap((int) mWidth, (int) mHeight, Bitmap.Config.ARGB_8888);
            Bitmap background = Bitmap.createBitmap((int) mWidth, (int) (mHeight + mDepth), Bitmap.Config.ARGB_8888);
            Bitmap shadow = null;

            Canvas srcCanvas = new Canvas(pie);
            Canvas bgCanvas = new Canvas(background);
            Canvas shadowCanvas = null;
            if(mHasShadow) {
                shadow = Bitmap.createBitmap((int) mWidth, (int) (mHeight + mDepth), Bitmap.Config.ARGB_8888);
                shadowCanvas = new Canvas(shadow);
            }

            srcCanvas.drawColor(BG_COLOR);

            mOvals.set(mGapLeft, mGapTop, mWidth - mGapRight, mHeight - mGapBottom);
            mOutline.set(mOvals.left, mOvals.top + mDepth, mOvals.right, mOvals.bottom + mDepth);

            mScaleMatrix.setScale(0.3f, 0.3f, mOvals.centerX(), mOvals.centerY());

            mOuterPath.reset();
            mCenterPath.reset();
            mBgCenterPath.reset();
            mDepthPath.reset();
            mShadowPath.reset();

            mOuterPath.addRect(0, 0, mWidth, mHeight + mDepth, Path.Direction.CW);

            mCenterPath.addOval(mOvals, Path.Direction.CW);
            mCenterPath.transform(mScaleMatrix);

            if (mChartType == CHART_TYPE_DONUT && !isInEditMode()) {
                srcCanvas.clipPath(mCenterPath, Region.Op.DIFFERENCE);
            }

            mDepthPath.arcTo(mOutline, 0, 180);
            mDepthPath.lineTo(mGapLeft, (mHeight - mDepth) / 2);
            mDepthPath.arcTo(mOvals, 180, 180);
            mDepthPath.lineTo(mWidth - mGapRight, (mHeight + mDepth) / 2);

            mBgCenterPath.addOval(mOvals, Path.Direction.CW);
            mScaleMatrix.setScale(0.3f, 0.3f, mOvals.centerX(), mOvals.centerY() + mDepth);
            mBgCenterPath.transform(mScaleMatrix);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bgCanvas.clipPath(mDepthPath);
            }
            else {
                bgCanvas.clipPath(mDepthPath, Region.Op.REPLACE);
            }

            if(shadowCanvas != null) {
                mShadowPath.addOval(mOutline, Path.Direction.CW);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    shadowCanvas.clipPath(mOuterPath);
                }
                else {
                    shadowCanvas.clipPath(mOuterPath, Region.Op.REPLACE);
                }
                if (mChartType == CHART_TYPE_DONUT) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        /*Path p = new Path();
                        p.addOval(mOvals, Path.Direction.CW);

                        p.op(mBgCenterPath, Path.Op.INTERSECT);*/
                        //shadowCanvas.clipOutPath(p);
                        mShadowPath.op(mBgCenterPath, Path.Op.DIFFERENCE);
                        shadowCanvas.drawPath(mShadowPath, mShadowPaint);
                    }
                    else {
                        shadowCanvas.clipPath(mBgCenterPath, Region.Op.XOR);
                        shadowCanvas.drawOval(mOutline, mShadowPaint);
                    }
                }
            }

            if (mOvals.height() * 0.5f > mDepth) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bgCanvas.clipOutPath(mBgCenterPath);
                }
                else {
                    bgCanvas.clipPath(mBgCenterPath, Region.Op.XOR);
                }
            }

            float mStart = mStartRadius;
            PieItem item;
            int prevX = (int) (getWidth() - mGapRight);
            float innerCircleStart = mOvals.centerX() - (mOvals.width() / 2) * 0.3f;
            float innerCircleStop = mOvals.centerX() + (mOvals.width() / 2) * 0.3f;
            Log.d(TAG, "inner circle start: " + innerCircleStart);
            Log.d(TAG, "inner circle stop: " + innerCircleStop);
            for (PieItem aMDataArray : mDataArray) {
                Log.d(TAG, "--------------------------------------------------");
                item = aMDataArray;
                mBgPaint.setColor(item.getColor());
                Bitmap tex = BitmapFactory.decodeResource(getResources(), item.getTexture());
                if (tex != null) {
                    BitmapShader textureShader = new BitmapShader(tex, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                    mBgPaint.setShader(textureShader);

                    mBgPaint.setColorFilter(new LightingColorFilter(0x00FFFFFF, item.getColor()));
                } else {
                    mBgPaint.setColorFilter(null);
                    mBgPaint.setShader(null);
                }
                float mSweep = (float) 360 * (item.getValue() / mMaxConnection);
                srcCanvas.drawArc(mOvals, mStart, mSweep, true, mBgPaint);
                Log.d(TAG, "item: " + item.getLabel());
                Log.d(TAG, "arc angle: " + (mStart + mSweep));
                if (tex != null) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    Bitmap rotatedTex = Bitmap.createBitmap(tex, 0, 0, tex.getWidth(), tex.getHeight(), matrix, true);
                    BitmapShader textureShader = new BitmapShader(rotatedTex, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                    mBgPaint.setShader(textureShader);
                }
                if (mStart + mSweep >= 0 && (mStart + mSweep <= 180 || mStart < 180)) {
                    Log.d(TAG, "outer depth");
                    int s_x = (int) (mOvals.centerX() + (mWidth / 2 - mGapRight) * Math.cos((mStart + mSweep) * Math.PI / 180));
                    if (mStart + mSweep > 180) {
                        s_x = (int) mGapRight;
                    }
                    Log.d(TAG, "arc end coords: " + s_x);
                    Log.d(TAG, "prev arc end coords: " + prevX);
                    bgCanvas.drawRect(s_x, 0, prevX, mHeight + mDepth, mBgPaint);
                    prevX = s_x;

                }
                if (mStart + mSweep > 180) {
                    Log.d(TAG, "inner depth");
                    int s_x = (int) (mOvals.centerX() + (mWidth / 2 - mGapRight) * 0.3f * Math.cos((mStart + mSweep) * Math.PI / 180));
                    if (mStart <= 180) {
                        prevX = (int) innerCircleStart;
                    }
                    if (s_x > innerCircleStart) {
                        if (s_x > innerCircleStop) {
                            s_x = (int) innerCircleStop;
                        }
                        Log.d(TAG, "arc end coords: " + s_x);
                        Log.d(TAG, "prev arc end coords: " + prevX);
                        bgCanvas.drawRect(prevX, 0, s_x, mHeight / 2, mBgPaint);
                        prevX = s_x;
                    }
                }
                mStart += mSweep;
            }
            Log.d(TAG, "--------------------------------------------------");

            if(mDepth > 0) {
                mClearPaint.setShader(new LinearGradient(0, 0, mWidth, 0, Color.TRANSPARENT, 0x4C000000, Shader.TileMode.MIRROR));
                bgCanvas.drawPath(mDepthPath, mClearPaint);
            }
            mClearPaint.setShader(null);

            if (mChartType == CHART_TYPE_DONUT && mOvals.height() * 0.5f > mDepth) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    bgCanvas.clipOutPath(mBgCenterPath);
                }
                else {
                    bgCanvas.clipPath(mBgCenterPath, Region.Op.XOR);
                }
                bgCanvas.drawPath(mBgCenterPath, mLinePaints);
            }

            if(shadow != null) {
                canvas.drawBitmap(shadow, 0, 0, mClearPaint);
            }

            if(mDepth > 0) {
                mClearPaint.setColorFilter(mClearLightingColorFilter);
                canvas.drawBitmap(background, 0, 0, mClearPaint);
                mClearPaint.setColorFilter(null);
                canvas.drawPath(mDepthPath, mLinePaints);
            }
            if(mDepth == 0) {
                mLinePaints.setColor(mBorderColor);
            }
            else {
                mLinePaints.setColor(0xFFFFFFFF);
            }
            if (mChartType == CHART_TYPE_DONUT) {
                canvas.drawPath(mCenterPath, mLinePaints);
            }

            canvas.drawBitmap(pie, 0, 0, mClearPaint);
            canvas.drawOval(mOvals, mLinePaints);

            setState(IS_READY_TO_DRAW);
        }
        super.onDraw(canvas);
	}

    public void setShadowRadius(int radius){
        if(radius > 0){
            mShadowRadius = radius;
            if(mShadowRadius > mGapLeft){
                mGapLeft = mShadowRadius;
            }
            if(mShadowRadius > mGapRight){
                mGapRight = mShadowRadius;
            }
            mHasShadow = true;
        }
        else {
            mHasShadow = false;
        }
    }

    public void setShadowColor(int color){
        mShadowColor = color;
        mBorderColor = color;
    }

    public void setGeometry(int width, int height, int gapLeft, int gapRight, int gapTop, int gapBottom, int depth) {
        Log.d(TAG, "setGeometry: " + width + "x" + height);
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, getResources().getDisplayMetrics());
        Log.d(TAG, "padding: " + padding);
        mWidth     = width;
        mHeight    = height;
        if(depth > -1) {
            mDepth = depth;
        }
        mGapLeft   = gapLeft + padding;
        mGapRight  = gapRight + padding;
        if(mShadowRadius > mGapLeft){
            mGapLeft = mShadowRadius;
        }
        if(mShadowRadius > mGapRight){
            mGapRight = mShadowRadius;
        }
        mGapTop    = gapTop + padding;
        mGapBottom = gapBottom + mDepth + padding;
        Log.d(TAG, "Size: " + mWidth + "x" + mHeight);

        setState(IS_READY_TO_DRAW);
	}

    /**
     * This is called during layout when the size of this view has changed. If
     * you were just added to the view hierarchy, you're called with the old
     * values of 0.
     *
     * @param w    Current width of this view.
     * @param h    Current height of this view.
     * @param oldw Old width of this view.
     * @param oldh Old height of this view.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if(oldh > 0 && oldw > 0) {
            Log.d(TAG, "onSizeChanged");
            Log.d(TAG, oldw + "x" + oldh);
            float wRatio = ((float)oldw) / w;
            float hRatio = ((float)oldh) / h;
            mWidth = w;
            mHeight = h;
            mDepth = mDepth / hRatio;
            mGapTop = mGapTop / hRatio;
            mGapLeft = mGapLeft / wRatio;
            mGapBottom = mGapBottom / hRatio;
            mGapRight = mGapRight / wRatio;
            setState(IS_READY_TO_DRAW);
        }
    }

    /**
     * <p>
     * Measure the view and its content to determine the measured width and the
     * measured height. This method is invoked by {@link #measure(int, int)} and
     * should be overriden by subclasses to provide accurate and efficient
     * measurement of their contents.
     * </p>
     * <p/>
     * <p>
     * <strong>CONTRACT:</strong> When overriding this method, you
     * <em>must</em> call {@link #setMeasuredDimension(int, int)} to store the
     * measured width and height of this view. Failure to do so will trigger an
     * <code>IllegalStateException</code>, thrown by
     * {@link #measure(int, int)}. Calling the superclass'
     * {@link View#onMeasure(int, int)} is a valid use.
     * </p>
     * <p/>
     * <p>
     * The base class implementation of measure defaults to the background size,
     * unless a larger size is allowed by the MeasureSpec. Subclasses should
     * override {@link View#onMeasure(int, int)} to provide better measurements of
     * their content.
     * </p>
     * <p/>
     * <p>
     * If this method is overridden, it is the subclass's responsibility to make
     * sure the measured height and width are at least the view's minimum height
     * and width ({@link #getSuggestedMinimumHeight()} and
     * {@link #getSuggestedMinimumWidth()}).
     * </p>
     *
     * @param widthMeasureSpec  horizontal space requirements as imposed by the parent.
     *                          The requirements are encoded with
     *                          {@link android.view.View.MeasureSpec}.
     * @param heightMeasureSpec vertical space requirements as imposed by the parent.
     *                          The requirements are encoded with
     *                          {@link android.view.View.MeasureSpec}.
     * @see #getMeasuredWidth()
     * @see #getMeasuredHeight()
     * @see #setMeasuredDimension(int, int)
     * @see #getSuggestedMinimumHeight()
     * @see #getSuggestedMinimumWidth()
     * @see android.view.View.MeasureSpec#getMode(int)
     * @see android.view.View.MeasureSpec#getSize(int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure");
        int width;
        if(getLayoutParams().width == ViewGroup.LayoutParams.WRAP_CONTENT){
            width = (int) mWidth;
        }
        else if(getLayoutParams().width == ViewGroup.LayoutParams.MATCH_PARENT) {
            width = MeasureSpec.getSize(widthMeasureSpec);
            mWidth = width;
        }
        else {
            width = getLayoutParams().width;
        }
        Log.d(TAG, "width: " + width + "(" + mWidth + ")");
        int height;
        if(getLayoutParams().height == ViewGroup.LayoutParams.WRAP_CONTENT){
            height = (int) (mHeight + mDepth);
        }
        else if(getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
            height = MeasureSpec.getSize(heightMeasureSpec);
        }
        else {
            height = (int) (getLayoutParams().height + mDepth);
        }
        setMeasuredDimension(width|MeasureSpec.EXACTLY, height|MeasureSpec.EXACTLY);
    }

    public void setData(List<PieItem> data) {
		mDataArray = data;
        mMaxConnection = 0;
        for (PieItem aMDataArray : mDataArray) {
            mMaxConnection += aMDataArray.getValue();
        }
		setState(IS_READY_TO_DRAW);
	}

    @SuppressWarnings("unused")
    public void setChartType(int type){
        mChartType = type;
    }

    @SuppressWarnings("unused")
    public void setStartRadius(float radius){
        mStartRadius = radius;
    }

    @SuppressWarnings("unused")
	private void setState(int State) {
		mState = State;
	}
}