package com.github.codepain.mediadownload.reader;


import java.io.IOException;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.download.DownloadedItem;
import com.github.codepain.mediadownload.listener.Event;
import com.github.codepain.mediadownload.listener.EventType;
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

	public HearThisAtReader(final URL url) {
		super(url);
	}

	@Override
	protected Downloadable fetchDownloadable() throws IOException {
		trigger(EventType.READER_STATUS, "reading " + url);
		final Document document = Jsoup.connect(url.toString()).get();
		// https://hearthis.at/bestofthebest/zomboy-dip-it/
		// check for a single track
		final Element trackElement = document.body().select(".playlist.top [data-mp3]").first();
		if (trackElement != null) {
			// gather all needed data
			final URL downloadUrl = new URL(trackElement.attr("abs:data-mp3"));
			final String artist = document.body().select("[data-userid]").first().text();
			final URL coverArtUrl = new URL(document.select("meta[property=og:image]").first().attr("abs:content"));
			final String title = document.select("meta[property=og:title]").first().attr("content");
			
			// download cover art
			final DownloadedItem coverArt = download(coverArtUrl);
			
			return new Track(title, downloadUrl).artist(artist).albumArt(coverArt);
		}
		
		throw new IOException("Unable to interpret [" + url + "]");
	}

	@Override
	protected void onEvent(final Event event) {
		// doing nothing for now
	}

}
