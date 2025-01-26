package info.sigmaclient.jelloprelauncher.resources;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import info.sigmaclient.jelloprelauncher.DownloadProgress;
import info.sigmaclient.jelloprelauncher.Utils;
import info.sigmaclient.jelloprelauncher.resources.type.Asset;
import info.sigmaclient.jelloprelauncher.resources.type.Client;
import info.sigmaclient.jelloprelauncher.resources.type.Library;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ResourceManager {
    private final List<Library> libraries = new ArrayList<>();
    private final List<Asset> assets = new ArrayList<>();
    private final JsonObject versionJson;
    private JsonObject assetsJson;
    private final Client client;

    public ResourceManager(File jsonFile) throws IOException {
        try (Scanner scanner = new Scanner(jsonFile)) {
            String text = scanner.useDelimiter("\\A").next();
            this.versionJson = Json.parse(text).asObject();
            parseLibraries();
            String versionName = versionJson.getString("id", null);
            String versionAssets = versionJson.getString("assets", null);
            this.client = new Client(null, null, 0, versionName, versionAssets);
        }
    }

    public ResourceManager(String jsonUrl) {
        this.versionJson = Utils.queryJson(jsonUrl);
        String versionName = versionJson.getString("id", null);
        String versionAssets = versionJson.getString("assets", null);

        JsonObject assetIndex = versionJson.get("assetIndex").asObject();
        String assetsUrl = assetIndex.getString("url", null);
        this.assetsJson = Utils.queryJson(assetsUrl);

        JsonObject downloads = versionJson.get("downloads").asObject().get("client").asObject();
        String clientSha1 = downloads.getString("sha1", null);
        String clientUrl = downloads.getString("url", null);
        int clientSize = downloads.getInt("size", 0);

        this.client = new Client(clientUrl, clientSha1, clientSize, versionName, versionAssets);
        parseAssets();
        parseLibraries();
    }

    private void parseAssets() {
        JsonObject assetObjects = assetsJson.get("objects").asObject();
        for (String name : assetObjects.names()) {
            JsonObject asset = assetObjects.get(name).asObject();
            this.assets.add(new Asset(name, asset.getString("hash", null), asset.getInt("size", 0)));
        }
    }

    private void parseLibraries() {
        String osName = Utils.getPlatformName();
        String arch = System.getProperty("sun.arch.data.model");
        JsonArray librariesArray = versionJson.get("libraries").asArray();

        for (JsonValue value : librariesArray) {
            JsonObject libraryJson = value.asObject();
            if (isAllowedByRules(libraryJson, osName)) {
                JsonObject downloads = libraryJson.get("downloads").asObject().get("artifact").asObject();
                String path = downloads.getString("path", null);

                if (!path.contains("realms")) {
                    libraries.add(new Library(path, downloads.getString("sha1", null), downloads.getInt("size", 0), downloads.getString("url", null)));

                    // Handle natives
                    if (libraryJson.get("natives") != null) {
                        handleNatives(libraryJson, osName, arch);
                    }
                }
            }
        }
    }

    private void handleNatives(JsonObject libraryJson, String osName, String arch) {
        JsonObject natives = libraryJson.get("natives").asObject();
        String nativeName = natives.getString(osName, natives.getString(osName + arch, null));

        if (nativeName != null) {
            JsonObject classifiers = libraryJson.get("downloads").asObject().get("classifiers").asObject();
            JsonObject nativeJson = classifiers.get(nativeName).asObject();

            String path = nativeJson.getString("path", null);
            if (nativeJson.getString("url", null).endsWith(".zip")) {
                path = path + "natives.zip";
            }

            libraries.add(new Library(path, nativeJson.getString("sha1", null), nativeJson.getInt("size", 0), nativeJson.getString("url", null)));
        }
    }

    private boolean isAllowedByRules(JsonObject libraryJson, String osName) {
        if (libraryJson.get("rules") == null) return true;

        boolean allowed = false;
        for (JsonValue ruleVal : libraryJson.get("rules").asArray()) {
            JsonObject rule = ruleVal.asObject();
            JsonObject os = rule.get("os") != null ? rule.get("os").asObject() : null;

            if (os == null || osName.equals(os.getString("name", null))) {
                allowed = "allow".equals(rule.getString("action", null));
            }
        }
        return allowed;
    }

    public void download(DownloadProgress progress) throws IOException {
        int totalSize = calculateTotalDownloadSize();
        System.out.println("Size to download: " + totalSize + "B");

        int downloadedSize = 0;
        for (Asset asset : assets) {
            downloadedSize += downloadAsset(asset, progress, totalSize, downloadedSize);
        }

        for (Library library : libraries) {
            downloadedSize += downloadLibrary(library, progress, totalSize, downloadedSize);
        }

        downloadedSize += downloadClient(progress, totalSize, downloadedSize);
        saveJsonFiles();
    }

    private int calculateTotalDownloadSize() {
        return assets.stream().mapToInt(Asset::getSize).sum()
                + libraries.stream().mapToInt(Library::getSize).sum()
                + client.getSize();
    }

    private int downloadAsset(Asset asset, DownloadProgress progress, int totalSize, int downloadedSize) {
        File destination = new File(Utils.getWorkingDirectory(), "assets" + File.separator + "objects" + File.separator + asset.getHash().substring(0, 2) + File.separator + asset.getHash());
        if (!destination.exists()) {
            Utils.downloadFileFromUrl("https://resources.download.minecraft.net/" + asset.getHash().substring(0, 2) + "/" + asset.getHash(), destination, null);
            progress.update(downloadedSize, totalSize);
            return asset.getSize();
        }
        return 0;
    }

    private int downloadLibrary(Library library, DownloadProgress progress, int totalSize, int downloadedSize) throws IOException {
        File destination = new File(Utils.getWorkingDirectory(), "libraries" + File.separator + library.getPath().replace("//", File.separator));

        String sha1 = library.getHash();
        String sha2 = Utils.getFileSha1Sum(destination);

        if (!destination.exists() || !Objects.equals(sha2, sha1)) {
            Utils.downloadFileFromUrl(library.getUrl(), destination, null);
            System.out.println(library.getUrl() + " - " + library.getHash());
            progress.update(downloadedSize, totalSize);
            extractLibraryIfNeeded(library, destination);
            return library.getSize();
        }
        return 0;
    }

    private void extractLibraryIfNeeded(Library library, File destination) throws IOException {
        if (library.getUrl().endsWith(".zip")) {
            try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(destination.toPath()))) {
                ZipEntry ze;
                byte[] buffer = new byte[4096];

                while ((ze = zis.getNextEntry()) != null) {
                    File destFile = new File(destination.getParent(), ze.getName());
                    if (ze.isDirectory()) {
                        destFile.mkdirs();
                    } else {
                        try (FileOutputStream fos = new FileOutputStream(destFile)) {
                            int read;
                            while ((read = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, read);
                            }
                        }
                    }
                }
            }
        }
    }

    private int downloadClient(DownloadProgress progress, int totalSize, int downloadedSize) {
        File clientPath = getClientPath();

        String sha1 = client.getHash();
        String sha2 = Utils.getFileSha1Sum(clientPath);

        if (!clientPath.exists() || !Objects.equals(sha2, sha1)) {
            Utils.downloadFileFromUrl(client.getUrl(), clientPath, (size, total) -> progress.update(downloadedSize + size, totalSize));
            return client.getSize();
        }
        return 0;
    }

    private void saveJsonFiles() throws IOException {
        Files.write(getClientJsonPath().toPath(), versionJson.toString().getBytes());
        Files.write(getAssetsJsonPath().toPath(), assetsJson.toString().getBytes());
    }

    public File getClientPath() {
        return new File(client.getClientWorkingDir(), client.getName() + ".jar");
    }

    public File getClientJsonPath() {
        return new File(client.getClientWorkingDir(), client.getName() + ".json");
    }

    public File getAssetsJsonPath() {
        return new File(Utils.getWorkingDirectory(), "assets" + File.separator + "indexes" + File.separator + client.getAssetsVersion() + ".json");
    }

    public List<Asset> getAssets() {
        return assets;
    }

    public List<Library> getLibraries() {
        return libraries;
    }

    public Client getClient() {
        return client;
    }
}
