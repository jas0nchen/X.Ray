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
import com.jasonchen.microlang.beans.CommentBean;
import com.jasonchen.microlang.beans.MessageBean;
import com.jasonchen.microlang.beans.ReplyDraftBean;
import com.jasonchen.microlang.services.SendReplyToCommentService;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.TimeLineUtility;

/**
 * jasonchen
 * 2015/04/17
 */
public class WriteReplyActivity extends AbstractWriteActivity {

    private AccountBean accountBean;
    private CommentBean commentBean;

    public static Intent startBecauseSendFailed(Context context,
                                                AccountBean account, String content, CommentBean oriMsg,
                                                ReplyDraftBean replyDraftBean, String repostContent,
                                                String failedReason) {
        Intent intent = new Intent(context, WriteReplyActivity.class);
        intent.setAction(ACTION_SEND_FAILED);
        intent.putExtra("account", account);
        intent.putExtra("content", content);
        intent.putExtra("oriMsg", oriMsg);
        intent.putExtra("failedReason", failedReason);
        intent.putExtra("repostContent", repostContent);
        intent.putExtra("replyDraftBean", replyDraftBean);
        return intent;
    }

    public static Intent newIntent(Context context, AccountBean accountBean, CommentBean commentBean, String token){
        Intent intent = new Intent(context, WriteReplyActivity.class);
        intent.putExtra("account", accountBean);
        intent.putExtra("message", commentBean);
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
        getSupportActionBar().setTitle(getString(R.string.write_reply));
        repostMsgToo.setVisibility(View.VISIBLE);
        addPic.setVisibility(View.GONE);
        visib.setVisibility(View.GONE);

        String action = getIntent().getAction();
        token = GlobalContext.getInstance().getSpecialToken();
        if(ACTION_SEND_FAILED.equals(getIntent().getAction())){
            accountBean = getIntent().getParcelableExtra("account");
            commentBean = getIntent().getParcelableExtra("oriMsg");
            content.setHint(commentBean.getText());
            String contentStr = getIntent().getStringExtra("content");
            if(!TextUtils.isEmpty(contentStr)) {
                SpannableString spannableString = SpannableString.valueOf(contentStr);
                TimeLineUtility.addEmotions(spannableString);
                content.setText(spannableString);
                content.setSelection(content.length());
            }
            String repostContent = getIntent().getStringExtra("repostContent");
            if(!TextUtils.isEmpty(repostContent)){
                repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_yes));
                repostToo = true;
            }else{
                repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_no));
            }

            if(!TextUtils.isEmpty(getIntent().getStringExtra("failedReason"))){
                Toast.makeText(WriteReplyActivity.this, getIntent().getStringExtra("failedReason"), Toast.LENGTH_SHORT).show();
            }
        }else if(ACTION_NEW.equals(action)){
            accountBean = getIntent().getParcelableExtra("account");
            commentBean = getIntent().getParcelableExtra("message");
            token = getIntent().getStringExtra("token");

            content.setHint(commentBean.getText());
        }
    }

    @Override
    protected void send() {
        if(canSend()){
            if(repostToo){
                repostComment();
            }else {
                replyComment();
            }
        }
    }

    private void repostComment() {
        Intent intent = SendReplyToCommentService.newIntent(accountBean, commentBean, content.getText().toString(), content.getText().toString());
        startService(intent);
        finishWithAnimation();
    }

    private void replyComment() {
        Intent intent = SendReplyToCommentService.newIntent(accountBean, commentBean, content.getText().toString(), "");
        startService(intent);
        finishWithAnimation();
    }

    @Override
    protected void atUser() {
        Intent intent = AtUserActivity.newIntent(WriteReplyActivity.this, accountBean, token);
        startActivityForResult(intent, AT_USER);
        openActivityWithAnimation();
    }

}
