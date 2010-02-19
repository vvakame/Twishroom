package jp.ne.hatena.vvakame;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jp.ne.hatena.vvakame.TwitterAgent.TwitterResponse;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class TwishroomActivity extends Activity implements TextWatcher,
		OnItemClickListener {
	// TODO OnItemLongClickListener を後日実装するように

	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();

		boolean prefixAtmark = false;
		String pullStr = "";
		String name = "";

		// Simejiから呼び出された時
		if (action != null && ACTION_INTERCEPT.equals(action)) {
			pullStr = intent.getStringExtra(REPLACE_KEY);
			if (pullStr != null && !pullStr.equals("")
					&& pullStr.charAt(0) == '@') {
				prefixAtmark = true;
			}
			UserModel user = detectUser(pullStr);

			if (user != null) {
				name = user.getName();
				if (prefixAtmark) {
					name = "@" + name;
				}

				pushToSimeji(name);
				return;
			}
		}

		// Simeji以外から呼出されたか、データがないとき
		UserDao dao = new UserDao(this);
		long count = dao.countAll();
		if (count != 0) {
			setContentView(R.layout.main);

			// EditText周りの処理
			EditText eText = (EditText) findViewById(R.id.editName);
			eText.addTextChangedListener(this);
			eText.setText(pullStr);

			// ListView周りの処理
			name = prefixAtmark ? pullStr.substring(1) : pullStr;
			List<UserModel> userList = dao.search(name);
			ArrayAdapter<UserModel> userAdapter = new TwitterAdapter(this,
					R.layout.view_for_list, userList);
			ListView listView = (ListView) findViewById(R.id.userList);
			listView.setAdapter(userAdapter);
			// TODO simejiからの呼び出しじゃない場合は挙動を変えるべき
			listView.setOnItemClickListener(this);
		} else {
			setContentView(R.layout.main_data_none);
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

		// TODO 画面の再描画を行うべき
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

	@Override
	public void afterTextChanged(Editable s) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		// ListView周りの処理
		String origStr = s.toString();
		boolean prefixAtmark = false;
		if (origStr != null && !origStr.equals("") && origStr.charAt(0) == '@') {
			prefixAtmark = true;
		}
		String name = prefixAtmark ? origStr.substring(1) : origStr;

		// TODO 入力ディレイが1秒あるまで再検索しない... とかするべき

		UserDao dao = new UserDao(this);

		List<UserModel> userList = dao.search(name);
		ArrayAdapter<UserModel> userAdapter = new TwitterAdapter(this,
				R.layout.view_for_list, userList);
		((ListView) findViewById(R.id.userList)).setAdapter(userAdapter);
	}

	@Override
	public void onItemClick(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
		TwitterAdapter adap = (TwitterAdapter) arg0.getAdapter();
		UserModel model = adap.getItem(arg2);
		EditText eText = (EditText) findViewById(R.id.editName);

		String origStr = eText.getText().toString();
		boolean prefixAtmark = false;
		if (origStr != null && !origStr.equals("") && origStr.charAt(0) == '@') {
			prefixAtmark = true;
		}

		String name = model.getScreenName();
		name = prefixAtmark ? "@" + name : name;

		pushToSimeji(name);
	}
}