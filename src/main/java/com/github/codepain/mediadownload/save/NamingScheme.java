package com.github.codepain.mediadownload.save;

import java.nio.file.Path;

import com.github.codepain.mediadownload.download.Downloadable;

/**
 * <p>
 * Implements a scheme to get a file name from a {@linkplain Downloadable
 * downloadable item}.
 * </p>
 * 
 * @author codepain
 *
 * @param <T>
 *            The type of {@link Downloadable}
 */
@FunctionalInterface
public interface NamingScheme<T extends Downloadable> {

	/**
	 * <p>
	 * Creates the {@link Path} containing the file name for the
	 * {@linkplain Downloadable downloadable item} in the specified root folder.
	 * </p>
	 * 
	 * @param root
	 *            The root folder, in which the file shall reside
	 * @param downloadItem
	 *            The {@linkplain Downloadable downloadable item}
	 * @return The {@linkplain Path file path} within the root folder
	 */
	Path nameOf(Path root, T downloadItem);
}
