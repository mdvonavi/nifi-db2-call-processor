package ibs.processors.db2_processor;

import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.jooq.tools.json.JSONArray;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

public class StatementHandler {


    public int getStatus_code() throws SQLException {
        return this.status_code;
    }

    public JSONArray getJson() {
        if ((this.cstmt != null) & (this.status_code == 0))
        {
            return ResultHandler.ResultToJSON(
                    this.cstmt,
                    this.context,
                    this.rows_number
            );
        }
        else
        {
            return null;
        }
    }

    public void close() throws SQLException {
        this.cstmt.close();
    }

    private CallableStatement cstmt;
    private int status_code;
    private JSONArray json;
    private int rows_number;
    private ProcessContext context;

    public StatementHandler(Connection connection, ProcessContext context, String sql, int rows_number, ComponentLog logger) {
        this.cstmt = null;
        this.rows_number = rows_number;
        this.context = context;

        try {
            this.cstmt = connection.prepareCall(sql);
            this.cstmt.registerOutParameter (1, Types.INTEGER);
            this.cstmt.execute();
            this.status_code = this.cstmt.getInt(1);
        }
        catch (Exception e){
            logger.warn(e.getMessage());
            logger.warn(Arrays.toString(e.getStackTrace()));
            this.status_code = -1;
        }
    }
}
