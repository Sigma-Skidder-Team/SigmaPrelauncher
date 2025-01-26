package info.sigmaclient.jelloprelauncher;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Utils {
    public static String queryUrl(String url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
                return sb.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static JsonObject queryJson(String url) {
        String query = queryUrl(url);
        return (query == null) ? null : Json.parse(query.replace("\"size\": ,", "")).asObject();
    }

    private static final char[] hexArray = "0123456789abcdef".toCharArray();

    public static String getFileSha1Sum(File file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream fis = Files.newInputStream(file.toPath())) {
                byte[] buffer = new byte[4096];
                int n;
                while ((n = fis.read(buffer)) != -1) {
                    digest.update(buffer, 0, n);
                }
            }
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException | IOException e) {
            return "";
        }
    }

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String getPlatformName() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.startsWith("mac") || osName.startsWith("darwin")) {
            return "osx";
        } else if (osName.contains("windows")) {
            return "windows";
        } else {
            return "linux";
        }
    }

    public static void downloadFileFromUrl(String url, File file, DownloadProgress progress) {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try {
            HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
            long totalFileSize = con.getContentLengthLong();
            try (InputStream is = con.getInputStream();
                 FileOutputStream fos = new FileOutputStream(file)) {

                byte[] buffer = new byte[8192];
                long totalDownloadedSize = 0;
                int bytesRead;
                int counter = 0;

                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    totalDownloadedSize += bytesRead;
                    if (++counter > 50 && progress != null) {
                        progress.update(totalDownloadedSize, totalFileSize);
                        counter = 0;
                    }
                }

                if (progress != null) {
                    progress.update(totalFileSize, totalFileSize);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static File getWorkingDirectory() {
        String userHome = System.getProperty("user.home", ".");
        String platform = getPlatformName();

        switch (platform) {
            case "linux":
                return new File(userHome, ".minecraft/");
            case "windows":
                return new File(System.getenv("APPDATA"), ".minecraft/");
            case "osx":
                return new File(userHome, "Library/Application Support/minecraft");
            default:
                return new File(userHome, "minecraft/");
        }
    }

    public static File getSigmaDirectory() {
        return new File(getWorkingDirectory(), "Sigma");
    }
}
