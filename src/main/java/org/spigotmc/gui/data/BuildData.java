package org.spigotmc.gui.data;

import com.google.common.base.Suppliers;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.jeff_media.javafinder.JavaInstallation;
import org.spigotmc.builder.BuildInfo;
import org.spigotmc.builder.Builder;
import org.spigotmc.builder.JavaVersion;
import org.spigotmc.gui.modals.MessageModal;
import org.spigotmc.utils.Constants;
import org.spigotmc.utils.Utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains important BuildData like versions and BuildInfo.
 */
public final class BuildData {

    private static final Pattern QUOTE_PATTERN = Pattern.compile("\".*?\"");
    private static final String VERSION_PATTERN = "\\b\\d+\\.[\\.\\d]*";

    private final Map<String, Supplier<CompletableFuture<BuildInfo>>> builds = new HashMap<>();
    private final List<String> versions = new ArrayList<>();

    private JavaInstallationManager javaInstallationManager;
    private String latestVersion = "unknown";

    public BuildData(final Consumer<Map<String, Supplier<CompletableFuture<BuildInfo>>>> whenComplete) {
        retrieveBuildInfo(whenComplete);
    }

    /**
     * Generates text to be used in a text field showing all build arguments with extra text.
     *
     * @param buildSettings the build settings
     * @return build settings
     */
    public List<String> generatePreCompilationText(BuildSettings buildSettings) {
        List<String> lines = new ArrayList<>();

        lines.add("Hey! You're about to run the following command. Does it look correct?");
        lines.add("");

        StringBuilder builder = new StringBuilder();
        for (String arg : buildArgs(buildSettings)) {
            builder.append(arg).append(" ");
        }

        lines.add(builder.toString());
        lines.add("");
        lines.add("If so, press the compile button already!");

        return lines;
    }

    /**
     * Creates a list of build arguments.
     *
     * @param buildSettings the buildSettings to use to create the build arguments
     * @return a list of build arguments
     */
    public List<String> buildArgs(final BuildSettings buildSettings) {
        List<String> arguments = new ArrayList<>();
        arguments.add(javaInstallationManager.getSelectedInstallation().getJavaExecutable().getAbsolutePath());
        arguments.addAll(javaInstallationManager.getJvmArguments());

        if (Utils.isRanFromJar()) {
            arguments.add("-jar");
            arguments.add(Utils.getFile().getPath());
        } else {
            arguments.add("-cp");
            arguments.add(System.getProperty("java.class.path"));
            arguments.add("org.spigotmc.builder.Bootstrap");
        }

        arguments.addAll(buildSettings.getArguments());

        return arguments;
    }

    /**
     * Updates the java executable used by the build.
     *
     * @param buildSettings the buildSettings used to get the current selected spigot version
     */
    public CompletableFuture<Boolean> updateJavaExecutable(final BuildSettings buildSettings, boolean showModal) {
        String checkVersion = buildSettings.getVersion();
        if (checkVersion.equals("experimental")) {
            String mostRecent = versions.get(2);
            if (mostRecent == null || mostRecent.isEmpty()) {
                mostRecent = "latest";
            }

            checkVersion = mostRecent;
        }

        return builds.get(checkVersion).get().thenApply((BuildInfo info) -> {
            if (buildSettings.isOverrideJavaExecutable()) {
                javaInstallationManager.setSelectedInstallation(javaInstallationManager.getSelectedInstallation());
                return true;
            }

            if (info.getJavaVersions() == null) {
                info.setJavaVersions(new int[]{
                        JavaVersion.JAVA_7.getVersion(),
                        JavaVersion.JAVA_8.getVersion()
                });
            }

            final JavaVersion min = JavaVersion.getByVersion(info.getJavaVersions()[0]);
            final JavaVersion max = JavaVersion.getByVersion(info.getJavaVersions()[1]);

            final JavaInstallation primary = javaInstallationManager.getPrimaryInstallation();
            if (primary.getVersion().getClassFileMajorVersion() >= min.getVersion() && primary.getVersion().getClassFileMajorVersion() <= max.getVersion()) {
                javaInstallationManager.setSelectedInstallation(primary);
                return true;
            }

            JavaInstallation newInstallation = null;
            for (JavaInstallation installation : javaInstallationManager.getInstallations()) {
                final int classFileMajorVersion = installation.getVersion().getClassFileMajorVersion();
                if (classFileMajorVersion == max.getVersion()) {
                    newInstallation = installation;
                    break;
                }

                if (classFileMajorVersion == min.getVersion()) {
                    newInstallation = installation;
                    break;
                }
            }

            if (newInstallation == null) {
                if (showModal) {
                    String style = Utils.getFileContentsFromResource("web/reset.css");
                    String message = Utils.getFileContentsFromResource("web/insufficient_java.html");
                    message = message.replace("%STYLESHEET%", "<style>" + style + "</style>");
                    message = message.replace("%JAVA_VERSION_MIN%", min.getName());
                    message = message.replace("%JAVA_VERSION_MAX%", max.getName());
                    message = message.replace("%VERSION%", buildSettings.getVersion());
                    message = message.replace("%OPENJDK_LINK%", Constants.DOWNLOAD_OPENJDK);
                    message = message.replace("%AZUL_LINK%", Constants.DOWNLOAD_AZUL);
                    message = message.replace("%ORACLE_LINK%", Constants.DOWNLOAD_ORACLE);

                    MessageModal.displayError(message);
                }
                return false;
            }

            javaInstallationManager.setSelectedInstallation(newInstallation);
            return true;
        }).whenComplete((Boolean completed, Throwable throwable) -> {
            if (throwable != null) {
                MessageModal.displayError(Utils.getReadableStacktrace(throwable));
            }
        });
    }

