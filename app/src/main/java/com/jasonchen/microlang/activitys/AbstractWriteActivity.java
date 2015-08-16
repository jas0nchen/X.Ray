package com.jasonchen.microlang.activitys;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.fragments.ConvertUriToCachePathAsyncTaskFragment;
import com.jasonchen.microlang.smilepicker.SmileyPicker;
import com.jasonchen.microlang.smilepicker.SmileyPickerUtility;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.utils.AndroidBug5497Workaround;
import com.jasonchen.microlang.utils.MythouCrashHandler;
import com.jasonchen.microlang.utils.TextNumLimitWatcher;
import com.jasonchen.microlang.utils.Utility;
import com.jasonchen.microlang.utils.ViewUtility;
import com.jasonchen.microlang.utils.image.ImageUtility;
import com.jasonchen.microlang.view.KeyboardControlEditText;
import com.jasonchen.microlang.view.SelectPictureDialog;

import java.io.File;

import me.drakeet.materialdialog.MaterialDialog;

/**
 * jasonchen
 * 2015/04/17
 */
public abstract class AbstractWriteActivity extends SwipeBackActivity implements View.OnClickListener, DialogInterface.OnClickListener {

    public static final String ACTION_NEW = "com.jasonchen.NEW";
    public static final String ACTION_DRAFT = "com.jasonchen.DRAFT";
    public static final String ACTION_SEND_FAILED = "com.jasonchen.SEND_FAILED";
    public static final String ACTION_AT = "com.jasonchen.AT";
    public static final String ACTION_FEED_BACK = "com.jasonchen.FEED_BACK";
    private static final String[] visiArr = {"所有人", "密友圈"};

    protected static final int COMMENT = 0;
    protected static final int REPOST = 1;
    private static final int RESULT_LOAD_IMAGE = 4;
    protected static final int AT_USER = 5;
    private static final int CAMERA_RESULT = 6;

    protected boolean hasPicture = false;
    protected boolean commentToo = false;
    protected boolean repostToo = false;
    protected Uri imageFileUri;
    protected AccountBean accountBean;
    protected String token;
    protected String picPath;

