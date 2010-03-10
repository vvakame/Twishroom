package jp.ne.hatena.vvakame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Twitterのデータ UserModelのAdapter
 * 
 * @author vvakame
 */
public class TwitterAdapter extends ArrayAdapter<UserModel> {

	private Context con = null;
	private List<UserModel> mUserList = null;
	private Map<String, UserModel> mUserMap = null;

	/**
	 * {@inheritDoc}
	 */
	public TwitterAdapter(Context context, int textViewResourceId,
			List<UserModel> objects) {
		super(context, textViewResourceId, objects);
		con = context;
		mUserList = objects;
		mUserMap = new HashMap<String, UserModel>();

		for (UserModel model : mUserList) {
			mUserMap.put(model.getScreenName(), model);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) con
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.view_for_list, null);
		}

		UserModel model = mUserList.get(position);

		if (convertView instanceof TextView) {
			TextView tView = (TextView) convertView;
			tView.setText(model.getScreenName());

			if (UserModel.FAVORITE_ON.equals(model.getFavorite())) {
				tView.setTextColor(con.getResources()
						.getColor(R.color.favorite));
			} else {
				tView.setTextColor(con.getResources().getColor(R.color.normal));
			}
		}
		return convertView;
	}

	/**
	 * 内部で保持しているユーザリストを返す 編集しないこと
	 * 
	 * @return ユーザリスト
	 */
	public List<UserModel> getUserList() {
		return mUserList;
	}

	/**
	 * 内部で保持しているユーザリストを返す 編集しないこと
	 * 
	 * @return ユーザのマップ
	 */
	public Map<String, UserModel> getUserMap() {
		return mUserMap;
	}

	/**
	 * 指定されたユーザに完全に一致するデータを持っていたらそれを返す
	 * 
	 * @param screenName
	 *            ユーザ名
	 * @return ユーザデータ
	 */
	public UserModel getUser(String screenName) {
		return mUserMap.get(screenName);
	}
}
