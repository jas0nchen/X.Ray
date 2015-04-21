package com.jasonchen.microlang.activitys;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.TextUtils;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.GeoBean;
import com.jasonchen.microlang.beans.StatusDraftBean;
import com.jasonchen.microlang.services.SendWeiboService;
import com.jasonchen.microlang.utils.GlobalContext;
import com.jasonchen.microlang.utils.TimeLineUtility;

/**
 * jasonchen
 * 2015/04/17
 */
public class WriteWeiboActivity extends AbstractWriteActivity {

    private StatusDraftBean statusDraftBean;
    private GeoBean geoBean;

    public static Intent startBecauseSendFailed(Context context,
                                                AccountBean accountBean, String content, String picPath,
                                                GeoBean geoBean, int visible, StatusDraftBean statusDraftBean,
                                                String failedReason) {
        Intent intent = new Intent(context, WriteWeiboActivity.class);
        intent.setAction(ACTION_SEND_FAILED);
        intent.putExtra("account", accountBean);
        intent.putExtra("content", content);
        intent.putExtra("failedReason", failedReason);
        intent.putExtra("picPath", picPath);
        intent.putExtra("geoBean", geoBean);
        intent.putExtra("visible", visible);
        intent.putExtra("statusDraftBean", statusDraftBean);
        return intent;
    }

    public static Intent newIntent(Context context, AccountBean accountBean, String name){
        Intent intent = new Intent(context, WriteWeiboActivity.class);
        intent.setAction(ACTION_AT);
        intent.putExtra("account", accountBean);
        intent.putExtra("name", name);
        return intent;
    }

    public static Intent newIntent(Context context, AccountBean accountBean){
        Intent intent = new Intent(context, WriteWeiboActivity.class);
        intent.setAction(ACTION_NEW);
        intent.putExtra("account", accountBean);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        getSupportActionBar().setTitle(getString(R.string.write_status));
        String action = getIntent().getAction();
        String type = getIntent().getType();
        token = GlobalContext.getInstance().getSpecialToken();
        if (action.equals(Intent.ACTION_SEND) && !TextUtils.isEmpty(type)) {
            if ("text/plain".equals(type)) {
                handleSendText(getIntent());
            } else if (type.startsWith("image/")) {
                handleSendImage(getIntent());
            }
        } else if(ACTION_SEND_FAILED.equals(getIntent().getAction())){
            accountBean = getIntent().getParcelableExtra("account");
            String contentStr = getIntent().getStringExtra("content");
            if(!TextUtils.isEmpty(contentStr)) {
                SpannableString spannableString = SpannableString.valueOf(contentStr);
                TimeLineUtility.addEmotions(spannableString);
                content.setText(spannableString);
                content.setSelection(content.length());
            }
            if(!TextUtils.isEmpty(getIntent().getStringExtra("picPath"))){
                picPath = getIntent().getStringExtra("picPath");
                enablePicture();
                hasPicture = true;
            }
            visibility = getIntent().getIntExtra("visible", 0);
            setWeiboVisible(visibility);
            if(!TextUtils.isEmpty(getIntent().getStringExtra("failedReason"))){
                Toast.makeText(WriteWeiboActivity.this, getIntent().getStringExtra("failedReason"), Toast.LENGTH_SHORT).show();
            }
        }else if(ACTION_AT.equals(getIntent().getAction())){
            accountBean = getIntent().getParcelableExtra("account");
            String name = getIntent().getStringExtra("name");
            content.setText("@" + name +" ");
            content.setSelection(content.getText().toString().length());
        }else if(ACTION_NEW.equals(action)){
            accountBean = getIntent().getParcelableExtra("account");
        }

    }

    private void setWeiboVisible(int visibility) {
        if(visibility == 0){
            visib.setSelection(0);
        }else{
            visib.setSelection(1);
        }
    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (!TextUtils.isEmpty(sharedText)) {
            content.setText(sharedText);
            content.setSelection(content.getText().toString().length());
        }
    }

    private void handleSendImage(Intent intent) {
        handleSendText(intent);

        Uri sharedImageUri = (Uri) intent.getParcelableExtra(Intent.EXTRA_STREAM);
        if (sharedImageUri != null) {
            imageFileUri = sharedImageUri;
            createTmpUploadFileFromUri();
        }
    }

    @Override
    protected void send() {
        if(canSend()){
            executeTask();
        }
    }

    @Override
    protected void atUser() {
        Intent intent = AtUserActivity.newIntent(WriteWeiboActivity.this, accountBean, token);
        startActivityForResult(intent, AT_USER);
        openActivityWithAnimation();
    }

    private void executeTask() {
        Intent intent = new Intent(WriteWeiboActivity.this,
                SendWeiboService.class);
        intent.putExtra("token", token);
        intent.putExtra("visible", visibility);
        intent.putExtra("picPath", picPath);
        intent.putExtra("account", accountBean);
        intent.putExtra("content", content.getText().toString());
        intent.putExtra("geo", geoBean);
        intent.putExtra("draft", statusDraftBean);
        startService(intent);
        finishWithAnimation();
    }

}
