package net.vvakame.twishroom;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

/**
 * 設定画面のActivity
 * 
 * @author vvakame
 */
public class PreferencesActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.layout.preferences);
	}

	/**
	 * 設定されているTwitterIDを取得
	 * 
	 * @param con
	 *            Context
	 * @return TwitterID
	 */
	public static String getTwitterId(Context con) {
		String key = "twitter_id";
		return PreferenceManager.getDefaultSharedPreferences(con).getString(
				key, "");
	}

	/**
	 * 先頭に"@"を付加するかの設定
	 * 
	 * @param con
	 *            Context
	 * @return trueの場合付加する falseの場合付加しない
	 */
	public static boolean isSignAtmark(Context con) {
		String key = "always_sign_atmark";
		return PreferenceManager.getDefaultSharedPreferences(con).getBoolean(
				key, false);
	}

	/**
	 * 末尾に" "を付加するかの設定
	 * 
	 * @param con
	 *            Context
	 * @return trueの場合付加する falseの場合付加しない
	 */
	public static boolean isAddWhitespace(Context con) {
		String key = "add_whitespace";
		return PreferenceManager.getDefaultSharedPreferences(con).getBoolean(
				key, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return true;
	}
}
