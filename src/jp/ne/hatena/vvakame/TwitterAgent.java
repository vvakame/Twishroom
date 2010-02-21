package jp.ne.hatena.vvakame;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.util.Xml;

public class TwitterAgent {

	public static final String TAG_USER = "user";
	public static final String TAG_NEXT_CURSOR = "next_cursor";
	public static final String TAG_ERROR = "error";

	public static final String NAME = "name";
	public static final String SCREEN_NAME = "screen_name";

	public static final long INITIAL_CURSOL = -1;
	public static final long END_CURSOL = 0;

	private static final int INIT_CAPACITY = 100;

	public class TwitterResponse {
		private List<UserModel> userList = null;
		private long nextCursor = -1;

		public List<UserModel> getUserList() {
			return userList;
		}

		public void setUserList(List<UserModel> userList) {
			this.userList = userList;
		}

		public long getNextCursor() {
			return nextCursor;
		}

		public void setNextCursor(long nextCursor) {
			this.nextCursor = nextCursor;
		}
	}

	public TwitterAgent() {
	}

	public TwitterResponse getFriendsStatus(Context con) throws IOException,
			TwitterException {
		return getFriendsStatus(con, -1);
	}

	public TwitterResponse getFriendsStatus(Context con, long cursor)
			throws IOException, TwitterException {
		URL url = null;
		try {
			String twitterId = PreferencesActivity.getTwitterId(con);
			if ("".equals(twitterId)) {
				// TODO UIスレッド以外から操作されている場合を考慮し例外を飛ばすべき
				throw new IllegalArgumentException();
			}
			url = new URL("http://api.twitter.com/1/statuses/friends/"
					+ twitterId + ".xml?cursor=" + String.valueOf(cursor));
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}

		InputStream isr = url.openConnection().getInputStream();

		XmlPullParser xmlParser = Xml.newPullParser();
		List<UserModel> friendsList = null;
		long nextCursor = -1;
		try {
			xmlParser.setInput(isr, null);

			int eventType = xmlParser.getEventType();
			UserModel currentFriend = null;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				String name = null;
				switch (eventType) {
				case XmlPullParser.START_DOCUMENT:
					friendsList = new ArrayList<UserModel>(INIT_CAPACITY);
					break;
				case XmlPullParser.START_TAG:
					name = xmlParser.getName();
					if (name.equalsIgnoreCase(TAG_USER)) {
						currentFriend = new UserModel();
					} else if (name.equalsIgnoreCase(TAG_NEXT_CURSOR)) {
						nextCursor = Long.parseLong(xmlParser.nextText());
					} else if (name.equalsIgnoreCase(TAG_ERROR)) {
						throw new TwitterException(xmlParser.nextText());
					} else if (currentFriend != null) {
						if (name.equalsIgnoreCase(NAME)) {
							currentFriend.setName(xmlParser.nextText());
						} else if (name.equalsIgnoreCase(SCREEN_NAME)) {
							currentFriend.setScreenName(xmlParser.nextText());
						}
					}
					break;
				case XmlPullParser.END_TAG:
					name = xmlParser.getName();
					if (name.equalsIgnoreCase(TAG_USER)) {
						friendsList.add(currentFriend);
						currentFriend = new UserModel();
					}
					break;
				}
				eventType = xmlParser.next();
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		}

		TwitterResponse res = new TwitterResponse();
		res.setUserList(friendsList);
		res.setNextCursor(nextCursor);

		return res;
	}
}
