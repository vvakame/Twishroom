package jp.ne.hatena.vvakame;

import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TwitterAdapter extends ArrayAdapter<UserModel> {

	private Context con = null;
	private List<UserModel> mUserList = null;

	public TwitterAdapter(Context context, int textViewResourceId,
			List<UserModel> objects) {
		super(context, textViewResourceId, objects);
		con = context;
		mUserList = objects;
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
				tView.setTextColor(Color.YELLOW);
			} else {
				tView.setTextColor(Color.WHITE);
			}
		}
		return convertView;
	}

	public List<UserModel> getUserList() {
		return mUserList;
	}
}
