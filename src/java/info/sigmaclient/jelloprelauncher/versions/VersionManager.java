package info.sigmaclient.jelloprelauncher.versions;

import info.sigmaclient.jelloprelauncher.Utils;

import java.io.File;
import java.util.HashMap;

public class VersionManager {
    HashMap<String, Version> versions = new HashMap<>();

    public VersionManager() {
        if (!getVersionFolder().exists()) {
            getVersionFolder().mkdirs();
        }

        this.versions.put("1.15.2", new Version("1.15.2", "https://jelloprg.sigmaclient.cloud/download/master/d1f4a078c47537bc55fec91d18654937226fe9d3", false));
        this.versions.put("1.16-rc1", new Version("1.16-rc1", "https://jelloprg.sigmaclient.cloud/download/master/b894c480c5a309a6dc22ec4cefbc947a8bc6af93", false));
        this.versions.put("pojav", new Version("pojav", "https://github.com/Sigma-Skidder-Team/SigmaRebase/releases/download/pojav-nightly/sigma-jello-5.1.0.json", false));
        this.versions.put("nightly", new Version("nightly", "https://github.com/Sigma-Skidder-Team/SigmaRebase/releases/download/nightly/sigma-jello-5.1.0.json", false));
        this.versions.put("release", new Version("release", "https://github.com/Sigma-Skidder-Team/SigmaRebase/releases/latest/download/1.16.4.json", false));
    }

    public HashMap<String, Version> getVersions() {
        return this.versions;
    }

    public static File getVersionFolder() {
        return new File(Utils.getSigmaDirectory(), "versions");
    }
}
