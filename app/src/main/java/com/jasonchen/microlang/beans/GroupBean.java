package com.jasonchen.microlang.beans;


import android.os.Parcel;
import android.os.Parcelable;

import com.jasonchen.microlang.utils.ObjectToStringUtility;

/**
 * jasonchen
 * 2015/04/10
 */
public class GroupBean implements Parcelable {

	//这里定义了三个变量id、idstr和name来说明读和写的顺序要一致  
	private String id;
	private String idstr;
	private String name;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		// 把javanbean中的数据写到Parcel。先写id然后写idstr、再写name  
		dest.writeString(id);
		dest.writeString(idstr);
		dest.writeString(name);
	}

	// 添加一个静态成员,名为CREATOR,该对象实现了Parcelable.Creator接口
	public static final Creator<GroupBean> CREATOR = new Creator<GroupBean>() {
		public GroupBean createFromParcel(Parcel in) {
			 // 从Parcel中读取数据，返回GroupBean对象 
			GroupBean groupBean = new GroupBean();
			groupBean.id = in.readString();
			groupBean.idstr = in.readString();
			groupBean.name = in.readString();
			return groupBean;
		}

		public GroupBean[] newArray(int size) {
			return new GroupBean[size];
		}
	};

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getIdstr() {
		return idstr;
	}

	public void setIdstr(String idstr) {
		this.idstr = idstr;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return ObjectToStringUtility.toString(this);
	}
}
