package com.example.mpchatr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.PathShape;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by 刘高坡
 * Date:  2018-10-24
 * Time:Time: 08:00
 * FIXME
 **/
public class MpChartView extends View {


    public MpChartView(Context context) {
        super(context);
        this.context = context;
        initAttrs(context, null);
    }

    public MpChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        initAttrs(context, attrs);
        initPaint();
    }

    public MpChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        initAttrs(context, attrs);
        initPaint();
    }

    public MpChartView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initAttrs(context, attrs);
        this.context = context;
        initPaint();
    }

    private int maxScore;
    private int minScore;
    private int brokenLineColor = Color.RED;
    private int textNormalColor = Color.BLUE;
    private int textSize = 10;
    private int straightLineColor = Color.GRAY;

    private int brokenLineWith = 6;
    private Path brokenPath;//折线图的路径
    private Paint brokenPaint;//折断的线
    private Paint straightPaint;//直线
    private Paint dottedPaint;//布满的Paint
    private Paint textPaint;//文字
    private int viewWith;
    private int viewHeight;

    private List<Point> pointList;
    private String[] scoreS = {"", 100 + "%", 80 + "%", 60 + "%", 40 + "%", 20 + "%", 0 + "%", -20 + "%", -40 + "%", -60 + "%"};
    private int[] scoreData = {-60, -20, -40, 0, -40, 20, 40, 80, 60, 100, 20};
    private String[] monthText = {"" + 1, 3 + "", "" + 6, "" + 9, "" + 12, "" + 15, "" + 18, "" + 21, "" + 24, "" + 27, "" + 30};
    private int monthCount = monthText.length;
    private int vCount = scoreS.length;
    private int selectMonth;//被选中的月份

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        this.getParent().requestDisallowInterceptTouchEvent(true);
        //一旦底层View收到touch的action后调用这个方法那么父层View就不会再调用onInterceptTouchEvent了，也无法截获以后的action，这个事件被消费了
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_UP://触摸(ACTION_DOWN操作)，滑动(ACTION_MOVE操作)和抬起(ACTION_UP)
                onActionUpEvent(event);
                this.getParent().requestDisallowInterceptTouchEvent(false);
                break;
            case MotionEvent.ACTION_CANCEL:
                this.getParent().requestDisallowInterceptTouchEvent(false);
                break;
        }
        return true;
    }

    private void onActionUpEvent(MotionEvent event) {

        boolean isValidTouch = validateTouch(event.getX(), event.getY());//判断是否是指定的触摸区域

        if (isValidTouch) {
            invalidate();
        }

    }

    //是否是有效的触摸范围
    private boolean validateTouch(float x, float y) {
        //曲线触摸区域
        for (int i = 0; i < pointList.size(); i++) {
            // dipToPx(8)乘以2为了适当增大触摸面积
            Log.e("/////2", pointList.get(i).x + "--" + pointList.get(i).y);

            if (x > (pointList.get(i).x - dipToPx(8) * 2) && x < (pointList.get(i).x + dipToPx(8) * 2)) {
                if (y > (pointList.get(i).y - dipToPx(8) * 2) && y < (pointList.get(i).y + dipToPx(8) * 2)) {
                    selectMonth = i + 1;
                    return true;
                }
            }
        }
        //月份触摸区域
        //计算每个月份X坐标的中心点
        float monthTouchY = viewHeight * 0.7f - dipToPx(3);//减去dipToPx(3)增大触摸面积
        float newWith = viewWith - (viewWith * 0.15f) * 2;//分隔线距离最左边和最右边的距离是0.15倍的viewWith
        float validTouchX[] = new float[monthText.length];
        for (int i = 0; i < monthText.length; i++) {
            validTouchX[i] = newWith * ((float) (i) / (monthCount - 1)) + (viewWith * 0.15f);
        }
        if (y > monthTouchY) {
            for (int i = 0; i < validTouchX.length; i++) {
                if (x < validTouchX[i] + dipToPx(8) && x > validTouchX[i] - dipToPx(8)) {
                    selectMonth = i + 1;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawDotsLine(canvas);
        drawText(canvas);//绘制文字，minScore，maxScore

        drawXLine(canvas);//X的线及坐标点
        drawYLine(canvas);//Y的线及坐标点
        drawBrokenLine(canvas);//绘制折线，就是画点，moveto连接
        drawPoint(canvas);//绘制穿过折线的点
    }

    private void drawDotsLine(Canvas canvas) {

        for (int i = 0; i <= vCount; i++) {
            //横虚线
            float y = ((viewHeight - (viewHeight * 0.15f) * 2) * ((float) (i) / (vCount)));
            drawDottedLine(canvas, xStartX, y, xStopX, y);// 虚线的画法
        }
        for (int i = 1; i <= monthCount; i++) {
//            //竖虚线
            float coordinateX = ((viewWith - (viewWith * 0.05f) * 2) * ((float) (i) / (monthCount)) + (viewWith * 0.15f)) - 50;
            drawDottedLine(canvas, coordinateX, 10, coordinateX, yStop - 50);//下面一条虚线的画法
        }
    }

    //这个方法是利用path和point画出图形，并设置背景颜色
    private void drawFloatTextBackground(Canvas canvas, int x, int y) {
        brokenPath.reset();
        brokenPaint.setColor(brokenLineColor);
        brokenPaint.setStyle(Paint.Style.FILL);

        //P1
        Point point = new Point(x, y);
        brokenPath.moveTo(point.x, point.y);

        //P2
        point.x = point.x + dipToPx(5);
        point.y = point.y - dipToPx(5);
        brokenPath.lineTo(point.x, point.y);

        //P3
        point.x = point.x + dipToPx(12);
        brokenPath.lineTo(point.x, point.y);

        //P4
        point.y = point.y - dipToPx(17);
        brokenPath.lineTo(point.x, point.y);

        //P5
        point.x = point.x - dipToPx(34);
        brokenPath.lineTo(point.x, point.y);

        //P6
        point.y = point.y + dipToPx(17);
        brokenPath.lineTo(point.x, point.y);

        //P7
        point.x = point.x + dipToPx(12);
        brokenPath.lineTo(point.x, point.y);

        //最后一个点连接到第一个点
        brokenPath.lineTo(x, y);
        canvas.drawPath(brokenPath, brokenPaint);
    }

    private void drawPoint(Canvas canvas) {
        if (pointList == null) {
            return;
        }
        brokenPaint.setStrokeWidth(dipToPx(1));
        for (int i = 0; i < pointList.size(); i++) {
            brokenPaint.setColor(brokenLineColor);
            brokenPaint.setStyle(Paint.Style.STROKE);
            canvas.drawCircle(pointList.get(i).x, pointList.get(i).y, dipToPx(3), brokenPaint);
            brokenPaint.setColor(Color.WHITE);
            brokenPaint.setStyle(Paint.Style.FILL);
            if (i == selectMonth - 1) {//默认选中的才会绘制不同的点，如图
                brokenPaint.setColor(0xffd0f3f2);
                canvas.drawCircle(pointList.get(i).x, pointList.get(i).y, dipToPx(8f), brokenPaint);
                brokenPaint.setColor(0xff81dddb);
                canvas.drawCircle(pointList.get(i).x, pointList.get(i).y, dipToPx(5f), brokenPaint);

                //绘制浮动文本背景框
                drawFloatTextBackground(canvas, pointList.get(i).x, pointList.get(i).y - dipToPx(8f));

                textPaint.setColor(0xffffffff);
                //绘制浮动文字
                canvas.drawText(String.valueOf(scoreData[i]), pointList.get(i).x, pointList.get(i).y - dipToPx(5f) - textSize, textPaint);
            }
            brokenPaint.setColor(0xffffffff);
            canvas.drawCircle(pointList.get(i).x, pointList.get(i).y, dipToPx(1.5f), brokenPaint);
            brokenPaint.setStyle(Paint.Style.STROKE);
            brokenPaint.setColor(brokenLineColor);
            canvas.drawCircle(pointList.get(i).x, pointList.get(i).y, dipToPx(2.5f), brokenPaint);
        }

    }

    //绘制折线
    private void drawBrokenLine(Canvas canvas) {
        brokenPath.reset();
        brokenPaint.setColor(brokenLineColor);
        brokenPaint.setStyle(Paint.Style.STROKE);
        if (scoreData.length == 0) {
            return;
        }
        brokenPath.moveTo(pointList.get(0).x, pointList.get(0).y);
        for (int i = 0; i < pointList.size(); i++) {
            brokenPath.lineTo(pointList.get(i).x, pointList.get(i).y);
        }
        canvas.drawPath(brokenPath, brokenPaint);
    }

    private float xNewWith;
    private float xStartX;
    private float xStartY;
    private float xStopX;
    private float xStopY;

    private float yStart;
    private float yStop;
    private float yStopY;

    private void initXY() {
        xNewWith = viewWith - (viewWith * 0.05f) * 2;//分隔线距离最左边和最右边的距离是0.1倍的viewWith
        xStartX = (xNewWith * ((float) (0) / (monthCount - 1)) + (viewWith * 0.15f));
        xStartY = viewHeight * 0.7f;
        xStopX = xNewWith + 10;
        xStopY = viewHeight * 0.7f;

        float yNewHeight = viewWith - (viewWith * 0.05f) * 2;
        yStart = yNewHeight * ((float) (0) / (vCount - 1)) + (viewWith * 0.15f);
        yStop = yNewHeight * ((float) (vCount - 1) / (vCount - 1)) + (viewWith * 0.15f) - dipToPx(75);
        yStopY = yNewHeight * ((float) (0) / (vCount - 1)) + (viewWith * 0.15f) - dipToPx(75);
    }


    private void drawYLine(Canvas canvas) {
        straightPaint.setStrokeWidth(dipToPx(1));
        //Y 横线
        //分隔线距离最左边和最右边的距离是0.1倍的viewWith
        canvas.drawLine(yStart - 50, yStopY, yStart - 50, yStop - 65, straightPaint);
        float coordinate;//分隔线X坐标
        for (int i = 0; i <= vCount; i++) {
            coordinate = ((viewHeight - (viewHeight * 0.15f) * 2) * ((float) (i) / (vCount)));
            canvas.drawLine(viewWith / 8f, coordinate, yStop / 8f, coordinate, straightPaint);
        }
    }

    private void drawXLine(Canvas canvas) {
        straightPaint.setStrokeWidth(dipToPx(1));
        //X 横线
        canvas.drawLine(xStartX, xStartY, xStopX, xStopY, straightPaint);
        float coordinateX;//分隔线X坐标
        for (int i = 0; i <= monthCount; i++) {
            coordinateX = (xNewWith * ((float) (i) / (monthCount)) + (viewWith * 0.15f)) - 50;
            canvas.drawLine(coordinateX, viewHeight * 0.7f, coordinateX, viewHeight * 0.7f + dipToPx(4), straightPaint);
        }
    }

    private void drawText(Canvas canvas) {
        textPaint.setTextSize(textSize);//默认字体15
        textPaint.setColor(textNormalColor);
        textPaint.setTextSize(textSize);
        textPaint.setStyle(Paint.Style.STROKE);
        textPaint.setColor(textNormalColor);
//        textSize = (int) textPaint.getTextSize();
        //分隔线距离最左边和最右边的距离是0.1倍的viewWith
        float coordinate;//分隔线X坐标
        for (int i = 0; i < scoreS.length; i++) {
            coordinate = ((viewWith - (viewWith * 0.15f) * 2) * ((float) (i) / (vCount - 1)));
            canvas.drawText(String.valueOf(scoreS[i]), viewWith / 10f - dipToPx(20), coordinate + 10, textPaint);
        }
//        textPaint.setColor(0xff7c7c7c);
        float newWith = viewWith - (viewWith * 0.09f) * 2;//分隔线距离最左边和最右边的距离是0.15倍的viewWith  0.05f 控制分割线间距
        float coordinateX;//分隔线X坐标
        for (int i = 0; i < monthText.length; i++) {//这里是绘制月份，从数组中取出来，一个个的写
            coordinateX = (newWith * ((float) (i) / (monthCount - 1)) + (viewWith * 0.15f)) - dipToPx(20);
            if (i == selectMonth - 1)//被选中的月份要单独画出来多几个圈圈
            {
                textPaint.setStyle(Paint.Style.STROKE);
                textPaint.setColor(brokenLineColor);
                RectF r2 = new RectF();
                r2.left = coordinateX - textSize - dipToPx(4);
                r2.top = viewHeight * 0.7f + dipToPx(4) + textSize / 2;
                r2.right = coordinateX + textSize + dipToPx(4);
                r2.bottom = viewHeight * 0.7f + dipToPx(4) + textSize + dipToPx(8);
                canvas.drawRoundRect(r2, 10, 10, textPaint);
            }
            //绘制月份
            canvas.drawText(monthText[i], coordinateX,
                    viewHeight * 0.7f + dipToPx(4) + textSize + dipToPx(5), textPaint);//不是就正常的画出

            textPaint.setColor(textNormalColor);
        }
    }


    /**
     * @param canvas 画布
     * @param startX 起始点X坐标
     * @param startY 起始点Y坐标
     * @param stopX  终点X坐标
     * @param stopY  终点Y坐标
     */
    private void drawDottedLine(Canvas canvas, float startX, float startY, float stopX, float stopY) {

        dottedPaint.setPathEffect(new DashPathEffect(new float[]{20, 10}, 4));//DashPathEffect理解，
        dottedPaint.setStrokeWidth(1);
        // 实例化路径
        Path mPath = new Path();
        mPath.reset();
        // 定义路径的起点
        mPath.moveTo(startX, startY);
        mPath.lineTo(stopX, stopY);
        canvas.drawPath(mPath, dottedPaint);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        viewWith = w;
        viewHeight = h;
        initXY();
        initData();
    }


    private void initData() {
        pointList = new ArrayList<>();
        float maxScoreYCoordinate = ((viewHeight - (viewHeight * 0.15f) * 3) * ((float) (scoreData.length) / vCount));
        float minScoreYCoordinate = xStartX * 0.15f;
        int coordinateX;
        for (int i = 0; i < scoreData.length; i++) {
            Point point = new Point();
            coordinateX = (int) ((xNewWith * ((float) (i) / (monthCount)) + (viewWith * 0.15f)) - 50);
            point.x = coordinateX;
            switch (scoreData[i]) {
                case -60:

                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate - 200);////确定point的Y坐标
                    break;
                case -40:

                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate - 178);////确定point的Y坐标Y坐标
                    break;
                case -20:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate - 150);////确定point的Y坐标
                    break;
                case 0:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate - 100);////确定point的Y坐标
                    break;
                case 20:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate - 80);////确定point的Y坐标
                    break;
                case 40:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate - 100);////确定point的Y坐标
                    break;
                case 60:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate);////确定point的Y坐标
                    break;
                case 80:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate + 30);////确定point的Y坐标
                    break;
                case 100:
                    point.y = (int) (((float) (maxScore - scoreData[i]) / (maxScore - minScore)) *
                            (maxScoreYCoordinate - minScoreYCoordinate) + minScoreYCoordinate + 50);////确定point的Y坐标
                    break;
            }
            pointList.add(point);
        }
        Log.e("lgph", pointList.toString());
    }

    private void initPaint() {
        brokenPath = new Path();

        brokenPaint = new Paint();
        brokenPaint.setAntiAlias(true);
        brokenPaint.setStyle(Paint.Style.STROKE);
        brokenPaint.setStrokeWidth(brokenLineWith);//dpToPX 转换
        brokenPaint.setStrokeCap(Paint.Cap.ROUND);

        straightPaint = new Paint();
        straightPaint.setAntiAlias(true);
        straightPaint.setStyle(Paint.Style.STROKE);
        straightPaint.setStrokeWidth(brokenLineWith);
        straightPaint.setColor((straightLineColor));
        straightPaint.setStrokeCap(Paint.Cap.ROUND);


        dottedPaint = new Paint();
        dottedPaint.setAntiAlias(true);
        dottedPaint.setStyle(Paint.Style.STROKE);
        dottedPaint.setStrokeWidth(brokenLineWith);
        dottedPaint.setColor((straightLineColor));
        dottedPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setColor((textNormalColor));
        textPaint.setTextSize(textSize);
    }

    @SuppressLint("CustomViewStyleable")
    public void initAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ChartView);
        maxScore = a.getInt(R.styleable.ChartView_max_score, 100);
        minScore = a.getInt(R.styleable.ChartView_min_score, -20);
        brokenLineColor = a.getColor(R.styleable.ChartView_broken_line_color, brokenLineColor);
        textNormalColor = a.getColor(R.styleable.ChartView_textColor, textNormalColor);
        textSize = a.getDimensionPixelSize(R.styleable.ChartView_textSize, (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                15, getResources().getDisplayMetrics()));
        straightLineColor = a.getColor(R.styleable.ChartView_dottedlineColor, straightLineColor);
        a.recycle();
    }

    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    private Context context;

    public int dipToPx(float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
