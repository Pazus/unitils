/*
 * Copyright 2008,  Unitils.org
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

import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.Assert.fail;

/**
 * @author Tim Ducheyne
 * @author Filip Neven
 */
public class RefreshDataSetLoaderTest extends DataSetLoaderTestBase {

    /* Tested object */
    private RefreshDataSetLoader dataSetLoader = new RefreshDataSetLoader();


    @Before
    public void initialize() throws Exception {
        initializeDataSetLoader(dataSetLoader);
    }


    @Test
    public void insertBecauseRowsAreNotYetInDatabase() throws Exception {
        initializePrimaryKeys("column_1", "column_1", "column_3", "column_3", "column_5", "column_5");
        dataSetLoader.load(dataSet, new ArrayList<String>());

        connection.assertInvoked().prepareStatement("update my_schema.table_a set column_1=?, column_2=? where column_1=?");
        connection.assertInvoked().prepareStatement("insert into my_schema.table_a (column_1,column_2) values (?,?)");
        connection.assertInvoked().prepareStatement("update my_schema.table_a set column_3=?, column_4=? where column_3=?");
        connection.assertInvoked().prepareStatement("insert into my_schema.table_a (column_3,column_4) values (?,?)");
        connection.assertInvoked().prepareStatement("update my_schema.table_b set column_5=?, column_6=? where column_5=?");
        connection.assertInvoked().prepareStatement("insert into my_schema.table_b (column_5,column_6) values (?,?)");
    }

    @Test
    public void updateBecauseRowsAlreadyInDatabase() throws Exception {
        initializePrimaryKeys("column_1", "column_3", "column_5");
        preparedStatement.returns(1).executeUpdate(); // update of row was successful
        dataSetLoader.load(dataSet, new ArrayList<String>());

        connection.assertInvoked().prepareStatement("update my_schema.table_a set column_1=?, column_2=? where column_1=?");
        connection.assertNotInvoked().prepareStatement("insert into my_schema.table_a (column_1,column_2) values (?,?)");
        connection.assertInvoked().prepareStatement("update my_schema.table_a set column_3=?, column_4=? where column_3=?");
        connection.assertNotInvoked().prepareStatement("insert into my_schema.table_a (column_3,column_4) values (?,?)");
        connection.assertInvoked().prepareStatement("update my_schema.table_b set column_5=?, column_6=? where column_5=?");
        connection.assertNotInvoked().prepareStatement("insert into my_schema.table_b (column_5,column_6) values (?,?)");
    }

    @Test
    public void insertDataSetWithLiteralValues() throws Exception {
        initializePrimaryKeys("column_1");
        dataSetLoader.load(dataSetWithLiteralValues, new ArrayList<String>());
        connection.assertInvoked().prepareStatement("update my_schema.table_a set column_1=sysdate, column_2=null, column_3=? where column_1=sysdate");
        preparedStatement.assertInvoked().setObject(1, "=escaped", 0);
    }

    @Test
    public void schemaWithEmtpyTable() throws Exception {
        dataSetLoader.load(dataSetWithEmptyTable, new ArrayList<String>());
        connection.assertNotInvoked().prepareStatement(null);
    }

    @Test
    public void schemaWithEmtpyRows() throws Exception {
        dataSetLoader.load(dataSetWithEmptyRows, new ArrayList<String>());
        connection.assertNotInvoked().prepareStatement(null);
    }

    @Test
    public void exceptionDuringLoadingOfRow() throws Exception {
        connection.resetBehavior();
        connection.raises(SQLException.class).prepareStatement(null);
        try {
            dataSetLoader.load(dataSet, new ArrayList<String>());
            fail("Exception expected");
        } catch (Exception e) {
            assertExceptionMessageContains(e, "my_schema");
            assertExceptionMessageContains(e, "table_a");
            assertExceptionMessageContains(e, "column_1=\"1\"");
            assertExceptionMessageContains(e, "column_2=\"2\"");
        }
    }
}