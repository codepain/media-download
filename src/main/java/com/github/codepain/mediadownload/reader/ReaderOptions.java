package com.github.codepain.mediadownload.reader;

import com.github.codepain.mediadownload.music.Discography;

/**
 * <p>
 * Options for the {@linkplain Reader}.
 * </p>
 * 
 * @author codepain
 *
 */
public class ReaderOptions {

	private boolean loadSamplers;

	/**
	 * <p>
	 * Sets whether or not samplers or compilations shall be read, either as
	 * {@linkplain Album album} or as part of a {@linkplain Discography
	 * discography}.
	 * </p>
	 * <p>
	 * Please note that recognizing a sampler/compilation is not very accurate
	 * and may fail often.
	 * </p>
	 * 
	 * @param loadSamplers
	 *            whether or not samplers/compilations shall be read
	 * @return The modified options, allowing for chaining
	 */
	public ReaderOptions loadSamplers(final boolean loadSamplers) {
		this.loadSamplers = loadSamplers;
		return this;
	}

	/**
	 * <p>
	 * Returns whether or not samplers/compilations shall be read. For a more
	 * detailed explanation see {@link #loadSamplers(boolean)}.
	 * </p>
	 * 
	 * @return
	 */
	public boolean loadSamplers() {
		return loadSamplers;
	}
}
