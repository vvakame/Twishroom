package jp.ne.hatena.vvakame;

/**
 * Twitterとお話中にエラーが発生した場合投げられる例外
 * 
 * @author vvakame
 */
public class TwitterException extends Exception {
	private static final long serialVersionUID = 1L;

	public TwitterException() {
		super();
	}

	public TwitterException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public TwitterException(String detailMessage) {
		super(detailMessage);
	}

	public TwitterException(Throwable throwable) {
		super(throwable);
	}
}
