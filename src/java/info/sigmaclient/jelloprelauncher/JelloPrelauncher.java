package info.sigmaclient.jelloprelauncher;

import info.sigmaclient.jelloprelauncher.gui.DownloadFrame;
import info.sigmaclient.jelloprelauncher.resources.ResourceManager;
import info.sigmaclient.jelloprelauncher.resources.type.Library;
import info.sigmaclient.jelloprelauncher.versions.Version;
import info.sigmaclient.jelloprelauncher.versions.VersionManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private static final File sigmaDir = Utils.getSigmaDirectory();
    private static final File jreDir;
    private Version toLaunch;
    private final VersionManager versionManager;

    public static void main(String[] args) {
        if (!sigmaDir.exists()) {
            sigmaDir.mkdirs();
        }

        shared = new JelloPrelauncher(args);
    }

    public static ResourceManager getResourceManager(Version version) throws IOException {
        return version.isOffline() ? new ResourceManager(new File(version.getUrl())) : new ResourceManager(version.getUrl());
    }

    public JelloPrelauncher(String[] args) {
        System.out.println("Starting...");
        new File(sigmaDir, "SigmaJello.jar");
        launchArgs = args;
        this.versionManager = new VersionManager("https://jelloprg.sigmaclient.cloud/version_manifest.json");
        this.setupWindow();
        Iterator<Entry<String, Version>> var3 = this.versionManager.getVersions().entrySet().iterator();
        if (var3.hasNext()) {
            Entry<String, Version> v = var3.next();
            this.toLaunch = v.getValue();
        }

        this.df.setVersions(this.versionManager.getVersions());
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

        if (!jreDir.exists()) {
            try {
                File temporaryJreFile = File.createTempFile("sigma", "jre");
                Utils.downloadFileFromUrl("https://jelloprg.sigmaclient.cloud/download/" + platform + "/jre", temporaryJreFile, (totalDownloadedSize, totalFileSize) -> this.df.setProgress((int) (100L * totalDownloadedSize / totalFileSize), "Updating Runtime"));
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
        ArrayList<File> fileList = new ArrayList<>();

        if (pFile != null && pFile.isDirectory()) {
            File[] files = pFile.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        fileList.addAll(this.getFilesRecursive(file));
                    } else {
                        fileList.add(file);
                    }
                }
            } else {
                System.err.println("Warning: listFiles() returned null for directory: " + pFile.getAbsolutePath());
            }
        } else {
            System.err.println("Warning: Provided file is not a valid directory: " + (pFile != null ? pFile.getAbsolutePath() : "null"));
        }

        return fileList;
    }

    public static void launchGame(ResourceManager manager, String jreBinLoc, boolean macos, boolean windows) {
        String mainClass = "net.minecraft.client.main.Main";
        String separator = windows ? ";" : ":";
        StringBuilder classPathBuilder = new StringBuilder();
        classPathBuilder.append(manager.getClientPath().getAbsolutePath());

        for (Library lib : manager.getLibraries()) {
            classPathBuilder.append(separator);
            classPathBuilder.append(lib.getFilePath().getAbsolutePath());
        }

        List<String> inputArguments = ManagementFactory.getRuntimeMXBean().getInputArguments();
        ArrayList<String> jvmArgs = new ArrayList<>();
        jvmArgs.add(jreBinLoc);
        if (macos) {
            jvmArgs.add("-XstartOnFirstThread");
        }

        if (launchArgs != null && launchArgs.length != 0) {
            jvmArgs.addAll(inputArguments);
        } else {
            String assets = (new File(Utils.getWorkingDirectory(), "assets")).getAbsolutePath();
            launchArgs = new String[]{"--version", "mcp", "--accessToken", "0", "--assetsDir", assets, "--assetIndex", manager.getClient().getAssetsVersion(), "--userProperties", "{}"};
        }

        jvmArgs.add("-cp");
        jvmArgs.add(classPathBuilder.toString());
        jvmArgs.add(mainClass);
        jvmArgs.addAll(Arrays.asList(launchArgs));
        StringBuilder sb = new StringBuilder("Launching game");

        for (String arg : jvmArgs) {
            sb.append(" ").append(arg);
        }

        System.out.println(sb);

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(jvmArgs);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
        } catch (Exception var17) {
            var17.printStackTrace();
        }
    }

    public void play() {
        this.df.setProgress(0, "Launching Client");
        (new Thread(() -> {
            this.setupRuntime();
            ResourceManager resourceManager;
            try {
                resourceManager = getResourceManager(this.toLaunch);

                if (!this.toLaunch.isOffline()) {
                    try {
                        resourceManager.download((current, total) -> {
                            this.df.setProgress((int) (100L * current / total), "Updating Client");
                        });
                    } catch (IOException var6) {
                        var6.printStackTrace();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            this.df.setVisible(false);
            boolean macos = false;
            boolean windows = false;
            String osName = System.getProperty("os.name");
            String jreBinLoc;
            if (!osName.startsWith("Mac") && !osName.startsWith("Darwin")) {
                if (osName.toLowerCase().contains("windows")) {
                    jreBinLoc = jreDir.getAbsolutePath() + File.separator + "bin" + File.separator + "java.exe";
                    windows = true;
                } else {
                    jreBinLoc = jreDir.getAbsolutePath() + File.separator + "bin" + File.separator + "java";
                }
            } else {
                jreBinLoc = jreDir.getAbsolutePath() + File.separator + "Contents" + File.separator + "Home" + File.separator + "bin" + File.separator + "java";
                macos = true;
            }

            launchGame(resourceManager, jreBinLoc, macos, windows);
            System.exit(0);
        })).start();
    }

    public void setVersion(String s) {
        for (Entry<String, Version> stringVersionEntry : this.versionManager.getVersions().entrySet()) {
            if (s.equals(stringVersionEntry.getValue().getDisplayName())) {
                this.toLaunch = stringVersionEntry.getValue();
            }
        }
    }

    static {
        jreDir = new File(sigmaDir, "jre17.0.14");
    }
}