package com.github.codepain.mediadownload.music;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.imageio.ImageIO;

import com.github.codepain.mediadownload.download.BundleDownload;
import com.github.codepain.mediadownload.download.Download;
import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.save.MimeMappings;
import com.github.codepain.mediadownload.save.SaveOptions;

/**
 * <p>
 * A {@linkplain Downloadable downloadable item} that represents an album, i.e.
 * bundles several {@linkplain Track tracks} and has some meta information like
 * an album art (the cover).
 * </p>
 * 
 * @author codepain
 *
 */
public class Album extends Downloadable {

	private final URL url;

	private final List<Track> tracks = new ArrayList<>();

	private String artist;

	private String title;

	private DownloadedItem albumArt;

	private boolean downloadFinished;

	private BundleDownload download;

	/**
	 * <p>
	 * Constructs a {@link Album} with the specified {@link URL}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the album
	 */
	public Album(final URL url) {
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

			if (options.saveCoverArtSeparately() && albumArt != null) {
				if (albumArt.mimeType() != null) {
					final Path albumArtFile = albumRoot
							.resolve("cover." + MimeMappings.getExtension(albumArt.mimeType()));
					trigger(EventType.SAVE_START, "saving cover art [" + albumArtFile + "]");
					try {
						Files.copy(new ByteArrayInputStream(albumArt.data()), albumArtFile,
								StandardCopyOption.REPLACE_EXISTING);
						trigger(EventType.SAVE_FINISHED, albumArtFile);
					} catch (final IOException e) {
						triggerError(new IOException("Unable to save cover art", e));
					}
				} else {
					// no MIME type present, try to read image and save it as
					// JPEG
					try (final ByteArrayInputStream bis = new ByteArrayInputStream(albumArt.data())) {
						final BufferedImage image = ImageIO.read(bis);
						final Path albumArtFile = albumRoot.resolve("cover.jpg");
						trigger(EventType.SAVE_START, "saving cover art [" + albumArtFile + "]");
						ImageIO.write(image, "jpg", albumArtFile.toFile());
						trigger(EventType.SAVE_FINISHED, albumArtFile);
					} catch (final IOException e) {
						System.err.println("Cannot save album cover art, do not have any MIME type (" + this
								+ ") and saving as JPEG failed: " + e.getMessage());
					}
				}
			}

			trigger(EventType.SAVE_FINISHED, this);
		} else {
			// not downloaded yet ...
			// start the download and save it then
			final Album that = this;
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
			track.album(this);
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
	 * Returns the title of this album.
	 * </p>
	 * 
	 * @return
	 */
	public String title() {
		return title;
	}

	/**
	 * <p>
	 * Sets the specified title for this album.
	 * </p>
	 * 
	 * @param title
	 *            The new title to set
	 * @return The album with the new title, allowing for chaining
	 */
	public Album title(final String title) {
		this.title = title;
		return this;
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
	 * @return The album with the new artist, allowing for chaining
	 */
	public Album artist(final String artist) {
		this.artist = artist;
		return this;
	}

	/**
	 * <p>
	 * Returns the {@linkplain DownloadedItem album art}.
	 * </p>
	 * 
	 * @return The {@linkplain DownloadedItem album art}
	 */
	public DownloadedItem albumArt() {
		return albumArt;
	}

	/**
	 * <p>
	 * Sets the {@linkplain DownloadedItem album art}.
	 * </p>
	 * 
	 * @param albumArt
	 *            The album art to set
	 * @return The album with the album art set, allowing for chaining
	 */
	public Album albumArt(final DownloadedItem albumArt) {
		this.albumArt = albumArt;
		return this;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + title + "]";
	}
}
