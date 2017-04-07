package Server;

final class Deal {

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    private Deal() {}

    /**
     * Inner class that defines constant values for the pets database table.
     * Each entry in the table represents a single pet.
     */
    static final class Entries {

        // SQLite connection string
        final static String url = "jdbc:sqlite:/home/igor/fish/aqua.db";

        /** Name of database table for pets
         */
        final static String TABLE_NAME = "fishies";

        /** Token of the pet.
         * Type: TEXT
         */
        final static String PET_TOKEN ="Token";

        /**Points of the fish.
         * Type: INTEGER
         */
        final static String PET_POINTS = "Points";

        // SQL statement for creating a new table
        final static String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (\n"
                + PET_TOKEN + " TEXT NOT NULL,\n"
                + PET_POINTS + " INTEGER NOT NULL DEFAULT 0);";
    }

}

