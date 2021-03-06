package com.jasonchen.microlang.beans;


import android.os.Parcel;
import android.os.Parcelable;

import com.jasonchen.microlang.utils.ObjectToStringUtility;

import java.util.ArrayList;
import java.util.List;

/**
 * jasonchen
 * 2015/04/10
 */
public class GroupListBean implements Parcelable {

    private List<GroupBean> lists = new ArrayList<GroupBean>();
    private String total_number = "0";

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(total_number);
        dest.writeTypedList(lists);
    }

    public static final Creator<GroupListBean> CREATOR =
            new Creator<GroupListBean>() {
                public GroupListBean createFromParcel(Parcel in) {
                    GroupListBean groupListBean = new GroupListBean();

                    groupListBean.total_number = in.readString();

                    groupListBean.lists = new ArrayList<GroupBean>();
                    in.readTypedList(groupListBean.lists, GroupBean.CREATOR);

                    return groupListBean;
                }

                public GroupListBean[] newArray(int size) {
                    return new GroupListBean[size];
                }
            };

    public List<GroupBean> getLists() {
        return lists;
    }

    public void setLists(List<GroupBean> lists) {
        this.lists = lists;
    }

    public String getTotal_number() {
        return total_number;
    }

    public void setTotal_number(String total_number) {
        this.total_number = total_number;
    }

    @Override
    public String toString() {
        return ObjectToStringUtility.toString(this);
    }
}
