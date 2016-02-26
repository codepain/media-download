package com.github.codepain.mediadownload.listener;

/**
 * <p>
 * An event that was created by an {@linkplain EventSource event source}.
 * </p>
 * <p>
 * Stores information about the {@linkplain EventType type}, the event source,
 * the original event source in case it got {@linkplain #withSource(EventSource)
 * modified}, and any event object.
 * </p>
 * 
 * @author codepain
 *
 */
public class Event {

	private final EventType type;

	private final Object eventObject;

	private final EventSource source;

	private final EventSource originalSource;

	/**
	 * <p>
	 * Creates an {@linkplain Event event} with the specified attributes.
	 * </p>
	 * 
	 * @param source
	 *            The {@linkplain EventSource event source}, which is per
	 *            definition also the original event source
	 * @param type
	 *            The {@linkplain EventType event type}
	 * @param eventObject
	 *            The event object, may be anything that suits the needs of the
	 *            event, or {@code null}
	 */
	public Event(final EventSource source, final EventType type, final Object eventObject) {
		this(source, source, type, eventObject);
	}

	private Event(final EventSource source, final EventSource originalSource, final EventType type,
			final Object eventObject) {
		this.source = source;
		this.originalSource = originalSource;
		this.type = type;
		this.eventObject = eventObject;
	}

	/**
	 * <p>
	 * Changes the event's source to the specified one. The original source can
	 * still be obtained by calling {@link #originalSource()}.
	 * </p>
	 * 
	 * @param source
	 *            The new {@link EventSource}
	 * @return The resulting {@link Event}, allowing for method chaining
	 */
	public Event withSource(final EventSource source) {
		return new Event(source, originalSource, type, eventObject);
	}

	/**
	 * <p>
	 * Returns the {@linkplain EventSource event source} of the event. This
	 * event source can already be modified by any {@linkplain Listener
	 * listener} in the chain that the event usually bubbles up. To obtain the
	 * original event source, use {@link #originalSource()}.
	 * </p>
	 * 
	 * @return The {@link EventSource} of the event
	 * @see #originalSource()
	 */
	public EventSource source() {
		return source;
	}

	/**
	 * <p>
	 * Returns the original {@linkplain EventSource event source} of the event,
	 * as opposed to the event source obtained by {@link #source()}. It is
	 * hardly ever necessary to retrieve the original source, but do so, if you
	 * like.
	 * </p>
	 * 
	 * @return The {@linkplain EventSource original event source} by which this
	 *         event has been created
	 * @see #source()
	 */
	public EventSource originalSource() {
		return originalSource;
	}

	/**
	 * <p>
	 * Returns the {@linkplain EventType event type}.
	 * </p>
	 * 
	 * @return The {@link EventType}
	 */
	public EventType type() {
		return type;
	}

	/**
	 * <p>
	 * Returns the event object, which can be anything that may be useful
	 * handling the event, or even {@code null}.
	 * </p>
	 * 
	 * @return The event object, which may be {@code null}
	 */
	public Object eventObject() {
		return eventObject;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + type + " / " + source + " (" + originalSource + ") / " + eventObject
				+ "]";
	}

}
