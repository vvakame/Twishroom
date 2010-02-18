package jp.ne.hatena.vvakame;

import twitter4j.PagableResponseList;
import twitter4j.TwitterException;
import twitter4j.User;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class TwishroomActivity extends Activity implements OnClickListener {
	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	private static final int MENU_PREFERENCES = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();

		if (action != null && ACTION_INTERCEPT.equals(action)) {
			// Simejiから呼び出された時
			String pullStr = intent.getStringExtra(REPLACE_KEY);

			pushToSimeji(pullStr.replaceAll("test", "HelloSimeji!"));
		} else {
			// Simeji以外から呼出された時
			setContentView(R.layout.main);
			((Button) findViewById(R.id.twit)).setOnClickListener(this);
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
		MenuItem pref = menu.add(Menu.NONE, MENU_PREFERENCES, Menu.NONE,
				getText(R.string.preferences));

		pref.setIcon(android.R.drawable.ic_menu_preferences);

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		boolean ret = true;
		Intent intent = null;
		switch (item.getItemId()) {
		case MENU_PREFERENCES:
			intent = new Intent(this, PreferencesActivity.class);
			startActivity(intent);
			break;
		default:
			ret = super.onOptionsItemSelected(item);
			break;
		}
		return ret;
	}

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.twit:
			PagableResponseList<User> list = null;
			try {
				list = TwitterUtil.getFriends(this);
			} catch (TwitterException e) {
				e.printStackTrace();
				return;
			}
			
			FriendsDao dao = new FriendsDao(this);
			
			for(User user : list){
				FriendsModel model = new FriendsModel();
				model.setScreenName(user.getScreenName());
				model.setName(user.getName());
				dao.save(model);
			}
			
			Object obj = TwitterUtil.getFriendsData(this);
			break;
		default:
			break;
		}
	}
}