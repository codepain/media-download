package com.github.codepain.mediadownload.save;

import java.nio.file.Path;

import com.github.codepain.mediadownload.music.LooseTrackSet;
import com.github.codepain.mediadownload.music.Track;

/**
 * <p>
 * The default {@linkplain NamingScheme naming scheme} for
 * {@linkplain LooseTrackSet loose track sets}.
 * </p>
 * <p>
 * As the loose track set does not get saved directly, but its {@linkplain Track
 * tracks}, it only denotes a sub folder within the root folder. The name of
 * this folder is the {@linkplain LooseTrackSet#artist() artist name}.
 * </p>
 * 
 * @author codepain
 *
 */
public class DefaultLooseTrackSetNamingScheme extends AbstractNamingScheme<LooseTrackSet> {

	@Override
	public Path nameOf(final Path root, final LooseTrackSet album) {
		return root.resolve(sanitizeFileName(album.artist()));
	}
}
