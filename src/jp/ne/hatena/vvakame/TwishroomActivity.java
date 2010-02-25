package jp.ne.hatena.vvakame;

import java.io.IOException;
import java.util.List;

import jp.ne.hatena.vvakame.TwitterAgent.TwitterResponse;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

public class TwishroomActivity extends Activity implements TextWatcher {

	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	private static final int DIALOG_PROGRESS = 1;
	private static final int DIALOG_CONTENTS = 2;

	private static final int MESSAGE_REFRESH_FOLLOWER = 1;

	private boolean mDone = false;
	private ProgressDialog mProgDialog = null;
	private Handler mProgHandler = null;

	private OnItemClickListener mOnClickImpl = null;
	private OnItemLongClickListener mOnLongClickImple = null;

	private String mEditStr = "";
	private boolean mFromSimeji = false;

	private UserModel mCurrentUser = null;

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

				pushToSimeji(name, true);
				return;
			}
		}

		// 呼び出し元によってリスナーを振り分ける。
		if (mFromSimeji) {
			FromSimejiImpl tmp = new FromSimejiImpl();
			mOnClickImpl = tmp;
			mOnLongClickImple = tmp;
		} else {
			OrdinaryImpl tmp = new OrdinaryImpl();
			mOnClickImpl = tmp;
			mOnLongClickImple = tmp;
		}

		// DBからデータ展開
		long count = new UserDao(this).countAll();
		if (count != 0) {
			setContentView(R.layout.main);

			// EditText周りの処理
			EditText eText = (EditText) findViewById(R.id.edit_name);
			eText.addTextChangedListener(this);
			eText.setText(mEditStr);

			// ListView周りの処理
			refreshListView(mEditStr);
			ListView listView = (ListView) findViewById(R.id.user_list);
			listView.setOnItemClickListener(mOnClickImpl);
			listView.setOnItemLongClickListener(mOnLongClickImple);
		} else {
			setContentView(R.layout.main_data_none);
		}

		createHandler();
	}

	private void createHandler() {
		mProgHandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				super.handleMessage(msg);
				switch (msg.what) {
				case MESSAGE_REFRESH_FOLLOWER:

					if (mDone) {
						dismissDialog(DIALOG_PROGRESS);
						EditText eText = (EditText) findViewById(R.id.edit_name);
						if (eText == null) {
							setContentView(R.layout.main);

							eText = (EditText) findViewById(R.id.edit_name);
							eText.setText(mEditStr);
							eText
									.addTextChangedListener(TwishroomActivity.this);

							ListView listView = (ListView) findViewById(R.id.user_list);
							listView.setOnItemClickListener(mOnClickImpl);
							listView
									.setOnItemLongClickListener(mOnLongClickImple);
						}
						refreshListView(mEditStr);
					} else {
						mProgHandler.sendEmptyMessageDelayed(
								MESSAGE_REFRESH_FOLLOWER, 100);
					}

					break;

				default:
					break;
				}
			}
		};
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

	private void pushToSimeji(String result, boolean applyOption) {
		if (applyOption) {
			result = PreferencesActivity.isSignAtmark(this) ? addAtmark(result)
					: result;
			result = PreferencesActivity.isAddWhitespace(this) ? result + " "
					: result;
		}

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
		ListView listView = (ListView) findViewById(R.id.user_list);
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
		mDone = false;

		showDialog(DIALOG_PROGRESS);
		mProgHandler.sendEmptyMessage(MESSAGE_REFRESH_FOLLOWER);

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
		case DIALOG_CONTENTS:
			AlertDialog.Builder altBuilder = new AlertDialog.Builder(this);

			LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.long_click_dialog, null);

			TextView screenNameText = (TextView) layout
					.findViewById(R.id.screen_name);
			screenNameText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mCurrentUser == null) {
						throw new IllegalStateException();
					}
					pushToSimeji(mCurrentUser.getScreenName(), true);
				}
			});

			TextView nameText = (TextView) layout.findViewById(R.id.name);
			nameText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mCurrentUser == null) {
						throw new IllegalStateException();
					}
					pushToSimeji(mCurrentUser.getName(), false);
				}
			});

			TextView favoriteText = (TextView) layout
					.findViewById(R.id.favorite);
			favoriteText.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					if (mCurrentUser == null) {
						throw new IllegalStateException();
					}
					// TODO まじめに実装する
					pushToSimeji("test", false);
				}
			});

			altBuilder.setView(layout);

			return altBuilder.create();
		default:
			break;
		}
		return null;
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

	class FromSimejiImpl implements OnItemClickListener,
			OnItemLongClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			TwitterAdapter adap = (TwitterAdapter) parent.getAdapter();
			UserModel model = adap.getItem(position);
			EditText eText = (EditText) findViewById(R.id.edit_name);

			String origStr = eText.getText().toString();

			String name = model.getScreenName();
			mEditStr = isPrefixAtmark(origStr) ? addAtmark(name) : name;

			pushToSimeji(mEditStr, true);
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			TwitterAdapter adap = (TwitterAdapter) parent.getAdapter();
			mCurrentUser = adap.getItem(position);
			showDialog(DIALOG_CONTENTS);
			return true;
		}
	}

	class OrdinaryImpl implements OnItemClickListener, OnItemLongClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			notCalledBySimejiToast();
		}

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			notCalledBySimejiToast();
			return true;
		}

		private void notCalledBySimejiToast() {
			Toast.makeText(TwishroomActivity.this,
					R.string.not_called_by_simeji, Toast.LENGTH_SHORT).show();
		}
	}
}