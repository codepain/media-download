package com.github.codepain.mediadownload.download;

public class DownloadedItem {

	private final String mimeType;

	private final byte[] data;

	public DownloadedItem(String mimeType, byte[] data) {
		this.mimeType = mimeType;
		this.data = data;
	}

	public byte[] data() {
		return data;
	}

	public String mimeType() {
		return mimeType;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + data.length + " bytes, " + mimeType + "]";
	}
}
