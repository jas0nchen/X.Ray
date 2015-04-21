package com.jasonchen.microlang.activitys;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.jasonchen.microlang.R;
import com.jasonchen.microlang.beans.AccountBean;
import com.jasonchen.microlang.beans.AtUserBean;
import com.jasonchen.microlang.dao.AtUserDao;
import com.jasonchen.microlang.database.AtUsersDBTask;
import com.jasonchen.microlang.exception.WeiboException;
import com.jasonchen.microlang.swipeback.app.SwipeBackActivity;
import com.jasonchen.microlang.tasks.MyAsyncTask;
import com.jasonchen.microlang.utils.ViewUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/17
 */
public class AtUserActivity extends SwipeBackActivity {

    private String token;
    private AccountBean accountBean;

    private ListView listView;
    private List<AtUserBean> atList = new ArrayList<AtUserBean>();
    private List<String> result = new ArrayList<String>();;
    private AtUserTask task;
    private MyAdapter adapter;

    public static Intent newIntent(Context context, AccountBean accountBean, String token){
        Intent intent = new Intent(context, AtUserActivity.class);
        intent.putExtra("account", accountBean);
        intent.putExtra("token", token);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        mLayout = R.layout.activity_at_user;
        super.onCreate(savedInstanceState);
        initView();
    }

    private void initView() {
        getSupportActionBar().setTitle("");
        if(getIntent() != null){
            token = getIntent().getStringExtra("token");
            accountBean = getIntent().getParcelableExtra("account");
        }
        listView = ViewUtility.findViewById(this, R.id.listView);
        adapter = new MyAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent();
                intent.putExtra("name", atList.get(i).getNickname());
                setResult(Activity.RESULT_OK, intent);
                AtUsersDBTask.add(atList.get(i), accountBean.getUid());
                finish();
            }
        });

        //get db cache
        atList = AtUsersDBTask.get(accountBean.getUid());

        for (AtUserBean b : atList) {
            result.add(b.getNickname());
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_activity_at_user, menu);
        initSearchView(menu);
        return super.onCreateOptionsMenu(menu);
    }

    private void initSearchView(Menu menu) {
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (!TextUtils.isEmpty(newText)) {
                    if (task != null) {
                        task.cancel(true);
                    }
                    task = new AtUserTask(newText);
                    task.executeOnExecutor(MyAsyncTask.THREAD_POOL_EXECUTOR);
                } else {
                    if (task != null) {
                        task.cancel(true);
                    }
                    atList.clear();
                    result.clear();
                    atList = AtUsersDBTask.get(accountBean.getUid());
                    for (AtUserBean b : atList) {
                        result.add(b.getNickname());
                    }
                    adapter.notifyDataSetChanged();
                }
                return false;
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.search:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    class AtUserTask extends MyAsyncTask<Void, List<AtUserBean>, List<AtUserBean>> {
        WeiboException e;
        String q;

        public AtUserTask(String q) {
            this.q = q;
        }

        @Override
        protected List<AtUserBean> doInBackground(Void... params) {
            AtUserDao dao = new AtUserDao(token, q);
            try {
                return dao.getUserInfo();
            } catch (WeiboException e) {
                this.e = e;
                cancel(true);
                return null;
            }
        }

        @Override
        protected void onPostExecute(List<AtUserBean> atUserBeans) {
            super.onPostExecute(atUserBeans);
            if (isCancelled())
                return;
            if (atUserBeans == null || atUserBeans.size() == 0) {
                result.clear();
                atList.clear();
                adapter.notifyDataSetChanged();
                return;
            }

            result.clear();
            for (AtUserBean b : atUserBeans) {
                if (b.getRemark().contains(q)) {
                    result.add(b.getNickname() + "(" + b.getRemark() + ")");
                } else {
                    result.add(b.getNickname());
                }
            }
            atList = atUserBeans;
            adapter.notifyDataSetChanged();
        }
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return result.size();
        }

        @Override
        public Object getItem(int position) {
            return result.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView,
                            ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(AtUserActivity.this).inflate(
                        R.layout.at_user_item_layout, null);
            }
            TextView name = (TextView) convertView.findViewById(R.id.name);
            name.setText(result.get(position));
            return convertView;
        }

    }

}
