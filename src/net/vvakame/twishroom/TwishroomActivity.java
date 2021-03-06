package net.vvakame.twishroom;

import java.util.List;
import java.util.Map;

import net.vvakame.twishroom.TwitterAgent.TwitterResponse;
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

/**
 * TwishroomのメインとなるActivityです。
 * 
 * @author vvakame
 */
public class TwishroomActivity extends Activity implements TextWatcher {

	private static final String ACTION_INTERCEPT = "com.adamrocker.android.simeji.ACTION_INTERCEPT";
	private static final String REPLACE_KEY = "replace_key";

	/** フォロり更新中ダイアログ */
	private static final int DIALOG_PROGRESS = 1;

	/** フォロり更新中の処理監視のMessage */
	private static final int MESSAGE_REFRESH_FOLLOWER = 1;
	/** フォロり更新中の進捗更新のMessage */
	private static final int MESSAGE_UPDATE_PROGRESS = 2;
	/** フォロり更新中にエラーが発生した場合のMessage */
	private static final int MESSAGE_ERROR_HANDLING = 3;

	/** フォロり更新完了のフラグ */
	private boolean mDone = false;
	/** フォロり更新中のダイアログ */
	private ProgressDialog mProgDialog = null;

	/** ListViewのアイテムクリックイベントリスナーの実装 */
	private OnItemClickListener mOnClickImpl = null;
	/** ListViewのアイテムロングクリックイベントリスナーの実装 */
	private OnItemLongClickListener mOnLongClickImple = null;

	/** 現在編集中の文字列を保持 */
	private String mEditStr = "";
	/** Simejiからの呼び出しか否かを保持 */
	private boolean mFromSimeji = false;

	/** 現在処理対象としているユーザ */
	private UserModel mCurrentUser = null;

	/** フォロリ更新周りの挙動制御用Handler */
	private Handler mProgHandler = new Handler() {
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
						eText.addTextChangedListener(TwishroomActivity.this);

						ListView listView = (ListView) findViewById(R.id.user_list);
						listView.setOnItemClickListener(mOnClickImpl);
						listView.setOnItemLongClickListener(mOnLongClickImple);
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

	/**
	 * {@inheritDoc}
	 */
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
	}

	/**
	 * 渡された文字列の先頭に"@"がついているかを返す
	 * 
	 * @param str
	 *            対象文字列
	 * @return strの先頭が"@"であればtrue, そうでなければfalse
	 */
	private boolean isPrefixAtmark(String str) {
		boolean ret = false;
		if (str != null && !str.equals("") && str.charAt(0) == '@') {
			ret = true;
		}
		return ret;
	}

	/**
	 * 渡された文字列の先頭に"@"をつけて返す 既に付加されている場合は何もせず返す
	 * 
	 * @param str
	 *            対象文字列
	 * @return strの頭に"@"を付加した文字列
	 */
	private String addAtmark(String str) {
		if (isPrefixAtmark(str)) {
			return str;
		} else {
			return "@" + str;
		}
	}

	/**
	 * 渡された文字列の先頭に"@"がついていれば取り除いて返す 最初から付加されていなければ何もせず返す
	 * 
	 * @param str
	 *            対象文字列
	 * @return strの先頭から"@"を取り除いた文字列
	 */
	private String removeAtmark(String str) {
		if (isPrefixAtmark(str)) {
			return str.substring(1);
		} else {
			return str;
		}
	}

	/**
	 * Simejiに文字列を返す "@"付加や" "付加の設定があれば付加してから送る TwishroomActivityは終了する
	 * 
	 * @param result
	 *            返したい文字列
	 * @param applyOption
	 *            オプションを反映するか否か
	 */
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

	/**
	 * 渡された文字列を元に、DBより該当のユーザ達を検索して返す
	 * 
	 * @param partOfScreenName
	 *            検索したいユーザ(先頭一致)
	 */
	private void refreshListView(String partOfScreenName) {
		String name = removeAtmark(partOfScreenName);

		UserDao dao = new UserDao(this);

		List<UserModel> userList = dao.search(name);
		ArrayAdapter<UserModel> userAdapter = new TwitterAdapter(this,
				R.layout.view_for_list, userList);
		ListView listView = (ListView) findViewById(R.id.user_list);
		listView.setAdapter(userAdapter);
	}

	/**
	 * DBより指定のユーザを探索する 複数見つかった場合nullを返す
	 * 
	 * @param name
	 *            検索したいユーザ(先頭一致)
	 * @return 1名見つかった場合はそのユーザ それ以外の場合はnull
	 */
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
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

	/**
	 * フォロり更新の実行 別スレッドを作成し非同期に更新を行う 更新動作中はプログレスダイアログを表示し、ユーザを待たせる
	 */
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

				} catch (Exception e) {
					// TODO もうちょっとユーザフレンドリなメッセージを出させてもよい
					String msgStr = e.getClass().getSimpleName() + ": "
							+ e.getMessage();
					Message msg = Message.obtain(mProgHandler,
							MESSAGE_ERROR_HANDLING, msgStr);
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

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_PROGRESS:
			mProgDialog = new ProgressDialog(this);
			onPrepareDialog(id, mProgDialog);

			return mProgDialog;
		default:
			break;
		}
		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch (id) {
		case DIALOG_PROGRESS:
			ProgressDialog progDialog = (ProgressDialog) dialog;
			progDialog.setTitle(getString(R.string.now_get_friends));
			progDialog.setMessage(getString(R.string.wait_a_moment));
			progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progDialog.setCancelable(false);

			break;
		default:
			break;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void afterTextChanged(Editable s) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		mEditStr = s.toString();
		// TODO 入力ディレイが1秒あるまで再検索しない... とかしたほうがいいかも？使ってみて決める
		refreshListView(mEditStr);
	}

	/**
	 * Simejiから呼び出された場合のイベントリスナの実装
	 * 
	 * @author vvakame
	 */
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

	/**
	 * Simeji以外から呼び出された場合のイベントリスナの実装
	 * 
	 * @author vvakame
	 */
	class NotCalledBySimejiImpl extends UsuallyImpl implements
			OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			Toast.makeText(TwishroomActivity.this,
					R.string.not_called_by_simeji, Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * イベントリスナの実装 呼び出し元に依存しない
	 * 
	 * @author vvakame
	 */
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