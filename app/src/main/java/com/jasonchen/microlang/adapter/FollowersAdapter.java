package com.jasonchen.microlang.adapter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.UserActivity;
import com.jasonchen.microlang.activitys.WeiboDetailActivity;
import com.jasonchen.microlang.activitys.WriteReplyActivity;
import com.jasonchen.microlang.beans.CommentBean;
import com.jasonchen.microlang.beans.CommentListBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.RepostListBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.CommentsTimeLineByIdDao;
import com.jasonchen.microlang.dao.DestroyCommentDao;
import com.jasonchen.microlang.dao.RepostsTimeLineByIdDao;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.HackyMovementMethod;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.view.HackyTextView;
import com.jasonchen.microlang.view.TimeLineRoundAvatarImageView;
import com.jasonchen.microlang.view.TimeTextView;
import com.jasonchen.microlang.workers.PictureBitmapDrawable;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * jasonchen
 * 2015/04/10
 */
public class FollowersAdapter extends BaseAdapter {

	private Set<Integer> tagIndexList = new HashSet<Integer>();

	private Activity activity;
	private LayoutInflater mInflater;
	private LoadListView listView;
	private List<UserBean> list;

	public FollowersAdapter(Activity activity, List<UserBean> list, LoadListView listview) {
		super();
		this.activity = activity;
		this.mInflater = LayoutInflater.from(activity);
		this.list = list;
		this.listView = listview;

		this.listView.setRecyclerListener(new AbsListView.RecyclerListener() {
			@Override
			public void onMovedToScrapHeap(View view) {
				Integer index = (Integer) view
						.getTag(R.string.listview_index_tag);
				if (index == null) {
					return;
				}

				for (Integer tag : tagIndexList) {

					ViewHolder holder = (ViewHolder) view.getTag(tag);

					if (holder != null) {
						Drawable drawable = holder.avatar.getImageView()
								.getDrawable();
						clearAvatarBitmap(holder, drawable);
						
						if (!tag.equals(index)) {
	                        holder.layout.removeAllViewsInLayout();
	                        holder.layout = null;
	                        view.setTag(tag, null);
	                    }
					}
					
				}
			}

			void clearAvatarBitmap(ViewHolder holder, Drawable drawable) {
				if (!(drawable instanceof PictureBitmapDrawable)) {
					holder.avatar.setImageDrawable(null);
					holder.avatar.getImageView().clearAnimation();
				}
			}

		});

	}

	private Activity getActivity() {
		return activity;
	}

	public void setList(List<UserBean> list){
		this.list = list;
	}

	@Override
	public int getCount() {
		return list.size();
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null || convertView.getTag(R.drawable.ic_launcher + getItemViewType(position)) == null) {
			convertView = mInflater.inflate(R.layout.follow_fan_normal_item, null);
			holder = new ViewHolder();
			holder.avatar = ViewUtility.findViewById(convertView, R.id.avatar);
			holder.username = ViewUtility.findViewById(convertView, R.id.username);
			holder.description = ViewUtility.findViewById(convertView, R.id.description);
			convertView.setTag(R.drawable.ic_launcher + getItemViewType(position), holder);
			convertView.setTag(R.string.listview_index_tag, R.drawable.ic_launcher + getItemViewType(position));
			tagIndexList.add(R.drawable.ic_launcher + getItemViewType(position));
		} else {
			holder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher + getItemViewType(position));
		}

		ViewGroup.LayoutParams params = holder.avatar.getLayoutParams();
		TimeLineBitmapDownloader.getInstance().downloadAvatar(holder.avatar, list.get(position), false);
		holder.avatar.checkVerified(list.get(position));
		holder.username.setText(list.get(position).getScreen_name());
		if(!TextUtils.isEmpty(list.get(position).getDescription())){
			holder.description.setVisibility(View.VISIBLE);
			holder.description.setText(list.get(position).getDescription());
		}else{
			holder.description.setVisibility(View.GONE);
		}
		convertView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = UserActivity.newIntent(getActivity(), list.get(position));
				getActivity().startActivity(intent);
				getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
			}
		});
		return convertView;
	}

	private static class ViewHolder {
		LinearLayout layout;
		TimeLineRoundAvatarImageView avatar;
		TextView username;
        TextView description;
	}
}
