package com.jia.jason.airshiplandinsea.activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

/**
 * Created by xin.jia
 * since 2016/1/12
 */
public class BaseActivity extends Activity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void jStartActivity(Intent intent) {
        this.startActivity(intent);
    }

    public void jStartActivity(Class<? extends Activity> cls) {
        this.jStartActivity((Class) cls, (Bundle) null);
    }

    public void jStartActivity(Class<? extends Activity> cls, Bundle bundle) {
        Intent intent = new Intent();
        if(bundle != null) {
            intent.putExtras(bundle);
        }

        intent.setClass(this, cls);
        this.startActivity(intent);
    }

    @Override
    public void onClick(View v) {

    }

}
