package com.bazaarvoice.core.db;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.ThrowsAdvice;
import org.springframework.beans.factory.annotation.Required;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.text.MessageFormat;

public class DataSourceExceptionRethrower implements ThrowsAdvice {
    private static final Log _sLog = LogFactory.getLog(DataSourceExceptionRethrower.class);

    private String _jdbcURL;

    @Required
    public void setJdbcURL(String jdbcURL) {
        _jdbcURL = jdbcURL;
    }

    public void afterThrowing(Method method, Object[] args, Object target, SQLException exception) throws SQLException {
        if (method.getName().equals("getConnection")) {
            // rethrow the original exception, wrapped in a new exception that includes the JDBC URL
            // to make it clear what database connection caused the exception.
            throw new WrappedSQLException(_jdbcURL, exception);
        }
    }

    private static class WrappedSQLException extends SQLException {

        public WrappedSQLException(String jdbcURL, SQLException cause) {
            super(MessageFormat.format("SQL exception raised for JDBC URL=\"{1}\"! MESSAGE: {0}",
                            cause.getMessage(), jdbcURL), cause.getSQLState());
            super.initCause(cause);
        }
    }
}
