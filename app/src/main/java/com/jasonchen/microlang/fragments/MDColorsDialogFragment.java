package com.jasonchen.microlang.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.AbstractAppActivity;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.utils.ThemeUtility;
import com.jasonchen.microlang.view.CircleImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * jasonchen
 * 2015/05/27
 *
 */
public class MDColorsDialogFragment extends DialogFragment implements OnItemClickListener{

	public static void launch(Activity context) {
		Fragment fragment = context.getFragmentManager().findFragmentByTag("MDColorsDialogFragment");
    	if (fragment != null) {
    		context.getFragmentManager().beginTransaction().remove(fragment).commit();
    	}
    	
    	MDColorsDialogFragment dialogFragment = new MDColorsDialogFragment();
    	dialogFragment.show(context.getFragmentManager(), "MDColorsDialogFragment");
	}
	
	private Map<String, ColorDrawable> colorMap = new HashMap<String, ColorDrawable>();
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		setCancelable(true);
		
		View view = View.inflate(getActivity(), R.layout.dialog_themepicker, null);
		
		GridView gridView = (GridView) view.findViewById(R.id.grid);
		gridView.setAdapter(new MDColorsAdapter());
		gridView.setOnItemClickListener(this);

		return new AlertDialog.Builder(getActivity()).setView(view).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		}).create();
	}

	class MDColorsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return ThemeUtility.themeColorArr.length;
		}

		@Override
		public Object getItem(int position) {
			return ThemeUtility.themeColorArr[position];
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null)
				convertView = View.inflate(getActivity(), R.layout.item_themepicker, null);

			if (!colorMap.containsKey(String.valueOf(position)))
				colorMap.put(String.valueOf(position), new ColorDrawable(getResources().getColor(ThemeUtility.themeColorArr[position])));

			CircleImageView imgColor = (CircleImageView) convertView.findViewById(R.id.imgColor);
			ColorDrawable colorDrawable = colorMap.get(String.valueOf(position));
			imgColor.setImageDrawable(colorDrawable);
			
			View imgSelected = convertView.findViewById(R.id.imgSelected);
			imgSelected.setVisibility(SettingUtility.getThemeIndex() == position ? View.VISIBLE : View.GONE);
			
			return convertView;
		}
		
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position == SettingUtility.getThemeIndex()) {
            dismiss();

            return;
        }

        SettingUtility.setTheme(position);

    	dismiss();

        if (getActivity() instanceof AbstractAppActivity)
            ((AbstractAppActivity) getActivity()).reload();
	}

}
