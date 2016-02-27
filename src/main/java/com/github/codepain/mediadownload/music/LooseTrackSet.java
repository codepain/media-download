package com.github.codepain.mediadownload.music;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.codepain.mediadownload.download.BundleDownload;
import com.github.codepain.mediadownload.download.Download;
import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.save.SaveOptions;

/**
 * <p>
 * This {@linkplain Downloadable downloadable item} is a weakly coupled bunch of
 * {@linkplain Track tracks} that only share the same {@linkplain #artist()
 * artist}.
 * </p>
 * <p>
 * An {@linkplain Album album} in opposition is a more strongly coupled set of
 * tracks, as they usually share some more meta information, like the cover art.
 * </p>
 * 
 * @author codepain
 * @see Album
 */
public class LooseTrackSet extends Downloadable {

	protected final URL url;

	private String artist;

	protected final List<Track> tracks = new ArrayList<>();

	protected boolean downloadFinished;

	protected BundleDownload download;

	public LooseTrackSet(final URL url) {
		this.url = url;
	}

	@Override
	public Download download() {
		download = (BundleDownload) new BundleDownload(this).listener(this);

		synchronized (tracks) {
			for (final Track track : tracks) {
				download.add(track.listener(download).download());
			}
		}

		return download;
	}

	@Override
	protected void onEvent(final Event event) {
		if (EventType.DOWNLOAD_FINISHED.equals(event.type()) && event.source() == download) {
			downloadFinished = true;
		}
	}

	@Override
	public void save(final SaveOptions options) {
		if (downloadFinished) {
			// already downloaded, just save it
			final Path albumRoot = options.nameOf(this);
			trigger(EventType.SAVE_START, "saving album into folder [" + albumRoot + "]");
			if (!Files.exists(albumRoot)) {
				try {
					Files.createDirectories(albumRoot);
				} catch (final IOException e) {
					triggerError(e);
					return;
				}
			}

			final SaveOptions trackOptions = options.copyWithRoot(albumRoot);
			synchronized (tracks) {
				for (final Track track : tracks) {
					track.save(trackOptions);
				}
			}

			trigger(EventType.SAVE_FINISHED, this);
		} else {
			// not downloaded yet ...
			// start the download and save it then
			final LooseTrackSet that = this;
			download().start().whenFinished(downloadItem -> that.save(options));
		}
	}

	/**
	 * <p>
	 * Adds the {@linkplain Track track} to this album.
	 * </p>
	 * <p>
	 * After adding it the album is {@linkplain Track#album(Album) set} in the
	 * track and registers as
	 * {@linkplain Track#listener(com.github.codepain.mediadownload.listener.Listener)
	 * listener}.
	 * </p>
	 * 
	 * @param track
	 *            The track to add
	 * @throws NullPointerException
	 *             If the track is {@code null}
	 */
	public void add(final Track track) {
		synchronized (tracks) {
			tracks.add(Objects.requireNonNull(track));
			track.listener(this);
		}
	}

	/**
	 * <p>
	 * Returns a copy of the list of {@linkplain Track tracks} of this album in
	 * no specific order.
	 * </p>
	 * 
	 * @return A copy of the list of {@linkplain Track tracks} of this album
	 */
	public List<Track> tracks() {
		synchronized (tracks) {
			return new ArrayList<>(tracks);
		}
	}

	/**
	 * <p>
	 * Returns the {@link URL} of this album.
	 * </p>
	 * 
	 * @return
	 */
	public URL url() {
		return url;
	}

	/**
	 * <p>
	 * Returns the album's artist.
	 * </p>
	 * 
	 * @return
	 */
	public String artist() {
		return artist;
	}

	/**
	 * <p>
	 * Sets the artist of this album.
	 * </p>
	 * 
	 * @param artist
	 *            The artist to set
	 * @return The loose track set with the new artist, allowing for chaining
	 */
	public LooseTrackSet artist(final String artist) {
		this.artist = artist;
		return this;
	}
}