import java.util.HashMap;
import java.util.Map;

public class DatabaseManager {
    private final Map<String, Databank> databases = new HashMap<>();

    public Databank createDatabase(String name) {
        Databank db = new Databank();
        databases.put(name, db);
        return db;
    }

    public Databank getDatabase(String name) {
        return databases.get(name);
    }
}
