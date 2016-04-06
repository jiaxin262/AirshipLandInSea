package com.jia.jason.airshiplandinsea.view;

import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import com.jia.jason.airshiplandinsea.R;
import com.jia.jason.airshiplandinsea.activity.JAirShipActivity;

/**
 * Created by xin.jia
 * since 2016/2/29
 */
public class JAirShipView extends SurfaceView implements SurfaceHolder.Callback {

    private Context mContext;
    private TextView mStatusText;
    private JAirShipThread thread;
    private SensorManager sm;
    private Sensor sensor;
    private SensorEventListener mySensorListener;
    private float x = 0, y = 0, z = 0;  //重力感应数据

    public JAirShipView(Context context, AttributeSet attrs) {
        super(context, attrs);
        SurfaceHolder holder = getHolder();
        holder.addCallback(this);

        sm = (SensorManager) context.getSystemService(Service.SENSOR_SERVICE);
        sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mySensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                x = event.values[0]; //手机横向翻滚,x>0 说明当前手机左翻 x<0右翻
                y = event.values[1]; //手机纵向翻滚,y>0 说明当前手机下翻 y<0上翻
                z = event.values[2]; //屏幕的朝向,z>0 手机屏幕朝上 z<0 手机屏幕朝下
                if (thread.mMode == JAirShipThread.STATE_RUNNING) {
                    thread.landerXPos -= x;
                }
            }

