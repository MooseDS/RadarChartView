package com.dongseok.libarary;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;
import static android.graphics.Paint.LINEAR_TEXT_FLAG;
import static android.graphics.Paint.Style.FILL_AND_STROKE;
import static android.graphics.Paint.Style.STROKE;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.cos;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.sin;

public class RadarChartView extends View {

    // TODO 필수 - 텍스트 OnClickListener
    // TODO 선택사항 드로우 애니메이션
    private Context context;
    private double angle;           // 한 칸 각도

    // center ( x, y )
    private int centerX;            // 원 중심 X
    private int centerY;            // 원 줌심 Y

    // Line Data
    private int lineCount = 8;      // 기본 선의 개수
    private float lineRadius;           // 선의 반지름
    private int lineSpace;

    // Text Data
    private Rect textRect;          // 텍스트 영역
    private int textSpace;          // 글씨 선 사이 여백
    private float textRadius;
    private float textSize;
    private SparseArray<String> textValue;

    // base five polygon
    //private int[] polygonAlpha;            // 투명도
    private int polygonCount;

    // data value polygon
    private int maxValue;
    private List<Integer> pointValue;

    // Draw Object
    private float[] vertices;       // 선의 좌표
    private float[] textVertices;   // 텍스트 좌표

    private Path path;
    private Paint linePaint;            // 선의 색상
    private Paint textPaint;        // 글씨 생삭
    private Paint polygonPaint;     // 5개의 단계 구분선
    private Paint pointPaint;       // 각 영역 별 값의 선

    private RadarTextItemClickListener listener;

    // public method 사용
    // protected method 상속. 변경에 열림
    public RadarChartView(Context context) {
        this(context, null);
    }

