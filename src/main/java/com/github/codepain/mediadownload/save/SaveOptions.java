package com.github.codepain.mediadownload.save;

import java.nio.file.Path;
import java.util.Objects;

import com.github.codepain.mediadownload.music.Album;
import com.github.codepain.mediadownload.music.LooseTrackSet;
import com.github.codepain.mediadownload.music.Track;

public class SaveOptions {

	private final Path root;

	private boolean saveCoverArtSeparately;

	private NamingScheme<Album> albumNamingScheme = new DefaultAlbumNamingScheme();

	private NamingScheme<Track> trackNamingScheme = new DefaultTrackNamingScheme();

	private NamingScheme<LooseTrackSet> looseTrackSetNamingScheme = new DefaultLooseTrackSetNamingScheme();

	public SaveOptions(Path root) {
		this.root = root;
	}

	public Path nameOf(final LooseTrackSet looseTrackSet) {
		return looseTrackSetNamingScheme.nameOf(root, looseTrackSet);
	}

	public Path nameOf(final Album album) {
		return albumNamingScheme.nameOf(root, album);
	}

	public Path nameOf(final Track track) {
		return trackNamingScheme.nameOf(root, track);
	}

	public SaveOptions copyWithRoot(final Path root) {
		final SaveOptions options = new SaveOptions(root).saveCoverArtSeparately(saveCoverArtSeparately);
		return options;
	}

	public Path root() {
		return root;
	}

	public SaveOptions saveCoverArtSeparately(final boolean saveCoverArtSeparately) {
		this.saveCoverArtSeparately = saveCoverArtSeparately;
		return this;
	}

	public boolean saveCoverArtSeparately() {
		return saveCoverArtSeparately;
	}

	public SaveOptions looseTrackSetNamingScheme(final NamingScheme<LooseTrackSet> looseTrackSetNamingScheme) {
		this.looseTrackSetNamingScheme = Objects.requireNonNull(looseTrackSetNamingScheme);
		return this;
	}

	public SaveOptions albumNamingScheme(final NamingScheme<Album> albumNamingScheme) {
		this.albumNamingScheme = Objects.requireNonNull(albumNamingScheme);
		return this;
	}

	public SaveOptions trackNamingScheme(final NamingScheme<Track> trackNamingScheme) {
		this.trackNamingScheme = Objects.requireNonNull(trackNamingScheme);
		return this;
	}
}
