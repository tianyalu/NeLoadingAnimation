package com.sty.ne.loading.animation;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;

public class LoadingView extends SurfaceView implements SurfaceHolder.Callback, Runnable{
    private enum LoadingState{
        DOWN, UP, FREE
    }

    private LoadingState loadingState = LoadingState.DOWN;
    private int ballColor; //小球颜色
    private int ballRadius; //小球半径
    private int lineColor; //小球颜色
    private int lineWidth; //连线长度
    private int strokeWidth; //绘制线宽
    private float downDistance = 0; //水平位置下降的距离
    private float maxDownDistance; //水平位置下降的距离(最低点)
    private float upDistance = 0; //从底部上弹到水平线的距离
    private float freeDownDistance = 0; //自由落体的距离
    private float maxFreeDownDistance = 0; //自由落体的距离（最高点-水平线之间的距离）

    private ValueAnimator downControl;
    private ValueAnimator upControl;
    private ValueAnimator freeDownControl;
    private AnimatorSet animatorSet;
    private boolean isAnimationShowing;
    private SurfaceHolder holder;
    private Canvas canvas;
    private Paint paint;
    private Path path;
    private static boolean isRunning = false; //标志新线程是否在运行

    public LoadingView(Context context) {
        this(context, null);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //初始化
        initAttr(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(strokeWidth);
        path = new Path();
        //获取holder
        holder = getHolder();
        holder.addCallback(this);
        //初始化距离控制
        initControl();
    }

    private void initControl() {
        downControl = ValueAnimator.ofFloat(0, maxDownDistance);
        downControl.setDuration(500);
        downControl.setInterpolator(new DecelerateInterpolator());
        downControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                downDistance = (float) animation.getAnimatedValue();
            }
        });
        downControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.DOWN;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        upControl = ValueAnimator.ofFloat(0, maxDownDistance);
        upControl.setDuration(500);
        upControl.setInterpolator(new ShockInterpolator());
        upControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                upDistance = (float) animation.getAnimatedValue();
                if(upDistance >= maxDownDistance && freeDownControl != null
                        && !freeDownControl.isRunning() && !freeDownControl.isStarted()) {
                    freeDownControl.start();
                }
                //震荡的绳子位置调整，绳子不能超过小球底部
                if(upDistance - maxDownDistance >= freeDownDistance) {
                    upDistance = maxDownDistance + freeDownDistance;
                }
            }
        });
        upControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.UP;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
            }
        });

        //参考show/analyse.png
        //△h = v0t + 1/2gt^2 = 1/2gt^2 = 5t^2
        //--> t = Math.sqrt(△h/5)
        //--> T = 2*Math.sqrt(maxFreeDownDistance/5)
        freeDownControl = ValueAnimator.ofFloat(0, (float) (2 * Math.sqrt(maxFreeDownDistance / 5))); //0~2T
        freeDownControl.setDuration(700);
        freeDownControl.setInterpolator(new AccelerateInterpolator());
        freeDownControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                float t = (float) animation.getAnimatedValue();

                //S总 - S现 = 2*（1/2）gT^2 - 1/2gt^2
                //v0 = gt = 10*Math.sqrt(maxFreeDownDistance/5)
                freeDownDistance = (float) (10 * Math.sqrt(maxFreeDownDistance/5) * t - 5*t*t);
            }
        });
        freeDownControl.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                loadingState = LoadingState.FREE;
                isAnimationShowing = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimationShowing = false;
                
                //重新开启动画
                startAnimator();
            }
        });
        animatorSet = new AnimatorSet();
        animatorSet.play(downControl).before(upControl);
    }

    private void startAnimator() {
        if(isAnimationShowing) {
            return;
        }
        if(animatorSet.isRunning()) {
            animatorSet.end();
            animatorSet.cancel();
        }
        if(freeDownControl.isRunning()) {
            freeDownControl.end();
            freeDownControl.cancel();
        }
        loadingState = LoadingState.DOWN;

        //动画开启
        animatorSet.start();
    }


    private void initAttr(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.LoadingView);
        //拿到对应的属性值
        ballColor = typedArray.getColor(R.styleable.LoadingView_ball_color, Color.BLUE);
        lineColor = typedArray.getColor(R.styleable.LoadingView_line_color, Color.BLUE);
        //同getDimensionPixelSize 只是其值会被强转成int
        lineWidth = typedArray.getDimensionPixelOffset(R.styleable.LoadingView_line_width, 200);
        strokeWidth = typedArray.getDimensionPixelOffset(R.styleable.LoadingView_stroke_width, 4);
        maxDownDistance = typedArray.getDimensionPixelSize(R.styleable.LoadingView_max_down, 50);
        maxFreeDownDistance = typedArray.getDimensionPixelSize(R.styleable.LoadingView_max_up, 50);
        ballRadius = typedArray.getDimensionPixelSize(R.styleable.LoadingView_ball_radius, 10);
        typedArray.recycle();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //创建时触发
        //开启绘制线程
        isRunning = true;
        new Thread(this).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //格式大小发生变化时触发
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //销毁时触发
        isRunning = false;
        if(animatorSet.isRunning()) {
            animatorSet.end();
            animatorSet.cancel();
        }
        if(freeDownControl.isRunning()) {
            freeDownControl.end();
            freeDownControl.cancel();
        }
    }

    @Override
    public void run() {
        //绘制动画（死循环）
        while (isRunning) {
            drawView();
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void drawView() {
        try {
            if (holder != null) {
                canvas = holder.lockCanvas();
                //todo
                path.reset();
                path.moveTo(getWidth()/2f - lineWidth/2f, getHeight()/2f);
                if(loadingState == LoadingState.DOWN) {
                    //小球在绳子上下降
                    /**
                     * t = 0.5;
                     * cp[1].x = (cp[0].x + cp[2].x)/2; 即连线中点
                     * float c0 = (1-t) * (1-t);  0.25
                     * float c1 = 2*t*(1-t); 0.5
                     * float c2 = t * t;
                     * growX = c0 * cp[0]
                     * growY =
                     */
                    path.rQuadTo(lineWidth/2f, 2 * downDistance, lineWidth, 0);
                    paint.setColor(lineColor);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                    //绘制小球
                    paint.setColor(ballColor);
                    paint.setStyle(Paint.Style.FILL);
                    canvas.drawCircle(getWidth()/2f, getHeight()/2 + downDistance - ballRadius
                            - strokeWidth/2f, ballRadius, paint);
                }else {
                    //小球在绳子上上升（自由落体）
                    path.rQuadTo(lineWidth/2f, 2 * (maxDownDistance - upDistance), lineWidth, 0);
                    paint.setColor(lineColor);
                    paint.setStyle(Paint.Style.STROKE);
                    canvas.drawPath(path, paint);
                    //绘制小球
                    paint.setColor(ballColor);
                    paint.setStyle(Paint.Style.FILL);
                    if(loadingState == LoadingState.FREE) {
                        //自由落体
//                        canvas
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if(canvas != null) {
                assert holder != null; //断言，一旦失败，后面代码就不执行
                holder.unlockCanvasAndPost(canvas);
            }
        }
    }


    class ShockInterpolator implements Interpolator {

        @Override
        public float getInterpolation(float input) {
            float value = (float) (1 - Math.exp(-3 * input) * Math.cos(10 * input));
            return value;
        }
    }

}