    // constructor
    public RadarChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RadarChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init(context, attrs);
    }


    private void init(Context context, @Nullable AttributeSet attrs) {

        // 1. 초기 이미지 설정 또는 값 설정 ( Attribute 의 영향이 없는 고정 변수 )
        textRect = new Rect();
        path = new Path();
        pointValue = new ArrayList<>();     // 항목별 통계값 리스트, 수치 퍼센트의 분자

        // 2. Attribute 입력된 값에 해당하는 정보
        // default init value
        if (attrs != null) {
            initAttributes(context, attrs);
        }
        polygonCount = 5;

        // 360 도 = 2 * PI
        vertices = new float[lineCount * 2];
        textVertices = new float[lineCount * 2];
        textValue = new SparseArray<>();                // 항목명 리스트
        for(int i=0 ; i<lineCount ; i++){
            String str = "항목 " + String.valueOf(i+1);
            textValue.put(i, str);
        }

        //Management Color & Line Style
        linePaint = new Paint(ANTI_ALIAS_FLAG);
        linePaint.setStyle(STROKE);
        linePaint.setColor(Color.BLACK);

        textPaint = new Paint(LINEAR_TEXT_FLAG);
        textPaint.setTextSize(textSize);
        textPaint.setStyle(STROKE);
        textPaint.setColor(Color.BLACK);

        polygonPaint = new Paint(ANTI_ALIAS_FLAG);
        polygonPaint.setStyle(STROKE);
        DashPathEffect dashPath = new DashPathEffect(new float[]{5,5}, 2);
        polygonPaint.setPathEffect(dashPath);
        polygonPaint.setColor(Color.BLACK);

        pointPaint = new Paint(ANTI_ALIAS_FLAG);
        pointPaint.setStyle(FILL_AND_STROKE);
        pointPaint.setColor(Color.parseColor("#ea6568"));
    }

    private void initAttributes(Context context, @Nullable AttributeSet attrs) {

        // add custom attributes
        TypedArray attr = context.getTheme().obtainStyledAttributes(attrs, R.styleable.radarChart, 0, 0);
        if(attr == null) return;

        lineCount = attr.getInteger(R.styleable.radarChart_chartCount, 8);
        textSize = attr.getDimensionPixelSize(R.styleable.radarChart_textSize, 36); // pixel size
        textSpace = attr.getInteger(R.styleable.radarChart_textSpace, 100);         // parent - text 공백
        lineSpace = attr.getInteger(R.styleable.radarChart_lineSpace, 100);          // text - line 공백
        maxValue = attr.getInteger(R.styleable.radarChart_maxValue, 100);           // 수치 퍼센트의 분모
    }

    // getter, setter
    // invalidate 화면을 다시 그리기, requestLayout() layout 을 갱신

    public int getLineCount() {
        return lineCount;
    }

    public void setLineCount(int lineCount) {
        this.lineCount = lineCount;
        invalidate();
    }

    public void setTextValueList(String[] strings) {
        textValue.clear();
        for(int i=0; i<strings.length; i++){
            String str = strResize(strings[i]);
            textValue.put(i, str);
        }
    }

    public void setTextValueList(List<String> strings){
        textValue.clear();
        for(int i=0; i<strings.size(); i++){
            String str = strResize(strings.get(i));
            textValue.put(i, str);
        }
    }

    public void setTextValuePosition(String value, int pos){
        textValue.put(pos, strResize(value));
    }

    private String strResize(String string) {
        //TODO max size 만큼 나누기
        String str = string.replaceAll("\n"," ");
        // substring i ~ i + textMasSize
        return string;
    }

    public void setPointValueList(int[] list){
        pointValue.clear();
        for(int i=0; i<list.length; i++){
            pointValue.add(i, list[i]);
        }
    }

    public void setPointValueList(List<Integer> list){
        pointValue.clear();
        for(int i=0; i<list.size(); i++){
            pointValue.add(i, list.get(i));
        }
    }

    public void setPointValuePosition(int pos, int point){
        pointValue.add(pos, point);
        requestLayout();
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public void setTextSpace(int textSpace) {
        this.textSpace = textSpace;
    }

    public void setLineSpace(int lineSpace) {
        this.lineSpace = lineSpace;
    }

    public void setListener(RadarTextItemClickListener listener) {
        this.listener = listener;
    }

    public RadarTextItemClickListener getListener() {
        return listener;
    }

    // onSizeChanged() - handle the padding values
    // resolveSizeAndState() is used to create the final width and height values
    // onMeasure() has no return value. Instead, the method communicates its results by calling setMeasuredDimension()

    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    // What to draw, handled by Canvas ex) line
    // How to draw, handled by Paint    ex) line color

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // creation and measuring code defined. can implement onDraw()
        int height = getHeight();
        int width = getWidth();

        // 영역 중심
        centerX = width/2;
        centerY = height/2;

        angle = 2 * Math.PI / lineCount;

        // 영역 반지름
        int yLength = height - (getPaddingTop() + getPaddingBottom());
        int xLength = width - (getPaddingLeft() + getPaddingRight());
        int areaRadius = min(xLength, yLength);

        // Text Area
        textRadius = areaRadius/2 - textSpace;

        // radius = 텍스트 영억을 뺀 나머지 + 여백 ( set padding )
        lineRadius = textRadius - lineSpace;

        //line count
        calculateVertices();

        drawRadarValuePoint(canvas);
        drawRadarPolygons(canvas);
        drawAxis(canvas);
        //drawRadarLine(canvas);
        drawRadarText(canvas);

    }


    private float getTextWidth(int i) {
        if(textValue.size() == 0) return 0;
        return textPaint.measureText(textValue.get(i));
    }

    private float getTextHeight(int i) {
        if(textValue.size() == 0) return 0;
        textRect.set(0,0,0,0);
        textPaint.getTextBounds(textValue.get(i), 0, textValue.get(i).length(), textRect);
        return textRect.height();
    }

    // 동적 Draw, 변수 : 항목 갯수(각도), 텍스트 여백, 선 여백
    private void calculateVertices() {
        int j=0;
        for(int i=0; i < lineCount * 2; i += 2){
            double alpha = angle * j++ - PI / 2;
            vertices[i] = (float) (lineRadius * cos(alpha) +  centerX);
            vertices[i+1] = (float) (lineRadius * sin(alpha)  + centerY);
            textVertices[i] = (float) (textRadius * cos(alpha) +  centerX);
            textVertices[i+1] = (float) (textRadius * sin(alpha)  + centerY);
        }
    }

    // 고정된 Draw
    private void drawAxis(Canvas canvas) {
        canvas.drawLine(centerX - lineRadius, centerY, centerX + lineRadius, centerY, linePaint);
        canvas.drawLine(centerX, centerY - lineRadius, centerX, centerY + lineRadius, linePaint);
    }

    // 동적 Draw, 변수 : 항목 갯수,
    private void drawRadarPolygons(Canvas canvas) {

        if(lineCount < 3){
            for(int i=polygonCount-2; i>=0; i--) {
                float point = lineRadius / polygonCount;
                //polygonPaint.setAlpha( polygonAlpha[i] );
                if(i == 0)
                    canvas.drawCircle(centerX, centerY, lineRadius - point * i, polygonPaint);
                else
                    canvas.drawCircle(centerX, centerY, lineRadius - point * i, polygonPaint);
            }
        }else {
            for(int i=polygonCount-2; i>=0; i--) {

                int k = polygonCount - i;
                float startX = (vertices[0] * k) / polygonCount + (centerX * i) / polygonCount;
                float startY = (vertices[1] * k) / polygonCount + (centerY * i) / polygonCount;

                path.reset();
                path.moveTo(startX, startY);
                path.setLastPoint(startX, startY);

                for (int j = 2; j < lineCount * 2; j += 2) {
                    path.lineTo( (vertices[j] * k)/polygonCount + (centerX * i)/polygonCount, (vertices[j + 1] * k)/polygonCount + (centerY * i)/polygonCount );
                }
                path.close();

                if(i == 0)
                    canvas.drawPath(path, linePaint);
                else
                    canvas.drawPath(path, polygonPaint);
            }

        }
    }

    private void drawRadarLine(Canvas canvas) {
        for (int i = 0; i < lineCount * 2 ; i += 2) {
            canvas.drawLine(centerX, centerY, vertices[i], vertices[i+1], linePaint);
        }
    }


    private void drawRadarText(Canvas canvas) {
        if(textValue.size() != 0) {
            for (int i = 0; i < lineCount * 2; i += 2) {
                if(!TextUtils.isEmpty(textValue.get(i/2))) {
                    float width = getTextWidth(i / 2);
                    float height = getTextHeight(i / 2);
                    canvas.drawText(textValue.get(i/2), textVertices[i] - width / 2, textVertices[i + 1] + height / 2, textPaint);
                }
            }
        }
    }

    private void drawRadarValuePoint(Canvas canvas) {

        if(lineCount == 0) return;
        path.reset();

        float percent = (float) pointValue.get(0) / maxValue;
        float pointX  = vertices[0] * percent + centerX - centerX * percent;
        float pointY  = vertices[1] * percent + centerY - centerY * percent;

        path.moveTo(pointX, pointY);
        path.setLastPoint(pointX, pointY);

        for (int i = 2; i < lineCount * 2; i += 2) {
            percent = (float) pointValue.get(i / 2) / maxValue;
            pointX = vertices[i] * percent + centerX - centerX * percent;
            pointY = vertices[i + 1] * percent + centerY - centerY * percent;
            path.lineTo(pointX, pointY);
        }

        if(lineCount == 1){
            path.lineTo(centerX, centerY);
        }
        path.close();
        canvas.drawPath(path, pointPaint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN && listener != null){
            float x = event.getX();
            float y = event.getY();

            for(int i=0; i<lineCount*2; i+=2){
                float width = getTextWidth(i/2);
                float height = getTextHeight(i/2);

                float x1 = textVertices[i]- width;
                float x2 = textVertices[i] + width;
                float y1 = textVertices[i+1] - height;
                float y2 = textVertices[i+1] + height;

                if( x1 <= x && x2 >= x && y1 <= y && y2 >= y){
                    listener.onItemClick(i/2);
                    return true;
                }
            }
        }
        return super.onTouchEvent(event);
    }

}
