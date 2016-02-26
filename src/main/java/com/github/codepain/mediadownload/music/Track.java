package com.github.codepain.mediadownload.music;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.github.codepain.mediadownload.download.Download;
import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.download.SingleDownload;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.save.SaveOptions;
import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

/**
 * <p>
 * A {@linkplain Downloadable downloadable item} that contains the raw music
 * data as well as some meta information like the artist or the track index.
 * </p>
 * 
 * @author codepain
 *
 */
public class Track extends Downloadable {

	private final String title;

	private final URL downloadUrl;

	private int index;

	private String artist;

	private String year;

	private DownloadedItem albumArt;

	private DownloadedItem downloadedItem;

	private boolean downloadFinished;

	private Album album;

	/**
	 * <p>
	 * Constructs a {@link Track} with the specified title and {@link URL}.
	 * </p>
	 * 
	 * @param title
	 *            The track title
	 * @param downloadUrl
	 *            The {@link URL} of the raw music data (for download)
	 */
	public Track(final String title, final URL downloadUrl) {
		this.title = title;
		this.downloadUrl = downloadUrl;
	}

	@Override
	public Download download() {
		return new SingleDownload(this, downloadUrl).listener(this);
	}

	@Override
	protected void onEvent(final Event event) {
		if (EventType.ERROR.equals(event.type())) {
			downloadFinished = true;
		} else if (EventType.DOWNLOAD_FINISHED.equals(event.type())) {
			downloadedItem = (DownloadedItem) event.eventObject();
			downloadFinished = true;
		}
	}

	@Override
	public void save(final SaveOptions options) {
		if (downloadFinished) {
			if (downloadedItem == null) {
				// an error occurred during download, we cannot save anything
				triggerError(new IOException("The download was erroneous, cannot save anything"));
				return;
			}

			// already downloaded, just save it
			final Path file = options.nameOf(this);
			final Path tmpFile = options.root().resolve(file.getFileName() + ".tag");
			trigger(EventType.SAVE_START, "saving tag file [" + tmpFile + "]");
			try (final ByteArrayInputStream bis = new ByteArrayInputStream(downloadedItem.data())) {
				Files.copy(bis, tmpFile, StandardCopyOption.REPLACE_EXISTING);
				trigger(EventType.SAVE_FINISHED, tmpFile);
				trigger(EventType.SAVE_START, "enriching tag file with IDv3 tags [" + tmpFile + " -> " + file + "]");
				enrichWithMetaData(tmpFile, file);
				trigger(EventType.SAVE_FINISHED, file);
			} catch (final IOException e) {
				triggerError(e);
			}
		} else {
			// not downloaded yet ...
			// so start a download and save the item then
			final Track that = this;
			download().start().whenFinished(downloadItem -> that.save(options));
		}
	}

