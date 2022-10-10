package ibs.processors.db2_processor;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

import org.apache.nifi.processor.ProcessContext;
import org.jooq.tools.json.JSONArray;
import org.jooq.tools.json.JSONObject;

public class ResultHandler {

    public static JSONArray ResultToJSON(java.sql.CallableStatement cstmt, ProcessContext context, int rows_number) {
        try {
            ResultSet rs;
            rs = cstmt.getResultSet();
            JSONArray json = new JSONArray();
            ResultSetMetaData rsmd = rs.getMetaData();
            int counter = 0;
            while(rs.next()) {
                int numColumns = rsmd.getColumnCount();
                JSONObject obj = new JSONObject();

                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsmd.getColumnName(i);
                    obj.put(column_name, rs.getObject(column_name));
                }
                json.add(obj);

                counter++;
                if (counter == rows_number) {
                    break;
                }
            }
            return json;
        }
        catch (Exception e) {
            return null;
        }
    }
}
