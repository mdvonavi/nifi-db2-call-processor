package ibs.processors.db2_processor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class DBHandler {

    public static Connection CreateConnection(String url, String user, String password) throws ClassNotFoundException, SQLException{
        Class.forName("com.ibm.db2.jcc.DB2Driver");
        Connection con = DriverManager.getConnection(url, user, password);
        return con;
    }

}
