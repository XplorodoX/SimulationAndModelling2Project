public class CsvImporter {
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            System.err.println("Usage: java CsvImporter <tableName> <csvFile> [jdbcUrl]");
            return;
        }
        String tableName = args[0];
        java.io.File csv = new java.io.File(args[1]);
        if (!csv.isFile()) {
            System.err.println("CSV file not found: " + csv);
            return;
        }
        String url = args.length == 3 ? args[2] : null;
        try (var conn = url == null ? AnyLogicDBUtil.openHSQLDBConnection() : AnyLogicDBUtil.openConnection(url)) {
            AnyLogicDBUtil.importTableFromFile(conn, tableName, csv);
            System.out.println("Imported " + csv + " into table " + tableName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
