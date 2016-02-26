package com.github.codepain.mediadownload.reader;

import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.music.Album;
import com.github.codepain.mediadownload.music.Discography;
import com.github.codepain.mediadownload.music.Track;

/**
 * <p>
 * A {@link Reader} for web pages on {@code bandcamp.com}.
 * </p>
 * 
 * @author codepain
 *
 */
public class BandcampReader extends Reader {

	/**
	 * <p>
	 * Constructs a {@link BandcampReader}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the Bandcamp page
	 */
	public BandcampReader(final URL url) {
		super(url);
	}

	@Override
	public Downloadable fetchDownloadable() throws IOException {
		trigger(EventType.READER_STATUS, "reading " + url);
		final Document document = Jsoup.connect(url.toString()).get();

		final Element albumList = document.body().select("ol.music-grid").first();
		if (albumList != null) {
			final Discography discography = readDiscography(url);
			if (discography.albums().isEmpty()) {
				throw new IOException(Discography.class.getSimpleName() + " is empty");
			}

			return discography;
		} else {
			final Album album = readAlbum(url);
			if (album == null) {
				throw new IOException(Album.class.getSimpleName() + " is disqualified, because it is a sampler");
			}

			return album;
		}
	}

	private Discography readDiscography(final URL url) throws IOException {
		trigger(EventType.READER_STATUS, "reading discography [" + url + "]");
		final Discography discography = new Discography(url);

		final Document document = Jsoup.connect(url.toString()).get();
		final Elements albumList = document.body().select("ol.music-grid").first().select("li a");
		trigger(EventType.READER_STATUS, "reading " + albumList.size() + " album(s)");
		int index = 0;
		for (final Element albumItem : albumList) {
			index++;
			final URL albumUrl = new URL(albumItem.attr("abs:href"));
			trigger(EventType.READER_STATUS, "reading album " + index + "/" + albumList.size() + " [" + albumUrl + "]");
			final Album album = readAlbum(albumUrl);
			if (album != null) {
				discography.add(album);
			}
		}

		trigger(EventType.SUB_ITEMS_FOUND, discography);
		return discography;
	}

	private Album readAlbum(final URL url) throws IOException {
		trigger(EventType.READER_STATUS, "reading album [" + url + "]");
		final Document document = Jsoup.connect(url.toString()).get();

		// check whether it is a sampler
		if (!options.loadSamplers()) {
			// on bandcamp there is no really safe way to identify a sampler,
			// only indicators ... we assume that one indicator is enough to
			// qualify an album as a sampler

			// does it contain a tag "compilation"?
			if (!document.select(".tag:containsOwn(compilation)").isEmpty()) {
				trigger(EventType.ITEM_DISQUALIFIED, Album.class.getSimpleName() + " with URL [" + url
						+ "] is disqualified, because it is a sampler");
				return null;
			}
		}

		final Album album = new Album(url);
		final DateFormat df = new SimpleDateFormat("dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
		final Elements scripts = document.select("script");
		for (final Element script : scripts) {
			final String js = script.html();
			if (js.contains("var TralbumData")) {
				final String[] lines = js.split("\n", -1);
				final StringBuilder sb = new StringBuilder();
				boolean gather = false;
				for (final String line : lines) {
					if (line.contains("var TralbumData")) {
						sb.append(line);
						sb.append("\n");
						gather = true;
					} else if (gather) {
						if (line.contains("};")) {
							sb.append(line.substring(0, line.indexOf(';')));
							break;
						} else if (!line.trim().startsWith("//")) {
							sb.append(line);
							sb.append("\n");
						}
					}
				}

				final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
				try {
					engine.eval(sb.toString());
					album.title((String) engine.eval("TralbumData.current.title"));
					final String artist = (String) engine.eval("TralbumData.artist");
					album.artist(artist);

					// read year of release date
					String year = null;
					try {
						final Date releaseDate = df.parse((String) engine.eval("TralbumData.current.release_date"));
						final Calendar cal = Calendar.getInstance();
						cal.setTime(releaseDate);
						year = "" + cal.get(Calendar.YEAR);
					} catch (final NullPointerException | ParseException e) {
						// nevermind, it's just the year
					}

					// album art
					try {
						final URL albumArt = new URL((String) engine.eval("TralbumData.artFullsizeUrl"));
						final DownloadedItem downloadedItem = download(albumArt);
						album.albumArt(downloadedItem);
					} catch (final NullPointerException | IOException e) {
						// okay, let's at least try to get the thumb
						try {
							final URL albumArtThumb = new URL((String) engine.eval("TralbumData.artThumbURL"));
							final DownloadedItem downloadedItem = download(albumArtThumb);
							album.albumArt(downloadedItem);
						} catch (final NullPointerException | IOException e1) {
							// nevermind, it's just meta information
						}
					}

					for (int i = 0; i < (long) engine.eval("TralbumData.trackinfo.length"); i++) {
						try {
							final String title = (String) engine.eval("TralbumData.trackinfo[" + i + "].title");
							final URL trackUrl = new URL(album.url().getProtocol() + ":"
									+ (String) engine.eval("TralbumData.trackinfo[" + i + "].file['mp3-128']"));

							final Track track = new Track(title, trackUrl);
							track.artist(artist);
							final Object trackNum = engine.eval("TralbumData.trackinfo[" + i + "].track_num");
							if (trackNum != null) {
								track.index((int) trackNum);
							}
							track.year(year);

							album.add(track);
						} catch (final ScriptException e) {
							trigger(EventType.ERROR, "Unable to interpret track #" + i + " (" + e.getMessage() + ")");
						}
					}
				} catch (final ScriptException e) {
					throw new IOException("Unable to read album: " + url, e);
				}

				break;
			}
		}

		trigger(EventType.SUB_ITEMS_FOUND, album);
		return album;
	}

	@Override
	protected void onEvent(final Event event) {
		// doing nothing for now
	}

}
