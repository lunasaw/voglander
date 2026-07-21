package io.github.lunasaw.voglander.intergration.wrapper.image.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import io.github.lunasaw.voglander.client.domain.image.ImageContent;
import io.github.lunasaw.voglander.client.domain.image.ImagePromoteCommand;
import io.github.lunasaw.voglander.client.domain.image.ImageStageCommand;
import io.github.lunasaw.voglander.client.domain.image.StagedImage;
import io.github.lunasaw.voglander.client.domain.image.StoredImage;
import io.github.lunasaw.voglander.client.service.image.ImageStorageService;

/** Safe local implementation of the provider-independent image storage port. */
public class LocalImageStorageService implements ImageStorageService {

    private static final String STAGING_PREFIX = ".staging";
    private static final String FINAL_PREFIX = "images";
    private final Path root;
    private final Path stagingRoot;
    private final Clock clock;
    private final AtomicLong atomicMoveFallbackCount = new AtomicLong();

    public LocalImageStorageService(Path root, String workerNode) {
        this(root, workerNode, Clock.systemUTC());
    }

    public LocalImageStorageService(Path root, String workerNode, Clock clock) {
        this.root = canonicalRoot(root);
        this.stagingRoot = this.root.resolve(STAGING_PREFIX).resolve(safeSegment(workerNode)).normalize();
        this.clock = Objects.requireNonNull(clock, "clock");
        try {
            Files.createDirectories(this.stagingRoot);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize image storage root", exception);
        }
    }

    public LocalImageStorageService(String root, String workerNode) {
        this(Path.of(Objects.requireNonNull(root, "root")), workerNode);
    }

    public LocalImageStorageService(String root, String workerNode, Clock clock) {
        this(Path.of(Objects.requireNonNull(root, "root")), workerNode, clock);
    }

    @Override
    public StagedImage stage(ImageStageCommand command, InputStream content) throws IOException {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(content, "content");
        if (command.workerNode() != null && !safeSegment(command.workerNode()).equals(stagingRoot.getFileName().toString())) {
            throw new IOException("Image staging worker node does not match provider node");
        }
        Path target = stagingRoot.resolve(UUID.randomUUID().toString().replace("-", "") + ".part");
        long size = 0;
        MessageDigest digest = sha256();
        byte[] buffer = new byte[8192];
        try (InputStream input = content;
            java.io.OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE)) {
            int read;
            while ((read = input.read(buffer)) != -1) {
                size = Math.addExact(size, read);
                if (size > command.maxBytes()) {
                    throw new IOException("Image staging exceeds configured byte limit");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        } catch (IOException | ArithmeticException exception) {
            Files.deleteIfExists(target);
            if (exception instanceof ArithmeticException) {
                throw new IOException("Image staging size overflow", exception);
            }
            throw exception;
        }
        String key = root.relativize(target).toString().replace('\\', '/');
        return new StagedImage(key, size, HexFormat.of().formatHex(digest.digest()));
    }

    @Override
    public InputStream openStaged(String stagingKey) throws IOException {
        Path path = resolveExisting(stagingKey, STAGING_PREFIX);
        return Files.newInputStream(path, StandardOpenOption.READ);
    }

    @Override
    public StoredImage promote(ImagePromoteCommand command) throws IOException {
        Objects.requireNonNull(command, "command");
        Path source = resolveExisting(command.stagingKey(), STAGING_PREFIX);
        Path target = resolveKey(command.finalKey(), FINAL_PREFIX);
        Files.createDirectories(target.getParent());
        verifyRealParent(target);
        // ATOMIC_MOVE is allowed to replace an existing target on some providers.  Check before
        // moving so a retry can never silently overwrite a previously promoted asset.
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            throw new java.nio.file.FileAlreadyExistsException(command.finalKey());
        }
        try {
            moveAtomically(source, target);
        } catch (java.nio.file.AtomicMoveNotSupportedException exception) {
            atomicMoveFallbackCount.incrementAndGet();
            Files.move(source, target);
        }
        return new StoredImage(command.finalKey(), Files.size(target));
    }

