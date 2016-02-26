package com.github.codepain.mediadownload.listener;

import com.github.codepain.mediadownload.download.Downloadable;
import com.github.codepain.mediadownload.reader.Reader;

/**
 * <p>
 * The several types of {@linkplain Event events}.
 * </p>
 * 
 * @author codepain
 *
 */
public enum EventType {

	/**
	 * The status of a {@link Reader} changed
	 */
	READER_STATUS,
	
	/**
	 * All sub items of a {@link Downloadable} were found
	 */
	SUB_ITEMS_FOUND,
	
	/**
	 * The {@link Downloadable} item got disqualified, so it does not get processed
	 */
	ITEM_DISQUALIFIED,
	
	/**
	 * A download is in progress
	 */
	DOWNLOAD_PROGRESS,
	
	/**
	 * The download has finished
	 */
	DOWNLOAD_FINISHED,
	
	/**
	 * The save process starts, i.e. saving anything on the disk
	 */
	SAVE_START,
	
	/**
	 * The save process finished, i.e. all data has been written to the disk
	 */
	SAVE_FINISHED,
	
	/**
	 * Any error occurred
	 */
	ERROR;
}