	private void enrichWithMetaData(final Path tagFile, final Path destFile) {
		try {
			final Mp3File mp3File = new Mp3File(tagFile.toFile());
			if (mp3File.hasId3v1Tag()) {
				final ID3v1 tag = mp3File.getId3v1Tag();
				if (tag.getTitle() == null) {
					tag.setTitle(title);
				}
				if (tag.getTrack() == null) {
					tag.setTrack("" + index);
				}
				if (tag.getAlbum() == null && album != null) {
					tag.setAlbum(album.title());
				}
				if (tag.getArtist() == null) {
					tag.setArtist(artist != null ? artist : (album != null ? album.artist() : null));
				}
				if (tag.getYear() == null) {
					tag.setYear(year);
				}
			} else {
				final ID3v2 tag;
				if (mp3File.hasId3v2Tag()) {
					tag = mp3File.getId3v2Tag();
				} else {
					tag = new ID3v24Tag();
					mp3File.setId3v2Tag(tag);
				}
				if (tag.getTitle() == null) {
					tag.setTitle(title);
				}
				if (tag.getTrack() == null) {
					tag.setTrack("" + index);
				}
				if (tag.getAlbum() == null && album != null) {
					tag.setAlbum(album.title());
				}
				if (tag.getArtist() == null) {
					tag.setArtist(artist != null ? artist : (album != null ? album.artist() : null));
				}
				if (tag.getAlbumArtist() == null) {
					tag.setAlbumArtist((album != null ? album.artist() : null));
				}
				if (tag.getYear() == null) {
					tag.setYear(year);
				}
				if (tag.getAlbumImage() == null) {
					if (albumArt != null) {
						tag.setAlbumImage(albumArt.data(), albumArt.mimeType());
					} else if (album != null && album.albumArt() != null) {
						tag.setAlbumImage(album.albumArt().data(), album.albumArt().mimeType());
					}
				}
			}

			mp3File.save(destFile.toString());
		} catch (final UnsupportedTagException | InvalidDataException | IOException | NotSupportedException e) {
			// failed, but let's not be too angry about it, it's just meta data
			trigger(EventType.ERROR, this + " Cannot write ID3 tags: " + e.getMessage());

			// just copy the original file
			try {
				Files.copy(tagFile, destFile, StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException e1) {
				trigger(EventType.ERROR, this + " Unable to copy track file, track is lost: " + e1.getMessage());
			}
		} finally {
			try {
				Files.deleteIfExists(tagFile);
			} catch (final IOException e) {
				trigger(EventType.ERROR, this + " Cannot delete tag file: " + e.getMessage());
			}
		}
	}

	/**
	 * <p>
	 * Returns the title of the track.
	 * </p>
	 * 
	 * @return
	 */
	public String title() {
		return title;
	}

	/**
	 * <p>
	 * Returns the download {@link URL}.
	 * </p>
	 * 
	 * @return
	 */
	public URL downloadUrl() {
		return downloadUrl;
	}

	/**
	 * <p>
	 * Returns the artist of the track.
	 * </p>
	 * 
	 * @return
	 */
	public String artist() {
		return artist;
	}

	/**
	 * <p>
	 * Sets the artist of the track.
	 * </p>
	 * 
	 * @param artist
	 *            The artist to set
	 * @return The track with the artist set, allowing for chaining
	 */
	public Track artist(final String artist) {
		this.artist = artist;
		return this;
	}

	/**
	 * <p>
	 * Returns the track index.
	 * </p>
	 * 
	 * @return
	 */
	public int index() {
		return index;
	}

	/**
	 * <p>
	 * Sets the track index.
	 * </p>
	 * 
	 * @param index
	 *            The track index to set
	 * @return The track with the new index set, allowing for chaining
	 */
	public Track index(final int index) {
		this.index = index;
		return this;
	}

	/**
	 * <p>
	 * Returns the release year of the track.
	 * </p>
	 * 
	 * @return
	 */
	public String year() {
		return year;
	}

	/**
	 * <p>
	 * Sets the release year of the track.
	 * </p>
	 * 
	 * @param year
	 *            The release year to set
	 * @return The track with the new release year set, allowing for chaining
	 */
	public Track year(final String year) {
		this.year = year;
		return this;
	}

	/**
	 * <p>
	 * Sets the album to which the track belongs.
	 * </p>
	 * 
	 * @param album
	 *            The album of the track
	 * @return The track with the album set, allowing for chaining
	 */
	public Track album(final Album album) {
		this.album = album;
		return this;
	}

	/**
	 * <p>
	 * Returns the {@linkplain Album album} to which the track belongs.
	 * </p>
	 * 
	 * @return
	 */
	public Album album() {
		return album;
	}

	/**
	 * <p>
	 * Sets the cover art for this track.
	 * </p>
	 * 
	 * @param albumArt
	 *            The {@linkplain DownloadedItem cover art} to set
	 * @return The track with the cover art set, allowing for chaining
	 */
	public Track albumArt(final DownloadedItem albumArt) {
		this.albumArt = albumArt;
		return this;
	}

	/**
	 * <p>
	 * Returns the {@linkplain DownloadedItem cover art}.
	 * </p>
	 * 
	 * @return
	 */
	public DownloadedItem albumArt() {
		return albumArt;
	}

	/**
	 * <p>
	 * Returns the MIME type of the downloaded track (for example
	 * {@code audio/mp3}), or {@code null} if it has not been downloaded yet.
	 * </p>
	 * 
	 * @return
	 */
	public String mimeType() {
		if (downloadedItem != null) {
			return downloadedItem.mimeType();
		}

		return null;
	}

	@Override
	public String toString() {
		return "[" + getClass().getSimpleName() + ":" + title + "]";
	}
}