    protected LinearLayout root;
    protected KeyboardControlEditText content;
    protected ImageView statusImage;
    protected ImageView addPic;
    protected ImageView addTopic;
    protected ImageView addAtUser;
    protected ImageView addEmotion;
    protected ImageView commentBtn;
    protected ImageView repostBtn;
    protected SmileyPicker smileyPicker;
    protected LinearLayout container;
    protected TextView restNumber;
    protected LinearLayout commentMsgToo;
    protected LinearLayout repostMsgToo;
    protected static MenuItem send;
    protected int visibility = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_abstract_write;
        super.onCreate(savedInstanceState);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            AndroidBug5497Workaround.assistActivity(this);
        }
        Thread.setDefaultUncaughtExceptionHandler(new MythouCrashHandler());
        initView();
    }

    private void initView() {
        root = ViewUtility.findViewById(this, R.id.root);
        content = ViewUtility.findViewById(this, R.id.status_content);
        statusImage = ViewUtility.findViewById(this, R.id.status_image);
        addPic = ViewUtility.findViewById(this, R.id.editor_pic);
        addTopic = ViewUtility.findViewById(this, R.id.editor_topic);
        addAtUser = ViewUtility.findViewById(this, R.id.editor_at);
        addEmotion = ViewUtility.findViewById(this, R.id.editor_emoji);
        smileyPicker = ViewUtility.findViewById(this, R.id.smilepicker);
        container = ViewUtility.findViewById(this, R.id.container);
        restNumber = ViewUtility.findViewById(this, R.id.rest);
        commentMsgToo = ViewUtility.findViewById(this, R.id.comment_msg);
        repostMsgToo = ViewUtility.findViewById(this, R.id.repost_msg);
        commentBtn = ViewUtility.findViewById(this, R.id.comment_too);
        repostBtn = ViewUtility.findViewById(this, R.id.repost_too);

        addPic.setOnClickListener(this);
        addTopic.setOnClickListener(this);
        addAtUser.setOnClickListener(this);
        addEmotion.setOnClickListener(this);
        content.setOnClickListener(this);
        commentMsgToo.setOnClickListener(this);
        repostMsgToo.setOnClickListener(this);

        smileyPicker.setEditText(this, root, content);
        content.addTextChangedListener(new TextNumLimitWatcher(restNumber,
                content, this));

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, visiArr);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_abstract_write, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.send:
                send();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected abstract void send();

    /**
     * set comment or repost title
     * @param commentOrRepost
     *          COMMENT for comment and REPOST for repost
     */
    protected void setCommentOrRepostToo(int commentOrRepost){
        switch (commentOrRepost){
            case COMMENT:
                commentMsgToo.setVisibility(View.VISIBLE);
                break;
            case REPOST:
                repostMsgToo.setVisibility(View.VISIBLE);
                break;

        }
    }

    protected boolean getCommentToo(){
        return commentToo;
    }

    protected boolean getRepostToo(){
        return repostToo;
    }

    /**
     * set hint for content
     * @param hint
     */
    protected void setHint(String hint){
        content.setHint(hint);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.editor_pic:
                if (TextUtils.isEmpty(picPath)) {
                    addPic();
                } else {
                    showPic();
                }
                break;
            case R.id.editor_topic:
                content.append("##");
                content.setSelection(content.getText().toString().length() - 1);
                break;
            case R.id.editor_at:
                atUser();
                break;
            case R.id.editor_emoji:
                if (smileyPicker.isShown()) {
                    hideSmileyPicker(true);
                } else {
                    showSmileyPicker(SmileyPickerUtility.isKeyBoardShow(AbstractWriteActivity.this));
                }
                break;
            case R.id.status_content:
                if(smileyPicker.isShown()) {
                    hideSmileyPicker(true);
                }
                break;
            case R.id.comment_msg:
                commentToo = !commentToo;
                if(commentToo){
                    commentBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_yes));
                }else{
                    commentBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_no));
                }
                break;
            case R.id.repost_msg:
                repostToo = !repostToo;
                if(repostToo){
                    repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_yes));
                }else{
                    repostBtn.setImageDrawable(getResources().getDrawable(R.drawable.ic_switcher_no));
                }
                break;
        }
    }

    protected abstract void atUser();

    private void showSmileyPicker(boolean showAnimation) {
        this.smileyPicker.show(AbstractWriteActivity.this, showAnimation);
        lockContainerHeight(SmileyPickerUtility.getAppContentHeight(AbstractWriteActivity.this));
    }

    public void hideSmileyPicker(boolean showKeyBoard) {
        if (this.smileyPicker.isShown()) {
            if (showKeyBoard) {
                // this time softkeyboard is hidden
                LinearLayout.LayoutParams localLayoutParams = (LinearLayout.LayoutParams) this.container.getLayoutParams();
                localLayoutParams.height = smileyPicker.getTop();
                localLayoutParams.weight = 0.0F;
                this.smileyPicker.hide(AbstractWriteActivity.this);

                SmileyPickerUtility.showKeyBoard(content);
                content.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        unlockContainerHeightDelayed();
                    }
                }, 200L);
            } else {
                this.smileyPicker.hide(AbstractWriteActivity.this);
                unlockContainerHeightDelayed();
            }
        }
    }

    protected void createTmpUploadFileFromUri() {
        ConvertUriToCachePathAsyncTaskFragment fragment = ConvertUriToCachePathAsyncTaskFragment
                .newInstance(imageFileUri);
        getSupportFragmentManager().beginTransaction().add(fragment, "")
                .commit();
    }

    public void picConvertSucceedKK(String path) {
        if (TextUtils.isEmpty(content.getText().toString())) {
            content.setText(getString(R.string.share_pic));
            content.setSelection(content.getText().toString().length());
        }

        picPath = path;
        enablePicture();
    }


    protected void enablePicture() {
        Bitmap bitmap = ImageUtility.getWriteWeiboPictureThumblr(picPath);
        if (bitmap != null) {
            hasPicture = true;
            statusImage.setImageBitmap(bitmap);
            statusImage.setVisibility(View.VISIBLE);
        }
    }

    protected void disablePicture() {
        if (picPath != null) {
            new File(picPath).delete();
        }

        hasPicture = false;

        if (content.getText().toString()
                .equals(getString(R.string.share_pic))) {
            content.setText("");
        }
        statusImage.setVisibility(View.GONE);

    }

    private void addPic() {
        SelectPictureDialog.newInstance().show(getFragmentManager(), "");
    }

    private void showPic() {
        final MaterialDialog deletePictureDialog = new MaterialDialog(AbstractWriteActivity.this);
        deletePictureDialog.setTitle(getString(R.string.notice)).setMessage(getString(R.string.delete_picture)).setPositiveButton(getString(R.string.confirm), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePictureDialog.dismiss();
                deletePicture();
            }
        }).setNegativeButton(getString(R.string.cancel), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deletePictureDialog.dismiss();
            }
        }).show();
    }

    public void deletePicture() {
        imageFileUri = null;
        picPath = null;
        disablePicture();
    }

    private void lockContainerHeight(int paramInt) {
        LinearLayout.LayoutParams localLayoutParams = (LinearLayout.LayoutParams) this.container.getLayoutParams();
        localLayoutParams.height = paramInt;
        localLayoutParams.weight = 0.0F;
    }

    public void unlockContainerHeightDelayed() {
        ((LinearLayout.LayoutParams) AbstractWriteActivity.this.container.getLayoutParams()).weight = 1.0F;
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CAMERA_RESULT:
                    createTmpUploadFileFromUri();
                    break;
                case RESULT_LOAD_IMAGE:
                    imageFileUri = intent.getData();
                    createTmpUploadFileFromUri();
                    break;
                case AT_USER:
                    String name = intent.getStringExtra("name");
                    content.append("@" + name + " ");
                    break;
            }
        }
    }

    protected boolean canSend() {

        boolean haveContent = !TextUtils.isEmpty(content.getText().toString());
        boolean haveToken = !TextUtils.isEmpty(token);

        int sum = Utility.length(content.getText().toString());
        int num = 140 - sum;

        boolean contentNumBelow140 = (num >= 0);

        if (haveContent && haveToken && contentNumBelow140) {
            return true;
        } else {
            if (!haveContent && !haveToken) {
                Toast.makeText(this, getString(R.string.content_cant_be_empty_and_dont_have_account), Toast.LENGTH_SHORT)
                        .show();
            } else if (!haveContent) {
                content.setError(getString(R.string.content_cant_be_empty));
            } else if (!haveToken) {
                Toast.makeText(this, getString(R.string.dont_have_account), Toast.LENGTH_SHORT).show();
            }

            if (!contentNumBelow140) {
                content.setError(getString(R.string.content_words_number_too_many));
            }

        }

        return false;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case 0:
                imageFileUri = getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        new ContentValues());
                if (imageFileUri != null) {
                    Intent i = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    i.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, imageFileUri);
                    if (Utility.isIntentSafe(AbstractWriteActivity.this, i)) {
                        startActivityForResult(i, CAMERA_RESULT);
                    } else {
                        Toast.makeText(AbstractWriteActivity.this,
                                getString(R.string.dont_have_camera_app), Toast.LENGTH_SHORT)
                                .show();
                    }
                } else {
                    Toast.makeText(AbstractWriteActivity.this,
                            getString(R.string.cant_insert_album),
                            Toast.LENGTH_SHORT).show();
                }
                break;

            case 1:
                Intent i = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, RESULT_LOAD_IMAGE);
                break;
            default:
                break;
        }
    }
}
