package com.github.codepain.mediadownload.save;

import com.github.codepain.mediadownload.download.Downloadable;

/**
 * <p>
 * An abstract {@link NamingScheme} offering helper methods for implementing the
 * naming scheme.
 * </p>
 * 
 * @author codepain
 *
 * @param <T>
 *            The type of {@linkplain Downloadable downloadable item}
 */
public abstract class AbstractNamingScheme<T extends Downloadable> implements NamingScheme<T> {

	/**
	 * <p>
	 * Sanitizes a file name, i.e. replaces any character that should not be
	 * used in a file name by {@code "-"}.
	 * </p>
	 * 
	 * @param filename
	 *            The file name to sanitize
	 * @return The sanitized file name
	 */
	protected String sanitizeFileName(final String filename) {
		return filename.replaceAll("[^0-9a-zA-Z_ :!,\\[\\]\\(\\)\\.'-]", "-");
	}
}
