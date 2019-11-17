package com.demo.bigimg;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Scroller;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

public class BigImgView extends View implements GestureDetector.OnGestureListener, View.OnTouchListener, GestureDetector.OnDoubleTapListener {

    private final Rect mRect;
    private final BitmapFactory.Options mOptions;
    private final GestureDetector mGestureDetector;
    private final Scroller mScroller;
    private final ScaleGestureDetector mScaleGestureDetector;
    private int mImageWidth;
    private int mImageHeight;
    private BitmapRegionDecoder mDecoder;
    private int mViewWidth;
    private int mViewHeight;
    private float mScale;
    private Bitmap mBitmap;
    private float mOriginalScale;

    public BigImgView(Context context) {
        this(context, null);
    }

    public BigImgView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BigImgView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public BigImgView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        // 创建一个矩形区域，用来展示图片
        mRect = new Rect();
        // 创建图片设置选项
        mOptions = new BitmapFactory.Options();
        // 创建手势识别器
        mGestureDetector = new GestureDetector(context, this);
        // 创建滚动监听器
        mScroller = new Scroller(context);
        // 缩放手势识别
        mScaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
        // 监听触摸事件
        setOnTouchListener(this);
    }


    public void setImage(InputStream is) {
        //如果设置为真，解码器将返回null(没有位图)，但是允许调用者
        //查询位图，而不必为其像素分配内存
        mOptions.inJustDecodeBounds = true;
        // 将输入流解码为位图 bitmap
        BitmapFactory.decodeStream(is, null, mOptions);
        // 获取真实图片的宽高
        mImageWidth = mOptions.outWidth;
        mImageHeight = mOptions.outHeight;
        mOptions.inMutable = true;// 开启内存复用
        // 设置解码格式  Bitmap.Config RGB_565：没有透明度，R=5，G=6，B=5，，那么一个像素点占5+6+5=16位（2字节）
        mOptions.inPreferredConfig = Bitmap.Config.RGB_565;
        mOptions.inJustDecodeBounds = false;

        // 创建一个区域解码器
        try {
            // BitmapRegionDecoder可以用来解码一个矩形区域的图像
            mDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // 获取控件本身的测量宽高
        mViewWidth = getMeasuredWidth();
        mViewHeight = getMeasuredHeight();

       /* // 确定加载图片的区域
        mRect.left = 0;
        mRect.top = 0;
        mRect.right = mImageWidth;
        // 根据图片实际宽度和控件测量宽度计算缩放因子
        mScale = mViewWidth / (float) mImageWidth;
        // 根据缩放因子计算出图片缩放后的高度
        mRect.bottom = (int) (mViewHeight / mScale);*/

        //==========================================//
        // 添加缩放手势识别后的测量逻辑
        mRect.top = 0;
        mRect.left = 0;
        mRect.right = Math.min(mViewWidth, mImageWidth);
        mRect.bottom = Math.min(mViewHeight, mImageHeight);
        // 计算一个最初加载图片的缩放因子
        mOriginalScale = mViewWidth / (float) mImageWidth;
        mScale = mOriginalScale;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mDecoder == null) {
            return;
        }
        mOptions.inBitmap = mBitmap;// 内存复用，注：内存复用的bitmap必须和即将解码的bitmap尺寸大小一致
        mBitmap = mDecoder.decodeRegion(mRect, mOptions);
        // 创建矩阵
        Matrix matrix = new Matrix();
        // 将矩阵按sx和sy进行缩放
        //matrix.setScale(mScale, mScale);
        // 当添加缩放手势识别后，缩放因子会随着手势改变
        matrix.setScale(mViewWidth / (float) mRect.width(), mViewWidth / (float) mRect.width());
        // 使用指定的矩阵绘制位图
        canvas.drawBitmap(mBitmap, matrix, null);
    }

    // 2、当手指按下时，停止滑动
    @Override
    public boolean onDown(MotionEvent e) {
        if (!mScroller.isFinished()) {
            mScroller.forceFinished(true);
        }
        return true;// 继续处理后续事件
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    // 3、滑动监听事件处理

    /**
     * @param e1        滑动开始事件
     * @param e2        当前滑动事件
     * @param distanceX X轴滑动距离
     * @param distanceY Y轴滑动距离
     * @return
     */
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        // 上下滑动时，相对应的mRect区域发生改变
        //mRect.offset(0, (int) distanceY);
        mRect.offset((int) distanceX, (int) distanceY);// 上下左右滑动都会影响mRect区域发生改变
        checkBound();
        invalidate();
        return false;
    }

    private void checkBound() {
        // 滑动时，当滑动到最顶部和底部时的处理逻辑
        if (mRect.bottom > mImageHeight) {
            mRect.bottom = mImageHeight;
            mRect.top = mImageHeight - (int) (mImageHeight / mScale);
        }
        if (mRect.top < 0) {
            mRect.top = 0;
            mRect.bottom = (int) (mImageHeight / mScale);
        }
        // 滑动时，当滑动到最左和右时的逻辑处理
        if (mRect.right > mImageWidth) {
            mRect.right = mImageWidth;
            mRect.left = mImageWidth - (int) (mViewWidth / mScale);
        }
        if (mRect.left < 0) {
            mRect.left = 0;
            mRect.right = (int) (mViewWidth / mScale);
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    // 5、处理惯性滑动
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        // 上下滑动惯性处理
        /*mScroller.fling(0, mRect.top, 0, (int) -velocityY, 0, 0,
                0, mImageHeight - (int) (mImageHeight / mScale));*/
        // 上下左右滑动惯性处理
        mScroller.fling(mRect.left, mRect.top, (int) -velocityX, (int) -velocityY, 0, mImageWidth - (int) (mImageWidth / mScale),
                0, mImageHeight - (int) (mImageHeight / mScale));
        return false;
    }

    // 4、计算区域位置
    @Override
    public void computeScroll() {
        if (mScroller.isFinished()) {
            return;
        }
        if (mScroller.computeScrollOffset()) {
            // 返回滚动中的当前Y偏移量
            mRect.top = mScroller.getCurrY();
            mRect.bottom = mRect.top + (int) (mImageHeight / mScale);
            invalidate();
        }
    }

    // 1、将触摸事件交给手势识别器处理
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //return mGestureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);// 将触摸事件传递给手势识别器
        mScaleGestureDetector.onTouchEvent(event);// 将触摸事件传递给缩放手势识别器
        return true;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        if (mScale < mOriginalScale * 1.5) {
            mScale = mOriginalScale * 3;
        } else {
            mScale = mOriginalScale;
        }
        mRect.right = mRect.left + (int) (mViewWidth / mScale);
        mRect.bottom = mRect.top + (int) (mViewHeight / mScale);
        // 超出边界判断逻辑
        checkBound();
        invalidate();
        return true;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        return false;
    }

    // 处理缩放手势识别的回调事件
    class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            // 根据缩放手势，相应的变换缩放因子
            float scale = mScale;
            scale += detector.getScaleFactor() - 1;
            if (scale <= mOriginalScale) {
                scale = mOriginalScale;
            } else if (scale >= mOriginalScale * 3) {
                scale = mOriginalScale * 3;
            }
            mRect.right = mRect.left + (int) (mViewWidth / scale);
            mRect.bottom = mRect.top + (int) (mViewHeight / scale);
            mScale = scale;
            invalidate();
            return true;
        }
    }
}
