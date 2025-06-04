import java.io.File;

/**
 * Command line utility that imports all CSV files in a directory into a
 * database. Table names are derived from the file names.
 */
public class CsvDirImporter {
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java CsvDirImporter <directory> [jdbcUrl]");
            return;
        }

        File dir = new File(args[0]);
        if (!dir.isDirectory()) {
            System.err.println("Not a directory: " + dir);
            return;
        }

        String url = args.length == 2 ? args[1] : null;
        try (var conn = url == null ? AnyLogicDBUtil.openHSQLDBConnection() :
                AnyLogicDBUtil.openConnection(url)) {
            AnyLogicDBUtil.importTablesFromDirectory(conn, dir);
            System.out.println("Imported CSV files from " + dir.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

