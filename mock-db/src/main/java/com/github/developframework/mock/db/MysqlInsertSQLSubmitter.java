package com.github.developframework.mock.db;

import com.github.developframework.mock.MockPlaceholder;
import com.github.developframework.mock.MockTask;
import com.github.developframework.mock.random.RandomGeneratorRegistry;
import com.github.developframework.mock.random.RandomGenerator;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.stream.Collectors;

/**
 * @author qiuzhenhao
 */
public class MysqlInsertSQLSubmitter extends InsertSQLSubmitter {

    public MysqlInsertSQLSubmitter(RandomGeneratorRegistry randomGeneratorRegistry, DBInfo dbInfo) {
        super(randomGeneratorRegistry, dbInfo);
    }

    private String build() {
        StringBuffer sb = new StringBuffer();
        sb.append("INSERT INTO ");
        if (database != null) {
            sb.append("`").append(database).append("`.");
        }
        if (table == null) {
            throw new DBMockException("table is undefined");
        }
        sb.append('`').append(table).append("`(");
        if (fields.isEmpty()) {
            throw new DBMockException("fields is empty.");
        }
        sb.append(StringUtils.join(fields.stream().map(pair -> "`" + pair.getKey() + "`").collect(Collectors.toList()), ", "));
        sb.append(") VALUES(");
        sb.append(StringUtils.join(fields.stream().map(pair -> "?").collect(Collectors.toList()), ", "));
        sb.append(')');
        return sb.toString();
    }

    @Override
    public int submit(int quantity) throws SQLException {
        try {
            Class.forName(dbInfo.getDriver());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        Connection connection = DriverManager.getConnection(dbInfo.getUrl(), dbInfo.getUser(), dbInfo.getPassword());
        connection.setAutoCommit(false);
        PreparedStatement preparedStatement = connection.prepareStatement(build());
        int r = 0;
        try {
            for (int i = 0; i < quantity; i++) {
                for (int j = 0; j < fields.size(); j++) {
                    MockTask mockTask = fields.get(j).getValue();
                    String value = mockTask.run();
                    preparedStatement.setString(j + 1, value);
                }
                r += preparedStatement.executeUpdate();
            }
        } catch(Exception e) {
            connection.rollback();
        }
        connection.commit();
        preparedStatement.close();
        connection.close();
        return r;
    }
}
