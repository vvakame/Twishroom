package jp.ne.hatena.vvakame;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper implements DBHelperIF {

	private static final String DB_CREATE = "create table "
			+ FriendsModel.TABLE_NAME + " (" + FriendsModel.COLUMN_ID
			+ " integer primary key autoincrement not null, "
			+ FriendsModel.COLUMN_SCREEN_NAME + " text not null, "
			+ FriendsModel.COLUMN_NAME + " text not null);";

	public DBHelper(Context con) {
		super(con, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DB_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// まだ、ない。
	}
}
