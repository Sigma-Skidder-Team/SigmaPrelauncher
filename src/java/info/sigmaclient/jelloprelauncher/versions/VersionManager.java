package info.sigmaclient.jelloprelauncher.versions;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import info.sigmaclient.jelloprelauncher.Utils;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class VersionManager {
   HashMap<String, Version> versions = new HashMap();

   public VersionManager(String versionUrl) {
      if (!getVersionFolder().exists()) {
         getVersionFolder().mkdirs();
      }

      File[] var2 = getVersionFolder().listFiles();
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         File file = var2[var4];
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

         Iterator var9 = versionArrayJson.iterator();

         while(var9.hasNext()) {
            JsonValue value = (JsonValue)var9.next();
            JsonObject versionInfo = value.asObject();
            this.versions.remove(versionInfo.getString("id", (String)null));
            this.versions.put(versionInfo.getString("id", (String)null), new Version(versionInfo.getString("id", (String)null), versionInfo.getString("url", (String)null), false));
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
