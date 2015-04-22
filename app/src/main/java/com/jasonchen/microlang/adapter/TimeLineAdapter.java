package com.jasonchen.microlang.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v4.app.Fragment;
import android.text.ClipboardManager;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.activitys.UserActivity;
import com.jasonchen.microlang.activitys.WeiboDetailActivity;
import com.jasonchen.microlang.activitys.WriteCommentActivity;
import com.jasonchen.microlang.activitys.WriteRepostActivity;
import com.jasonchen.microlang.beans.FavBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.dao.DestroyStatusDao;
import com.jasonchen.microlang.dao.FavDao;
import com.jasonchen.microlang.database.FriendsTimeLineDBTask;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.gallery.GalleryAnimationActivity;
import com.jasonchen.microlang.interfaces.IPictureWorker;
import com.jasonchen.microlang.interfaces.ISimRayDrawable;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.AnimationRect;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.HackyMovementMethod;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.TimeLineUtility;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.file.FileLocationMethod;
import com.jasonchen.microlang.view.HackyTextView;
import com.jasonchen.microlang.view.TimeLineAvatarImageView;
import com.jasonchen.microlang.view.TimeLineImageView;
import com.jasonchen.microlang.view.TimeLineRoundAvatarImageView;
import com.jasonchen.microlang.view.TimeTextView;
import com.jasonchen.microlang.workers.PictureBitmapDrawable;
import com.jasonchen.microlang.workers.TimeLineBitmapDownloader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/10
 */
public class TimeLineAdapter extends BaseAdapter {

    public static final int NO_ITEM_ID = -1;
    private static final int REQUEST_LIST_SIMPLE = 9;
    private Set<Integer> tagIndexList = new HashSet<Integer>();
    protected boolean showOriStatus = true;

    private boolean isFling = false;

    private List<MessageBean> list;
    private Context context;
    private Fragment fragment;
    private LoadListView listView;
    private LayoutInflater inflater;

    private DestroyStatusTask destroyStatusTask;
    private FavTask favTask = null;
    private UnFavTask unFavTask = null;

    @SuppressLint("NewApi")
    private LongSparseArray<Integer> msgHeights = new LongSparseArray<Integer>();
    @SuppressLint("NewApi")
    private LongSparseArray<Integer> msgWidths = new LongSparseArray<Integer>();
    @SuppressLint("NewApi")
    private LongSparseArray<Integer> oriMsgHeights = new LongSparseArray<Integer>();
    @SuppressLint("NewApi")
    private LongSparseArray<Integer> oriMsgWidths = new LongSparseArray<Integer>();

    public TimeLineAdapter(Fragment fragment, LoadListView listView,
                           List<MessageBean> list) {
        super();
        this.list = list;
        this.context = fragment.getActivity();
        this.fragment = fragment;
        this.listView = listView;
        this.inflater = getActivity().getLayoutInflater();

        listView.setRecyclerListener(
                new AbsListView.RecyclerListener() {
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
                                Drawable drawable = holder.avatar
                                        .getImageView().getDrawable();
                                clearAvatarBitmap(holder, drawable);
                                drawable = holder.content_pic.getImageView()
                                        .getDrawable();
                                clearPictureBitmap(holder, drawable);
                                drawable = holder.repost_content_pic
                                        .getImageView().getDrawable();
                                clearRepostPictureBitmap(holder, drawable);

                                clearMultiPics(holder.content_pic_multi);
                                clearMultiPics(holder.repost_content_pic_multi);

                                if (!tag.equals(index)) {
                                    holder.root.removeAllViewsInLayout();
                                    holder.root = null;
                                    view.setTag(tag, null);
                                }
                            }
                        }
                    }

                    void clearMultiPics(GridLayout gridLayout) {
                        if (gridLayout == null) {
                            return;
                        }
                        for (int i = 0; i < gridLayout.getChildCount(); i++) {
                            ImageView iv = (ImageView) gridLayout.getChildAt(i);
                            if (iv != null) {
                                iv.setImageDrawable(null);
                            }
                        }
                    }

