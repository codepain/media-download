package com.github.codepain.mediadownload.download;

/**
 * <p>
 * Represents a progress of a {@linkplain Download download}.
 * </p>
 * 
 * @author codepain
 *
 */
public class Progress {

	private final int read;

	private final int length;

	/**
	 * <p>
	 * Constructs a {@link Progress}, which is immutable.
	 * </p>
	 * 
	 * @param read
	 *            The number of bytes that were already read
	 * @param length
	 *            The number of bytes of the download file
	 */
	public Progress(Integer read, Integer length) {
		this.read = read;
		this.length = length;
	}

	/**
	 * <p>
	 * Number of read bytes.
	 * </p>
	 * 
	 * @return
	 */
	public Integer read() {
		return read;
	}

	/**
	 * <p>
	 * Number of bytes of the download.
	 * </p>
	 * 
	 * @return
	 */
	public Integer length() {
		return length;
	}

	/**
	 * <p>
	 * Percentage of the download process, i.e. {@code read / length}.
	 * </p>
	 * 
	 * @return
	 */
	public double percentage() {
		return (length == 0) ? 0.0 : ((double) read / length);
	}

	@Override
	public String toString() {
		return 100 * percentage() + "%";
	}
}