    /** Hook for provider-specific atomic move and deterministic fallback testing. */
    protected void moveAtomically(Path source, Path target) throws IOException {
        Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
    }

    public long getAtomicMoveFallbackCount() {
        return atomicMoveFallbackCount.get();
    }

    @Override
    public ImageContent open(String storageKey) throws IOException {
        Path path = resolveExisting(storageKey, FINAL_PREFIX);
        return new ImageContent(Files.newInputStream(path, StandardOpenOption.READ), Files.size(path));
    }

    @Override
    public boolean delete(String storageKey) throws IOException {
        Path path = resolveKey(storageKey, FINAL_PREFIX);
        verifyRealParent(path);
        return !Files.exists(path, LinkOption.NOFOLLOW_LINKS) || Files.deleteIfExists(path);
    }

    @Override
    public boolean exists(String storageKey) throws IOException {
        Path path = resolveKey(storageKey, FINAL_PREFIX);
        verifyRealParent(path);
        return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public void discardStaged(String stagingKey) throws IOException {
        Path path = resolveKey(stagingKey, STAGING_PREFIX);
        verifyRealParent(path);
        Files.deleteIfExists(path);
    }

    /** Removes stale staging parts and returns the number of deleted files. */
    public int sweepStaging(Instant now, Duration ttl) throws IOException {
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(ttl, "ttl");
        if (ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must not be negative");
        }
        int deleted = 0;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(stagingRoot, "*.part")) {
            for (Path path : files) {
                Instant modified = Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toInstant();
                if (!modified.plus(ttl).isAfter(now) && Files.deleteIfExists(path)) {
                    deleted++;
                }
            }
        }
        return deleted;
    }

    public int sweepStaging(Duration ttl) throws IOException {
        return sweepStaging(clock.instant(), ttl);
    }

    @Override
    public Set<String> listFinalKeys() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        Path finalRoot = root.resolve(FINAL_PREFIX);
        if (!Files.isDirectory(finalRoot, LinkOption.NOFOLLOW_LINKS)) return keys;
        try (java.util.stream.Stream<Path> files = Files.walk(finalRoot)) {
            files.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                .forEach(path -> keys.add(root.relativize(path).toString().replace('\\', '/')));
        }
        return keys;
    }

    public Path root() {
        return root;
    }

    @Override
    public boolean isHealthy() {
        return Files.isDirectory(root) && Files.isWritable(root) && Files.isDirectory(stagingRoot)
            && Files.isWritable(stagingRoot);
    }

    private Path resolveExisting(String key, String requiredPrefix) throws IOException {
        Path path = resolveKey(key, requiredPrefix);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            throw new IOException("Image storage object does not exist");
        }
        Path real = path.toRealPath();
        if (!real.startsWith(root)) {
            throw new IOException("Image storage object escapes root");
        }
        return real;
    }

    private Path resolveKey(String key, String requiredPrefix) throws IOException {
        if (key == null || key.trim().isEmpty()) {
            throw new IOException("Image storage key must not be blank");
        }
        String normalizedKey = key.replace('\\', '/');
        if (normalizedKey.startsWith("/") || normalizedKey.contains("../") || normalizedKey.equals("..")
            || !normalizedKey.equals(normalizedKey.replace("//", "/"))) {
            throw new IOException("Image storage key is invalid");
        }
        Path path = root.resolve(normalizedKey).normalize();
        if (!path.startsWith(root) || !normalizedKey.startsWith(requiredPrefix + "/")) {
            throw new IOException("Image storage key is outside its namespace");
        }
        return path;
    }

    private void verifyRealParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Path realParent = parent.toRealPath();
            if (!realParent.startsWith(root)) {
                throw new IOException("Image storage parent escapes root");
            }
        }
    }

    private static Path canonicalRoot(Path root) {
        try {
            Path absolute = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
            Files.createDirectories(absolute);
            return absolute.toRealPath();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot initialize image storage root", exception);
        }
    }

    private static String safeSegment(String value) {
        if (value == null || value.trim().isEmpty() || value.contains("/") || value.contains("\\")
            || value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException("workerNode must be a safe path segment");
        }
        return value;
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
