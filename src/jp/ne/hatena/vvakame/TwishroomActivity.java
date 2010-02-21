package jp.ne.hatena.vvakame;

import java.io.IOException;
import java.util.List;

import jp.ne.hatena.vvakame.TwitterAgent.TwitterResponse;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.Toast;
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
				name = user.getScreenName();
				if (prefixAtmark) {
					name = "@" + name;
				}

				// TODO アクティビティ非表示のまま返すとsimejiが再度アクティブになるまで返した文字列が入力されない。
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
			String twitterId = PreferencesActivity.getTwitterId(this);
			if ("".equals(twitterId)) {
				Toast.makeText(this, R.string.announce_first_step,
						Toast.LENGTH_SHORT).show();
				return false;
			}
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

	private boolean done = false;
	private ProgressDialog progDialog = null;
	private Handler progHandler = null;
	private static final int DIALOG_PROGRESS = 1;

	private void refreshFriendsStatus() {
		// Handlerの準備
		done = false;
		progHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (done) {
					dismissDialog(DIALOG_PROGRESS);
					EditText eText = (EditText) findViewById(R.id.editName);
					if (eText == null) {
						setContentView(R.layout.main);
						eText = (EditText) findViewById(R.id.editName);
						eText.addTextChangedListener(TwishroomActivity.this);
						ListView listView = (ListView) findViewById(R.id.userList);
						listView.setOnItemClickListener(TwishroomActivity.this);
					}
					String origStr = eText.getText().toString();
					refreshListView(origStr);
				} else {
					progHandler.sendEmptyMessageDelayed(0, 100);
				}
			}
		};

		showDialog(DIALOG_PROGRESS);
		progHandler.sendEmptyMessage(0);
		new Thread() {
			@Override
			public void run() {

				TwitterAgent agent = new TwitterAgent();
				long count = 0;

				UserDao dao = new UserDao(TwishroomActivity.this);
				dao.truncate();

				long cur = TwitterAgent.INITIAL_CURSOL;
				while (cur != TwitterAgent.END_CURSOL) {
					TwitterResponse res = null;
					try {
						res = agent.getFriendsStatus(TwishroomActivity.this,
								cur);
					} catch (IOException e) {
						// TODO 例外処理をかなり気合い入れてやるべき
						e.printStackTrace();
						done = true;
						return;
					} catch (TwitterException e) {
						// TODO 例外処理をかなり気合い入れてやるべき
						e.printStackTrace();
						done = true;
						return;
					}
					List<UserModel> list = res.getUserList();
					cur = res.getNextCursor();

					// TODO ProgressDialogの状態を更新し、進捗がわかるようにするべき
					count += list.size();

					for (UserModel model : list) {
						dao.save(model);
					}
				}

				done = true;
			}
		}.start();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			progDialog = new ProgressDialog(this);
			progDialog.setTitle(getString(R.string.now_get_friends));
			progDialog.setMessage(getString(R.string.wait_a_moment));
			progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progDialog.setCancelable(false);

			return progDialog;
		default:
			break;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
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
		refreshListView(s.toString());
	}

	private void refreshListView(String origStr) {
		boolean prefixAtmark = false;
		if (origStr != null && !origStr.equals("") && origStr.charAt(0) == '@') {
			prefixAtmark = true;
		}
		String name = prefixAtmark ? origStr.substring(1) : origStr;

		// TODO 入力ディレイが1秒あるまで再検索しない... とかしたほうがいいかも？使ってみて決める

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