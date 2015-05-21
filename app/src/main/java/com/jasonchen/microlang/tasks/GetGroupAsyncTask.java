
package com.jasonchen.microlang.tasks;

import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.FavBean;
import com.jasonchen.microlang.beans.GroupListBean;
import com.jasonchen.microlang.dao.FavDao;
import com.jasonchen.microlang.dao.FriendGroupDao;
import com.jasonchen.microlang.dao.GroupListDao;
import com.jasonchen.microlang.database.GroupDBTask;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.GlobalContext;

/**
 * jasonchen
 * 2015/04/23
 */
public class GetGroupAsyncTask extends MyAsyncTask<Void, GroupListBean, GroupListBean> {

    private String token;
    private WeiboException e;

    public GetGroupAsyncTask(String token) {
        this.token = token;
    }

    @Override
    protected GroupListBean doInBackground(Void... params) {
        FriendGroupDao dao = new FriendGroupDao(token);
        try {
            return dao.getGroup();
        } catch (WeiboException e) {
            this.e = e;
            cancel(true);
            return null;
        }
    }

    @Override
    protected void onCancelled(GroupListBean groupListBean) {
        super.onCancelled(groupListBean);
        if (groupListBean == null && this.e != null)
            Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(GroupListBean groupListBean) {
        super.onPostExecute(groupListBean);
        if (groupListBean != null) {
            GroupDBTask.update(groupListBean, GlobalContext.getInstance().getCurrentAccountId());
        }

    }
}
