package com.github.codepain.mediadownload.reader;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
 * A {@link Reader} for web pages of {@code soundcloud.com}.
 * </p>
 * 
 * @author codepain
 *
 */
public class SoundcloudReader extends Reader {

	private static final String CLIENT_ID = "02gUJC0hH2ct1EGOcYXQIzRFU91c72Ea";

	private static final String APP_VERSION = "cc53575";

	private final Pattern pattern = Pattern.compile("(var c=.*?\\]),o=Date.now");

	/**
	 * <p>
	 * Constructs a {@link SoundcloudReader}.
	 * </p>
	 * 
	 * @param url
	 *            The {@link URL} of the SoundCloud page
	 */
	public SoundcloudReader(final URL url) {
		super(url);
	}

	@Override
	public Downloadable fetchDownloadable() throws IOException {
		trigger(EventType.READER_STATUS, "reading " + url);
		final Document document = Jsoup.connect(url.toString()).get();

		final Elements scripts = document.select("script");
		for (final Element script : scripts) {
			final String js = script.html();
			if (js.startsWith("webpackJsonp(")) {
				final Matcher matcher = pattern.matcher(js);
				if (matcher.find()) {
					final String code = matcher.group(1) + ";";
					return interpretJsCode(code);
				}
			}
		}

		throw new IOException("Unable to interpret [" + url + "]");
	}

	private Downloadable interpretJsCode(final String code) throws IOException {
		final ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			engine.eval(code);

			// seems to be always the last item
			engine.eval("c = c[c.length-1].data[0];");
			final JSONParser parser = new JSONParser();

			// what kind of item do we have?
			final String kind = (String) engine.eval("c.kind");
			if ("track".equalsIgnoreCase(kind)) {
				// track info
				final String title = (String) engine.eval("c.title");
				final String artist = (String) engine.eval("c.user.username");

				// read year of release date
				String year = null;
				try {
					year = ((String) engine.eval("c.release_date")).substring(0, 4);
				} catch (final NullPointerException e) {
					// then we try to use the creation date
					try {
						year = ((String) engine.eval("c.create_date")).substring(0, 4);
					} catch (final NullPointerException e1) {
						// nevermind, it's just the year
					}
				}

				// album art
				DownloadedItem albumArt = null;
				try {
					final URL albumArtUrl = new URL((String) engine.eval("c.artwork_url"));
					albumArt = download(albumArtUrl);
				} catch (final NullPointerException | IOException e) {
					// nevermind, it's just meta information
				}

				// download URL
				final URL jsonUrl = new URL((String) engine.eval("c.uri") + "/streams?client_id=" + CLIENT_ID
						+ "&app_version=" + APP_VERSION);
				final DownloadedItem jsonDownload = download(jsonUrl);
				final String jsonData = new String(jsonDownload.data());
				final JSONObject json = (JSONObject) parser.parse(jsonData);
				final URL downloadUrl = new URL((String) json.get("http_mp3_128_url"));

				return new Track(title, downloadUrl).artist(artist).year(year).albumArt(albumArt);
			} else if ("playlist".equalsIgnoreCase(kind)) {

				// fetch JSON information about that playlist
				final URL jsonUrl = new URL("https://api.soundcloud.com/playlists/" + engine.eval("c.id")
						+ "?client_id=" + CLIENT_ID + "&app_version=" + APP_VERSION);
				trigger(EventType.READER_STATUS, "fetching info of album via API [" + jsonUrl + "]");
				final DownloadedItem jsonDownload = download(jsonUrl);
				final String jsonData = new String(jsonDownload.data());
				final JSONObject json = (JSONObject) parser.parse(jsonData);

				final Album album = interpretAlbum(json, parser);
				trigger(EventType.SUB_ITEMS_FOUND, album);
				return album;
			} else if ("user".equalsIgnoreCase(kind)) {
				// load all playlists as discography
				final Discography discography = new Discography(url);

				// load JSON data about the user's playlists
				final URL jsonUrl = new URL("https://api.soundcloud.com/users/" + engine.eval("c.id")
						+ "/playlists?client_id=" + CLIENT_ID + "&app_version=" + APP_VERSION);
				trigger(EventType.READER_STATUS, "fetching info of discography via API [" + jsonUrl + "]");
				final DownloadedItem jsonDownload = download(jsonUrl);
				final String jsonData = new String(jsonDownload.data());
				final JSONArray playlists = (JSONArray) parser.parse(jsonData);

				for (final Object o : playlists) {
					final JSONObject albumJson = (JSONObject) o;
					final Album album = interpretAlbum(albumJson, parser);
					if (album != null) {
						discography.add(album);
					}
				}

				trigger(EventType.SUB_ITEMS_FOUND, discography);
				return discography;
			}
		} catch (final ScriptException | ParseException e) {
			throw new IOException("Unable to read: " + url, e);
		}

