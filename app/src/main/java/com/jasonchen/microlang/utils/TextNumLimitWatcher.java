package com.jasonchen.microlang.utils;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.smilepicker.SmileyMap;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * jasonchen
 * 2015/04/10
 */
public class TextNumLimitWatcher implements TextWatcher {

    private TextView tv;
    private EditText et;
    private Activity activity;
    private Map<String,String> map;

    public TextNumLimitWatcher(TextView tv, EditText et, Activity activity) {
        this.tv = tv;
        this.et = et;
        this.activity = activity;
        map = new LinkedHashMap<String, String>();
		map.putAll(SmileyMap.getInstance().getGeneral());
		map.putAll(SmileyMap.getInstance().getHuahua());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        int sum = Utility.length(et.getText().toString());

        int left = 140 - sum;

        if (left == 140) {
            tv.setText("140x");
        } else {
            tv.setText(String.valueOf(left)+"x");
        }
        if (left < 0) {
            tv.setTextColor(activity.getResources().getColor(R.color.red));
        } else if (left >= 0 && left <= 140) {
            tv.setTextColor(activity.getResources().getColor(R.color.dimgrey));
        }
    }
    
    @Override
    public void afterTextChanged(Editable s) {
    }
}
