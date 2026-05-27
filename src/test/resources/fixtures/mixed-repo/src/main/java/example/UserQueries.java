package example;

public class UserQueries {

    private static final String FIND_USER_SQL = "SELECT * FROM users WHERE id = 1";

    public String findUserSql() {
        return FIND_USER_SQL;
    }
}
