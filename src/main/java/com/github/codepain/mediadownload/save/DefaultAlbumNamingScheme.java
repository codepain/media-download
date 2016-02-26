package com.github.codepain.mediadownload.save;

import java.nio.file.Path;

import com.github.codepain.mediadownload.music.Album;
import com.github.codepain.mediadownload.music.Track;

/**
 * <p>
 * The default {@linkplain NamingScheme naming scheme} for {@linkplain Album
 * albums}.
 * </p>
 * <p>
 * As the album does not get saved directly, but its {@linkplain Track tracks},
 * it only denotes a sub folder within the root folder:
 * <ol>
 * <li>The {@linkplain Album#artist() album's artist}</li>
 * <li>The {@linkplain Album#title() album's title}</li>
 * </ol>
 * </p>
 * 
 * @author codepain
 *
 */
public class DefaultAlbumNamingScheme extends AbstractNamingScheme<Album> {

	@Override
	public Path nameOf(final Path root, final Album album) {
		return root.resolve(sanitizeFileName(album.artist())).resolve(sanitizeFileName(album.title()));
	}

}
