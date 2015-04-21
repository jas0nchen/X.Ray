package com.jasonchen.microlang.beans;


import android.os.Parcel;
import android.os.Parcelable;

import com.jasonchen.microlang.utils.ObjectToStringUtility;

/**
 * jasonchen
 * 2015/04/10
 */
public class MessageReCmtCountBean implements Parcelable {

    private String id;
    private int comments = 0;
    private int reposts = 0;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeInt(comments);
        dest.writeInt(reposts);
    }

    public static final Creator<MessageReCmtCountBean> CREATOR =
            new Creator<MessageReCmtCountBean>() {
                public MessageReCmtCountBean createFromParcel(Parcel in) {
                    MessageReCmtCountBean messageReCmtCountBean = new MessageReCmtCountBean();
                    messageReCmtCountBean.id = in.readString();
                    messageReCmtCountBean.comments = in.readInt();
                    messageReCmtCountBean.reposts = in.readInt();
                    return messageReCmtCountBean;
                }

                public MessageReCmtCountBean[] newArray(int size) {
                    return new MessageReCmtCountBean[size];
                }
            };

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getComments() {
        return comments;
    }

    public void setComments(int comments) {
        this.comments = comments;
    }

    public int getReposts() {
        return reposts;
    }

    public void setReposts(int reposts) {
        this.reposts = reposts;
    }

    @Override
    public String toString() {
        return ObjectToStringUtility.toString(this);
    }
}