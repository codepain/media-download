package com.github.codepain.mediadownload;

import java.io.IOException;
import java.net.URL;

import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.reader.BandcampReader;
import com.github.codepain.mediadownload.reader.Reader;
import com.github.codepain.mediadownload.reader.SoundcloudReader;

/**
 * <p>
 * Allows to download the media files of various sites, such as
 * {@code bandcamp.com} or {@code soundcloud.com}.
 * </p>
 * <p>
 * The simplest way to fetch some files is
 * {@code MediaDownload.read(url).save(rootPath)}.
 * </p>
 * 
 * @author codepain
 *
 */
public final class MediaDownload {

	private MediaDownload() {
	}

	/**
	 * <p>
	 * Connects to the {@link URL}, i.e. fetches an appropriate {@link Reader}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the web page with the media file(s) on it
	 * @return The {@linkplain Downloadable downloadable item}
	 * @throws IOException
	 *             If no {@link Reader} is available that can handle the web
	 *             page
	 * @throws NullPointerException
	 *             If the parameter is {@code null}
	 */
	public static Reader connect(final URL url) throws IOException {
		if (url == null) {
			throw new NullPointerException("URL must not be null");
		}

		Reader reader = null;
		if (url.getHost().toLowerCase().endsWith("bandcamp.com")) {
			reader = new BandcampReader(url);
		} else if (url.getHost().toLowerCase().endsWith("soundcloud.com")) {
			reader = new SoundcloudReader(url);
		}

		if (reader == null) {
			throw new IOException("No reader available for " + url);
		}

		return reader;
	}

	/**
	 * <p>
	 * {@linkplain #connect(URL) Connects} to the specified {@link URL} and
	 * {@linkplain Reader#read() reads} it.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the web page with the media file(s) on it
	 * @return The {@linkplain Downloadable downloadable item}
	 * @throws IOException
	 *             If connecting to or reading the page fails
	 * @throws NullPointerException
	 *             If the parameter is {@code null}
	 */
	public static Downloadable read(final URL url) throws IOException {
		return connect(url).read();
	}

}
