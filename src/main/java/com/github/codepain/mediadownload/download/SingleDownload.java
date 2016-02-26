package com.github.codepain.mediadownload.download;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.function.Consumer;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Response;

import com.github.codepain.mediadownload.listener.Event;

/**
 * <p>
 * A {@link Download} that downloads a single item.
 * </p>
 * 
 * @author codepain
 * 
 */
public class SingleDownload extends Download {

	private static final int MAX_TRIES = 3;

	private static final String USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.82 Safari/537.36";

	private final URL url;

	private int length = 0;

	private int read = 0;

	private boolean downloadFinished;

	private boolean downloadStarted;

	private final Object waitForDownload = new Object();

	private final Random random = new Random();

	/**
	 * <p>
	 * Constructs a {@link SingleDownload} for the specified item and URL.
	 * </p>
	 * 
	 * @param downloadItem
	 *            The {@linkplain Downloadable downloadable item}
	 * @param url
	 *            The {@linkplain URL} of the file to download
	 */
	public SingleDownload(final Downloadable downloadItem, final URL url) {
		super(downloadItem);
		this.url = url;
	}

	@Override
	public Download start() {
		if (!downloadFinished) {
			downloadStarted = true;
			int tries = 0;
			String mimeType = null;
			try (final ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
				do {
					try {
						final String rangeHeader = "bytes=" + (read == 0 ? "0" : read) + "-";
						final Response response = Jsoup.connect(url.toString()).ignoreContentType(true)
								.header("Host", url.getHost()).header("Connection", "keep-alive")
								.header("Pragma", "no-cache").header("Cache-Control", "no-cache")
								.header("Accept-Encoding", "identity;q=1, *;q=0").header("User-Agent", USER_AGENT)
								.header("Accept", "*/*").header("Range", rangeHeader).execute();
						final byte[] res = response.bodyAsBytes();
						bos.write(res, 0, res.length);
						read += res.length;

						if (mimeType == null) {
							mimeType = response.contentType();
						}

						if (length == 0) {
							length = Integer.valueOf(response.header("Content-Length")) - 1;
						}

						triggerProgress();
					} catch (final IOException e) {
						tries++;
						System.err.println(downloadItem + " (try " + tries + "/" + MAX_TRIES + ") " + e.getMessage());
						// wait random time
						try {
							Thread.sleep(random.nextInt(tries * 3000));
						} catch (final InterruptedException e1) {
							Thread.currentThread().interrupt();
						}
						if (tries > MAX_TRIES) {
							throw e;
						}
					}
				} while (length == 0 || read < length);

				triggerFinished(new DownloadedItem(mimeType, bos.toByteArray()));
			} catch (final IOException e) {
				triggerError(
						new IOException("Error reading " + url + " (read " + read + " of " + length + " bytes)", e));
			} finally {
				downloadFinished = true;
			}

			synchronized (waitForDownload) {
				waitForDownload.notifyAll();
			}
		}

		return this;
	}

	@Override
	public void whenFinished(final Consumer<Downloadable> callback) {
		if (callback == null) {
			throw new NullPointerException("Callback must not be null");
		}

		synchronized (waitForDownload) {
			while (!downloadFinished) {
				try {
					waitForDownload.wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}

		callback.accept(downloadItem);
	}

	@Override
	public void waitTillFinished() {
		// is per se finished, as start() is already blocking
		if (!downloadStarted) {
			start();
		}
	}

	@Override
	public Progress progress() {
		return new Progress(read, length);
	}

	@Override
	protected void onEvent(final Event event) {
		// nothing to do
	}

}
