package jp.ne.hatena.vvakame;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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
import android.widget.AdapterView.OnItemLongClickListener;

public class TwishroomActivity extends Activity implements TextWatcher {

	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	private static final int DIALOG_PROGRESS = 1;

	private static final int MESSAGE_REFRESH_FOLLOWER = 1;
	private static final int MESSAGE_UPDATE_PROGRESS = 2;
	private static final int MESSAGE_ERROR_HANDLING = 3;

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
			NotCalledBySimejiImpl tmp = new NotCalledBySimejiImpl();
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

				case MESSAGE_UPDATE_PROGRESS:
					mProgDialog.setMessage(msg.obj.toString());
					break;

				case MESSAGE_ERROR_HANDLING:
					String msgStr = msg.obj.toString();
					Toast.makeText(TwishroomActivity.this, msgStr,
							Toast.LENGTH_LONG).show();
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
				// TODO save始める直前でもよい(Twitterのエラー)
				dao.truncate();

				ListView userListView = (ListView) findViewById(R.id.user_list);
				Map<String, UserModel> oldMap = null;
				if (userListView != null) {
					TwitterAdapter currentUserAdapter = (TwitterAdapter) userListView
							.getAdapter();
					oldMap = currentUserAdapter.getUserMap();
				}

				try {
					String screenName = PreferencesActivity
							.getTwitterId(TwishroomActivity.this);

					UserModel targetModel = agent.getShowUser(screenName);

					constructProgressMsg(count, targetModel).sendToTarget();

					long cur = TwitterAgent.INITIAL_CURSOL;
					while (cur != TwitterAgent.END_CURSOL) {
						TwitterResponse res = null;
						res = agent.getFriendsStatus(screenName, cur);
						List<UserModel> list = res.getUserList();
						cur = res.getNextCursor();

						count += list.size();

						for (UserModel model : list) {
							if (oldMap != null
									&& oldMap
											.containsKey(model.getScreenName())) {
								UserModel oldModel = oldMap.get(model
										.getScreenName());
								oldModel.updateFrom(model);
								model = oldModel;
							}
							dao.save(model);
						}

						constructProgressMsg(count, targetModel).sendToTarget();
					}

				} catch (IOException e) {
					// TODO もうちょっとユーザフレンドリなメッセージを出させてもよい
					String msgStr = e.getClass().getSimpleName() + ": "
							+ e.getMessage();
					Message msg = Message.obtain(mProgHandler,
							MESSAGE_ERROR_HANDLING, msgStr);
					msg.sendToTarget();
				} catch (TwitterException e) {
					// TODO もうちょっとユーザフレンドリなメッセージを出させてもよい
					Message msg = Message.obtain(mProgHandler,
							MESSAGE_ERROR_HANDLING, e.getMessage());
					msg.sendToTarget();
				}

				mDone = true;
			}

			private Message constructProgressMsg(long count,
					UserModel targetModel) {
				Message msg = Message.obtain(mProgHandler);
				msg.what = MESSAGE_UPDATE_PROGRESS;
				String msgTemplate = getString(R.string.progress_twitter_fetch);
				msg.obj = String.format(msgTemplate, count, targetModel
						.getFriendsCount());

				return msg;
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

	class FromSimejiImpl extends UsuallyImpl implements OnItemClickListener {
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
	}

	class NotCalledBySimejiImpl extends UsuallyImpl implements
			OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Toast.makeText(TwishroomActivity.this,
					R.string.not_called_by_simeji, Toast.LENGTH_SHORT).show();
		}
	}

	class UsuallyImpl implements OnItemLongClickListener {
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view,
				int position, long id) {
			TwitterAdapter adap = (TwitterAdapter) parent.getAdapter();
			mCurrentUser = adap.getItem(position);

			String message = null;
			if (UserModel.FAVORITE_ON.equals(mCurrentUser.getFavorite())) {
				message = getString(R.string.favorite_off_message);
			} else {
				message = getString(R.string.favorite_on_message);
			}

			message = String.format(message, mCurrentUser.getScreenName());

			mCurrentUser.toggleFavorite();
			UserDao dao = new UserDao(TwishroomActivity.this);
			dao.save(mCurrentUser);

			Toast.makeText(TwishroomActivity.this, message, Toast.LENGTH_LONG)
					.show();
			refreshListView(mEditStr);

			return true;
		}
	}
}