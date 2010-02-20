package jp.ne.hatena.vvakame;

import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

public class PreferencesActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	public static final int NUM_OF_PEOPLE_DEFAULT = 8;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preference);
	}

	public static String getTwitterId(Context con) {
		String key = "twitter_id";
		return PreferenceManager.getDefaultSharedPreferences(con).getString(
				key, "");
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return true;
	}
}
