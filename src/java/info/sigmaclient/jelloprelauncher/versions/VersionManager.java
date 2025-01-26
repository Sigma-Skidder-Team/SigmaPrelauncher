package info.sigmaclient.jelloprelauncher.versions;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import info.sigmaclient.jelloprelauncher.Utils;

import java.io.File;
import java.util.HashMap;

public class VersionManager {
    HashMap<String, Version> versions = new HashMap<>();

    public VersionManager(String versionUrl) {
        if (!getVersionFolder().exists()) {
            getVersionFolder().mkdirs();
        }

        File[] listedFiles = getVersionFolder().listFiles();

        for (File file : listedFiles) {
            String name = file.getName();
            if ((new File(file, name + ".jar")).exists() && (new File(file, name + ".json")).exists()) {
                this.versions.put(name, new Version(name, (new File(file, name + ".json")).getAbsolutePath(), true));
            }
        }

        JsonObject mainJson = Utils.queryJson(versionUrl);
        if (mainJson != null) {
            JsonArray versionArrayJson = mainJson.get("versions").asArray();
            if (!versionArrayJson.isEmpty()) {
                this.versions.clear();
            }

            for (JsonValue value : versionArrayJson) {
                JsonObject versionInfo = value.asObject();
                this.versions.remove(versionInfo.getString("id", null));
                this.versions.put(versionInfo.getString("id", null), new Version(versionInfo.getString("id", null), versionInfo.getString("url", null), false));
            }
        }
    }

    public HashMap<String, Version> getVersions() {
        return this.versions;
    }

    public static File getVersionFolder() {
        return new File(Utils.getSigmaDirectory(), "versions");
    }
}
