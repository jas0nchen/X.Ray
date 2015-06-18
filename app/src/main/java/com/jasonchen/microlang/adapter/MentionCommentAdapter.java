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
import com.jasonchen.microlang.activitys.WriteReplyActivity;
import com.jasonchen.microlang.beans.CommentBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.UserBean;
import com.jasonchen.microlang.swiperefresh.LoadListView;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.HackyMovementMethod;
import com.jasonchen.microlang.utils.SettingUtility;
import com.jasonchen.microlang.utils.TimeLineUtility;
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
public class MentionCommentAdapter extends BaseAdapter {

    public static final int NO_ITEM_ID = -1;
    private Set<Integer> tagIndexList = new HashSet<Integer>();
    protected boolean showOriStatus = true;

    private boolean isFling = false;

    private List<CommentBean> list;
    private Context context;
    private Fragment fragment;
    private LoadListView listView;
    private LayoutInflater inflater;

    @SuppressLint("NewApi")
    private LongSparseArray<Integer> msgHeights = new LongSparseArray<Integer>();
    @SuppressLint("NewApi")
    private LongSparseArray<Integer> msgWidths = new LongSparseArray<Integer>();
    @SuppressLint("NewApi")
    private LongSparseArray<Integer> oriMsgHeights = new LongSparseArray<Integer>();
    @SuppressLint("NewApi")
    private LongSparseArray<Integer> oriMsgWidths = new LongSparseArray<Integer>();

    public MentionCommentAdapter(Fragment fragment, LoadListView listView,
                                 List<CommentBean> list, Context context) {
        super();
        this.list = list;
        this.context = context;
        this.fragment = fragment;
        this.listView = listView;
        this.inflater = fragment.getActivity().getLayoutInflater();

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
                                if (!tag.equals(index)) {
                                    holder.root.removeAllViewsInLayout();
                                    holder.root = null;
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

    protected android.support.v4.app.FragmentActivity getActivity() {
        return fragment.getActivity();
    }

    public void setList(List<CommentBean> list) {
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

    public List<CommentBean> getList() {
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

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();
        int itemViewType = getItemViewType(position);
        if (convertView == null || convertView.getTag(R.drawable.ic_launcher + itemViewType) == null) {
            convertView = inflater.inflate(
                    R.layout.mention_comment_normal_layout, parent, false);
            //msg layout
            holder.username = ViewUtility.findViewById(convertView,
                    R.id.username);
            TextPaint tp = holder.username.getPaint();
            if (tp != null) {
                tp.setFakeBoldText(true);
            }

            holder.root = ViewUtility.findViewById(convertView, R.id.root);
            holder.avatar = ViewUtility.findViewById(convertView, R.id.avatar);
            holder.ori_content = ViewUtility.findViewById(convertView, R.id.repost_content);
            holder.time = ViewUtility.findViewById(convertView, R.id.time);
            holder.source = ViewUtility.findViewById(convertView, R.id.source);
            holder.more = ViewUtility.findViewById(convertView, R.id.more);
            holder.content = ViewUtility.findViewById(convertView, R.id.content);

            //repost msg layout
            holder.ori_layout = ViewUtility.findViewById(convertView, R.id.repost_layout);
            holder.ori_comment = ViewUtility.findViewById(convertView, R.id.ori_comment);
            holder.ori_comment_flag = ViewUtility.findViewById(convertView, R.id.ori_comment_flag);
            holder.ori_content = ViewUtility.findViewById(convertView, R.id.repost_content);

            holder.root.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {

                }
            });

            //set tag for convertview
            convertView.setTag(R.drawable.ic_launcher + getItemViewType(position), holder);
            convertView.setTag(R.string.listview_index_tag,
                    R.drawable.ic_launcher + getItemViewType(position));
            tagIndexList.add(R.drawable.ic_launcher + getItemViewType(position));
        } else {
            holder = (ViewHolder) convertView
                    .getTag(R.drawable.ic_launcher + getItemViewType(position));
        }
        configViewFont(holder);
        bindViewData(holder, position);
        return convertView;
    }

    @SuppressLint("NewApi")
    private void bindViewData(final ViewHolder holder, final int position) {

        final CommentBean msg = list.get(position);
        UserBean user = msg.getUser();
        holder.more.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(getActivity(), holder.more);
                popupMenu.inflate(R.menu.menu_mention_comment);
                Menu menu = popupMenu.getMenu();
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        int id = item.getItemId();
                        if(id == R.id.copy){
                            ClipboardManager cmb = (ClipboardManager) getActivity().getSystemService(getActivity().CLIPBOARD_SERVICE);
                            cmb.setText(msg.getText().toString());
                            Toast.makeText(getActivity(), getActivity().getString(R.string.copy_successfully), Toast.LENGTH_SHORT).show();
                            popupMenu.dismiss();
                        }else if(id == R.id.reply){
                            popupMenu.dismiss();
                            getActivity().startActivity(WriteReplyActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), msg, GlobalContext.getInstance().getSpecialToken()));
                            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
                        }else if(id == R.id.show_ori){
                            popupMenu.dismiss();
                            getActivity().startActivity(WeiboDetailActivity.newIntent(getActivity(), GlobalContext.getInstance().getAccountBean(), msg.getStatus(), GlobalContext.getInstance().getSpecialToken()));
                            getActivity().overridePendingTransition(R.anim.push_left_in, R.anim.stay);
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
            if (!showOriStatus
                    && !SettingUtility.getEnableCommentRepostListAvatar()) {
                holder.avatar.setLayoutParams(new RelativeLayout.LayoutParams(
                        0, 0));
            } else {
                buildAvatar(holder.avatar, position, user);
            }
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

            if(msg.getReply_comment() != null){
                holder.ori_comment.setVisibility(View.VISIBLE);
                holder.ori_comment_flag.setVisibility(View.VISIBLE);
                holder.ori_comment.setText(msg.getReply_comment().getListViewSpannableString());
            }else{
                holder.ori_comment.setVisibility(View.GONE);
                holder.ori_comment_flag.setVisibility(View.GONE);
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
            if(msg.getReply_comment() != null){
                holder.ori_comment.setVisibility(View.VISIBLE);
                holder.ori_comment_flag.setVisibility(View.VISIBLE);
                holder.ori_comment.setText(msg.getReply_comment().getListViewSpannableString());
            }else{
                holder.ori_comment.setVisibility(View.GONE);
                holder.ori_comment_flag.setVisibility(View.GONE);
            }
            TimeLineUtility.addJustHighLightLinks(msg);
            holder.content.setText(msg.getListViewSpannableString());
        }
        holder.content.setMovementMethod(HackyMovementMethod.getInstance());

        holder.time.setTime(msg.getMills());
        if (holder.source != null) {
            holder.source.setText(msg.getSourceString());
        }

        holder.ori_layout.setVisibility(View.GONE);

        final MessageBean ori_msg = msg.getStatus();

        if (ori_msg != null && showOriStatus) {
            if (holder.ori_layout != null) {
                holder.ori_layout.setVisibility(View.VISIBLE);
            }

            buildRepostContent(msg, ori_msg, holder, position);
        } else {
            if (holder.ori_layout != null) {
                holder.ori_layout.setVisibility(View.GONE);
            }
        }

    }

