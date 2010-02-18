package jp.ne.hatena.vvakame;

import java.io.Serializable;

public class FriendsModel implements Serializable {
	private static final long serialVersionUID = 1L;

	public static final String TABLE_NAME = "twitter";

	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "name";
	public static final String COLUMN_SCREEN_NAME = "screen_name";

	private Long rowId = null;
	private String name = null;
	private String screenName = null;

	public Long getRowId() {
		return rowId;
	}

	public void setRowId(Long rowId) {
		this.rowId = rowId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getScreenName() {
		return screenName;
	}

	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}

	@Override
	public String toString() {
		StringBuilder stb = new StringBuilder();

		stb.append("rowId:").append(rowId).append(", ");
		stb.append("screenName:").append(screenName).append(", ");
		stb.append("name:").append(name).append(".");

		return stb.toString();
	}
}
