package com.jasonchen.microlang.beans;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * jasonchen
 * 2015/04/10
 */
public class AdBean implements Parcelable {

    private String id;
    private String mark;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(mark);
    }

    public static final Creator<AdBean> CREATOR =
            new Creator<AdBean>() {
                public AdBean createFromParcel(Parcel in) {
                    AdBean adBean = new AdBean();
                    adBean.id = in.readString();
                    adBean.mark = in.readString();
                    return adBean;
                }

                public AdBean[] newArray(int size) {
                    return new AdBean[size];
                }
            };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMark() {
        return mark;
    }

    public void setMark(String mark) {
        this.mark = mark;
    }
}
