# media-download

[![](https://jitpack.io/v/codepain/media-download.svg)](https://jitpack.io/#codepain/media-download)

A Java library for downloading media files like MP3 from various sites:
* [Soundcloud](https://soundcloud.com)
* [Bandcamp](http://bandcamp.com)
* [HearThisAt](http://hearthis.at)

### Include in your build

When using Gradle:
* Add the repository:
```
repositories {
	...
	maven { url "https://jitpack.io" }
}
```
* Add the dependency:
```
dependencies {
  compile 'com.github.codepain:media-download:0.2.0'
}
```

If you are using other build tools, take a look at [jitpack.io](https://jitpack.io/#codepain/media-download) how to include it.

### Usage

The simplest way to download some files:
```java
URL url = new URL(urlToWebPage);
Path rootPath = Paths.get("/path/to/save/location");
MediaDownload.read(url).save(rootPath);
```

If you want a more verbose output, you can hook into the [listener chain](https://github.com/codepain/media-download/wiki/Listener):
```java
MediaDownload.read(url).listener(
  event -> 
    System.out.println(event.type() + ": " + event.source() + " / " + event.eventObject())
  )
).save(rootPath);
```

You can influence the process by setting some [options](https://github.com/codepain/media-download/wiki/Options).
