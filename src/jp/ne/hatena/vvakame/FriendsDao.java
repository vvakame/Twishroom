package jp.ne.hatena.vvakame;

import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class FriendsDao {

	private DBHelper helper = null;

	public FriendsDao(Context con) {
		helper = new DBHelper(con);
	}

	public FriendsModel save(FriendsModel model) {
		SQLiteDatabase db = helper.getWritableDatabase();
		FriendsModel result = null;
		try {
			ContentValues values = new ContentValues();
			values.put(FriendsModel.COLUMN_SCREEN_NAME, model.getScreenName());
			values.put(FriendsModel.COLUMN_NAME, model.getName());

			Long rowId = model.getRowId();
			// IDがnullの場合はinsert
			if (rowId == null) {
				rowId = db.insert(FriendsModel.TABLE_NAME, null, values);
			} else {
				db.update(FriendsModel.TABLE_NAME, values,
						FriendsModel.COLUMN_ID + "=?", new String[] { String
								.valueOf(rowId) });
			}
			result = load(rowId);
		} finally {
			db.close();
		}
		return result;
	}

	public void delete(FriendsModel model) {
		SQLiteDatabase db = helper.getWritableDatabase();
		try {
			db.delete(FriendsModel.TABLE_NAME, FriendsModel.COLUMN_ID + "=?",
					new String[] { String.valueOf(model.getRowId()) });
		} finally {
			db.close();
		}
	}

	public FriendsModel load(Long rowId) {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = null;

		FriendsModel model = null;
		try {
			cursor = db.query(FriendsModel.TABLE_NAME, null,
					FriendsModel.COLUMN_ID + "=?", new String[] { String
							.valueOf(rowId) }, null, null, null);
			cursor.moveToFirst();
			model = getFriendsModel(cursor);
		} finally {
			cursor.close();
			db.close();
		}
		return model;
	}

	public List<FriendsModel> list() {
		SQLiteDatabase db = helper.getReadableDatabase();
		Cursor cursor = null;

		List<FriendsModel> modelList;
		try {
			cursor = db.query(FriendsModel.TABLE_NAME, null, null, null, null,
					null, FriendsModel.COLUMN_ID);
			modelList = new ArrayList<FriendsModel>();
			cursor.moveToFirst();
			while (!cursor.isAfterLast()) {
				modelList.add(getFriendsModel(cursor));
				cursor.moveToNext();
			}
		} finally {
			cursor.close();
			db.close();
		}
		return modelList;
	}

	private FriendsModel getFriendsModel(Cursor cursor) {
		FriendsModel model = new FriendsModel();

		model.setRowId(cursor.getLong(0));
		model.setScreenName(cursor.getString(1));
		model.setName(cursor.getString(2));

		return model;
	}

}
