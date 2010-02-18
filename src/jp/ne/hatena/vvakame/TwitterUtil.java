package jp.ne.hatena.vvakame;

import java.util.List;

import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.User;
import android.app.Activity;
import android.content.Context;

public class TwitterUtil {

	private static Twitter tw = new TwitterFactory().getInstance();

	private TwitterUtil() {
	}

	public static PagableResponseList<User> getFriends(Context con)
			throws TwitterException {
		String twitterId = PreferencesActivity.getTwitterId(con);

		PagableResponseList<User> user = tw.getFriendsStatuses(twitterId);

		return user;
	}

	public static List<FriendsModel> getFriendsData(Activity con) {
		/*
		 * con.getIntent().setData( Uri.parse("content://" +
		 * con.getString(R.string.provider_authorities)));
		 * 
		 * ContentValues values = new ContentValues(); values.put("name",
		 * "Pen"); values.put("description", "This is a pen");
		 * con.getContentResolver().insert(con.getIntent().getData(), values);
		 * 
		 * Cursor cur = con.managedQuery(con.getIntent().getData(), null, null,
		 * null, null); while (cur.moveToNext()) { Log.d(cur.getString(1),
		 * cur.getString(2));
		 * }
		 */

		FriendsDao dao = new FriendsDao(con);
		return dao.list();
	}
}
