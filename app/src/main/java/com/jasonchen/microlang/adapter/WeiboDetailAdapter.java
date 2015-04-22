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
import com.jasonchen.microlang.dao.CommentsTimeLineByIdDao;
import com.jasonchen.microlang.dao.DestroyCommentDao;
import com.jasonchen.microlang.dao.RepostsTimeLineByIdDao;
import com.jasonchen.microlang.debug.AppLogger;
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
import com.jasonchen.microlang.view.TimeTextView;
import com.jasonchen.microlang.workers.PictureBitmapDrawable;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.HashSet;
import java.util.Set;

/**
 * jasonchen
 * 2015/04/10
 */
public class WeiboDetailAdapter extends BaseAdapter {

	private Set<Integer> tagIndexList = new HashSet<Integer>();

	private static final int COMMENT_COMPLETE = 0;
	private static final int REPOST_COMPLETE = 1;
	private static final int NETWORK_ERROR = 2;

	private Fragment fragment;
	private Context mContext;
	private LayoutInflater mInflater;
	private CommentListBean commentList;
	private RepostListBean repostList;
	private MessageBean msgBean;
	private Handler handler;
	private LoadListView listView;
	private boolean canLoadComment = true;
	private boolean canLoadRepost = true;

	private TextView repost;
	private TextView comment;
	private boolean isFling;

	private RemoveTask removeTask;