		throw new IOException("Format of the page not known");
	}

	private Album interpretAlbum(final JSONObject json, final JSONParser parser) throws IOException, ParseException {
		trigger(EventType.READER_STATUS, "reading album [" + url + "]");
		// create album and add separate tracks
		final Album album = new Album(url);

		// sampler?
		if (!options.loadSamplers() && "compilation".equals(json.get("playlist_type"))) {
			return null;
		}

		// album info itself
		album.title((String) json.get("title"));
		album.artist((String) ((JSONObject) json.get("user")).get("username"));

		// album art
		try {
			final URL albumArt = new URL((String) json.get("artwork_url"));
			final DownloadedItem downloadedItem = download(albumArt);
			album.albumArt(downloadedItem);
		} catch (final NullPointerException | IOException e) {
			// nevermind, it's just meta information
		}

		String albumReleaseYear = json.get("release_year") != null ? String.valueOf(json.get("release_year")) : null;
		if (albumReleaseYear == null) {
			try {
				albumReleaseYear = ((String) json.get("created_at")).substring(0, 4);
			} catch (final NullPointerException e) {
				// nevermind, it's meta data
			}
		}

		// fetch the tracks
		final JSONArray tracks = (JSONArray) json.get("tracks");
		int index = 0;
		for (final Object o : tracks) {
			final JSONObject jsonTrack = (JSONObject) o;
			index++;

			// data of track
			final String title = (String) jsonTrack.get("title");
			final String artist = (String) ((JSONObject) jsonTrack.get("user")).get("username");
			String year = json.get("release_year") != null ? String.valueOf(json.get("release_year")) : null;
			if (year == null) {
				try {
					year = ((String) jsonTrack.get("created_at")).substring(0, 4);
				} catch (final NullPointerException e) {
					// nevermind, it's meta data
				}
			}

			// album art of track
			DownloadedItem trackArt = null;
			try {
				final URL albumArtUrl = new URL((String) jsonTrack.get("artwork_url"));
				trackArt = download(albumArtUrl);
			} catch (final NullPointerException | IOException e) {
				// nevermind, it's just meta information
			}

			// download URL
			final URL trackJsonUrl = new URL(
					(String) jsonTrack.get("uri") + "/streams?client_id=" + CLIENT_ID + "&app_version=" + APP_VERSION);
			final DownloadedItem trackJsonDownload = download(trackJsonUrl);
			final String trackJsonData = new String(trackJsonDownload.data());
			final URL downloadUrl = new URL(
					(String) ((JSONObject) parser.parse(trackJsonData)).get("http_mp3_128_url"));

			final Track track = new Track(title, downloadUrl).artist(artist).index(index)
					.year(year == null ? albumReleaseYear : year).albumArt(trackArt);
			album.add(track);
		}

		return album;
	}

	@Override
	protected void onEvent(final Event event) {
		// doing nothing for now
	}
}
