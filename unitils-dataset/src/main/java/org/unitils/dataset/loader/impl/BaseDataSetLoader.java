/*
 * Copyright 2009,  Unitils.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.unitils.dataset.loader.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.unitils.core.UnitilsException;
import org.unitils.dataset.core.*;
import org.unitils.dataset.core.preparedstatement.BasePreparedStatement;
import org.unitils.dataset.loader.DataSetLoader;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Tim Ducheyne
 * @author Filip Neven
 */
public abstract class BaseDataSetLoader implements DataSetLoader {

    /* The logger instance for this class */
    private static Log logger = LogFactory.getLog(BaseDataSetLoader.class);

    protected DataSource dataSource;


    public void init(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void load(DataSet dataSet, List<String> variables) {
        try {
            loadDataSet(dataSet, variables);
        } catch (UnitilsException e) {
            throw e;
        } catch (Exception e) {
            throw new UnitilsException("Unable to load data set.", e);
        }
    }

    protected abstract BasePreparedStatement createPreparedStatementWrapper(String schemaName, String tableName, Connection connection) throws Exception;


    protected void loadDataSet(DataSet dataSet, List<String> variables) throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            for (Schema schema : dataSet.getSchemas()) {
                loadSchema(schema, variables, connection);
            }
        } finally {
            connection.close();
        }
    }

    protected void loadSchema(Schema schema, List<String> variables, Connection connection) throws SQLException {
        String schemaName = schema.getName();
        for (Table table : schema.getTables()) {
            loadTable(schemaName, table, variables, connection);
        }
    }

    protected void loadTable(String schemaName, Table table, List<String> variables, Connection connection) {
        String tableName = table.getName();
        for (Row row : table.getRows()) {
            if (row.getNrOfColumns() == 0) {
                continue;
            }
            loadRowHandleExceptions(schemaName, tableName, row, variables, connection);
        }
    }

    protected int loadRowHandleExceptions(String schemaName, String tableName, Row row, List<String> variables, Connection connection) {
        try {
            return loadRow(schemaName, tableName, row, variables, connection);
        } catch (Exception e) {
            throw new UnitilsException("Unable to load data set row for schema: " + schemaName + ", table: " + tableName + ", row: [" + row + "], variables: " + variables, e);
        }
    }

    protected int loadRow(String schemaName, String tableName, Row row, List<String> variables, Connection connection) throws Exception {
        BasePreparedStatement preparedStatementWrapper = createPreparedStatementWrapper(schemaName, tableName, connection);
        try {
            return loadRow(row, variables, preparedStatementWrapper);
        } finally {
            preparedStatementWrapper.close();
        }
    }

    protected int loadRow(Row row, List<String> variables, BasePreparedStatement preparedStatementWrapper) throws SQLException {
        for (Column column : row.getColumns()) {
            preparedStatementWrapper.addColumn(column, variables);
        }
        return preparedStatementWrapper.executeUpdate();
    }

}