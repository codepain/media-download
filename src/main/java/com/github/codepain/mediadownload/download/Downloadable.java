package com.github.codepain.mediadownload.download;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.listener.Listener;
import com.github.codepain.mediadownload.save.Savable;
import com.github.codepain.mediadownload.save.SaveOptions;

/**
 * <p>
 * An item that can be downloaded.
 * </p>
 * <p>
 * As it can be downloaded, it is {@linkplain Savable savable} and also
 * implements {@link Listener}, so it can be a part of a listener chain.
 * </p>
 * 
 * @author codepain
 *
 */
public abstract class Downloadable implements Savable, Listener {

	private Listener listener;

	@Override
	public void save(final Path root) {
		save(new SaveOptions(Objects.requireNonNull(root)));
	}

	@Override
	public Downloadable listener(final Listener listener) {
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
	 * Returns a {@linkplain Download download object}, which can be used to
	 * download this {@linkplain Downloadable}.
	 * </p>
	 * 
	 * @return A {@link Download} that downloads this item
	 */
	public abstract Download download();

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
	 * Creates an {@linkplain EventType#ERROR error} {@linkplain Event event}
	 * and tells the {@linkplain Listener listener}, if one is present.
	 * </p>
	 * 
	 * @param e
	 *            The {@link IOException} that shall be the
	 *            {@linkplain Event#eventObject() event object}
	 * @see #trigger(EventType, Object)
	 */
	protected void triggerError(final IOException e) {
		if (listener != null) {
			listener.event(new Event(this, EventType.ERROR, e));
		}
	}

	/**
	 * <p>
	 * Creates an {@linkplain Event event} with the specified type and event
	 * object.
	 * </p>
	 * 
	 * @param type
	 *            The {@link EventType}
	 * @param data
	 *            The {@linkplain Event#eventObject() event object}, may be
	 *            {@code null}
	 * @see #triggerError(IOException)
	 */
	protected void trigger(final EventType type, final Object data) {
		if (listener != null) {
			listener.event(new Event(this, type, data));
		}
	}
}
