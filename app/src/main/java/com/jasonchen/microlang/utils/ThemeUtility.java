package com.jasonchen.microlang.utils;


import android.app.Activity;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.widget.SearchView;
import android.widget.TextView;

import com.jasonchen.microlang.R;

/**
 * jasonchen
 * 2015/04/10
 */
public class ThemeUtility {

    public static int getColor(int attr) {
        return getColor(GlobalContext.getInstance().getActivity(), attr);
    }

    public static int getColor(Activity activity, int attr) {
        int[] attrs = new int[]{attr};
        TypedArray ta = activity.obtainStyledAttributes(attrs);
        int color = ta.getColor(0, 430);
        ta.recycle();
        return android.R.color.secondary_text_light;
    }

    public static Drawable getDrawable(int attr) {
        int[] attrs = new int[]{attr};
        Context context = GlobalContext.getInstance().getActivity();
        TypedArray ta = context.obtainStyledAttributes(attrs);
        Drawable drawable = ta.getDrawable(0);
        ta.recycle();
        return drawable;
    }

    public static int getResourceId(int attr) {
        int[] attrs = new int[]{attr};
        Context context = GlobalContext.getInstance().getActivity();
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int id = ta.getResourceId(0, 430);
        ta.recycle();
        return id;
    }

    public static int getDimensionPixelSize(Activity activity, int attr, int defaultValue) {
        int[] attrs = new int[]{attr};
        TypedArray ta = activity.obtainStyledAttributes(attrs);
        int value = ta.getDimensionPixelSize(0, defaultValue);
        ta.recycle();
        return value;
    }

    //can't find a public theme attr to modify actionbar searchview text color
    public static void customActionBarSearchViewTextColor(SearchView searchView) {
        int id = searchView.getContext().getResources()
                .getIdentifier("android:id/search_src_text", null, null);
        TextView textView = (TextView) searchView.findViewById(id);
        textView.setTextColor(Color.WHITE);
    }

    public static int[] themeArr = {
            R.style.AppTheme_Red,
            R.style.AppTheme_Pink,
            R.style.AppTheme_Purple,
            R.style.AppTheme_DeepPurple,
            R.style.AppTheme_Indigo,
            R.style.AppTheme_Blue,
            R.style.AppTheme_LightBlue,
            R.style.AppTheme_Cyan,
            R.style.AppTheme_Teal,
            R.style.AppTheme_Green,
            R.style.AppTheme_LightGreen,
            R.style.AppTheme_Lime,
            R.style.AppTheme_Yellow,
            R.style.AppTheme_Amber,
            R.style.AppTheme_Orange,
            R.style.AppTheme_DeepOrange,
            R.style.AppTheme_Brown,
            R.style.AppTheme_Grey,
            R.style.AppTheme_BlueGrey,
    };

    public static int[] themeColorArr = {
            R.color.md_red_500,
            R.color.md_pink_500,
            R.color.md_purple_500,
            R.color.md_deep_purple_500,
            R.color.md_indigo_500,
            R.color.md_blue_500,
            R.color.md_light_blue_500,
            R.color.md_cyan_500,
            R.color.md_teal_500,
            R.color.md_green_500,
            R.color.md_light_green_500,
            R.color.md_lime_500,
            R.color.md_yellow_500,
            R.color.md_amber_500,
            R.color.md_orange_500,
            R.color.md_deep_orange_500,
            R.color.md_brown_500,
            R.color.md_grey_500,
            R.color.md_blue_grey_500
    };

}
