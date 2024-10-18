package info.sigmaclient.jelloprelauncher.versions;

public class Version {
   private String id;
   private String url;
   private boolean offline;

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
       switch (this.id) {
          case "1.15.2":
             return "1.15.2 - 1.8 (Optifine)";
           case "1.16-rc1":
               return "1.16.1 - 1.8 (Optifine)";
          case "1.16.4":
             return "1.16.4 - 1.8 (Optifine)";
           case "1.21":
               return "1.21 - 1.8 (Optifine)";
           default:
               return this.id;
       }
   }
}
