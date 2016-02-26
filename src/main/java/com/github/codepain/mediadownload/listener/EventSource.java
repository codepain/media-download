package com.github.codepain.mediadownload.listener;

/**
 * <p>
 * An event source can create {@linkplain Event events}, which get sent to a
 * {@linkplain Listener listener}.
 * </p>
 * 
 * @author codepain
 *
 */
public interface EventSource {

	/**
	 * <p>
	 * Sets the {@linkplain Listener listener} that shall receive the
	 * {@linkplain Event events} that are created by this event source.
	 * </p>
	 * 
	 * @param listener
	 *            The {@link Listener} to receive the events, or {@code null},
	 *            if no listener shall receive any event
	 * @return The event source itself to allow for chaining
	 */
	EventSource listener(Listener listener);
}
