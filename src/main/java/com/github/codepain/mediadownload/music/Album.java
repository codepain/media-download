package com.github.codepain.mediadownload.music;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;

import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.save.MimeMappings;
import com.github.codepain.mediadownload.save.SaveOptions;

/**
 * <p>
 * A {@linkplain Downloadable downloadable item} that represents an album, i.e.
 * bundles several {@linkplain Track tracks} and has some meta information like
 * an album art (the cover).
 * </p>
 * <p>
 * The bundled tracks are strongly coupled as they share some more meta information,
 * opposed to q {@linkplain LooseTrackSet loosely coupled track set}.
 * </p>
 * 
 * @author codepain
 * @see LooseTrackSet
 */
public class Album extends LooseTrackSet {

	private String title;

	DownloadedItem albumArt;

	/**
	 * <p>
	 * Constructs a {@link Album} with the specified {@link URL}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the album
	 */
	public Album(final URL url) {
		super(url);
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

	@Override
	public void add(final Track track) {
		super.add(track);
		track.album(this);
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
