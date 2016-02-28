package com.github.codepain.mediadownload.reader;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Method;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
import com.github.codepain.mediadownload.music.LooseTrackSet;
import com.github.codepain.mediadownload.music.Track;

/**
 * <p>
 * A {@link Reader} for web pages on {@code hearthis.at}.
 * </p>
 * 
 * @author codepain
 *
 */
public class HearThisAtReader extends Reader {

	private static final String SERVICE_URL = "https://hearthis.at/user_ajax_more.php";

	private static final Pattern userIdPattern = Pattern.compile("intInternalId = (\\d+);");

	public HearThisAtReader(final URL url) {
		super(url);
	}

	@Override
	protected Downloadable fetchDownloadable() throws IOException {
		trigger(EventType.READER_STATUS, "reading " + url);
		final Document document = Jsoup.connect(url.toString()).get();

		// check for a single track
		final Element singleTrackElement = document.body().select(".playlist.top [data-mp3]").first();
		if (singleTrackElement != null) {
			// gather all needed data
			final URL downloadUrl = new URL(singleTrackElement.attr("abs:data-mp3"));
			final String artist = document.body().select("[data-userid]").first().text();
			final URL coverArtUrl = new URL(document.select("meta[property=og:image]").first().attr("abs:content"));
			final String title = document.select("meta[property=og:title]").first().attr("content");

			// download cover art
			final DownloadedItem coverArt = download(coverArtUrl);

			return new Track(title, downloadUrl).artist(artist).albumArt(coverArt);
		}

		// fetch all tracks we can get
		Elements trackElements = document.body().select("[data-mp3]");

		// if none found, try to fetch some of the user
		if (trackElements.isEmpty()) {
			// get the user ID
			final Matcher matcher = userIdPattern.matcher(document.body().select("script").html());
			if (matcher.find()) {
				Integer userId = null;
				try {
					userId = Integer.valueOf(matcher.group(1));
				} catch (final NumberFormatException e) {
					throw new IOException("Unable to parse the user ID: " + matcher.group(1), e);
				}

				// assemble the parameters for requesting more tracks
				final Map<String, String> params = new HashMap<>();
				params.put("user", userId.toString());
				params.put("start", "");
				params.put("end", "");
				params.put("filter", "");
				params.put("searchtext", "");

				// loop, loop, loop, ...
				int index = 0;
				Document tracks = null;
				final LooseTrackSet looseTrackSet = new LooseTrackSet(url);
				do {
					params.put("min", "" + index * 10);
					params.put("max", "" + (index + 1) * 10);

					tracks = Jsoup.parseBodyFragment(
							injectCommon(Jsoup.connect(SERVICE_URL).data(params).method(Method.POST)).execute().body());
					trackElements = tracks.select("[data-mp3]");
					for (final Element trackElement : trackElements) {
						final URL downloadUrl = new URL(trackElement.attr("abs:data-mp3"));
						final Element parent = trackElement.parent();
						final String title = parent.attr("data-playlist-title");
						final Track track = new Track(title, downloadUrl);

						// artist
						final String artist = parent.attr("data-playlist-author").substring("by ".length());
						track.artist(artist);

						// download cover art
						try {
							final URL coverArtUrl = new URL(parent.attr("abs:data-playlist-image"));
							final DownloadedItem coverArt = download(coverArtUrl);
							track.albumArt(coverArt);
						} catch (final IOException e) {
							// nevermind, not that bad
						}

						looseTrackSet.add(track);
					}

					index++;
				} while (!trackElements.isEmpty());

				return looseTrackSet;
			}
		} else {
			final LooseTrackSet looseTrackSet = new LooseTrackSet(url);
			for (final Element trackElement : trackElements) {
				final URL downloadUrl = new URL(trackElement.attr("abs:data-mp3"));
				final Element parent = trackElement.parent();
				final String title = parent.attr("data-playlist-title");
				final Track track = new Track(title, downloadUrl);

				// artist
				final String artist = parent.attr("data-playlist-author").substring("by ".length());
				track.artist(artist);

				// download cover art
				try {
					final URL coverArtUrl = new URL(parent.attr("abs:data-playlist-image"));
					final DownloadedItem coverArt = download(coverArtUrl);
					track.albumArt(coverArt);
				} catch (final IOException e) {
					// nevermind, not that bad
				}

				looseTrackSet.add(track);
			}

			return looseTrackSet;
		}

		throw new IOException("Unable to interpret [" + url + "]");
	}

	@Override
	protected void onEvent(final Event event) {
		// doing nothing for now
	}

}
