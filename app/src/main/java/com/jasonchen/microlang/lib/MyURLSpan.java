package com.jasonchen.microlang.lib;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Parcel;
import android.provider.Browser;
import android.support.v4.app.FragmentActivity;
import android.text.ParcelableSpan;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.BrowserActivity;
import com.jasonchen.microlang.beans.MessageListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.ShowUserDao;
import com.jasonchen.microlang.dao.StatusesTimeLineDao;
import com.jasonchen.microlang.settings.SettingUtility;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;

import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.Utility;

/**
 * jasonchen
 * 2015/04/10
 */
@SuppressLint("ResourceAsColor")
public class MyURLSpan extends ClickableSpan implements ParcelableSpan {

	private final String mURL;

	public MyURLSpan(String url) {
		mURL = url;
	}

	public MyURLSpan(Parcel src) {
		mURL = src.readString();
	}

	public int getSpanTypeId() {
		return 11;
	}

	public int describeContents() {
		return 0;
	}

	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mURL);
	}

	public String getURL() {
		return mURL;
	}

	public void onClick(View widget) {
		Uri uri = Uri.parse(getURL());
		Context context = widget.getContext();
		if (uri.getScheme().startsWith("http")) {
			String url = uri.toString();
			if (Utility.isWeiboAccountIdLink(url)) {
                /*Intent intent = UserDetailActivity.newIntent(Utility.getIdFromWeiboAccountLink(url), null, context);
                context.startActivity(intent);
                ((FragmentActivity)context).overridePendingTransition(R.anim.push_left_in,
                        R.anim.stay);*/
			} else if (Utility.isWeiboAccountDomainLink(url)) {
                /*Intent intent = UserDetailActivity.newIntent(null, Utility.getDomainFromWeiboAccountLink(url), context);
                context.startActivity(intent);
                ((FragmentActivity)context).overridePendingTransition(R.anim.push_left_in,
								R.anim.stay);*/
			} else {
				// otherwise some urls cant be opened, will be redirected to
				// sina error page
				String openUrl = url;
				if (openUrl.endsWith("/")) {
					openUrl = openUrl.substring(0, openUrl.lastIndexOf("/"));
				}
                if (SettingUtility.allowInternalWebBrowser()) {
                    Intent intent = BrowserActivity.newIntent(Uri.parse(openUrl));
                    context.startActivity(intent);
                    ((FragmentActivity)context).overridePendingTransition(R.anim.push_left_in,
                            R.anim.stay);
                } else {
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                    context.startActivity(intent);
                }
			}
		} else {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
            context.startActivity(intent);
		}
	}

	@Override
	public void updateDrawState(TextPaint tp) {
		tp.setColor(GlobalContext.getInstance().getResources()
				.getColor(R.color.colorPrimary));

	}
}
