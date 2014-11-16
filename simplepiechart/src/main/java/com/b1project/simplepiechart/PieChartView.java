package com.b1project.simplepiechart;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
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
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

public class PieChartView extends View {

    public static final int CHART_TYPE_PIE = 0x00;
    public static final int CHART_TYPE_DONUT = 0x01;

    private static final String TAG = PieChartView.class.getSimpleName();
    private static final int BG_COLOR = 0x00000000;
	private static final int WAIT = 0;
	private static final int IS_READY_TO_DRAW = 1;
	private static final int IS_DRAWING = 2;
	private static final float START_RADIUS = 0;
	private Paint mBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
	private Paint mLinePaints = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mClearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mShadowPaint = new Paint(0);
	private float   mWidth = 64;
	private float   mHeight = 64;
    private float   mDepth = 10.0f;
	private float   mGapLeft = 8;
	private float   mGapRight = 8;
	private float   mGapTop = 8;
	private float   mGapBottom = 16;
	private int     mState = WAIT;
    private float   mMaxConnection;
	private List<PieItem> mDataArray;
    private int mChartType = CHART_TYPE_PIE;

	public PieChartView (Context context){
		super(context);
	}

	public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PieChartView);
        mChartType = a.getInt(R.styleable.PieChartView_chart_type, CHART_TYPE_PIE);
        mWidth = a.getDimensionPixelSize(R.styleable.PieChartView_chart_width, 64);
        mHeight = a.getDimensionPixelSize(R.styleable.PieChartView_chart_height, 64);
        a.recycle();
        setState(IS_READY_TO_DRAW);
	}

	@SuppressLint("DrawAllocation")
	@Override 
	protected void onDraw(Canvas canvas) {

        if (mState == IS_READY_TO_DRAW) {
            setState(IS_DRAWING);

            Bitmap pie = Bitmap.createBitmap((int) mWidth, (int) mHeight, Bitmap.Config.ARGB_8888);
            Bitmap background = Bitmap.createBitmap((int) mWidth, (int) (mHeight + mDepth), Bitmap.Config.ARGB_8888);
            Bitmap shadow = Bitmap.createBitmap((int) mWidth, (int) (mHeight + mDepth), Bitmap.Config.ARGB_8888);
            Canvas srcCanvas = new Canvas(pie);
            Canvas bgCanvas = new Canvas(background);
            Canvas shadowCanvas = new Canvas(shadow);

            srcCanvas.drawColor(BG_COLOR);

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

            mShadowPaint.setColor(0xFF101010);
            mShadowPaint.setMaskFilter(new BlurMaskFilter(8, BlurMaskFilter.Blur.NORMAL));
            setLayerType(LAYER_TYPE_SOFTWARE, mShadowPaint);

            RectF mOvals = new RectF(mGapLeft, mGapTop, mWidth - mGapRight, mHeight - mGapBottom);
            RectF mOutline = new RectF(mOvals.left, mOvals.top + mDepth, mOvals.right, mOvals.bottom + mDepth);

            Matrix scaleMatrix = new Matrix();
            scaleMatrix.setScale(0.3f, 0.3f, mOvals.centerX(), mOvals.centerY());

            Path outerPath = new Path();
            Path centerPath = new Path();
            Path bgCenterPath = new Path();
            Path depthPath = new Path();
            Path shadowPath = new Path();

            outerPath.addRect(0, 0, mWidth, mHeight, Path.Direction.CW);

            centerPath.addOval(mOvals, Path.Direction.CW);
            centerPath.transform(scaleMatrix);

            if (mChartType == CHART_TYPE_DONUT) {
                srcCanvas.clipPath(centerPath, Region.Op.DIFFERENCE);
            }

            depthPath.arcTo(mOutline, 0, 180);
            depthPath.lineTo(mGapLeft, (mHeight - mDepth) / 2);
            depthPath.arcTo(mOvals, 180, 180);
            depthPath.lineTo(mWidth - mGapRight, (mHeight + mDepth) / 2);

            bgCenterPath.addOval(mOvals, Path.Direction.CW);
            scaleMatrix.setScale(0.3f, 0.3f, mOvals.centerX(), mOvals.centerY() + mDepth);
            bgCenterPath.transform(scaleMatrix);

            bgCanvas.clipPath(depthPath, Region.Op.REPLACE);

            shadowPath.addOval(mOutline, Path.Direction.CW);
            shadowCanvas.clipPath(outerPath, Region.Op.REPLACE);
            if (mChartType == CHART_TYPE_DONUT) {
                shadowCanvas.clipPath(bgCenterPath, Region.Op.XOR);
            }
            shadowCanvas.drawOval(mOutline, mShadowPaint);

            if (mOvals.height() * 0.3f > mDepth) {
                bgCanvas.clipPath(bgCenterPath, Region.Op.XOR);
            }

            float mStart = START_RADIUS;
            PieItem item;
            int prevX = (int) (bgCanvas.getWidth() - mGapRight);
            float innerCircleStart = mOvals.centerX() - (mOvals.width() / 2) * 0.3f;
            float innerCircleStop = mOvals.centerX() + (mOvals.width() / 2) * 0.3f;
            Log.d(TAG, "inner circle start: " + innerCircleStart);
            Log.d(TAG, "inner circle stop: " + innerCircleStop);
            for (PieItem aMDataArray : mDataArray) {
                item = aMDataArray;
                mBgPaint.setColor(item.Color);
                Bitmap tex = BitmapFactory.decodeResource(getResources(), item.Texture);
                if (tex != null) {
                    BitmapShader textureShader = new BitmapShader(tex, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT);
                    mBgPaint.setShader(textureShader);
                    mBgPaint.setColorFilter(new LightingColorFilter(0x00FFFFFF, item.Color));
                } else {
                    mBgPaint.setColorFilter(null);
                    mBgPaint.setShader(null);
                }
                float mSweep = (float) 360 * (item.Count / mMaxConnection);
                srcCanvas.drawArc(mOvals, mStart, mSweep, true, mBgPaint);
                Log.d(TAG, "item: " + item.Label);
                Log.d(TAG, "arc angle: " + (mStart + mSweep));
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

            mClearPaint.setShader(new LinearGradient(0, 0, mWidth, 0, Color.TRANSPARENT, 0x4C000000, Shader.TileMode.MIRROR));
            bgCanvas.drawPath(depthPath, mClearPaint);
            mClearPaint.setShader(null);

            if (mChartType == CHART_TYPE_DONUT && mOvals.height() * 0.3f > mDepth) {
                bgCanvas.clipPath(bgCenterPath, Region.Op.XOR);
                bgCanvas.drawPath(bgCenterPath, mLinePaints);
            }

            canvas.drawBitmap(shadow, 0, 0, mClearPaint);

            mClearPaint.setColorFilter(new LightingColorFilter(0xFFDDDDDD, 0xFF000000));
            canvas.drawBitmap(background, 0, 0, mClearPaint);
            mClearPaint.setColorFilter(null);

            canvas.drawPath(depthPath, mLinePaints);
            mLinePaints.setColor(0xFFFFFFFF);
            if (mChartType == CHART_TYPE_DONUT) {
                canvas.drawPath(centerPath, mLinePaints);
            }

            canvas.drawBitmap(pie, 0, 0, mClearPaint);
            canvas.drawOval(mOvals, mLinePaints);
            setState(IS_READY_TO_DRAW);
        }
		super.onDraw(canvas);
	}

    public void setGeometry(float width, float height, float gapLeft, float gapRight, float gapTop, float gapBottom, float depth) {
        Resources r = getResources();
        float padding = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, r.getDisplayMetrics());
        mWidth     = width;
        mHeight    = height;
        mGapLeft   = gapLeft + padding;
        mGapRight  = gapRight + padding;
        mGapTop    = gapTop + padding;
        mGapBottom = gapBottom + depth + padding*2;
        ViewGroup.LayoutParams params = getLayoutParams();
        if(params == null){
            params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        params.height = (int) height;
        params.width = (int) width;
        setLayoutParams(params);
        if(depth > -1) {
        		mDepth = depth;
        }
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
            Log.d(TAG, oldw + "x" + oldh);
            float wRatio = oldw / w;
            float hRatio = oldh / h;
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

    public void setData(List<PieItem> data) {
		mDataArray = data;
        mMaxConnection = 0;
        for (PieItem aMDataArray : mDataArray) {
            mMaxConnection += aMDataArray.Count;
        }
		mState = IS_READY_TO_DRAW;
	}

    public void setChartType(int type){
        mChartType = type;
    }

	private void setState(int State) {
		mState = State;
	}
}