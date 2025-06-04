public class DatabaseTest {
    public static void main(String[] args) throws Exception {
        Databank db = new Databank();
        db.setElectricityPrice(42.0);

        try (var conn = AnyLogicDBUtil.openConnection()) {
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
