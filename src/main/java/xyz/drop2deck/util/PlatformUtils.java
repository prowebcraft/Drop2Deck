package xyz.drop2deck.util;

import xyz.drop2deck.enums.Platform;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.ImageSingletons;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

public class PlatformUtils {
    private static final String LINUX_BOARD_VENDOR_PATH = "/sys/devices/virtual/dmi/id/board_vendor";
    private static final String STEAM_DECK_RUN_MEDIA_PATH = "/run/media";
    private static final String VALVE_VENDOR = "valve";
    private static final String STEAM_DECK_CODENAME = "jupiter";

    public static Platform detectPlatform() {
        String osName = ImageInfo.inImageCode()
                ? ImageSingletons.lookup(org.graalvm.nativeimage.Platform.class).getOS()
                : System.getProperty("os.name");

        if (isOnSteamDeck()) {
            return Platform.STEAM_DECK;
        } else if (isWindows(osName)) {
            return Platform.WINDOWS;
        } else if (isUnix(osName)) {
            return Platform.LINUX;
        } else if (isMac(osName)) {
            return Platform.MAC_OSX;
        }
        throw new RuntimeException("Unsupported platform!");
    }

    public static boolean isOnSteamDeck() {
        try {
            return Files.readAllLines(Paths.get(LINUX_BOARD_VENDOR_PATH))
                    .stream()
                    .map(String::toLowerCase)
                    .anyMatch(value -> value.contains(VALVE_VENDOR) || value.contains(STEAM_DECK_CODENAME));
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isWindows(String osName) {
        String os = osName.toLowerCase();
        return os.contains("win");
    }

    public static boolean isMac(String osName) {
        String os = osName.toLowerCase();
        return os.contains("mac") || os.contains("darwin");
    }

    public static boolean isUnix(String osName) {
        String os = osName.toLowerCase();
        return os.contains("nix") || os.contains("nux");
    }

    public static Optional<String> findSteamDeckExternalPath() {
        Path runMediaPath = Paths.get(STEAM_DECK_RUN_MEDIA_PATH);
        if (!Files.isDirectory(runMediaPath)) {
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.find(
                runMediaPath,
                2,
                (path, attrs) -> attrs.isDirectory() && !path.equals(runMediaPath) && Files.isReadable(path)
        )) {
            return paths
                    .sorted(Comparator
                            .comparingInt((Path path) -> -runMediaPath.relativize(path).getNameCount())
                            .thenComparing(Path::toString))
                    .map(Path::toString)
                    .findFirst();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
