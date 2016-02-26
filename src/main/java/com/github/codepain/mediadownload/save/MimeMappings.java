package com.github.codepain.mediadownload.save;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <p>
 * Allows mapping the MIME type to a file extension and vice versa.
 * </p>
 * <p>
 * This is by far no complete list, but it must only contain the typical MIME
 * types needed for the downloaded files, i.e. media files and images.
 * </p>
 * 
 * @author codepain
 *
 */
public final class MimeMappings {

	private static final Map<String, String> mimeMappings = new HashMap<>();

	private static final Map<String, String> reverseMimeMappings = new HashMap<>();

	static {
		mimeMappings.put("audio/mpeg", "mp3");
		mimeMappings.put("image/jpeg", "jpg");
		mimeMappings.put("image/png", "png");
		mimeMappings.put("image/gif", "gif");

		reverseMimeMappings.put("mp3", "audio/mpeg");
		reverseMimeMappings.put("jpg", "image/jpeg");
		reverseMimeMappings.put("jpeg", "image/jpeg");
		reverseMimeMappings.put("jpe", "image/jpeg");
		reverseMimeMappings.put("png", "image/png");
		reverseMimeMappings.put("gif", "image/gif");
	}

	private MimeMappings() {
	}

	/**
	 * <p>
	 * Returns the mapped file extension for the specified MIME type.
	 * </p>
	 * 
	 * @param mimeType
	 *            The MIME type
	 * @return The file extension
	 * @throws NullPointerException
	 *             If the parameter is {@code null}
	 */
	public static String getExtension(final String mimeType) {
		return mimeMappings.get(Objects.requireNonNull(mimeType).toLowerCase());
	}

	/**
	 * <p>
	 * Returns the mapped MIME type for the specified file extension.
	 * </p>
	 * 
	 * @param extension
	 *            The file extension
	 * @return The MIME type
	 * @throws NullPointerException
	 *             If the parameter is {@code null}
	 */
	public static String getMimeType(final String extension) {
		return reverseMimeMappings.get(Objects.requireNonNull(extension).toLowerCase());
	}

}
