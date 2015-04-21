package com.jasonchen.microlang.beans;


import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import com.jasonchen.microlang.utils.ObjectToStringUtility;

/**
 * jasonchen
 * 2015/04/10
 */
public class AccountBean implements Parcelable {

	// 用户的access_token、expires_time信息
	private String access_token;
	private long expires_time;
	// 黑魔法access_token、expires_time信息
	private String access_token_secret;
	private long expires_time_secret;
	// 用户信息Bean
	private UserBean info;
	private boolean black_magic;
	private int navigationPosition;

	public String getUid() {
		return (info != null ? info.getId() : "");
	}

	public String getUsernick() {
		return (info != null ? info.getScreen_name() : "");
	}

	public String getAvatar_url() {
		return (info != null ? info.getProfile_image_url() : "");
	}

	public String getAccess_token() {
		return access_token;
	}

	public void setAccess_token(String access_token) {
		this.access_token = access_token;
	}

	public long getExpires_time() {
		return expires_time;
	}

	public void setExpires_time(long expires_time) {
		this.expires_time = expires_time;
	}

	public String getAccess_token_secret() {
		return access_token_secret;
	}

	public void setAccess_token_secret(String access_token) {
		this.access_token_secret = access_token;
	}

	public long getExpires_time_secret() {
		return expires_time_secret;
	}

	public void setExpires_time_secret(long expires_time) {
		this.expires_time_secret = expires_time;
	}

	public UserBean getInfo() {
		return info;
	}

	public void setInfo(UserBean info) {
		this.info = info;
	}

	public boolean isBlack_magic() {
		return black_magic;
	}

	public void setBlack_magic(boolean black_magic) {
		this.black_magic = black_magic;
	}

	public int getNavigationPosition() {
		return navigationPosition;
	}

	public void setNavigationPosition(int navigationPosition) {
		this.navigationPosition = navigationPosition;
	}

	@Override
	public String toString() {
		return ObjectToStringUtility.toString(this);
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(access_token);
		dest.writeLong(expires_time);
		dest.writeString(access_token_secret);
		dest.writeLong(expires_time_secret);
		dest.writeInt(navigationPosition);
		dest.writeBooleanArray(new boolean[] { this.black_magic });
		dest.writeParcelable(info, flags);
	}

	public static final Creator<AccountBean> CREATOR = new Creator<AccountBean>() {
		public AccountBean createFromParcel(Parcel in) {
			AccountBean accountBean = new AccountBean();
			accountBean.access_token = in.readString();
			accountBean.expires_time = in.readLong();
			accountBean.access_token_secret = in.readString();
			accountBean.expires_time_secret = in.readLong();
			accountBean.navigationPosition = in.readInt();

			boolean[] booleans = new boolean[1];
			in.readBooleanArray(booleans);
			accountBean.black_magic = booleans[0];

			accountBean.info = in.readParcelable(UserBean.class
					.getClassLoader());

			return accountBean;
		}

		public AccountBean[] newArray(int size) {
			return new AccountBean[size];
		}
	};

	@Override
	public boolean equals(Object o) {

		return o instanceof AccountBean
				&& !TextUtils.isEmpty(((AccountBean) o).getUid())
				&& ((AccountBean) o).getUid().equalsIgnoreCase(getUid());
	}

	@Override
	public int hashCode() {
		return info.hashCode();
	}
}