            //传感器的精度发生改变时响应此函数
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // TODO Auto-generated method stub
            }
        };
        sm.registerListener(mySensorListener, sensor, SensorManager.SENSOR_DELAY_GAME);

        thread = new JAirShipThread(holder, context, new Handler() {
            @Override
            public void handleMessage(Message m) {
                String text = m.getData().getString("text");
                mStatusText.setVisibility(m.getData().getInt("viz"));
                mStatusText.setText(text);
            }
        });

        setFocusable(true); // make sure we get key events
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (thread.getState() == Thread.State.TERMINATED) {
            thread = new JAirShipThread(holder, mContext, new Handler() {
                @Override
                public void handleMessage(Message m) {
                    String text = m.getData().getString("text");
                    mStatusText.setVisibility(m.getData().getInt("viz"));
                    mStatusText.setText(text);
                }
            });
        }
        thread.setRunning(true);
        if (thread.getState() == Thread.State.NEW) {
            thread.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        thread.setSurfaceSize(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);
        if (!hasWindowFocus) {
            thread.pause();
        } else {
            if (thread.mMode == JAirShipThread.STATE_BG) {
                thread.restoreState();
                thread.mMode = thread.getmMode();
                if (thread.mMode == JAirShipThread.STATE_RUNNING) {
                    thread.mMode = JAirShipThread.STATE_BG;
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                thread.performOnclick();
                break;
            case MotionEvent.ACTION_UP:
                thread.doScreenUp();
                break;
        }
        return true;
    }

    public void setTextView(TextView textView) {
        mStatusText = textView;
    }

    public JAirShipThread getThread() {
        return thread;
    }

    public class JAirShipThread extends Thread {

        /** 游戏状态 */
        public static final int STATE_BG = 0;
        public static final int STATE_LOSE = 1;
        public static final int STATE_READY = 3;
        public static final int STATE_RUNNING = 4;
        public static final int STATE_WIN = 5;

        /** UI参数 */
        public static final int LANDER_INIT_Y = 10; //登录器初始时上边坐标
        public static final float ACC_POWER = 1.02f; //重力加速倍数
        public static final float ACC_INIT = 0.4f;  //初始加速度
        public static final float TARGET_SPEED = 13;  //最大着陆速度
        public static final float TARGET_SPEED_WIDTH = 130;  //目标速度显示的宽度
        public static final float FULL_FUEL = 300;  //燃料量
        public static final int FUEL_X_ADD = 65;   //燃油，速度X位置偏移
        public static final int FUEL_Y_ADD = 38;   //燃油，速度Y位置偏移


        private int mCanvasHeight = 1;
        private int mCanvasWidth = 1;
        private float landerXPos;  //登录器左边坐标
        private float landerYPos;  //登录器上边坐标
        private float landerXAcc;  //X方向上增加的距离
        private float landerYAcc;  //Y方向上增加的距离
        private int mLanderWidth, mLanderHeight;
        private int targetPlatHeight;
        private int leftCloudWidth, leftCloudHeight, rightCloudWidth, rightCloudHeight;
        private int seaHeight;  //海平面高度
        private int textX, textY; //燃料，速度文字的x,y坐标
        private int shipWidth, shipHeight, shipX, shipY;
        private Drawable targetPlatImage;
        private Drawable mLanderImage;
        private Drawable mFiringImage;
        private Drawable mCrashedImage;
        private Drawable cloudLeftImage;
        private Drawable cloudRightImage;
        private Drawable shipImage;
        private Paint bottomSeaPaint;   //下方海的画笔
        private Paint targetLinePaint;  //目标降落区域画笔
        private Paint speedPaint;   //最大速度画笔
        private Paint currentSpeedPaint;    //实时速度画笔
        private Paint fuelPaint;     //燃料画笔
        private Paint textPaint;    //文字画笔
        private int targetXPos;  //目标降落地X坐标
        private int targetWidth; //目标降落地宽度
        private int targetY;
        private float currentSpeed; //当前速度
        private float currentFule = FULL_FUEL;  //当前燃料
        private boolean isFuelAllUsed;  //燃料是否用完

        /** 逻辑参数 */
        private int mMode;
        private boolean mEngineFiring;
        private boolean mRun = false;
        private boolean isPausedInRunning = false;

        private Handler mHandler;
        private SurfaceHolder mSurfaceHolder;

        private static final String KEY_LANDER_HEIGHT = "mLanderHeight";
        private static final String KEY_LANDER_WIDTH = "mLanderWidth";

        public JAirShipThread(SurfaceHolder surfaceHolder, Context context, Handler handler) {

            mSurfaceHolder = surfaceHolder;
            mHandler = handler;
            mContext = context;

            Resources res = context.getResources();
            targetPlatImage = res.getDrawable(R.drawable.target_plat);
            mLanderImage = res.getDrawable(R.drawable.air_ship_normal);
            mFiringImage = res.getDrawable(R.drawable.air_ship_fire);
            mCrashedImage = res.getDrawable(R.drawable.air_ship_crash);
            cloudLeftImage = res.getDrawable(R.drawable.cloud_left);
            cloudRightImage = res.getDrawable(R.drawable.cloud_right);
            shipImage = res.getDrawable(R.drawable.ship);

            targetPlatHeight = targetPlatImage.getIntrinsicHeight();
            mLanderWidth = mLanderImage.getIntrinsicWidth();
            mLanderHeight = mLanderImage.getIntrinsicHeight();
            leftCloudWidth = cloudLeftImage.getIntrinsicWidth();
            leftCloudHeight = cloudLeftImage.getIntrinsicHeight();
            rightCloudWidth = cloudRightImage.getIntrinsicWidth();
            rightCloudHeight = cloudRightImage.getIntrinsicHeight();
            shipWidth = shipImage.getIntrinsicWidth();
            shipHeight = shipImage.getIntrinsicHeight();
            mEngineFiring = true;
            landerXPos = mLanderWidth;
            landerYPos = mLanderHeight * 5;
            shipX = 50;

            bottomSeaPaint = new Paint();
            bottomSeaPaint.setAntiAlias(true);
            bottomSeaPaint.setColor(Color.parseColor("#75acd9"));

            targetLinePaint = new Paint();
            targetLinePaint.setAntiAlias(true);
            targetLinePaint.setColor(Color.GREEN);
            targetLinePaint.setStrokeWidth(3);

            speedPaint = new Paint();
            speedPaint.setAntiAlias(true);
            speedPaint.setColor(Color.parseColor("#559ACD32"));
            speedPaint.setStrokeWidth(20);

            currentSpeedPaint = new Paint();
            currentSpeedPaint.setAntiAlias(true);
            currentSpeedPaint.setColor(Color.parseColor("#73bfba"));
            currentSpeedPaint.setStrokeWidth(20);

            fuelPaint = new Paint();
            fuelPaint.setAntiAlias(true);
            fuelPaint.setColor(Color.parseColor("#73bfba"));
            fuelPaint.setStrokeWidth(30);

            textPaint = new Paint();
            textPaint.setAntiAlias(true);
            textPaint.setColor(Color.parseColor("#73bfba"));
            textPaint.setTextSize(27);
        }

        public void performOnclick() {
            Log.e("mMode", mMode+"");
            if (mMode != STATE_RUNNING) {
                doStart();
            } else {
                thread.doScreenDown();
            }
        }

        public void doStart() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_LOSE || mMode == STATE_READY || mMode == STATE_WIN) {
                    landerXPos = mCanvasWidth / 2 - mLanderWidth / 2;
                    landerYPos = LANDER_INIT_Y;
                    landerXAcc = 0;
                    landerYAcc = ACC_INIT;
                    mEngineFiring = false;
                    targetWidth = mLanderWidth * 3 / 2;
                    targetXPos = (int) (Math.random() * (mCanvasWidth - targetWidth));
                    seaHeight = mCanvasHeight / 11;
                    targetY = mCanvasHeight - targetPlatHeight - 5;
                    textX = mCanvasWidth / 17;
                    textY = mCanvasHeight / 17;
                    shipY = mCanvasHeight - seaHeight - shipHeight / 3 * 2;

                    currentSpeed = 0;
                    currentFule = FULL_FUEL;
                    isFuelAllUsed = false;
                }
                setState(STATE_RUNNING);
            }
        }

        @Override
        public void run() {
            while (mRun) {
                Canvas c = null;
                try {
                    c = mSurfaceHolder.lockCanvas(null);
                    synchronized (mSurfaceHolder) {
                        if (mMode == STATE_RUNNING) {
                            updatePhysics();
                        }
                        doDraw(c);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (c != null) {
                        mSurfaceHolder.unlockCanvasAndPost(c);
                    }
                }
            }
        }

        private void updatePhysics() {
            shipX += 1;
            if (shipX >= mCanvasWidth) {
                shipX = -shipWidth;
            }
            landerXPos += landerXAcc;
            /**Y方向偏移：加速下落+喷射器*/
            if (currentFule < 0) {
                isFuelAllUsed = true;
                currentFule = 0;
                setFiring(false);
                landerYAcc = ACC_INIT;
            }
            if (landerYAcc > 0) {
                landerYAcc *= ACC_POWER;
            }
            if (mEngineFiring) {
                landerYAcc -= 0.2;
                currentFule -= 0.2*7;
            }
            landerYPos += landerYAcc;
            double yAccTemp = landerYAcc;
            if (yAccTemp < 0) {
                yAccTemp = 0;
            }
            currentSpeed = (float) (yAccTemp+1) * 10;
            if (landerYAcc > TARGET_SPEED) {
                currentSpeedPaint.setColor(Color.parseColor("#da0f7a"));
            } else {
                currentSpeedPaint.setColor(Color.parseColor("#73bfba"));
            }
            /**边缘检测*/
            if (landerXPos <= 0) {
                landerXPos = 0;
            }
            if (landerXPos + mLanderWidth >= mCanvasWidth) {
                landerXPos = mCanvasWidth - mLanderWidth;
            }
            if (landerYPos <= 0) {
                landerYPos = 0;
            }
            /**检测是否降落到地面*/
            if (landerYPos + mLanderHeight >= targetY+mLanderHeight/4) {
                int result = STATE_LOSE;
                CharSequence message = "";
                Resources res = mContext.getResources();
                //检测是否降落到目标区域内
                if (landerXPos >= targetXPos && landerXPos + mLanderWidth <= targetXPos + targetWidth) {
                    //检测速度是否在限定范围内
                    if (landerYAcc <= TARGET_SPEED) {
                        result = STATE_WIN;
                    } else {
                        message = res.getText(R.string.message_too_fast);
                    }
                } else {
                    message = res.getText(R.string.message_off_pad);
                }
                setState(result, message);
            }
        }

        private void doDraw(Canvas canvas) {
            canvas.drawColor(Color.parseColor("#bae0ff"));
            cloudLeftImage.setBounds(mCanvasWidth / 6, 200, mCanvasWidth / 6 + leftCloudWidth, 200 + leftCloudHeight);
            cloudLeftImage.draw(canvas);
            cloudLeftImage.setBounds(-leftCloudWidth / 3, 700, -leftCloudWidth / 3 + leftCloudWidth, 700 + leftCloudHeight);
            cloudLeftImage.draw(canvas);
            cloudRightImage.setBounds(mCanvasWidth - mCanvasWidth / 6, 400, mCanvasWidth - mCanvasWidth / 6 + rightCloudWidth, 400 + rightCloudHeight);
            cloudRightImage.draw(canvas);
            if (mMode != STATE_READY) {
                canvas.drawRect(new RectF(0, mCanvasHeight - seaHeight, mCanvasWidth, mCanvasHeight), bottomSeaPaint);
                targetPlatImage.setBounds(targetXPos, targetY,
                        targetXPos + targetWidth, targetY + targetPlatHeight);
                targetPlatImage.draw(canvas);
                shipImage.setBounds(shipX, shipY, shipX + shipWidth,
                        shipY + shipHeight);
                shipImage.draw(canvas);
                /**画文字*/
                canvas.drawText("燃料:", textX, textY+5, textPaint);
                canvas.drawText("速度:", textX, textY+45, textPaint);
                /**画最大速度表*/
                canvas.drawLine(textX+FUEL_X_ADD, textY+FUEL_Y_ADD, TARGET_SPEED_WIDTH + textX+FUEL_X_ADD, textY+FUEL_Y_ADD, speedPaint);
                /**画实时速度表*/
                canvas.drawLine(textX+FUEL_X_ADD, textY+FUEL_Y_ADD, currentSpeed + textX+FUEL_X_ADD, textY+FUEL_Y_ADD, currentSpeedPaint);
                /**画燃料表*/
                canvas.drawLine(textX+FUEL_X_ADD, textY, currentFule + textX+FUEL_X_ADD, textY, fuelPaint);
            }
            /**画登月舱*/
            int yTop = (int) landerYPos;
            int xLeft = (int) landerXPos;
            if (mMode == STATE_LOSE) {
                mCrashedImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop + mLanderHeight);
                mCrashedImage.draw(canvas);
            } else if (mEngineFiring && !isFuelAllUsed) {
                mFiringImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop + mLanderHeight);
                mFiringImage.draw(canvas);
            } else {
                mLanderImage.setBounds(xLeft, yTop, xLeft + mLanderWidth, yTop + mLanderHeight);
                mLanderImage.draw(canvas);
            }
        }

        public void doScreenDown() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING && !isFuelAllUsed) {
                    setFiring(true);
                }
            }
        }

        public void doScreenUp() {
            synchronized (mSurfaceHolder) {
                if (mMode == STATE_RUNNING) {
                    setFiring(false);
                    if (landerYAcc <= 0) {
                        landerYAcc = ACC_INIT;
                    }
                }
            }
        }

        public void setFiring(boolean firing) {
            synchronized (mSurfaceHolder) {
                mEngineFiring = firing;
            }
        }

        public void setSurfaceSize(int width, int height) {
            // synchronized to make sure these all change atomically
            synchronized (mSurfaceHolder) {
                mCanvasWidth = width;
                mCanvasHeight = height;
            }
        }

        public void setState(int mode) {
            synchronized (mSurfaceHolder) {
                setState(mode, null);
            }
        }

        public void setState(int mode, CharSequence message) {
            synchronized (mSurfaceHolder) {
                mMode = mode;

                if (mMode == STATE_RUNNING) {
                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", "");
                    b.putInt("viz", View.INVISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                } else {
                    mEngineFiring = false;
                    Resources res = mContext.getResources();
                    CharSequence str = "";
                    if (mMode == STATE_READY)
                        str = res.getText(R.string.mode_ready);
                    else if (mMode == STATE_BG)
                        str = res.getText(R.string.mode_pause);
                    else if (mMode == STATE_LOSE)
                        str = res.getText(R.string.mode_lose);
                    else if (mMode == STATE_WIN)
                        str = res.getString(R.string.mode_win_suffix);

                    if (message != null) {
                        str = message + "\n" + str;
                    }

                    Message msg = mHandler.obtainMessage();
                    Bundle b = new Bundle();
                    b.putString("text", str.toString());
                    b.putInt("viz", View.VISIBLE);
                    msg.setData(b);
                    mHandler.sendMessage(msg);
                }
            }
        }

        public void pause() {
            synchronized (mSurfaceHolder) {
                saveState();
                setState(STATE_BG);
            }
        }

        public void saveState() {
            SharedPreferences statePreference = mContext.getSharedPreferences(JAirShipActivity.TAG, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = statePreference.edit();
            editor.putBoolean("isPausedInRunning", isPausedInRunning);
            editor.putFloat("landerXPos", landerXPos);
            editor.putFloat("landerYPos", landerYPos);
            editor.putFloat("landerXAcc", landerXAcc);
            editor.putFloat("landerYAcc", landerYAcc);
            editor.putInt("mLanderWidth", mLanderWidth);
            editor.putInt("mLanderHeight", mLanderHeight);
            editor.putInt("targetPlatHeight", targetPlatHeight);
            editor.putInt("leftCloudHeight", leftCloudHeight);
            editor.putInt("leftCloudWidth", leftCloudWidth);
            editor.putInt("rightCloudWidth", rightCloudWidth);
            editor.putInt("rightCloudHeight", rightCloudHeight);
            editor.putInt("seaHeight", seaHeight);
            editor.putInt("textX", textX);
            editor.putInt("textY", textY);
            editor.putInt("shipWidth", shipWidth);
            editor.putInt("shipHeight", shipHeight);
            editor.putInt("shipX", shipX);
            editor.putInt("shipY", shipY);
            editor.putInt("targetXPos", targetXPos);
            editor.putInt("targetWidth", targetWidth);
            editor.putInt("targetY", targetY);
            editor.putFloat("currentSpeed", currentSpeed);
            editor.putFloat("currentFule", currentFule);
            editor.putBoolean("isFuelAllUsed", isFuelAllUsed);
            editor.putInt("mMode", mMode);
            editor.apply();
        }

        public void restoreState() {
            SharedPreferences initStatus = mContext.getSharedPreferences(JAirShipActivity.TAG, Context.MODE_PRIVATE);
            landerXPos = initStatus.getFloat("landerXPos", 0);
            landerYPos = initStatus.getFloat("landerYPos", 0);
            landerXAcc = initStatus.getFloat("landerXAcc", 0);
            landerYAcc = initStatus.getFloat("landerYAcc", 0);
            mLanderWidth = initStatus.getInt("mLanderWidth", 0);
            mLanderHeight = initStatus.getInt("mLanderHeight", 0);
            targetPlatHeight = initStatus.getInt("targetPlatHeight", 0);
            leftCloudHeight = initStatus.getInt("leftCloudHeight", 0);
            leftCloudWidth = initStatus.getInt("leftCloudWidth", 0);
            rightCloudWidth = initStatus.getInt("rightCloudWidth", 0);
            rightCloudHeight = initStatus.getInt("rightCloudHeight", 0);
            seaHeight = initStatus.getInt("seaHeight", 0);
            textX = initStatus.getInt("textX", 0);
            textY = initStatus.getInt("textY", 0);
            shipWidth = initStatus.getInt("shipWidth", 0);
            shipHeight = initStatus.getInt("shipHeight", 0);
            shipX = initStatus.getInt("shipX", 0);
            shipY = initStatus.getInt("shipY", 0);
            targetXPos = initStatus.getInt("targetXPos", 0);
            targetWidth = initStatus.getInt("targetWidth", 0);
            targetY = initStatus.getInt("targetY", 0);
            currentSpeed = initStatus.getFloat("currentSpeed", 0);
            currentFule = initStatus.getFloat("currentFule", 0);
            isFuelAllUsed = initStatus.getBoolean("isFuelAllUsed", false);
            mEngineFiring = initStatus.getBoolean("mEngineFiring", false);
        }

        public int getmMode() {
            SharedPreferences initStatus = mContext.getSharedPreferences(JAirShipActivity.TAG, Context.MODE_PRIVATE);
            return initStatus.getInt("mMode", STATE_BG);
        }

        public void setRunning(boolean b) {
            mRun = b;
        }

    }

}
