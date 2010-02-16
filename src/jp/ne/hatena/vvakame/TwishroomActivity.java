package jp.ne.hatena.vvakame;

import java.util.ArrayList;
import java.util.List;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
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

	private Twitter tw = null;
	private int[] friendsIds = null;

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

	private void testTwitter() throws TwitterException {
		if (tw == null) {
			String twitterId = PreferencesActivity.getTwitterId(this);
			String twitterPw = PreferencesActivity.getTwitterPassword(this);
			tw = new TwitterFactory().getInstance(twitterId, twitterPw);
		}
		if (friendsIds == null) {
			friendsIds = tw.getFollowersIDs().getIDs();
		}

		List<User> userList = new ArrayList<User>();
		for (int id : friendsIds) {
			User user = tw.showUser(id);
			userList.add(user);
			user.getScreenName();
		}
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
			try {
				testTwitter();
			} catch (TwitterException e) {
				e.printStackTrace();
			}
			break;
		default:
			break;
		}
	}
}