import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class Scores {
    private static final String HEADER = "DevOpsSubmarine scores v1";
    private static final Comparator<Score> RANKING = Comparator.comparingInt(Score::kills).reversed()
            .thenComparing(Comparator.comparingLong(Score::distance).reversed()).thenComparingLong(Score::id);

    public record Score(long id, String player, Game.Difficulty difficulty, int seconds, long distance,
                        int kills, int level, int crew, Game.StopReason reason, Instant endedAt) {
        public Score {
            player = playerName(player);
            if (id <= 0 || seconds < 0 || distance < 0 || kills < 0 || level < 1 || level > Game.MAX_LEVEL
                    || crew < 0 || crew > Submarine.Crew.TOTAL || difficulty == null || reason == null || endedAt == null) {
                throw new IllegalArgumentException("Invalid session statistics");
            }
        }

        String encode() {
            return id + "\t" + player + "\t" + difficulty + "\t" + seconds + "\t" + distance
                    + "\t" + kills + "\t" + level + "\t" + crew + "\t" + reason + "\t" + endedAt;
        }

        static Score decode(String line) {
            String[] fields = line.split("\t", -1);
            if (fields.length != 10) throw new IllegalArgumentException("Expected ten score fields");
            return new Score(Long.parseLong(fields[0]), fields[1], Game.Difficulty.valueOf(fields[2]),
                    Integer.parseInt(fields[3]), Long.parseLong(fields[4]), Integer.parseInt(fields[5]),
                    Integer.parseInt(fields[6]), Integer.parseInt(fields[7]), Game.StopReason.valueOf(fields[8]),
                    Instant.parse(fields[9]));
        }
    }

    public record Summary(int sessions, int kills, long distance, double averageSeconds) {}

    private final Path path;
    private final ArrayList<Score> records = new ArrayList<>();
    private long nextId = 1;

    public Scores(Path path) throws IOException {
        this.path = path;
        if (Files.notExists(path)) return;
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lines.isEmpty() || !lines.get(0).equals(HEADER)) throw new IOException("Unsupported score file format: " + path);
        for (int row = 1; row < lines.size(); row++) {
            try {
                Score score = Score.decode(lines.get(row));
                if (records.stream().anyMatch(existing -> existing.id() == score.id())) {
                    throw new IllegalArgumentException("Duplicate score ID");
                }
                records.add(score);
                nextId = Math.max(nextId, Math.addExact(score.id(), 1));
            } catch (IllegalArgumentException | java.time.format.DateTimeParseException | ArithmeticException e) {
                throw new IOException("Invalid score at line " + (row + 1) + ": " + e.getMessage(), e);
            }
        }
    }

    public static String playerName(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 24 || name.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Captain name must contain 1-24 printable characters");
        }
        return name.strip();
    }

    public Score add(String player, Game game) throws IOException {
        if (game.isRunning()) throw new IllegalArgumentException("Only finished sessions can be saved");
        Score score = new Score(nextId, player, game.getDifficulty(), game.getSurvivalSeconds(), game.getDistance(),
                game.getKills(), game.getLevel(), game.getSubmarine().getCrew().getLiving(), game.getStopReason(), Instant.now());
        ArrayList<Score> updated = new ArrayList<>(records);
        updated.add(score);
        persist(updated);
        nextId++;
        return score;
    }

    public List<Score> all() {
        return records.stream().sorted(RANKING).toList();
    }

    public List<Score> find(String player) {
        String query = playerName(player).toLowerCase(Locale.ROOT);
        return all().stream().filter(score -> score.player().toLowerCase(Locale.ROOT).contains(query)).toList();
    }

    public boolean remove(long id) throws IOException {
        if (id <= 0) throw new IllegalArgumentException("Score ID must be a positive number");
        ArrayList<Score> updated = new ArrayList<>(records);
        if (!updated.removeIf(score -> score.id() == id)) return false;
        persist(updated);
        return true;
    }

    public Summary summary() {
        return new Summary(records.size(), records.stream().mapToInt(Score::kills).sum(),
                records.stream().mapToLong(Score::distance).sum(), records.stream().mapToInt(Score::seconds).average().orElse(0));
    }

    private void persist(List<Score> updated) throws IOException {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        List<String> lines = new ArrayList<>();
        lines.add(HEADER);
        for (Score score : updated) lines.add(score.encode());
        Files.write(temporary, lines, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
        records.clear();
        records.addAll(updated);
    }
}
