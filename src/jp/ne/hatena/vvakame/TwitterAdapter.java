package jp.ne.hatena.vvakame;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TwitterAdapter extends ArrayAdapter<UserModel> {

	private List<UserModel> userList = null;

	public TwitterAdapter(Context context, int textViewResourceId,
			List<UserModel> objects) {
		super(context, textViewResourceId, objects);
		userList = objects;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		if (view instanceof TextView) {
			TextView tView = (TextView) view;
			tView.setText(userList.get(position).getScreenName());
		}
		return view;
	}
}
