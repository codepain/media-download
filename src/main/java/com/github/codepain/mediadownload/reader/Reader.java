package com.github.codepain.mediadownload.reader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Random;

import org.jsoup.Connection.Response;

import com.github.codepain.mediadownload.MediaDownload;
import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventSource;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.listener.Listener;

import org.jsoup.Jsoup;

/**
 * <p>
 * A reader interprets a web page and fetches the {@linkplain Downloadable
 * downloadable items}.
 * </p>
 * <p>
 * To retrieve an appropriate reader use {@link MediaDownload#connect(URL)}.
 * </p>
 * 
 * @author codepain
 * 
 **/
public abstract class Reader implements EventSource, Listener {
	
	private static final String[] USER_AGENTS = {
		"Mozilla/5.0 (Windows NT 6.1; WOW64; Trident/7.0; AS; rv:11.0) like Gecko",
		"Mozilla/5.0 (compatible, MSIE 11, Windows NT 6.3; Trident/7.0;  rv:11.0) like Gecko",
		"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/42.0.2311.135 Safari/537.36 Edge/12.246",
		"Mozilla/5.0 (compatible; MSIE 10.0; Windows NT 7.0; InfoPath.3; .NET CLR 3.1.40767; Trident/6.0; en-IN)",
		"Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
		"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2227.0 Safari/537.36",
		"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36",
		"Mozilla/5.0 (Windows NT 6.1; WOW64; rv:40.0) Gecko/20100101 Firefox/40.1",
		"Mozilla/5.0 (Windows NT 6.3; rv:36.0) Gecko/20100101 Firefox/36.0"
	};

	protected final URL url;

	protected ReaderOptions options = new ReaderOptions();

	private Listener listener;
	
	private final Random random = new Random();

	/**
	 * <p>
	 * Constructs a {@link Reader} for the specified {@link URL}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the web page containing the
	 *            {@linkplain Downloadable downloadable item}
	 */
	protected Reader(final URL url) {
		this.url = url;
	}

	/**
	 * <p>
	 * Fetches the web page and interprets it, i.e. creating a
	 * {@linkplain Downloadable downloadable item} of it.
	 * </p>
	 * 
	 * @throws IOException
	 *             If reading/interpreting the web page fails
	 */
	public Downloadable read() throws IOException {
		final Downloadable downloadable = fetchDownloadable();
		if (downloadable == null) {
			return null;
		}

		return downloadable.listener(listener);
	}

	/**
	 * <p>
	 * Gets called internally by {@link #read()} to actually do the work. The
	 * method reads the web page identified by {@link #url} and interprets it,
	 * returning an according {@linkplain Downloadable downloadable item}.
	 * Interpreting the web page may include {@linkplain #download(URL) loading}
	 * other pages.
	 * </p>
	 * <p>
	 * The {@link #options} shall be respected when processing the web page.
	 * </p>
	 */
	protected abstract Downloadable fetchDownloadable() throws IOException;

	@Override
	public Reader listener(final Listener listener) {
		this.listener = listener;
		return this;
	}

	@Override
	public void event(Event event) {
		onEvent(event);

		if (listener != null) {
			if (!(event.source() instanceof Downloadable)) {
				event = event.withSource(this);
			}
			listener.event(event);
		}
	}

	/**
	 * <p>
	 * Gets called in case of an {@linkplain Event event}.
	 * </p>
	 * <p>
	 * Handling the {@linkplain Listener listener chain} is already done in
	 * {@link #event(Event)}.
	 * </p>
	 */
	protected abstract void onEvent(final Event event);

	/**
	 * <p>
	 * Trigger an {@linkplain Event event} of the specified
	 * {@linkplain EventType type} and with the specified
	 * {@linkplain Event#eventObject() event object}.
	 * </p>
	 * 
	 * @param type
	 *            The {@linkplain EventType event type}
	 * @param data
	 *            The {@linkplain Event#eventObject() event object}, which may
	 *            be {@code null}
	 */
	protected void trigger(final EventType type, final Object data) {
		if (listener != null) {
			listener.event(new Event(this, type, data));
		}
	}

	/**
	 * <p>
	 * Sets the {@linkplain ReaderOptions options} for this {@linkplain Reader
	 * reader}.
	 * </p>
	 * 
	 * @param options
	 *            The {@linkplain ReaderOptions reader options}
	 * @return The modified {@link Reader}, allowing for chaining
	 */
	public Reader options(final ReaderOptions options) {
		this.options = Objects.requireNonNull(options);
		return this;
	}

	/**
	 * <p>
	 * Helper method to conveniently download the content of the specified
	 * {@link URL}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL}
	 * @return The {@linkplain DownloadedItem downloaded item}
	 * @throws IOException
	 *             If the download fails after trying multiple times
	 */
	protected DownloadedItem download(final URL url) throws IOException {
		int tries = 0;
		String mimeType = null;
		final String userAgent = USER_AGENTS[random.nextInt(USER_AGENTS.length)];
		try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			try {
				final Response response = Jsoup.connect(url.toString()).ignoreContentType(true)
						.header("Host", url.getHost()).header("Connection", "keep-alive").header("Pragma", "no-cache")
						.header("Cache-Control", "no-cache").header("Accept-Encoding", "identity;q=1, *;q=0")
						.header("User-Agent", userAgent).header("Accept", "*/*").execute();

				if (mimeType == null) {
					mimeType = response.header("Content-Type");
				}

				bos.write(response.bodyAsBytes());
			} catch (final IOException e) {
				tries++;
				if (tries > 3) {
					throw e;
				}
			}

			return new DownloadedItem(mimeType, bos.toByteArray());
		} catch (final IOException e) {
			throw new IOException("Error reading " + url, e);
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + url + "]";
	}
}
