package com.jia.jason.airshiplandinsea.activity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jia.jason.airshiplandinsea.dialog.PauseDialog;
import com.jia.jason.airshiplandinsea.view.JAirShipView;
import com.jia.jason.airshiplandinsea.R;
import com.qhad.ads.sdk.adcore.Qhad;
import com.qhad.ads.sdk.interfaces.IQhBannerAd;

import java.util.Timer;
import java.util.TimerTask;

public class JAirShipActivity extends BaseActivity {

    public Context context = this;
    public static final String TAG = "JAirShipView";
    private JAirShipView.JAirShipThread jAirShipThread;
    private JAirShipView jAirShipView;
    private PauseDialog pauseDialog;
    public LinearLayout adContainer;
    final String adSpaceid = "FuFbu9q1v8";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.j_lunar_layout);

        adContainer = (LinearLayout) findViewById(R.id.ad_container);
        jAirShipView = (JAirShipView) findViewById(R.id.j_lunar);
        jAirShipThread = jAirShipView.getThread();
        jAirShipView.setTextView((TextView) findViewById(R.id.lunar_text));
        jAirShipThread.setState(JAirShipView.JAirShipThread.STATE_READY);

        IQhBannerAd bannerAd = Qhad.showBanner(adContainer, JAirShipActivity.this, adSpaceid, true);

        pauseDialog = new PauseDialog(context, R.style.result_dialog, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pauseDialog.dismiss();
                finish();
            }
        });
        pauseDialog.setCancelable(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            existBy2Click();
            //pauseDialog.show();
            jAirShipView.getThread().pause();
        }
        return false;
    }

    public static boolean isExist = false;// 标记是否退出应用

    private void existBy2Click() {
        Timer existTimer;
        if (!isExist) {
            isExist = true;
            Toast.makeText(this, "再按一次返回键退出游戏", Toast.LENGTH_SHORT).show();
            existTimer = new Timer();
            existTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isExist = false;
                }
            }, 2000);
        } else {
            finish();
            System.exit(0);
        }
    }
}
