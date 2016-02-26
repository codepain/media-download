package com.github.codepain.mediadownload.save;

import java.nio.file.Path;

import com.github.codepain.mediadownload.listener.EventSource;

/**
 * <p>
 * Object can be saved on the disk.
 * </p>
 * 
 * @author codepain
 *
 */
public interface Savable extends EventSource {

	/**
	 * <p>
	 * Saves the object on the disk using the specified {@linkplain SaveOptions
	 * options}.
	 * </p>
	 * 
	 * @param options
	 *            The {@link SaveOptions} to use
	 * @see #save(Path)
	 */
	void save(SaveOptions options);

	/**
	 * <p>
	 * Saves the object on the disk within the specified root folder (or a sub
	 * folder), using default {@linkplain SaveOptions options}.
	 * </p>
	 * 
	 * @param root
	 *            The root folder
	 * @see #save(SaveOptions)
	 */
	void save(Path root);
}
