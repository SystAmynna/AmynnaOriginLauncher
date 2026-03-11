package com.amynna.OriginLauncher.download;

public interface DownloadContainer {

    void setDownloader(Downloader downloader);

    Downloader getDownloader();

    default void download() {
        getDownloader().download();
    }

}
