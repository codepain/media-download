package com.github.codepain.mediadownload.download;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;

/**
 * <p>
 * A {@link Download} that bundles several other downloads. The separate
 * downloads get processed by a {@linkplain ExecutorService thread pool} that
 * usually contains five threads.
 * </p>
 * 
 * @author codepain
 *
 */
public class BundleDownload extends Download {

	private final List<Download> downloads = new ArrayList<>();

	private int numberOfFinishedDownloads;

	private final ExecutorService executor;

	private boolean downloadStarted;

	private final Object waitObject = new Object();

	/**
	 * <p>
	 * Constructs a {@link BundleDownload} for the specified
	 * {@linkplain Downloadable downloadable item} using a thread pool with five
	 * threads.
	 * </p>
	 * 
	 * @param downloadable
	 *            The {@linkplain Downloadable downloadable item}
	 * @see #BundleDownload(Downloadable, int)
	 */
	public BundleDownload(final Downloadable downloadable) {
		this(downloadable, 5);
	}

	/**
	 * <p>
	 * Constructs a {@link BundleDownload} for the specified
	 * {@linkplain Downloadable downloadable item} using a thread pool with the
	 * specified number of threads.
	 * </p>
	 * 
	 * @param downloadItem
	 *            The {@linkplain Downloadable downloadable item}
	 * @param numberThreads
	 *            The number of threads to use for the thread pool
	 * @throws IllegalArgumentException
	 *             If the number of threads is less than one
	 * @see #BundleDownload(Downloadable)
	 */
	public BundleDownload(final Downloadable downloadItem, final int numberThreads) {
		super(downloadItem);
		if (numberThreads < 1) {
			throw new IllegalArgumentException("Number of threads must be at least one");
		}
		executor = Executors.newFixedThreadPool(numberThreads);
	}

	/**
	 * <p>
	 * Adds a {@linkplain Download download object} to this bundle.
	 * </p>
	 * 
	 * @param download
	 *            The {@linkplain Download download object} to add
	 */
	public void add(final Download download) {
		downloads.add(download);
	}

	@Override
	public Download start() {
		downloadStarted = true;

		if (numberOfFinishedDownloads < downloads.size()) {
			for (final Download download : downloads) {
				executor.submit(new Runnable() {

					@Override
					public void run() {
						download.start().waitTillFinished();
						;
					}
				});
			}
		}

		return this;
	}

	@Override
	public void whenFinished(final Consumer<Downloadable> callback) {
		if (callback == null) {
			throw new NullPointerException("Callback must not be null");
		}

		waitTillFinished();

		callback.accept(downloadItem);
	}

	@Override
	public void waitTillFinished() {
		if (!downloadStarted) {
			start();
		}

		// let's really wait until all sub-downloads finished
		executor.shutdown();
		while (!executor.isTerminated()) {
			try {
				executor.awaitTermination(10, TimeUnit.SECONDS);
			} catch (final InterruptedException e) {
				// set the interrupt flag again
				Thread.currentThread().interrupt();
			}
		}

		// TODO: is this necessary with the former executor shutdown?
		while (numberOfFinishedDownloads < downloads.size()) {
			synchronized (waitObject) {
				try {
					waitObject.wait();
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	@Override
	public Progress progress() {
		int read = 0;
		int length = 0;
		for (final Download download : downloads) {
			read += download.progress().read();
			length = download.progress().length();
		}

		return new Progress(read, length);
	}

	@Override
	public void onEvent(final Event event) {
		if (EventType.DOWNLOAD_FINISHED.equals(event.type()) || EventType.ERROR.equals(event.type())) {
			// only increment counter etc. if it is one of "our" direct child
			// downloads
			if (downloads.contains(event.originalSource())) {
				numberOfFinishedDownloads++;

				if (numberOfFinishedDownloads == downloads.size()) {
					synchronized (waitObject) {
						waitObject.notifyAll();
					}
					triggerFinished(null);
				}
			}
		}
	}

}
