package jp.ne.hatena.vvakame;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class UserDao {

	private DBHelper helper = null;

	public UserDao(Context con) {
		helper = new DBHelper(con);
	}

	public UserModel save(UserModel model) {
		SQLiteDatabase db = helper.getWritableDatabase();
		UserModel result = null;
		try {
			ContentValues values = new ContentValues();
			values.put(UserModel.COLUMN_SCREEN_NAME, model.getScreenName());
			values.put(UserModel.COLUMN_NAME, model.getName());

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
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(UserModel.TABLE_NAME, UserModel.COLUMN_NAME + "=?",
					new String[] { String.valueOf(model.getName()) });
		} finally {
			db.close();
		}
	}

	public void truncate(){
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.execSQL("delete from " + UserModel.TABLE_NAME);
		} finally {
			db.close();
		}
	}
	
	public UserModel load(Long rowId) {
		SQLiteDatabase db = helper.getReadableDatabase();
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
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = null;

		UserModel model = null;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null, UserModel.COLUMN_NAME
					+ "=?", new String[] { name }, null, null, null);
			cursor.moveToFirst();
			model = getUserModel(cursor);
		} finally {
			cursor.close();
			db.close();
		}
		return model;
	}

	public List<UserModel> search(String name) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = null;

		List<UserModel> userList = null;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null, UserModel.COLUMN_NAME
					+ " like ? || '%'", new String[] { name }, null, null,
					UserModel.COLUMN_NAME);

			userList = new ArrayList<UserModel>();
			while (cursor.moveToNext()) {
				UserModel model = getUserModel(cursor);
				userList.add(model);
			}
		} finally {
			cursor.close();
			db.close();
		}
		return userList;
	}

	public List<UserModel> list() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = null;

		List<UserModel> modelList;
		try {
			cursor = db.query(UserModel.TABLE_NAME, null, null, null, null,
					null, UserModel.COLUMN_ID);
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

	private UserModel getUserModel(Cursor cursor) {
		UserModel model = new UserModel();

		model.setRowId(cursor.getLong(0));
		model.setScreenName(cursor.getString(1));
		model.setName(cursor.getString(2));

		return model;
	}

}
