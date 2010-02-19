package jp.ne.hatena.vvakame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.vvakame.TwitterAgent.TwitterResponse;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class TwishroomActivity extends Activity {
	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();

		if (action != null && ACTION_INTERCEPT.equals(action)) {
			// Simejiから呼び出された時
			String pullStr = intent.getStringExtra(REPLACE_KEY);

			UserModel user = detectUser(pullStr);

			if (user != null) {
				String name = user.getName();
				if (pullStr != null && !pullStr.equals("")
						&& pullStr.charAt(0) == '@') {
					name = "@" + name;
				}

				pushToSimeji(name);
			}
		} else {
			// Simeji以外から呼出された時
			setContentView(R.layout.main);
		}
	}

	private void pushToSimeji(String result) {
		Intent data = new Intent();
		data.putExtra(REPLACE_KEY, result);
		setResult(RESULT_OK, data);
		finish();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = true;
		Intent intent = null;
		switch (item.getItemId()) {
		case R.id.refresh_friends:
			refreshFriendsStatus();
			break;

		case R.id.preferences:
			intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
			break;

		default:
			ret = super.onOptionsItemSelected(item);
			break;
		}
		return ret;
	}

	private void refreshFriendsStatus() {
		List<UserModel> friendsList = new ArrayList<UserModel>();

		TwitterAgent agent = new TwitterAgent();

		long cur = TwitterAgent.INITIAL_CURSOL;
		while (cur != TwitterAgent.END_CURSOL) {
			TwitterResponse res = null;
			try {
				res = agent.getFriendsStatus(this, cur);
			} catch (IOException e) {
				e.printStackTrace();
			}
			List<UserModel> list = res.getUserList();
			cur = res.getNextCursor();

			friendsList.addAll(list);
		}

		UserDao dao = new UserDao(this);
		dao.truncate();
		for (UserModel model : friendsList) {
			dao.save(model);
		}
	}

	private UserModel detectUser(String name) {
		if (name == null || name.equals("")) {
			return null;
		}

		// @ が先頭につけられていることを考慮する。
		boolean atmarkFirst = name.charAt(0) == '@' ? true : false;
		String strippedName = atmarkFirst ? name.substring(1) : name;

		UserDao dao = new UserDao(this);
		List<UserModel> userList = dao.search(strippedName);

		if (userList.size() == 1) {
			return userList.get(0);
		} else {
			return null;
		}
	}
}