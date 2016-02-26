package com.github.codepain.mediadownload.music;

import java.net.URL;
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
 * A {@linkplain Downloadable downloadable item} that bundles several
 * {@linkplain Album albums}.
 * </p>
 * 
 * @author codepain
 *
 */
public class Discography extends Downloadable {

	private final URL url;

	private final List<Album> albums = new ArrayList<>();

	private BundleDownload download;

	private boolean downloadFinished;

	/**
	 * <p>
	 * Constructs a {@link Discography} with the specified {@link URL}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the discography
	 */
	public Discography(URL url) {
		this.url = url;
	}

	/**
	 * <p>
	 * Adds the {@linkplain Album album} to this discography.
	 * </p>
	 * 
	 * @param album
	 *            The {@link Album} to add
	 * @throws NullPointerException
	 *             If the album is {@code null}
	 */
	public void add(final Album album) {
		albums.add(Objects.requireNonNull(album));
	}

	public List<Album> albums() {
		return new ArrayList<>(albums);
	}

	@Override
	public Download download() {
		download = (BundleDownload) new BundleDownload(this, 1).listener(this);

		for (final Album album : albums) {
			download.add(album.listener(download).download());
		}

		return download;
	}

	@Override
	public void save(final SaveOptions options) {
		if (downloadFinished) {
			// already downloaded, just save it
			trigger(EventType.SAVE_START, "saving discography with " + albums.size() + " albums");
			for (final Album album : albums) {
				album.save(options);
			}
			trigger(EventType.SAVE_FINISHED, this);
		} else {
			// not downloaded yet ...
			// start the download and save it then
			final Discography that = this;
			download().start().whenFinished(downloadItem -> that.save(options));
		}
	}

	@Override
	protected void onEvent(final Event event) {
		if (EventType.DOWNLOAD_FINISHED.equals(event.type()) && event.source() == download) {
			downloadFinished = true;
		}
	}

	/**
	 * <p>
	 * Returns the {@link URL} of this discography.
	 * </p>
	 * 
	 * @return
	 */
	public URL url() {
		return url;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + albums;
	}
}