    public void setIsFling(boolean isFling) {
        this.isFling = isFling;
    }

    @SuppressLint("NewApi")
    private void buildRepostContent(CommentBean msg,
                                    final MessageBean ori_msg, ViewHolder holder, int position) {
        holder.ori_layout.setVisibility(View.VISIBLE);

        if (!ori_msg.getId().equals((String) holder.ori_content.getTag())) {
            boolean haveCachedHeight = oriMsgHeights.get(msg.getIdLong()) != null;
            LayoutParams layoutParams = holder.ori_content
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
            holder.ori_content.requestLayout();
            holder.ori_content.setText(ori_msg
                    .getListViewSpannableString());

            if (!haveCachedHeight) {
                oriMsgHeights.append(msg.getIdLong(), layoutParams.height);
            }

            if (!haveCachedWidth) {
                oriMsgWidths.append(msg.getIdLong(), layoutParams.width);
            }

            holder.ori_content.setTag(ori_msg.getId());
        } else {
            holder.ori_content.setText(ori_msg
                    .getListViewSpannableString());
        }
        holder.ori_content.setMovementMethod(HackyMovementMethod.getInstance());

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

    public static class ViewHolder {
        LinearLayout root;
        TextView username;
        HackyTextView content;
        TimeTextView time;
        TextView source;
        ImageView more;
        TimeLineRoundAvatarImageView avatar;
        LinearLayout ori_layout;
        HackyTextView ori_comment;
        LinearLayout ori_comment_flag;
        HackyTextView ori_content;
    }

    private void configViewFont(ViewHolder holder) {
        int prefFontSizeSp = SettingUtility.getFontSize();
        float currentWidgetTextSizePx;

        currentWidgetTextSizePx = holder.content.getTextSize();

        if (Utility.sp2px(prefFontSizeSp - 2) != currentWidgetTextSizePx) {
            holder.content.setTextSize(prefFontSizeSp - 2);
        }
    }

}