	public WeiboDetailAdapter(Fragment fragment, final Context mContext, MessageBean msgBean,CommentListBean commentList, RepostListBean repostList,LoadListView listview) {
		super();
		this.fragment = fragment;
		this.mContext = mContext;
		this.mInflater = LayoutInflater.from(mContext);
		this.commentList = commentList;
		this.repostList = repostList;
		this.msgBean = msgBean;
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

		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case COMMENT_COMPLETE:
					CommentListBean list = (CommentListBean) msg.obj;
					setCommentList(list);
					notifyDataSetChanged();
					break;
				case REPOST_COMPLETE:
					RepostListBean newRepostList = (RepostListBean) msg.obj;
					setRepostList((RepostListBean) msg.obj);
					if(newRepostList != null && newRepostList.getSize() > 0){
						listView.getFooterView().show();
					}
					notifyDataSetChanged();
					break;
				case NETWORK_ERROR:
					String errorStr = (String) msg.obj;
					Toast.makeText(mContext, errorStr, Toast.LENGTH_SHORT)
							.show();
					break;
				}
			}
		};
	}

	private Activity getActivity() {
		return (Activity) mContext;
	}

	public void setComRepCount(int repostCount, int commentCount) {
		msgBean.setComments_count(commentCount);
		msgBean.setReposts_count(repostCount);
		if(repost != null){
			repost.setText(msgBean.getRepostscountString());
		}
		if(comment != null){
			comment.setText(msgBean.getCommentscountString());
		}
	}

	public void setCommentList(CommentListBean list) {
		this.commentList = list;
	}

	public void setRepostList(RepostListBean list) {
		this.repostList = list;
	}

	public CommentListBean getCommentListBean() {
		return commentList;
	}

	public RepostListBean getRepostListBean() {
		return repostList;
	}

	public void setIsCommentList(boolean isCommentList) {
		this.isCommentList = isCommentList;
	}

	public void setIsRepostList(boolean isRepostList) {
		this.isRepostList = isRepostList;
	}

	public void setIsFling(boolean isFling) {
		this.isFling = isFling;
	}

	private void switchToRepost() {
		if (firstLoadRepostList) {
			isRepostList = true;
			isCommentList = false;
			notifyDataSetChanged();
			asyncDownloadRepost();
			firstLoadRepostList = false;
		} else {
			if (isCommentList) {
				isRepostList = true;
				isCommentList = false;
				notifyDataSetChanged();
			} else {
				if (repostList != null && repostList.getItemList().size() > 0) {
					notifyDataSetChanged();
				} else {
					asyncDownloadRepost();
				}
			}
			if (repost != null && comment != null) {
				comment.setTextSize(14f);
				repost.setTextSize(18f);
			}
		}
	}

	private void switchToComment() {
		if (isCommentList) {
			asyncDownloadComment();
		} else {
			isCommentList = true;
			isRepostList = false;
			notifyDataSetChanged();
		}
		if (repost != null && comment != null) {
			comment.setTextSize(18f);
			repost.setTextSize(14f);
		}

	}

	private boolean isCommentList = true;
	private boolean isRepostList = false;
	public boolean firstLoadRepostList = true;

	public boolean getIsCommentList() {
		return isCommentList;
	}

	public boolean getIsFirstLoadRepost() {
		return firstLoadRepostList;
	}

	@Override
	public int getCount() {
		if (isCommentList) {
			if (commentList != null && commentList.getItemList().size() > 0) {
				return commentList.getItemList().size() + 1;
			} else {
				return 2;
			}
		} else {
			if (repostList != null && repostList.getItemList().size() > 0) {
				return repostList.getItemList().size() + 1;
			} else {
				return 2;
			}
		}
	}

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return 0;
	}

	@SuppressLint({ "InflateParams", "ResourceAsColor" })
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {

		if (position == 0) {
			View view = mInflater.inflate(
					R.layout.weibodetail_repost_comment_layout, null);
			repost = ViewUtility.findViewById(view, R.id.weibodetail_repost_count);
			comment = ViewUtility.findViewById(view, R.id.weibodetail_comment_count);
			repost.setText(String.valueOf("转发 " + msgBean.getRepostscountString()));
			comment.setText(String.valueOf("评论 " + msgBean.getCommentscountString()));
			if (isCommentList) {
				comment.setTextSize(18f);
				repost.setTextSize(14f);
			} else {
				comment.setTextSize(14f);
				repost.setTextSize(18f);
			}

			repost.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					switchToRepost();
					comment.setTextSize(14f);
					repost.setTextSize(18f);
				}
			});
			comment.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					comment.setTextSize(18f);
					repost.setTextSize(14f);
					switchToComment();
				}
			});
			return view;
		} else {
			if (((commentList == null || commentList.getItemList().size() == 0) && isCommentList)
					|| (isRepostList && repostList == null)) {
				View view = mInflater.inflate(
						R.layout.weibodetail_list_null_layout, null);
				listView.getFooterView().hide();
				return view;
			} else {
				ViewHolder holder = null;
				if (convertView == null || convertView.getTag(R.drawable.ic_launcher + getItemViewType(position)) == null) {
					convertView = mInflater.inflate(R.layout.weibodetail_normal_item, null);
					holder = new ViewHolder();
					holder.layout = ViewUtility.findViewById(convertView, R.id.listview_root);
					holder.avatar = ViewUtility.findViewById(convertView, R.id.avatar);
					holder.username = ViewUtility.findViewById(convertView, R.id.username);
					holder.time = ViewUtility.findViewById(convertView, R.id.time);
					holder.source = ViewUtility.findViewById(convertView, R.id.source);
					holder.content = ViewUtility.findViewById(convertView, R.id.content);
					holder.more = ViewUtility.findViewById(convertView, R.id.more);
					holder.content.setMovementMethod(HackyMovementMethod.getInstance());
					convertView.setTag(R.drawable.ic_launcher + getItemViewType(position), holder);
					convertView.setTag(R.string.listview_index_tag, R.drawable.ic_launcher + getItemViewType(position));
					tagIndexList.add(R.drawable.ic_launcher + getItemViewType(position));
				} else {
					holder = (ViewHolder) convertView.getTag(R.drawable.ic_launcher + getItemViewType(position));
				}
				if (isCommentList && commentList != null && commentList.getItemList().size() > 0) {
					final CommentBean msg = commentList.getItemList().get(position - 1);
					final ViewHolder finalHolder = holder;
					convertView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							PopupMenu popupMenu = new PopupMenu(getActivity(), finalHolder.more);
							popupMenu.inflate(R.menu.menu_weibodetail_item);
							Menu menu = popupMenu.getMenu();
							if(msg.getUser().getId().equals(GlobalContext.getInstance().getAccountBean().getUid())){
								menu.findItem(R.id.delete).setVisible(true);
							}

							popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
								@Override
								public boolean onMenuItemClick(MenuItem menuItem) {
									int id = menuItem.getItemId();
									if (id == R.id.copy) {
										ClipboardManager cmb = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
										cmb.setText(msg.getText().toString());
										Toast.makeText(getActivity(), getActivity().getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
									} else if (id == R.id.reply) {
										Intent intent = WriteReplyActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), msg, GlobalContext.getInstance().getSpecialToken());
										getActivity().startActivity(intent);
									} else if (id == R.id.delete) {
										if (!isCommentList) {
											return false;
										}
										if (removeTask == null || removeTask.getStatus() == MyAsyncTask.Status.FINISHED) {
											removeTask = new RemoveTask(GlobalContext.getInstance().getSpecialToken(),
													commentList.getItemList().get(position - 1).getId(), position);
											removeTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
										}
									}
									return false;
								}
							});

							popupMenu.show();
						}
					});
					holder.avatar.checkVerified(msg.getUser());
					TimeLineBitmapDownloader.getInstance().downloadAvatar(
							holder.avatar.getImageView(),msg.getUser(), false);
					holder.username.setText(msg.getUser().getName());
					holder.time.setTime(msg.getMills());
					holder.source.setText(msg.getSourceString());
					holder.content.setText(msg.getListViewSpannableString());
					holder.avatar.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = UserActivity.newIntent(getActivity(), msg.getUser());
							getActivity().startActivity(intent);
							getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
						}
					});
					holder.more.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View view) {
							PopupMenu popupMenu = new PopupMenu(getActivity(), finalHolder.more);
							popupMenu.inflate(R.menu.menu_weibodetail_item);
							Menu menu = popupMenu.getMenu();
							if(msg.getUser().getId().equals(GlobalContext.getInstance().getAccountBean().getUid())){
								menu.findItem(R.id.delete).setVisible(true);
							}

							popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
								@Override
								public boolean onMenuItemClick(MenuItem menuItem) {
									int id = menuItem.getItemId();
									if (id == R.id.copy) {
										ClipboardManager cmb = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
										cmb.setText(msg.getText().toString());
										Toast.makeText(getActivity(), getActivity().getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
									} else if (id == R.id.reply) {
										Intent intent = WriteReplyActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), msg, GlobalContext.getInstance().getSpecialToken());
										getActivity().startActivity(intent);
									} else if (id == R.id.delete) {
										if (!isCommentList) {
											return false;
										}
										if (removeTask == null || removeTask.getStatus() == MyAsyncTask.Status.FINISHED) {
											removeTask = new RemoveTask(GlobalContext.getInstance().getSpecialToken(),
													commentList.getItemList().get(position - 1).getId(), position);
											removeTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
										}
									}
									return false;
								}
							});

							popupMenu.show();
						}
					});

				} else if (isRepostList && repostList != null && repostList.getItemList().size() > 0) {
					final MessageBean repmsg = repostList.getItemList().get(position - 1);
					holder.more.setVisibility(View.GONE);
					holder.avatar.checkVerified(repmsg.getUser());
					TimeLineBitmapDownloader.getInstance().downloadAvatar(
							holder.avatar.getImageView(), repmsg.getUser(), false);
					holder.username.setText(repmsg.getUser().getName());
                    convertView.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = WeiboDetailActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), repmsg, GlobalContext.getInstance().getSpecialToken());
							getActivity().startActivity(intent);
						}
					});
					holder.time.setTime(repmsg.getMills());
					holder.source.setText(repmsg.getSourceString());
					holder.content.setText(repmsg.getListViewSpannableString());
					holder.avatar.setOnClickListener(new OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = UserActivity.newIntent(getActivity(), repmsg.getUser());
							getActivity().startActivity(intent);
							getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
						}
					});
				}else{
					View view = mInflater.inflate(
							R.layout.weibodetail_list_null_layout, null);
					listView.getFooterView().hide();
					return view;
				}
				return convertView;
			}

		}
	}

	private void asyncDownloadRepost() {
		new Thread() {
			public void run() {
				RepostsTimeLineByIdDao dao = new RepostsTimeLineByIdDao(
						GlobalContext.getInstance().getSpecialToken(),
						msgBean.getId());
				RepostListBean result = null;
				try {
					result = dao.getGSONMsgList();
					Message msg = Message.obtain();
					msg.what = REPOST_COMPLETE;
					msg.obj = result;
					handler.sendMessage(msg);
				} catch (WeiboException e) {
					e.printStackTrace();
					Message msg = Message.obtain();
					msg.what = NETWORK_ERROR;
					msg.obj = e.getError();
					handler.sendMessage(msg);
				}
			};
		}.start();
	}

	private void asyncDownloadComment() {
		new Thread() {
			public void run() {
				CommentsTimeLineByIdDao dao = new CommentsTimeLineByIdDao(
						GlobalContext.getInstance().getSpecialToken(),
						msgBean.getId());
				CommentListBean result = null;
				try {
					result = dao.getGSONMsgList();
					Message msg = Message.obtain();
					msg.what = COMMENT_COMPLETE;
					msg.obj = result;
					handler.sendMessage(msg);
				} catch (WeiboException e) {
					e.printStackTrace();
					Message msg = Message.obtain();
					msg.what = NETWORK_ERROR;
					msg.obj = e.getError();
					handler.sendMessage(msg);
				}
			};
		}.start();
	}

	private static class ViewHolder {
		LinearLayout layout;
		ISimRayDrawable avatar;
		TimeTextView time;
		TextView source;
		TextView username;
        HackyTextView content;
		ImageView more;
	}

	public void removeCommentItem(final int postion) {
		if (postion >= 0 && postion < commentList.getSize()) {
			Animation anim = AnimationUtils.loadAnimation(
					fragment.getActivity(), R.anim.account_delete_slide_out_right
			);

			anim.setAnimationListener(new Animation.AnimationListener() {
				@Override
				public void onAnimationStart(Animation animation) {

				}

				@Override
				public void onAnimationEnd(Animation animation) {
					commentList.getItemList().remove(postion - 1);
					WeiboDetailAdapter.this.notifyDataSetChanged();
				}

				@Override
				public void onAnimationRepeat(Animation animation) {

				}
			});

			int positonInListView = postion + 1;
			int start = listView.getFirstVisiblePosition();
			int end = listView.getLastVisiblePosition();

			if (positonInListView >= start && positonInListView <= end) {
				int positionInCurrentScreen = postion - start;
				listView.getChildAt(positionInCurrentScreen + 1).startAnimation(anim);
			} else {
				commentList.getItemList().remove(postion);
				WeiboDetailAdapter.this.notifyDataSetChanged();
			}
		}
	}

	class RemoveTask extends MyAsyncTask<Void, Void, Boolean> {

		String token;
		String id;
		int positon;
		WeiboException e;

		public RemoveTask(String token, String id, int positon) {
			this.token = token;
			this.id = id;
			this.positon = positon;
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			DestroyCommentDao dao = new DestroyCommentDao(token, id);
			try {
				return dao.destroy();
			} catch (WeiboException e) {
				this.e = e;
				cancel(true);
				return false;
			}
		}

		@Override
		protected void onCancelled(Boolean aBoolean) {
			super.onCancelled(aBoolean);
			if (Utility.isAllNotNull(getActivity(), this.e)) {
				Toast.makeText(getActivity(), e.getError(), Toast.LENGTH_SHORT).show();
			}
		}

		@Override
		protected void onPostExecute(Boolean aBoolean) {
			super.onPostExecute(aBoolean);
			if (aBoolean) {
				WeiboDetailAdapter.this.removeCommentItem(positon);
			}
		}
	}

}
