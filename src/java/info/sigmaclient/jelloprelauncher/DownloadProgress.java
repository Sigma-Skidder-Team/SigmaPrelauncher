package info.sigmaclient.jelloprelauncher;

public interface DownloadProgress {
    void update(long size, long total);
}
