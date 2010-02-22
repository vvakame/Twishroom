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

public class TwishroomActivity extends Activity implements TextWatcher {
	// TODO OnItemLongClickListener を後日実装するように

	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	private static final int DIALOG_PROGRESS = 1;

	private boolean mDone = false;
	private ProgressDialog mProgDialog = null;
	private Handler mProgHandler = null;

	private FromSimejiImpl mSimejiImpl = new FromSimejiImpl();
	private OrdinaryImpl mOrdinaryImpl = new OrdinaryImpl();

	private String mEditStr = "";
	private boolean mFromSimeji = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		String action = intent.getAction();

		// Simejiから呼び出された時
		if (action != null && ACTION_INTERCEPT.equals(action)) {
			mEditStr = intent.getStringExtra(REPLACE_KEY);
			mFromSimeji = true;

			UserModel user = detectUser(mEditStr);

			if (user != null) {
				String name = user.getScreenName();
				name = isPrefixAtmark(mEditStr) ? addAtmark(name) : name;

				// TODO アクティビティ非表示のまま返すとsimejiが再度アクティブになるまで返した文字列が入力されない。
				pushToSimeji(name);
				return;
			}
		}

		// DBからデータ展開
		long count = new UserDao(this).countAll();
		if (count != 0) {
			setContentView(R.layout.main);

			// EditText周りの処理
			EditText eText = (EditText) findViewById(R.id.editName);
			eText.addTextChangedListener(this);
			eText.setText(mEditStr);

			// ListView周りの処理
			refreshListView(mEditStr);
			ListView listView = (ListView) findViewById(R.id.userList);

			// Simejiからの呼び出しじゃない場合は押しても挙動無し
			if (mFromSimeji) {
				listView.setOnItemClickListener(mSimejiImpl);
			} else {
				listView.setOnItemClickListener(mOrdinaryImpl);
			}
		} else {
			setContentView(R.layout.main_data_none);
		}
	}

	private boolean isPrefixAtmark(String str) {
		boolean ret = false;
		if (str != null && !str.equals("") && str.charAt(0) == '@') {
			ret = true;
		}
		return ret;
	}

	private String addAtmark(String str) {
		if (isPrefixAtmark(str)) {
			return str;
		} else {
			return "@" + str;
		}
	}

	private String removeAtmark(String str) {
		if (isPrefixAtmark(str)) {
			return str.substring(1);
		} else {
			return str;
		}
	}

	private void pushToSimeji(String result) {
		result = PreferencesActivity.isSignAtmark(this) ? addAtmark(result)
				: result;

		Intent data = new Intent();
		data.putExtra(REPLACE_KEY, result);
		setResult(RESULT_OK, data);
		finish();
	}

	private void refreshListView(String partOfScreenName) {
		String name = removeAtmark(partOfScreenName);

		UserDao dao = new UserDao(this);

		List<UserModel> userList = dao.search(name);
		ArrayAdapter<UserModel> userAdapter = new TwitterAdapter(this,
				R.layout.view_for_list, userList);
		ListView listView = (ListView) findViewById(R.id.userList);
		listView.setAdapter(userAdapter);
	}

	private UserModel detectUser(String name) {
		String strippedName = removeAtmark(name);

		UserDao dao = new UserDao(this);
		List<UserModel> userList = dao.search(strippedName);

		if (userList.size() == 1) {
			return userList.get(0);
		} else {
			return null;
		}
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

	private void refreshFriendsStatus() {
		// Handlerの準備
		mDone = false;
		mProgHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				if (mDone) {
					dismissDialog(DIALOG_PROGRESS);
					EditText eText = (EditText) findViewById(R.id.editName);
					if (eText == null) {
						setContentView(R.layout.main);
						eText = (EditText) findViewById(R.id.editName);
						ListView listView = (ListView) findViewById(R.id.userList);
						eText.setText(mEditStr);
						eText.addTextChangedListener(TwishroomActivity.this);
						if (mFromSimeji) {
							listView.setOnItemClickListener(mSimejiImpl);
						} else {
							listView.setOnItemClickListener(mOrdinaryImpl);
						}
					}
					refreshListView(mEditStr);
				} else {
					mProgHandler.sendEmptyMessageDelayed(0, 100);
				}
			}
		};

		showDialog(DIALOG_PROGRESS);
		mProgHandler.sendEmptyMessage(0);
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
						mDone = true;
						return;
					} catch (TwitterException e) {
						// TODO 例外処理をかなり気合い入れてやるべき
						e.printStackTrace();
						mDone = true;
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

				mDone = true;
			}
		}.start();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			mProgDialog = new ProgressDialog(this);
			mProgDialog.setTitle(getString(R.string.now_get_friends));
			mProgDialog.setMessage(getString(R.string.wait_a_moment));
			mProgDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			mProgDialog.setCancelable(false);

			return mProgDialog;
		default:
			break;
		}
		return null;
	}

	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		super.onPrepareDialog(id, dialog);
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
		mEditStr = s.toString();
		// TODO 入力ディレイが1秒あるまで再検索しない... とかしたほうがいいかも？使ってみて決める
		refreshListView(mEditStr);
	}

	class FromSimejiImpl implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			TwitterAdapter adap = (TwitterAdapter) arg0.getAdapter();
			UserModel model = adap.getItem(arg2);
			EditText eText = (EditText) findViewById(R.id.editName);

			String origStr = eText.getText().toString();

			String name = model.getScreenName();
			mEditStr = isPrefixAtmark(origStr) ? addAtmark(name) : name;

			pushToSimeji(mEditStr);
		}
	}

	class OrdinaryImpl implements OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			Toast.makeText(TwishroomActivity.this,
					R.string.not_called_by_simeji, Toast.LENGTH_SHORT).show();
		}
	}
}