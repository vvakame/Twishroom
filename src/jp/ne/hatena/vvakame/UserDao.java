package jp.ne.hatena.vvakame;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDao {

	private static final String ORDER_BY = UserModel.COLUMN_FAVORITE
			+ " desc, upper(" + UserModel.COLUMN_SCREEN_NAME + ")";

	private DBHelper mHelper = null;

	public UserDao(Context con) {
		mHelper = new DBHelper(con);
	}

	public UserModel save(UserModel model) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		UserModel result = null;
		try {
			ContentValues values = new ContentValues();
			values.put(UserModel.COLUMN_SCREEN_NAME, model.getScreenName());
			values.put(UserModel.COLUMN_NAME, model.getName());
			values.put(UserModel.COLUMN_FAVORITE, model.getFavorite());

			Long rowId = model.getRowId();
			// IDがnullの場合はinsert
			if (rowId == null) {
				rowId = db.insert(UserModel.TABLE_NAME, null, values);
			} else {
				db.update(UserModel.TABLE_NAME, values, UserModel.COLUMN_ID
						+ "=?", new String[] { String.valueOf(rowId) });
			}
			result = load(rowId);
		} finally {
			db.close();
		}
		return result;
	}

	public void delete(UserModel model) {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		try {
			db.delete(UserModel.TABLE_NAME,
					UserModel.COLUMN_SCREEN_NAME + "=?", new String[] { String
							.valueOf(model.getName()) });
		} finally {
			db.close();
		}
	}

	public void truncate() {
		SQLiteDatabase db = mHelper.getWritableDatabase();
		try {
			db.execSQL("delete from " + UserModel.TABLE_NAME);
		} finally {
			db.close();
		}
	}

	public UserModel load(Long rowId) {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = null;

		UserModel model = null;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null, UserModel.COLUMN_ID
					+ "=?", new String[] { String.valueOf(rowId) }, null, null,
					null);
			cursor.moveToFirst();
			model = getUserModel(cursor);
		} finally {
			cursor.close();
			db.close();
		}
		return model;
	}

	public UserModel load(String name) {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = null;

		UserModel model = null;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null,
					UserModel.COLUMN_SCREEN_NAME + "=?", new String[] { name },
					null, null, null);
			cursor.moveToFirst();
			model = getUserModel(cursor);
		} finally {
			cursor.close();
			db.close();
		}
		return model;
	}

	public List<UserModel> search(String name) {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = null;

		List<UserModel> userList = null;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null,
					UserModel.COLUMN_SCREEN_NAME + " like ? || '%'",
					new String[] { name }, null, null, ORDER_BY);

			userList = new ArrayList<UserModel>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				userList.add(getUserModel(cursor));
				cursor.moveToNext();
			}
		} finally {
			cursor.close();
			db.close();
		}
		return userList;
	}

	public List<UserModel> list() {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = null;

		List<UserModel> modelList;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null, null, null, null,
					null, ORDER_BY);
			modelList = new ArrayList<UserModel>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				modelList.add(getUserModel(cursor));
				cursor.moveToNext();
			}
		} finally {
			cursor.close();
			db.close();
		}
		return modelList;
	}

	public long countAll() {
		SQLiteDatabase db = mHelper.getReadableDatabase();
		Cursor cursor = null;
		long count = 0;

		try {
			cursor = db.rawQuery(
					"select count(*) from " + UserModel.TABLE_NAME, null);
			cursor.moveToFirst();
			count = cursor.getLong(0);
		} finally {
			cursor.close();
			db.close();
		}
		return count;
	}

	private UserModel getUserModel(Cursor cursor) {
		UserModel model = new UserModel();

		int rowId = cursor.getColumnIndex(UserModel.COLUMN_ID);
		int screenName = cursor.getColumnIndex(UserModel.COLUMN_SCREEN_NAME);
		int name = cursor.getColumnIndex(UserModel.COLUMN_NAME);
		int favorite = cursor.getColumnIndex(UserModel.COLUMN_FAVORITE);

		model.setRowId(cursor.getLong(rowId));
		model.setScreenName(cursor.getString(screenName));
		model.setName(cursor.getString(name));
		model.setFavorite(cursor.getString(favorite));

		return model;
	}
}