    /**
     * @return a copy of the versions list.
     */
    public List<String> getVersions() {
        return new ArrayList<>(versions);
    }

    public JavaInstallationManager getJavaInstallationManager() {
        return javaInstallationManager;
    }

    private void retrieveBuildInfo(final Consumer<Map<String, Supplier<CompletableFuture<BuildInfo>>>> whenComplete) {
        CompletableFuture.runAsync(() -> {
            final URLConnection connection;
            try {
                connection = new URL("https://hub.spigotmc.org/versions").openConnection();
                connection.setConnectTimeout(Constants.GENERAL_TIMEOUT_MS);
                connection.setReadTimeout(Constants.GENERAL_TIMEOUT_MS);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            try (final InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                final Matcher matcher = QUOTE_PATTERN.matcher(CharStreams.toString(reader));
                builds.put("experimental", null);
                builds.put("latest", futureBuildInfo("latest"));
                versions.add("experimental");
                versions.add("latest");

                while (matcher.find()) {
                    final String version = matcher.group().replace('"', ' ').replace(".json", "").trim();

                    if (!version.matches(VERSION_PATTERN)) {
                        continue;
                    }
                    versions.add(version);
                    builds.put(version, futureBuildInfo(version));
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }).whenComplete((Void __, Throwable throwable) -> {
            if (throwable != null) {
                MessageModal.displayError(Utils.getReadableStacktrace(throwable));
                return;
            }
            sortVersions(versions);
            latestVersion = retrieveLatestVersion();
            javaInstallationManager = new JavaInstallationManager();
            whenComplete.accept(builds);
        });
    }

    private String retrieveLatestVersion() {
        BuildInfo latestInfo = builds.get("latest").get().join();

        for (String version : versions) {
            if (version.equals("latest") || version.equals("experimental")) {
                continue;
            }

            final BuildInfo info = builds.get(version).get().join();
            if (info.getName().equals(latestInfo.getName())) {
                return version;
            }

        }

        return "unknown";
    }

    private Supplier<CompletableFuture<BuildInfo>> futureBuildInfo(String version) {
        return Suppliers.memoize(() -> {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return new Gson().fromJson(Builder.get("https://hub.spigotmc.org/versions/" + version + ".json"), BuildInfo.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        });
    }

    private void sortVersions(List<String> versions) {
        versions.sort((a, b) -> {
            String[] aSplit = a.split("\\.");
            String[] bSplit = b.split("\\.");

            int result = 0;
            if (aSplit.length > 1 && bSplit.length > 1) {
                for (int i = 0; i < Math.min(aSplit.length, bSplit.length); i++) {
                    int aVer = Integer.parseInt(aSplit[i]);
                    int bVer = Integer.parseInt(bSplit[i]);

                    result = Integer.compare(bVer, aVer);
                    if (result != 0) {
                        break;
                    }
                }
            }

            return result;
        });
    }

    public String getLatestVersion() {
        return latestVersion;
    }
}
