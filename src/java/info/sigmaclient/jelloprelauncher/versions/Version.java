package info.sigmaclient.jelloprelauncher.versions;

public class Version {
    private final String id, url;
    private final boolean offline;

    public Version(String id, String url, boolean offline) {
        this.id = id;
        this.url = url;
        this.offline = offline;
    }

    public String getId() {
        return this.id;
    }

    public String getUrl() {
        return this.url;
    }

    public boolean isOffline() {
        return this.offline;
    }

    public String getDisplayName() {
        return this.id;
    }
}
