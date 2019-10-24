## NeLoadingAnimation SurfaceView实现的绳子弹小球Loading动画
### 一、动画拆分分析
1. 小球在水平线处向下移动至最低点（小球的移动，曲线？） ----> DOWN
2. 小球达到最低点向上回到水平位置（小球的移动，曲线？） ----> UP 
3. 小球脱离绳子向上然后回到水平位置-上下自由落体（小球的位置）----> FREE

### 二、难点
#### 1. DOWN状态小球自水平线下降
```android
    /**
     * 小球在绳子水平线下方，从水平线处到最低点的运动过程
     */
    downControl = ValueAnimator.ofFloat(0, maxDownDistance); //0~50dp(0~150px)
    downControl.setDuration(500);
    downControl.setInterpolator(new DecelerateInterpolator()); //减速插值器
    downControl.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            downDistance = (float) animation.getAnimatedValue();
            //Log.e("sty", "downDistance: " + downDistance);
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
```  

#### 2. UP状态的小球自最低点运动到水平线位置
```android
    /**
     * 小球在绳子水平线下方，从最低点到水平线处的运动过程
     */
    upControl = ValueAnimator.ofFloat(0, maxDownDistance); //0~maxDownDistance*1.x
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
            //Log.e("sty", "upDistance: " + upDistance);
            //震荡的绳子位置调整，绳子不能超过小球底部 
            if(upDistance - maxDownDistance >= freeDownDistance) { //震荡插值器生成的距离可能大于maxDownDistance
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
```  
![image](https://github.com/tianyalu/NeLoadingAnimation/blob/master/show/shock_line.png)  

#### 3. FREE状态的自由落体运动
```android
    /**
     * 小球在绳子水平线上方做自由落体运动，先上升后下降↑↓
     */
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

            //△h↑ = v0t - 1/2gt^2 （上升阶段，加速度为负值）
            //v0 = gt = 10*Math.sqrt(maxFreeDownDistance/5)
            freeDownDistance = (float) (10 * Math.sqrt(maxFreeDownDistance/5) * t - 5*t*t);
            //该方程为抛物线，t=0,h=0  t=T,h=50 t=2T,h=0
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
```
![image](https://github.com/tianyalu/NeLoadingAnimation/blob/master/show/analyse.png)  

#### 4. 贝塞尔曲线控制点相对位置的计算与理解 
```android
    path.moveTo(getWidth()/2f - lineWidth/2f, getHeight()/2f); //起点
     //小球在绳子水平线处下降
    /** P0------P1-------P2
     * B(t) = (1-t)^2 * P0 + 2*t*(1-t)P1 + t^2 * P2, t[0,1]
     * B(t) = C0*P0 + C1*p1 + C2*P2
     * B(0.5) = 0.25*P0 + 0.5*P1 + 0.25*P2
     *
     * t = 0.5;
     * cp[1].x = (cp[0].x + cp[2].x)/2; 即连线中点
     * float c0 = (1-t) * (1-t);  0.25
     * float c1 = 2*t*(1-t); 0.5
     * float c2 = t * t;  0.25
     * growX = c0 * cp[0].x + c1 * cp[1].x + c2 * cp[2].x  //相对于起点的x偏移量
     * cp[0].y == cp[2].y
     * growY = c0 * cp[0].y + c1 * cp[1].y + c2 * cp[2].y  //相对于起点的y偏移量
     * cp[1].△y = (growY - 0.5cp[0].△y) * 2 // 此时cp[0].△y = 0
     *         = growY * 2
     *         = downDistance * 2
     */
    //参数表示相对位置（相对于起点）
    path.rQuadTo(lineWidth/2f, 2 * downDistance, lineWidth, 0); //控制点相对于起点偏移，终点相对于起点偏移
```  
![image](https://github.com/tianyalu/NeLoadingAnimation/blob/master/show/analyse_control_point.png)  

### 三、演示示例
![image](https://github.com/tianyalu/NeLoadingAnimation/blob/master/show/show.gif)  