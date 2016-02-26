package com.github.codepain.mediadownload.download;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;

import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventSource;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.listener.Listener;

/**
 * <p>
 * Represents a download process, which can be {@linkplain #start() started}.
 * The download process itself is asynchronous and so the call to
 * {@link #start()} does not block. You can use certain mechanisms to
 * {@linkplain #listener(Listener) get informed} about the status or
 * {@linkplain #whenFinished(Consumer) execute code} once the download finished.
 * Or you can make a blocking call to {@link #waitTillFinished()}.
 * </p>
 * <p>
 * A download process may consist of several download processes, i.e. bundling
 * them. A bundled download is finished, when all bundled downloads finished.
 * </p>
 * 
 * @author codepain
 *
 */
public abstract class Download implements EventSource, Listener {

	protected final Downloadable downloadItem;

	private Listener listener;

	/**
	 * <p>
	 * Constructs a {@link Download} for the {@linkplain Downloadable
	 * downloadable item}.
	 * </p>
	 * 
	 * @param downloadItem
	 *            The {@link Downloadable}
	 */
	public Download(final Downloadable downloadItem) {
		this.downloadItem = downloadItem;
	}

	@Override
	public Download listener(final Listener listener) {
		if (listener == null) {
			throw new NullPointerException("Download listener must not be null");
		}
		if (this.listener == null) {
			this.listener = Objects.requireNonNull(listener);
		} else {
			// respect the chain
			downloadItem.listener(listener);
		}

		return this;
	}

	/**
	 * <p>
	 * Starts the download process. This call is non-blocking and returns
	 * immediately.
	 * </p>
	 * <p>
	 * You can {@linkplain #listener(Listener) get informed} about
	 * {@linkplain Event events}, {@linkplain #whenFinished(Consumer) execute
	 * code} once the download finished, or use a call to
	 * {@link #waitTillFinished()} to block.
	 * </p>
	 * 
	 * @return The {@link Download} itself to allow for chaining
	 */
	public abstract Download start();

	/**
	 * <p>
	 * Executes the callback once the download finished. Calling this method
	 * blocks until the download is finished.
	 * </p>
	 * <p>
	 * Please ensure to {@linkplain #start() start} the download first, as it is
	 * not guaranteed that a call to this method will start the process.
	 * Blocking a download that has not started yet may lead to significantly
	 * longer execution times (i.e. you are stuck).
	 * </p>
	 * 
	 * @param callback
	 *            The {@link Consumer} that gets
	 *            {@linkplain Consumer#accept(Object) executed} once the
	 *            download finished
	 * @throws NullPointerException
	 *             If the callback is {@code null}
	 * @see #waitTillFinished()
	 */
	public abstract void whenFinished(Consumer<Downloadable> callback);

	/**
	 * <p>
	 * Blocks until the download finished.
	 * </p>
	 * <p>
	 * Please ensure to {@linkplain #start() start} the download first, as it is
	 * not guaranteed that a call to this method will start the process.
	 * Blocking a download that has not started yet may lead to significantly
	 * longer execution times (i.e. you are stuck).
	 * </p>
	 * 
	 * @see #whenFinished(Consumer)
	 */
	public abstract void waitTillFinished();

	/**
	 * <p>
	 * Returns the {@linkplain Progress progress} of the download so far.
	 * </p>
	 * 
	 * @return The {@link Progress} of the download
	 */
	protected abstract Progress progress();

	@Override
	public void event(Event event) {
		onEvent(event);

		if (listener != null) {
			if (!(event.source() instanceof Downloadable)) {
				event = new Event(this, event.type(), event.eventObject());
			}
			listener.event(event);
		}
	}

	/**
	 * <p>
	 * As the original {@link Listener#event(Event)} method is overridden to
	 * give support for a {@linkplain Listener listener} chain, this one gets
	 * called.
	 * </p>
	 * 
	 * @param event
	 *            The {@link Event}
	 */
	protected abstract void onEvent(final Event event);

	/**
	 * <p>
	 * Creates an {@linkplain EventType#DOWNLOAD_PROGRESS progress}
	 * {@linkplain Event event} and tells the {@linkplain Listener listener}, if
	 * one is present.
	 * </p>
	 * 
	 * @see #triggerError(IOException)
	 * @see #triggerFinished(DownloadedItem)
	 */
	protected void triggerProgress() {
		listener.event(new Event(this, EventType.DOWNLOAD_PROGRESS, progress()));
	}

	/**
	 * <p>
	 * Creates an {@linkplain EventType#DOWNLOAD_FINISHED finished}
	 * {@linkplain Event event} and tells the {@linkplain Listener listener}, if
	 * one is present.
	 * </p>
	 *
	 * @param downloadedItem
	 *            The {@linkplain DownloadedItem downloaded item}
	 * @see #triggerProgress()
	 * @see #triggerError(IOException)
	 */
	protected void triggerFinished(final DownloadedItem downloadedItem) {
		listener.event(new Event(this, EventType.DOWNLOAD_FINISHED, downloadedItem));
	}

	/**
	 * <p>
	 * Creates an {@linkplain EventType#ERROR error} {@linkplain Event event}
	 * and tells the {@linkplain Listener listener}, if one is present.
	 * </p>
	 * 
	 * @param e
	 *            The {@link IOException} that shall be the
	 *            {@linkplain Event#eventObject() event object}
	 * @see #triggerProgress()
	 * @see #triggerFinished(DownloadedItem)
	 */
	protected void triggerError(final IOException e) {
		listener.event(new Event(this, EventType.ERROR, e));
	}

}
