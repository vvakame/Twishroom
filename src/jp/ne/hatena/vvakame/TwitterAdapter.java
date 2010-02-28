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

public class TwitterAdapter extends ArrayAdapter<UserModel> {

	private Context con = null;
	private List<UserModel> mUserList = null;
	private Map<String, UserModel> mUserMap = null;

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

	public List<UserModel> getUserList() {
		return mUserList;
	}

	public Map<String, UserModel> getUserMap() {
		return mUserMap;
	}

	public UserModel getUser(String screenName) {
		return mUserMap.get(screenName);
	}
}
