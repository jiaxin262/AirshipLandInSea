package com.jia.jason.airshiplandinsea.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jia.jason.airshiplandinsea.R;
import com.jia.jason.airshiplandinsea.activity.JAirShipActivity;

/**
 * Created by xin.jia
 * since 2016/3/8
 */
public class PauseDialog extends Dialog {

    public View.OnClickListener onClickListener;

    public PauseDialog(Context context) {
        super(context);
    }

    public PauseDialog(Context context, int themeResId, View.OnClickListener onClickListener) {
        super(context, themeResId);
        this.onClickListener = onClickListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pause_dialog);

        TextView continueGame = (TextView) findViewById(R.id.click_to_continue);
        continueGame.setOnClickListener(onClickListener);
    }
}
