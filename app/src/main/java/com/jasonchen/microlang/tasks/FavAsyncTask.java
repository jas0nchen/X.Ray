
package com.jasonchen.microlang.tasks;

import android.widget.Toast;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.FavBean;
import com.jasonchen.microlang.dao.FavDao;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.utils.GlobalContext;

/**
 * jasonchen
 * 2015/04/19
 */
public class FavAsyncTask extends MyAsyncTask<Void, FavBean, FavBean> {

    private String token;
    private String id;
    private WeiboException e;

    public FavAsyncTask(String token, String id) {
        this.token = token;
        this.id = id;
    }

    @Override
    protected FavBean doInBackground(Void... params) {
        FavDao dao = new FavDao(token, id);
        try {
            return dao.favIt();
        } catch (WeiboException e) {
            this.e = e;
            cancel(true);
            return null;
        }
    }

    @Override
    protected void onCancelled(FavBean favBean) {
        super.onCancelled(favBean);
        if (favBean == null && this.e != null)
            Toast.makeText(GlobalContext.getInstance(), e.getError(), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onPostExecute(FavBean favBean) {
        super.onPostExecute(favBean);
        if (favBean != null) {
            Toast.makeText(GlobalContext.getInstance(), GlobalContext.getInstance().getString(R.string.favorite_success),
                    Toast.LENGTH_SHORT).show();
        }

    }
}
