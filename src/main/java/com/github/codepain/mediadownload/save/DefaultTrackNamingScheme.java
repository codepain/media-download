package com.github.codepain.mediadownload.save;

import java.nio.file.Path;

import com.github.codepain.mediadownload.music.Album;
import com.github.codepain.mediadownload.music.Track;

/**
 * <p>
 * The default {@linkplain NamingScheme naming scheme} for tracks, which is:
 * <ol>
 * <li>Choose a prefix:
 * <ul>
 * <li>If there is no {@linkplain Album album}, to which the track
 * {@linkplain Track#album() belongs}, take the {@linkplain Track#artist()
 * artist name}.</li>
 * <li>Otherwise make a zero-padded, two-digit index (01, 02, 03, ...).</li>
 * </ul>
 * <li>Choose the extension by using {@link MimeMappings#getExtension(String)}
 * with the track's {@linkplain Track#mimeType() MIME type}, assuming
 * {@code "audio/mpeg"} if there is none</li>
 * <li>Assemble the file name: {@code [prefix] - [title].[extension]}</li>
 * </ol>
 * </p>
 * 
 * @author codepain
 *
 */
public class DefaultTrackNamingScheme extends AbstractNamingScheme<Track> {

	@Override
	public Path nameOf(final Path root, final Track track) {
		final String extension = MimeMappings.getExtension(track.mimeType() == null ? "audio/mpeg" : track.mimeType());
		final String prefix = track.album() == null ? (track.artist() == null ? "" : track.artist() + " - ")
				: (track.index() < 10 ? "0" : "") + track.index() + " - ";
		final String name = sanitizeFileName(prefix + track.title()) + "." + extension;
		return root.resolve(name);
	}

}
