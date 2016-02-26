package com.github.codepain.mediadownload.listener;

/**
 * <p>
 * Gets notified of any {@linkplain Event events} that are created by an
 * {@linkplain EventSource event source}.
 * </p>
 * <p>
 * There is no way to have more than one listener for a single event source, it
 * is only allowed to have a chain of listeners. This is achieved by the classes
 * implementing {@link Listener} as well as {@link EventSource}, so an event
 * handled by the listener can be sent again, causing an event to bubble up.
 * </p>
 * 
 * @author codepain
 *
 */
public interface Listener {

	/**
	 * <p>
	 * Gets called when an {@linkplain Event event} is created by an
	 * {@linkplain EventSource event source}.
	 * </p>
	 * 
	 * @param event
	 *            The {@link Event}
	 */
	void event(Event event);

}
