package info.sigmaclient.jelloprelauncher;

import info.sigmaclient.jelloprelauncher.gui.DownloadFrame;
import info.sigmaclient.jelloprelauncher.ressources.RessourceManager;
import info.sigmaclient.jelloprelauncher.ressources.type.Library;
import info.sigmaclient.jelloprelauncher.versions.Version;
import info.sigmaclient.jelloprelauncher.versions.VersionManager;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.swing.SwingUtilities;

public class JelloPrelauncher {
    public static JelloPrelauncher shared;
    DownloadFrame df;
    private static String[] launchArgs;
    private static File sigmaDir = Utils.getSigmaDirectory();
    private static File jreDir;
    private Version toLaunch;
    private VersionManager versionManager;

    public static void main(String[] args) {
        shared = new JelloPrelauncher(args);
    }

    public static RessourceManager getRessourceManager(Version version) throws IOException {
        System.out.println(version.getDisplayName());
        return version.isOffline() ? new RessourceManager(new File(version.getUrl())) : new RessourceManager(version.getUrl());
    }

    public JelloPrelauncher(String[] args) {
        if (!sigmaDir.exists()) {
            sigmaDir.mkdirs();
        }

        System.out.println("Starting...");
        new File(sigmaDir, "SigmaJello.jar");
        launchArgs = args;
        this.versionManager = new VersionManager("https://jelloprg.sigmaclient.cloud/version_manifest.json");
        this.setupWindow();
        this.df.setVersions(this.versionManager.getVersions());
        versionManager.getVersions().forEach((s, version) -> {
            String display = version.getDisplayName();
            if (display.contains("Nightly") && !display.contains("Pojav")) {
                toLaunch = version;
            }
        });
    }

    public void setupRuntime() {
        String platform;
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
            if (osName.toLowerCase().contains("windows")) {
                platform = "windows";
            } else {
                platform = "linux";
            }
        } else {
            platform = "mac";
        }

        String jre = "jre";
        if (this.toLaunch.getDisplayName().contains("Nightly") && !this.toLaunch.getDisplayName().contains("Pojav")) {
            jre = "jre17";
            jreDir = new File(sigmaDir, "jre17.0.15");
        }

        if (!jreDir.exists()) {
            try {
                File temporaryJreFile = File.createTempFile("sigma", jre);
                Utils.downloadFileFromUrl("https://jelloprg.sigmaclient.cloud/download/" + platform + "/" + jre, temporaryJreFile, (totalDownloadedSize, totalFileSize) -> this.df.setProgress((int) (100L * totalDownloadedSize / totalFileSize), "Updating Runtime"));
                byte[] buffer = new byte[1024];
                ZipInputStream zis = new ZipInputStream(Files.newInputStream(temporaryJreFile.toPath()));

                for (ZipEntry zipEntry = zis.getNextEntry(); zipEntry != null; zipEntry = zis.getNextEntry()) {
                    File newFile = new File(sigmaDir, zipEntry.getName());
                    if (zipEntry.isDirectory()) {
                        newFile.mkdirs();
                    } else {
                        FileOutputStream fos = new FileOutputStream(newFile);

                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }

                        fos.close();
                    }
                }

                zis.closeEntry();
                zis.close();
            } catch (IOException var12) {
                var12.printStackTrace();
            }

            for (File f : this.getFilesRecursive(jreDir)) {
                f.setExecutable(true);
            }
        }
    }

    private void setupWindow() {
        if (this.df == null) {
            SwingUtilities.invokeLater(() -> JelloPrelauncher.this.df = new DownloadFrame());
        }

        while (this.df == null) {
            try {
                Thread.sleep(200L);
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }
        }
    }

    private ArrayList<File> getFilesRecursive(File pFile) {
        ArrayList f = new ArrayList();
        File[] v3 = pFile.listFiles();
        int i4 = v3.length;

        for (int i5 = 0; i5 < i4; ++i5) {
            File files = v3[i5];
            if (files.isDirectory()) {
                f.addAll(this.getFilesRecursive(files));
            } else {
                f.add(files);
            }
        }

        return f;
    }

    public static void launchGame(final RessourceManager manager, final String jreBinLoc, final boolean macos) {
        final String mainClass = "net.minecraft.client.main.Main";
        final String cpSeparator = System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":";

        StringBuilder classPathBuilder = new StringBuilder(manager.getClientPath().getAbsolutePath());
        for (Library lib : manager.getLibraries()) {
            classPathBuilder.append(cpSeparator).append(lib.getFilePath().getAbsolutePath());
        }

        List<String> jvmArgs = new ArrayList<>();
        jvmArgs.add(jreBinLoc); // full path to java binary
        if (macos) {
            jvmArgs.add("-XstartOnFirstThread");
        }

        // Optional: JVM tuning or custom arguments from current process
        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        for (String arg : inputArguments) {
            if (!arg.contains("-agentlib")) { // exclude things like agentlib if not needed
                jvmArgs.add(arg);
            }
        }

        jvmArgs.add("-cp");
        jvmArgs.add(classPathBuilder.toString());
        jvmArgs.add(mainClass);

        if (JelloPrelauncher.launchArgs == null || JelloPrelauncher.launchArgs.length == 0) {
            String assets = new File(Utils.getWorkingDirectory(), "assets").getAbsolutePath();
            JelloPrelauncher.launchArgs = new String[]{
                    "--version", "mcp",
                    "--accessToken", "0",
                    "--assetsDir", assets,
                    "--assetIndex", manager.getClient().getAssetsVersion(),
                    "--userProperties", "{}"
            };
        }

        jvmArgs.addAll(Arrays.asList(JelloPrelauncher.launchArgs));

        System.out.println("Launching game with arguments:");
        for (String arg : jvmArgs) {
            System.out.println(arg);
        }

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
            processBuilder.directory(Utils.getWorkingDirectory()); // optional: set working dir
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Game process exited with code: " + exitCode);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void play() {
        this.df.setProgress(0, "Launching Client");
        (new Thread(() -> {
            this.setupRuntime();
            RessourceManager ressourceManager = null;
            try {
                ressourceManager = getRessourceManager(this.toLaunch);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            if (!this.toLaunch.isOffline()) {
                try {
                    ressourceManager.download((current, total) -> {
                        this.df.setProgress((int) (100L * current / total), "Updating Client");
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            this.df.setVisible(false);
            boolean i2 = false;
            String osName = System.getProperty("os.name");
            String jreBinLoc;
            if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
                if (osName.toLowerCase().contains("windows")) {
                    jreBinLoc = jreDir.getAbsolutePath() + File.separator + "bin" + File.separator + "java.exe";
                } else {
                    jreBinLoc = jreDir.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
                }
            } else {
                jreBinLoc = jreDir.getAbsolutePath() + File.separator + "Contents" + File.separator + "Home" + File.separator + "bin" + File.separator + "java";
                i2 = true;
            }

            launchGame(ressourceManager, jreBinLoc, i2);
            System.exit(0);
        })).start();
    }

    public void setVersion(String s) {
        Iterator v2 = this.versionManager.getVersions().entrySet().iterator();

        while (v2.hasNext()) {
            Entry entry = (Entry) v2.next();
            if (s.equals(((Version) entry.getValue()).getDisplayName())) {
                this.toLaunch = (Version) entry.getValue();
            }
        }
    }

    static {
        jreDir = new File(sigmaDir, "jre1.8.0_202");
    }
}
