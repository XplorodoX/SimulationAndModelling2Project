public class DatabaseTest {
    public static void main(String[] args) throws Exception {
        Databank db = new Databank();
        db.setElectricityPrice(42.0);

        String url = args.length > 0 ? args[0] : null;
        try (var conn = (url != null)
                ? AnyLogicDBUtil.openConnection(url)
                : AnyLogicDBUtil.openHSQLDBConnection()) {
            AnyLogicDBUtil.createSchema(conn);
            AnyLogicDBUtil.insertDatabank(conn, "test", db);

            double price = AnyLogicDBUtil.loadElectricityPrice(conn, "test");
            if (price == db.getElectricityPrice()) {
                System.out.println("Test passed: price=" + price);
            } else {
                System.err.println("Test failed: expected " + db.getElectricityPrice() + " but got " + price);
            }
        }
    }
}