                    void clearAvatarBitmap(ViewHolder holder, Drawable drawable) {
                        if (!(drawable instanceof PictureBitmapDrawable)) {
                            holder.avatar.setImageDrawable(null);
                            holder.avatar.getImageView().clearAnimation();
                        }
                        holder.avatar.setImageDrawable(null);
                        holder.avatar.getImageView().clearAnimation();
                    }

                    void clearPictureBitmap(ViewHolder holder, Drawable drawable) {
                        if (!(drawable instanceof PictureBitmapDrawable)) {
                            holder.content_pic.setImageDrawable(null);
                            holder.content_pic.getImageView().clearAnimation();
                        }
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.content_pic.getLayoutParams();
                        params.height = Utility.dip2px(157);
                        params.width = Utility.dip2px(157);
                        holder.content_pic.getImageView().setScaleType(ImageView.ScaleType.FIT_XY);
                        holder.content_pic.setLayoutParams(params);
                        holder.content_pic.setImageDrawable(getActivity().getResources().getDrawable(R.color.gainsboro));
                    }

                    void clearRepostPictureBitmap(ViewHolder holder,
                                                  Drawable drawable) {
                        if (!(drawable instanceof PictureBitmapDrawable)) {
                            holder.repost_content_pic.setImageDrawable(null);
                            holder.repost_content_pic.getImageView()
                                    .clearAnimation();
                        }
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) holder.repost_content_pic.getLayoutParams();
                        params.height = Utility.dip2px(157);
                        params.width = Utility.dip2px(157);
                        holder.repost_content_pic.getImageView().setScaleType(ImageView.ScaleType.FIT_XY);
                        holder.repost_content_pic.setLayoutParams(params);
                        holder.repost_content_pic.setImageDrawable(getActivity().getResources().getDrawable(R.color.gainsboro));
                    }
                });
    }

    protected android.support.v4.app.FragmentActivity getActivity() {
        return fragment.getActivity();
    }

    public void setList(List<MessageBean> list) {
        this.list = list;
    }

    @Override
    public int getCount() {
        if (getList() != null) {
            return list.size();
        } else {
            return 0;
        }
    }

    public List<MessageBean> getList() {
        return list;
    }

    @Override
    public Object getItem(int position) {
        if (position >= 0 && getList() != null && getList().size() > 0
                && position < getList().size()) {
            return getList().get(position);
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        if (getList() != null && getList().get(position) != null
                && getList().size() > 0 && position < getList().size()) {
            return Long.valueOf(getList().get(position).getId());
        } else {
            return NO_ITEM_ID;
        }
    }

    @SuppressLint("ViewHolder")
    @SuppressWarnings("null")
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        int itemViewType = getItemViewType(position);
        if (convertView == null || convertView.getTag(R.drawable.ic_launcher + itemViewType) == null) {
            convertView = inflater.inflate(
                    R.layout.timeline_normal_layout, parent, false);
            //msg layout
            holder.username = ViewUtility.findViewById(convertView,
                    R.id.username);
            TextPaint tp = holder.username.getPaint();
            if (tp != null) {
                tp.setFakeBoldText(true);
            }
            holder.root = ViewUtility.findViewById(convertView, R.id.root);
            holder.avatar = ViewUtility.findViewById(convertView, R.id.avatar);
            holder.repost_content = ViewUtility.findViewById(convertView,
                    R.id.repost_content);
            holder.time = ViewUtility.findViewById(convertView, R.id.time);
            holder.source = ViewUtility.findViewById(convertView, R.id.source);
            holder.more = ViewUtility.findViewById(convertView, R.id.more);
            holder.content = ViewUtility
                    .findViewById(convertView, R.id.content);
            holder.content_pic = ViewUtility.findViewById(convertView, R.id.content_pic);
            holder.content_pic_multi = ViewUtility.findViewById(convertView, R.id.content_pic_multi);
            holder.repost_count = ViewUtility.findViewById(convertView, R.id.repost_count);
            holder.comment_count = ViewUtility.findViewById(convertView, R.id.comment_count);

            //repost msg layout
            holder.repost_layout = ViewUtility.findViewById(convertView, R.id.repost_layout);
            holder.repost_content = ViewUtility.findViewById(convertView, R.id.repost_content);
            holder.repost_content_pic = (TimeLineImageView) convertView
                    .findViewById(R.id.repost_content_pic);
            holder.repost_content_pic_multi = ViewUtility.findViewById(
                    convertView, R.id.repost_content_pic_multi);
            holder.repost_msg_repost_count = ViewUtility.findViewById(convertView,
                    R.id.repost_msg_repost_count);
            holder.repost_msg_comment_count = ViewUtility.findViewById(convertView,
                    R.id.repost_msg_comment_count);

            //set tag for convertview
            convertView.setTag(R.drawable.ic_launcher + getItemViewType(position), holder);
            convertView.setTag(R.string.listview_index_tag,
                    R.drawable.ic_launcher + getItemViewType(position));
            tagIndexList.add(R.drawable.ic_launcher + getItemViewType(position));
        } else {
            holder = (ViewHolder) convertView
                    .getTag(R.drawable.ic_launcher + getItemViewType(position));
        }
        bindViewData(holder, position);
        return convertView;
    }

    @SuppressLint("NewApi")
    private void bindViewData(final ViewHolder holder, final int position) {

        final MessageBean msg = list.get(position);
        UserBean user = msg.getUser();

        holder.root.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                fragment.startActivityForResult(
                        WeiboDetailActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), list.get(position), GlobalContext.getInstance().getSpecialToken()), 1);
                getActivity().overridePendingTransition(
                        R.anim.push_left_in, R.anim.stay);
            }
        });

        holder.more.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                final PopupMenu popupMenu = new PopupMenu(getActivity(), holder.more);
                popupMenu.inflate(R.menu.menu_timeline_item);
                Menu menu = popupMenu.getMenu();
                if (GlobalContext.getInstance().getAccountBean().getUid().equals(msg.getUser().getId())) {
                    menu.findItem(R.id.delete).setVisible(true);
                }
                if (msg.isFavorited()) {
                    menu.findItem(R.id.unfav).setVisible(true);
                } else {
                    menu.findItem(R.id.fav).setVisible(true);
                }
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        Intent intent = null;
                        if (id == R.id.copy) {
                            ClipboardManager cmb = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
                            cmb.setText(msg.getText().toString());
                            Toast.makeText(getActivity(), getActivity().getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
                        } else if (id == R.id.fav) {
                            if (Utility.isTaskStopped(favTask) && Utility.isTaskStopped(unFavTask)) {
                                favTask = new FavTask(getActivity(), position);
                                favTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        } else if (id == R.id.unfav) {
                            if (Utility.isTaskStopped(favTask) && Utility.isTaskStopped(unFavTask)) {
                                unFavTask = new UnFavTask(getActivity(), position);
                                unFavTask.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                            }
                        } else if (id == R.id.repost) {
                            intent = WriteRepostActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), msg, GlobalContext.getInstance().getSpecialToken());
                            getActivity().startActivity(intent);
                        } else if (id == R.id.comment) {
                            intent = WriteCommentActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), msg, GlobalContext.getInstance().getSpecialToken());
                            getActivity().startActivity(intent);
                        } else if (id == R.id.delete) {
                            final MaterialDialog alertDeleteDialog = new MaterialDialog(getActivity());
                            alertDeleteDialog.setTitle(getActivity().getString(R.string.notice)).setMessage(getActivity().getString(R.string.delete_status));
                            alertDeleteDialog.setPositiveButton(getActivity().getString(R.string.confirm), new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    destroyStatusTask = new DestroyStatusTask(context, position);
                                    destroyStatusTask
                                            .executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                                    alertDeleteDialog.dismiss();
                                }
                            }).setNegativeButton(getActivity().getString(R.string.cancel), new OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    alertDeleteDialog.dismiss();
                                }
                            }).show();
                        }
                        return true;
                    }
                });
                popupMenu.show();
            }
        });

        if (user != null) {
            holder.username.setVisibility(View.VISIBLE);
            if (!TextUtils.isEmpty(user.getRemark())) {
                holder.username
                        .setText(new StringBuilder(user.getScreen_name())
                                .append("(").append(user.getRemark())
                                .append(")").toString());
            } else {
                holder.username.setText(user.getScreen_name());
            }
            holder.avatar.checkVerified(user);
                buildAvatar(holder.avatar, position, user);
        } else {
            holder.username.setVisibility(View.INVISIBLE);
            holder.avatar.setVisibility(View.INVISIBLE);
        }

        if (!TextUtils.isEmpty(msg.getListViewSpannableString())) {
            boolean haveCachedHeight = msgHeights.get(msg.getIdLong()) != null;
            LayoutParams layoutParams = holder.content
                    .getLayoutParams();
            if (haveCachedHeight) {
                layoutParams.height = msgHeights.get(msg.getIdLong());
            } else {
                layoutParams.height = LayoutParams.WRAP_CONTENT;
            }

            boolean haveCachedWidth = msgWidths.get(msg.getIdLong()) != null;
            if (haveCachedWidth) {
                layoutParams.width = msgWidths.get(msg.getIdLong());
            } else {
                layoutParams.width = LayoutParams.WRAP_CONTENT;
            }

            holder.content.requestLayout();
            holder.content.setText(msg.getListViewSpannableString());
            if (!haveCachedHeight) {
                msgHeights.append(msg.getIdLong(), layoutParams.height);
            }

            if (!haveCachedWidth) {
                msgWidths.append(msg.getIdLong(), layoutParams.width);
            }
        } else {
            TimeLineUtility.addJustHighLightLinks(msg);
            holder.content.setText(msg.getListViewSpannableString());
        }
        holder.content.setMovementMethod(HackyMovementMethod.getInstance());

        holder.time.setTime(msg.getMills());
        if (holder.source != null) {
            holder.source.setText(msg.getSourceString());
        }

        if (showOriStatus) {
            boolean checkRepostsCount = (msg.getReposts_count() != 0);
            boolean checkCommentsCount = (msg.getComments_count() != 0);

            if (checkRepostsCount) {
                holder.repost_count.setText(String.valueOf(msg
                        .getRepostscountString()));
                holder.repost_count.setVisibility(View.VISIBLE);
            } else {
                holder.repost_count.setText("0");
                holder.repost_count.setVisibility(View.VISIBLE);
            }

            if (checkCommentsCount) {
                holder.comment_count.setText(String.valueOf(msg
                        .getCommentscountString()));
                holder.comment_count.setVisibility(View.VISIBLE);
            } else {
                holder.comment_count.setText("0");
                holder.comment_count.setVisibility(View.VISIBLE);
            }
        }

        holder.repost_layout.setVisibility(View.GONE);

        if (!SettingUtility.getIntelligencePic()) {
            if (msg.havePicture()) {
                if (msg.isMultiPics()) {
                    buildMultiPic(msg, holder.content_pic_multi);
                    holder.content_pic.setVisibility(View.GONE);
                } else {
                    buildPic(msg, holder.content_pic, position);
                    holder.content_pic_multi.setVisibility(View.GONE);
                }
            } else {
                holder.content_pic.setVisibility(View.GONE);
                holder.content_pic_multi.setVisibility(View.GONE);
            }
        } else {
            if (Utility.isWifi(getActivity())) {
                if (msg.havePicture()) {
                    if (msg.isMultiPics()) {
                        buildMultiPic(msg, holder.content_pic_multi);
                        holder.content_pic.setVisibility(View.GONE);
                    } else {
                        buildPic(msg, holder.content_pic, position);
                        holder.content_pic_multi.setVisibility(View.GONE);
                    }
                } else {
                    holder.content_pic.setVisibility(View.GONE);
                    holder.content_pic_multi.setVisibility(View.GONE);
                }
            } else {
                holder.content_pic.setVisibility(View.GONE);
                holder.content_pic_multi.setVisibility(View.GONE);
            }
        }

        final MessageBean repost_msg = msg.getRetweeted_status();

        if (repost_msg != null && showOriStatus) {
            if (holder.repost_layout != null) {
                holder.repost_layout.setVisibility(View.VISIBLE);
            }
            if (holder.content_pic.getVisibility() != View.GONE) {
                holder.content_pic.setVisibility(View.GONE);
            }
            if (holder.content_pic_multi.getVisibility() != View.GONE) {
                holder.content_pic_multi.setVisibility(View.GONE);
            }
            buildRepostContent(msg, repost_msg, holder, position);
        } else {
            if (holder.repost_layout != null) {
                holder.repost_layout.setVisibility(View.GONE);
            }
        }

        boolean interruptPic = false;
        boolean interruptMultiPic = false;
        boolean interruptRepostPic = false;
        boolean interruptRepostMultiPic = false;

        if (msg.havePicture()) {
            if (msg.isMultiPics()) {
                interruptPic = true;
            } else {
                interruptMultiPic = true;
            }
        }

        if (repost_msg != null && showOriStatus) {

            if (repost_msg.havePicture()) {
                if (repost_msg.isMultiPics()) {
                    interruptRepostPic = true;
                } else {
                    interruptRepostMultiPic = true;
                }
            }
        }

        if (interruptPic) {
            interruptPicDownload(holder.content_pic);
        }

        if (interruptMultiPic) {
            interruptPicDownload(holder.content_pic_multi);
        }

        if (interruptRepostPic) {
            interruptPicDownload(holder.repost_content_pic);
        }

        if (interruptRepostMultiPic) {
            interruptPicDownload(holder.repost_content_pic_multi);
        }

    }

    public void setIsFling(boolean isFling) {
        this.isFling = isFling;
    }

    protected void interruptPicDownload(GridLayout gridLayout) {
        for (int i = 0; i < gridLayout.getChildCount(); i++) {
            ImageView iv = (ImageView) gridLayout.getChildAt(i);
            if (iv != null) {
                Drawable drawable = iv.getDrawable();
                if (drawable instanceof PictureBitmapDrawable) {
                    PictureBitmapDrawable downloadedDrawable = (PictureBitmapDrawable) drawable;
                    IPictureWorker worker = downloadedDrawable
                            .getBitmapDownloaderTask();
                    if (worker != null) {
                        ((MyAsyncTask) worker).cancel(true);
                    }
                    iv.setImageDrawable(null);
                }
            }
        }
    }

    protected void interruptPicDownload(ISimRayDrawable view) {
        Drawable drawable = view.getImageView().getDrawable();
        if (drawable instanceof PictureBitmapDrawable) {
            PictureBitmapDrawable downloadedDrawable = (PictureBitmapDrawable) drawable;
            IPictureWorker worker = downloadedDrawable
                    .getBitmapDownloaderTask();
            if (worker != null) {
                ((MyAsyncTask) worker).cancel(true);
            }
        }
        view.getImageView().setImageDrawable(null);
    }

    @SuppressLint("NewApi")
    private void buildRepostContent(MessageBean msg,
                                    final MessageBean repost_msg, ViewHolder holder, int position) {
        holder.repost_layout.setVisibility(View.VISIBLE);

        if (!repost_msg.getId().equals((String) holder.repost_content.getTag())) {
            boolean haveCachedHeight = oriMsgHeights.get(msg.getIdLong()) != null;
            LayoutParams layoutParams = holder.repost_content
                    .getLayoutParams();
            if (haveCachedHeight) {
                layoutParams.height = oriMsgHeights.get(msg.getIdLong());
            } else {
                layoutParams.height = LayoutParams.WRAP_CONTENT;
            }

            boolean haveCachedWidth = oriMsgWidths.get(msg.getIdLong()) != null;
            if (haveCachedWidth) {
                layoutParams.width = oriMsgWidths.get(msg.getIdLong());
            } else {
                layoutParams.width = LayoutParams.WRAP_CONTENT;
            }

            holder.repost_content.requestLayout();
            holder.repost_content.setText(repost_msg
                    .getListViewSpannableString());

            if (!haveCachedHeight) {
                oriMsgHeights.append(msg.getIdLong(), layoutParams.height);
            }

            if (!haveCachedWidth) {
                oriMsgWidths.append(msg.getIdLong(), layoutParams.width);
            }

            holder.repost_content.setText(repost_msg
                    .getListViewSpannableString());
            holder.repost_content.setTag(repost_msg.getId());
        } else {
            holder.repost_content.setText(repost_msg
                    .getListViewSpannableString());
        }
        holder.repost_content.setMovementMethod(HackyMovementMethod.getInstance());
        holder.repost_msg_repost_count.setText("" + repost_msg.getRepostscountString());
        holder.repost_msg_comment_count.setText("" + repost_msg.getCommentscountString());

        if (SettingUtility.getIntelligencePic()) {
            if (Utility.isWifi(getActivity())) {
                if (repost_msg.havePicture()) {
                    if (repost_msg.isMultiPics()) {
                        buildMultiPic(repost_msg, holder.repost_content_pic_multi);
                        holder.repost_content_pic.setVisibility(View.GONE);
                    } else {
                        buildPic(repost_msg, holder.repost_content_pic, position);
                        holder.repost_content_pic_multi.setVisibility(View.GONE);
                    }
                } else {
                    holder.repost_content_pic.setVisibility(View.GONE);
                    holder.repost_content_pic_multi.setVisibility(View.GONE);
                }
            } else {
                holder.repost_content_pic.setVisibility(View.GONE);
                holder.repost_content_pic_multi.setVisibility(View.GONE);
            }
        } else {
            if (repost_msg.havePicture()) {
                if (repost_msg.isMultiPics()) {
                    buildMultiPic(repost_msg, holder.repost_content_pic_multi);
                    holder.repost_content_pic.setVisibility(View.GONE);
                } else {
                    buildPic(repost_msg, holder.repost_content_pic, position);
                    holder.repost_content_pic_multi.setVisibility(View.GONE);
                }
            } else {
                holder.repost_content_pic.setVisibility(View.GONE);
                holder.repost_content_pic_multi.setVisibility(View.GONE);
            }
        }

    }

    protected void buildAvatar(TimeLineRoundAvatarImageView view, int position,
                               final UserBean user) {
        view.setVisibility(View.VISIBLE);
        view.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = UserActivity.newIntent(getActivity(), user);
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
            }
        });

        view.checkVerified(user);
        buildAvatar(view.getImageView(), position, user);
    }

    protected void buildAvatar(ImageView view, int position, final UserBean user) {
        String image_url = user.getProfile_image_url();
        if (!TextUtils.isEmpty(image_url)) {
            view.setVisibility(View.VISIBLE);
            LayoutParams params = view.getLayoutParams();

            TimeLineBitmapDownloader.getInstance().downloadAvatar(view, user, false);

        } else {
            view.setVisibility(View.GONE);
        }
    }

    protected void buildMultiPic(final MessageBean msg,
                                 final GridLayout gridLayout) {
        if (SettingUtility.isEnablePic()) {
            gridLayout.setVisibility(View.VISIBLE);

            final int count = msg.getPicCount();
            for (int i = 0; i < count; i++) {
                final ISimRayDrawable pic = (ISimRayDrawable) gridLayout
                        .getChildAt(i);
                pic.setVisibility(View.VISIBLE);

                TimeLineBitmapDownloader.getInstance().displayMultiPicture(
                        pic, msg.getThumbnailPicUrls().get(i),
                        FileLocationMethod.picture_thumbnail, isFling);
                //}

                final int finalI = i;
                pic.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ArrayList<AnimationRect> animationRectArrayList = new ArrayList<AnimationRect>();
                        for (int i = 0; i < count; i++) {
                            final ISimRayDrawable pic = (ISimRayDrawable) gridLayout
                                    .getChildAt(i);
                            ImageView imageView = (ImageView) pic;
                            if (imageView.getVisibility() == View.VISIBLE) {
                                AnimationRect rect = AnimationRect
                                        .buildFromImageView(imageView);
                                animationRectArrayList.add(rect);
                            }
                        }

                        Intent intent = GalleryAnimationActivity.newIntent(msg,
                                animationRectArrayList, finalI);
                        getActivity().startActivity(intent);
                    }
                });
            }

            if (count < 9) {
                ImageView pic;
                switch (count) {
                    case 8:
                        pic = (ImageView) gridLayout.getChildAt(8);
                        pic.setVisibility(View.INVISIBLE);
                        break;
                    case 7:
                        for (int i = 8; i > 6; i--) {
                            pic = (ImageView) gridLayout.getChildAt(i);
                            pic.setVisibility(View.INVISIBLE);
                        }
                        break;
                    case 6:
                        for (int i = 8; i > 5; i--) {
                            pic = (ImageView) gridLayout.getChildAt(i);
                            pic.setVisibility(View.GONE);
                        }

                        break;
                    case 5:
                        for (int i = 8; i > 5; i--) {
                            pic = (ImageView) gridLayout.getChildAt(i);
                            pic.setVisibility(View.GONE);
                        }
                        pic = (ImageView) gridLayout.getChildAt(5);
                        pic.setVisibility(View.INVISIBLE);
                        break;
                    case 4:
                        for (int i = 8; i > 5; i--) {
                            pic = (ImageView) gridLayout.getChildAt(i);
                            pic.setVisibility(View.GONE);
                        }
                        pic = (ImageView) gridLayout.getChildAt(5);
                        pic.setVisibility(View.INVISIBLE);
                        pic = (ImageView) gridLayout.getChildAt(4);
                        pic.setVisibility(View.INVISIBLE);
                        break;
                    case 3:
                        for (int i = 8; i > 2; i--) {
                            pic = (ImageView) gridLayout.getChildAt(i);
                            pic.setVisibility(View.GONE);
                        }
                        break;
                    case 2:
                        for (int i = 8; i > 2; i--) {
                            pic = (ImageView) gridLayout.getChildAt(i);
                            pic.setVisibility(View.GONE);
                        }
                        pic = (ImageView) gridLayout.getChildAt(2);
                        pic.setVisibility(View.INVISIBLE);
                        break;
                }
            }
        } else {
            gridLayout.setVisibility(View.GONE);
        }
    }

    protected void buildPic(final MessageBean msg, final TimeLineImageView view,
                            int position) {
        if (SettingUtility.isEnablePic()) {
            view.setVisibility(View.VISIBLE);
            view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    ImageView imageView = view.getImageView();
                    AnimationRect rect = AnimationRect
                            .buildFromImageView(imageView);
                    ArrayList<AnimationRect> animationRectArrayList = new ArrayList<AnimationRect>();
                    animationRectArrayList.add(rect);

                    Intent intent = GalleryAnimationActivity.newIntent(msg,
                            animationRectArrayList, 0);
                    getActivity().startActivity(intent);
                }
            });
            buildPic(msg, view);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    private void buildPic(final MessageBean msg, TimeLineImageView view) {
        view.setVisibility(View.VISIBLE);
        TimeLineBitmapDownloader.getInstance().downContentPic(view, msg,
                isFling);
    }

    public static class ViewHolder {
        LinearLayout root;
        TextView username;
        HackyTextView content;
        TimeTextView time;
        TextView source;
        TextView repost_count;
        TextView comment_count;
        ImageView more;
        TimeLineRoundAvatarImageView avatar;
        TimeLineImageView content_pic;
        GridLayout content_pic_multi;
        LinearLayout repost_layout;
        HackyTextView repost_content;
        TimeLineImageView repost_content_pic;
        GridLayout repost_content_pic_multi;
        TextView repost_msg_repost_count;
        TextView repost_msg_comment_count;
    }

    private class FavTask extends MyAsyncTask<Void, Void, FavBean> {

        int position;
        FavBean favBean = null;
        Context context;

        private FavTask(Context context, int position) {
            this.context = context;
            this.position = position;
        }

        @Override
        protected FavBean doInBackground(Void... params) {
            FavDao dao = new FavDao(GlobalContext.getInstance().getSpecialToken(), list.get(position).getId());
            try {
                favBean = dao.favIt();
            } catch (WeiboException e) {
                e.printStackTrace();
            }
            return favBean;
        }

        @Override
        protected void onPostExecute(FavBean favBean) {
            super.onPostExecute(favBean);
            if (favBean != null) {
                Toast.makeText(context, getActivity().getResources().getString(R.string.favorite_success), Toast.LENGTH_SHORT).show();
                MessageBean bean = list.get(position);
                bean.setFavorited(true);
                list.remove(position);
                list.add(position, bean);
                FriendsTimeLineDBTask.asyncUpdateMsg(list.get(position).getId(), true);
            }
        }
    }

    private class UnFavTask extends MyAsyncTask<Void, Void, FavBean> {

        int position;
        FavBean favBean = null;
        Context context;
        WeiboException e;

        private UnFavTask(Context context, int position) {
            this.context = context;
            this.position = position;
        }

        @Override
        protected FavBean doInBackground(Void... params) {
            FavDao dao = new FavDao(GlobalContext.getInstance().getSpecialToken(), list.get(position).getId());
            try {
                favBean = dao.unFavIt();
            } catch (WeiboException e) {
                e.printStackTrace();
                this.e = e;
                cancel(true);
                return null;
            }
            return favBean;
        }

        @Override
        protected void onCancelled(FavBean favBean) {
            super.onCancelled(favBean);
            if (favBean == null && this.e != null) {
                Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        protected void onPostExecute(FavBean favBean) {
            super.onPostExecute(favBean);
            if (favBean != null) {
                Toast.makeText(context, getActivity().getResources().getString(R.string.unfavorite_success), Toast.LENGTH_SHORT).show();
                MessageBean bean = list.get(position);
                bean.setFavorited(false);
                list.remove(position);
                list.add(position, bean);
                FriendsTimeLineDBTask.asyncUpdateMsg(list.get(position).getId(), false);
            }
        }
    }

    private class DestroyStatusTask extends
            MyAsyncTask<Void, Void, DestroyResult> {

        int position;
        boolean result;
        Context context;

        public DestroyStatusTask(Context context, int position) {
            super();
            this.context = context;
            this.position = position;
        }

        @Override
        protected DestroyResult doInBackground(Void... params) {
            DestroyStatusDao dao = new DestroyStatusDao(GlobalContext
                    .getInstance().getSpecialToken(), list.get(position)
                    .getId());
            try {
                result = dao.destroy();
            } catch (WeiboException e) {
                e.printStackTrace();
            }
            if (result) {
                return DestroyResult.success;
            } else {
                return DestroyResult.failed;
            }
        }

        @Override
        protected void onPostExecute(DestroyResult result) {
            super.onPostExecute(result);
            switch (result) {
                case success:
                    Toast.makeText(context, "删除微博成功！", Toast.LENGTH_SHORT).show();
                    FriendsTimeLineDBTask.deleteMsg(GlobalContext.getInstance()
                            .getAccountBean().getUid(), list.get(position).getId());
                    list.remove(position);
                    notifyDataSetChanged();

                    break;

                case failed:
                    Toast.makeText(context, "删除微博失败！", Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    }

    public enum DestroyResult {
        success, failed
    }

}
