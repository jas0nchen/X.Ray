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
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.RepostDraftBean;
import com.jasonchen.microlang.services.SendRepostService;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.TimeLineUtility;

/**
 * jasonchen
 * 2015/04/17
 */
public class WriteRepostActivity extends AbstractWriteActivity {

    private AccountBean accountBean;
    private MessageBean messageBean;

    public static Intent startBecauseSendFailed(Context context,
                                                AccountBean accountBean, String content, MessageBean oriMsg, boolean is_comment,
                                                RepostDraftBean repostDraftBean, String failedReason) {
        Intent intent = new Intent(context, WriteRepostActivity.class);
        intent.setAction(ACTION_SEND_FAILED);
        intent.putExtra("account", accountBean);
        intent.putExtra("content", content);
        intent.putExtra("oriMsg", oriMsg);
        intent.putExtra("isComment", is_comment);
        intent.putExtra("failedReason", failedReason);
        intent.putExtra("repostDraftBean", repostDraftBean);
        return intent;
    }

    public static Intent newIntent(Context context, AccountBean accountBean, MessageBean messageBean, String token){
        Intent intent = new Intent(context, WriteRepostActivity.class);
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
        getSupportActionBar().setTitle(getString(R.string.write_repost));
        commentMsgToo.setVisibility(View.VISIBLE);
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
            boolean comment_too = getIntent().getBooleanExtra("isComment", false);
            commentToo = comment_too;
            if(comment_too){
                repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_yes));
            }else{
                repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_no));
            }
            if(!TextUtils.isEmpty(getIntent().getStringExtra("failedReason"))){
                Toast.makeText(WriteRepostActivity.this, getIntent().getStringExtra("failedReason"), Toast.LENGTH_SHORT).show();
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
            repostMessage();
        }
    }

    private void repostMessage() {
        Intent intent = SendRepostService.newIntent(WriteRepostActivity.this, messageBean, content.getText().toString(), commentToo);
        startService(intent);
        finishWithAnimation();
    }

    @Override
    protected void atUser() {
        Intent intent = AtUserActivity.newIntent(WriteRepostActivity.this, accountBean, token);
        startActivityForResult(intent, AT_USER);
        openActivityWithAnimation();
    }

}
