package com.jasonchen.microlang.activitys;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.CommentDraftBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.services.SendCommentService;
import com.jasonchen.microlang.services.SendRepostService;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.TimeLineUtility;

/**
 * jasonchen
 * 2015/04/17
 */
public class WriteCommentActivity extends AbstractWriteActivity {

    private AccountBean accountBean;
    private MessageBean messageBean;

    public static Intent startBecauseSendFailed(Context context,
                                                AccountBean account, String content, MessageBean oriMsg,
                                                CommentDraftBean draft, boolean repost_too, String failedReason) {
        Intent intent = new Intent(context, WriteCommentActivity.class);
        intent.setAction(ACTION_SEND_FAILED);
        intent.putExtra("account", account);
        intent.putExtra("content", content);
        intent.putExtra("oriMsg", oriMsg);
        intent.putExtra("repost_too", repost_too);
        intent.putExtra("failedReason", failedReason);
        intent.putExtra("draft", draft);
        return intent;
    }

    public static Intent newIntent(Context context, AccountBean accountBean, MessageBean messageBean, String token){
        Intent intent = new Intent(context, WriteCommentActivity.class);
        intent.putExtra("account", accountBean);
        intent.putExtra("message", messageBean);
        intent.putExtra("token", token);
        intent.setAction(ACTION_NEW);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        getSupportActionBar().setTitle(getString(R.string.write_comment));
        repostMsgToo.setVisibility(View.VISIBLE);
        addPic.setVisibility(View.GONE);
        visib.setVisibility(View.GONE);

        String action = getIntent().getAction();
        token = GlobalContext.getInstance().getSpecialToken();
        if(ACTION_SEND_FAILED.equals(getIntent().getAction())){
            accountBean = getIntent().getParcelableExtra("account");
            messageBean = getIntent().getParcelableExtra("oriMsg");
            content.setHint(messageBean.getText());

            String contentStr = getIntent().getStringExtra("content");
            if(!TextUtils.isEmpty(contentStr)) {
                SpannableString spannableString = SpannableString.valueOf(contentStr);
                TimeLineUtility.addEmotions(spannableString);
                content.setText(spannableString);
                content.setSelection(content.length());
            }
            boolean repost_too = getIntent().getBooleanExtra("repost_too", false);
            repostToo = repost_too;
            if(repost_too){
                repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_yes));
            }else{
                repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_no));
            }
            if(!TextUtils.isEmpty(getIntent().getStringExtra("failedReason"))){
                Toast.makeText(WriteCommentActivity.this, getIntent().getStringExtra("failedReason"), Toast.LENGTH_SHORT).show();
            }
        }else if(ACTION_NEW.equals(action)){
            accountBean = getIntent().getParcelableExtra("account");
            messageBean = getIntent().getParcelableExtra("message");
            token = getIntent().getStringExtra("token");

            content.setHint(messageBean.getText());
        }
    }

    @Override
    protected void send() {
        if(canSend()){
            if(repostToo){
                repostMessage();
            }else{
                commentMessage();
            }
        }
    }

    private void commentMessage() {
        Intent intent = SendCommentService.newIntent(GlobalContext.getInstance().getAccountBean(), messageBean, content.getText().toString(), false);
        startService(intent);
        finishWithAnimation();
    }

    private void repostMessage() {
        Intent intent = SendRepostService.newIntent(WriteCommentActivity.this, messageBean, content.getText().toString(), true);
        startService(intent);
        finishWithAnimation();
    }

    @Override
    protected void atUser() {
        Intent intent = AtUserActivity.newIntent(WriteCommentActivity.this, accountBean, token);
        startActivityForResult(intent, AT_USER);
        openActivityWithAnimation();
    }

}
