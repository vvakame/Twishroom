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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

public class TwishroomActivity extends Activity implements OnClickListener {
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

			for (UserModel model : friendsList) {
				dao.save(model);
			}

			Object obj = dao.list();
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

	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.twit:
			Toast.makeText(this, "Tweet!!", Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}
	}
}