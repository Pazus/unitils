/*
 * Copyright 2006 the original author or authors.
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
package org.unitils.dbmaintainer.version.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.unitils.core.UnitilsException;
import org.unitils.dbmaintainer.util.BaseDatabaseTask;
import org.unitils.dbmaintainer.version.Version;
import org.unitils.dbmaintainer.version.VersionSource;
import org.unitils.thirdparty.org.apache.commons.dbutils.DbUtils;
import org.unitils.util.PropertyUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Implementation of <code>VersionSource</code> that stores the version in the database. The version is stored in the
 * table whose name is defined by the property {@link #PROPKEY_VERSION_TABLE_NAME}. The version index column name is
 * defined by {@link #PROPKEY_VERSION_INDEX_COLUMN_NAME}, the version timestamp colmumn name is defined by
 * {@link #PROPKEY_VERSION_TIMESTAMP_COLUMN_NAME}. The last updated succeeded column name is defined by
 * {@link #PROPKEY_LAST_UPDATE_SUCCEEDED_COLUMN_NAME}.
 *
 * @author Filip Neven
 * @author Tim Ducheyne
 */
public class DBVersionSource extends BaseDatabaseTask implements VersionSource {

    /* The logger instance for this class */
    private static Log logger = LogFactory.getLog(DBVersionSource.class);

    /* The key of the property that specifies the name of the datase table in which the DB version is stored */
    public static final String PROPKEY_VERSION_TABLE_NAME = "dbMaintainer.dbVersionSource.tableName";

    /* The key of the property that specifies the name of the column in which the DB version index is stored */
    public static final String PROPKEY_VERSION_INDEX_COLUMN_NAME = "dbMaintainer.dbVersionSource.versionIndexColumnName";

    /* The key of the property that specifies the name of the column in which the DB version index is stored */
    public static final String PROPKEY_VERSION_TIMESTAMP_COLUMN_NAME = "dbMaintainer.dbVersionSource.versionTimeStampColumnName";

    /* The key of the property that specifies the name of the column in which is stored whether the last update succeeded. */
    public static final String PROPKEY_LAST_UPDATE_SUCCEEDED_COLUMN_NAME = "dbMaintainer.dbVersionSource.lastUpdateSucceededColumnName";

    /* The key of the property that specifies the name of the column in which the DB version index is stored */
    public static final String PROPKEY_CODESCRIPTS_TIMESTAMP_COLUMN_NAME = "dbMaintainer.dbVersionSource.codeScriptsTimeStampColumnName";

    /* The key of the property that specifies the name of the column in which is stored whether the last update succeeded. */
    public static final String PROPKEY_LAST_CODE_UPDATE_SUCCEEDED_COLUMN_NAME = "dbMaintainer.dbVersionSource.lastCodeUpdateSucceededColumnName";

    /* The name of the datase table in which the DB version is stored */
    private String versionTableName;

    /* The name of the datase column in which the DB version index is stored */
    private String versionIndexColumnName;

    /* The name of the datase column in which the DB version timestamp is stored */
    private String versionTimestampColumnName;

    /* The name of the database column in which is stored whether the last DB update succeeded */
    private String lastUpdateSucceededColumnName;

    /* The name of the database column in which the DB code scripts timestamp is stored */
    private String codeScriptsTimestampColumnName;

    /* The name of the database column in which is stored whether the last code update succeeded */
    private String lastCodeUpdateSucceededColumnName;


    /**
     * Initializes the name of the version table and its columns using the given configuration.
     *
     * @param configuration the configuration, not null
     */
    protected void doInit(Properties configuration) {
        this.versionTableName = PropertyUtils.getString(PROPKEY_VERSION_TABLE_NAME, configuration);
        this.versionIndexColumnName = PropertyUtils.getString(PROPKEY_VERSION_INDEX_COLUMN_NAME, configuration);
        this.versionTimestampColumnName = PropertyUtils.getString(PROPKEY_VERSION_TIMESTAMP_COLUMN_NAME, configuration);
        this.lastUpdateSucceededColumnName = PropertyUtils.getString(PROPKEY_LAST_UPDATE_SUCCEEDED_COLUMN_NAME, configuration);
        this.codeScriptsTimestampColumnName = PropertyUtils.getString(PROPKEY_CODESCRIPTS_TIMESTAMP_COLUMN_NAME, configuration);
        this.lastCodeUpdateSucceededColumnName = PropertyUtils.getString(PROPKEY_LAST_CODE_UPDATE_SUCCEEDED_COLUMN_NAME, configuration);

        // convert to correct case
        versionTableName = defaultDbSupport.toCorrectCaseIdentifier(versionTableName);
        versionIndexColumnName = defaultDbSupport.toCorrectCaseIdentifier(versionIndexColumnName);
        versionTimestampColumnName = defaultDbSupport.toCorrectCaseIdentifier(versionTimestampColumnName);
        lastUpdateSucceededColumnName = defaultDbSupport.toCorrectCaseIdentifier(lastUpdateSucceededColumnName);
        codeScriptsTimestampColumnName = defaultDbSupport.toCorrectCaseIdentifier(codeScriptsTimestampColumnName);
        lastCodeUpdateSucceededColumnName = defaultDbSupport.toCorrectCaseIdentifier(lastCodeUpdateSucceededColumnName);
    }


    /**
     * Gets the current version from the version table in the database.
     * The version table will be created (or altered) if needed.
     *
     * @return The current version of the database, not null
     */
    public Version getDbVersion() {
        try {
            return getDbVersionImpl();

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            return getDbVersionImpl();
        }
    }


    /**
     * Updates the version of the database to the given value.
     * The version table will be created (or altered) if needed.
     *
     * @param version The new version that the database should be updated to, not null
     */
    public void setDbVersion(Version version) {
        try {
            setDbVersionImpl(version);

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            setDbVersionImpl(version);
        }
    }


    /**
     * Tells us whether the last database version update succeeded or not.
     * The version table will be created (or altered) if needed.
     *
     * @return true if the last database version update succeeded, false otherwise
     */
    public boolean isLastUpdateSucceeded() {
        try {
            return isLastUpdateSucceededImpl();

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            return isLastUpdateSucceededImpl();
        }
    }


    /**
     * Notifies the VersionSource of the fact that the lastest version update has succeeded or not.
     * The version table will be created (or altered) if needed.
     */
    public void registerUpdateSucceeded(boolean succeeded) {
        try {
            registerUpdateSucceededImpl(succeeded);

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            registerUpdateSucceededImpl(succeeded);
        }
    }


    /**
     * Tells us whether the last database code update succeeded or not
     *
     * @return true if the last database code update succeeded, false otherwise
     */
    public boolean isLastCodeUpdateSucceeded() {
        try {
            return isLastCodeUpdateSucceededImpl();

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            return isLastCodeUpdateSucceededImpl();
        }
    }


    /**
     * Notifies the VersionSource of the fact that the lastest code update has succeeded or not
     *
     * @param succeeded True for success
     */
    public void registerCodeUpdateSucceeded(boolean succeeded) {
        try {
            registerCodeUpdateSucceededImpl(succeeded);

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            registerCodeUpdateSucceededImpl(succeeded);
        }
    }


    /**
     * @return The current timestamp of the code scripts
     */
    public long getCodeScriptsTimestamp() {
        try {
            return getCodeScriptsTimestampImpl();

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            return getCodeScriptsTimestampImpl();
        }
    }


    /**
     * Stores the timestamp of the code scripts in the VersionSource
     *
     * @param codeScriptsTimestamp The timestamp, not null
     */
    public void setCodeScriptsTimestamp(long codeScriptsTimestamp) {
        try {
            setCodeScriptsTimestampImpl(codeScriptsTimestamp);

        } catch (UnitilsException e) {
            if (checkVersionTable()) {
                throw e;
            }
            // try again, version table was not ok
            setCodeScriptsTimestampImpl(codeScriptsTimestamp);
        }
    }


    /**
     * @return The current version of the database
     */
    private Version getDbVersionImpl() {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = sqlHandler.getDataSource().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select " + versionIndexColumnName + ", " + versionTimestampColumnName + " from " + defaultDbSupport.qualified(versionTableName));
            rs.next();
            return new Version(rs.getLong(versionIndexColumnName), rs.getLong(versionTimestampColumnName));

        } catch (SQLException e) {
            throw new UnitilsException("Error while retrieving database version", e);
        } finally {
            DbUtils.closeQuietly(conn, st, rs);
        }
    }


    /**
     * Updates the version of the database to the given value
     *
     * @param version The new version that the database should be updated to
     */
    private void setDbVersionImpl(Version version) {
        int updateCount = sqlHandler.executeUpdate("update "
                + defaultDbSupport.qualified(versionTableName) + " set " + versionIndexColumnName
                + " = " + version.getIndex() + ", " + versionTimestampColumnName + " = "
                + version.getTimeStamp() + ", " + lastUpdateSucceededColumnName + " = 1");
        if (updateCount != 1 && sqlHandler.isDoExecuteUpdates()) {
            throw new UnitilsException(
                    "Error while setting database version. There should be exactly 1 version record, found "
                            + updateCount);
        }
    }


    /**
     * Tells us whether the last database version update succeeded or not
     *
     * @return True if the last database version update succeeded, false otherwise
     */
    private boolean isLastUpdateSucceededImpl() {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = sqlHandler.getDataSource().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select " + lastUpdateSucceededColumnName + " from "
                    + defaultDbSupport.qualified(versionTableName));
            if (rs.next()) {
                return (rs.getInt(lastUpdateSucceededColumnName) == 1);
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new UnitilsException("Error while checking whether last update succeeded", e);
        } finally {
            DbUtils.closeQuietly(conn, st, rs);
        }
    }


    /**
     * Notifies the VersionSource of the fact that the lastest version update has succeeded or not
     *
     * @param succeeded True for success
     */
    private void registerUpdateSucceededImpl(boolean succeeded) {
        int updateCount = sqlHandler.executeUpdate("update "
                + defaultDbSupport.qualified(versionTableName) + " set "
                + lastUpdateSucceededColumnName + " = " + (succeeded ? "1" : "0"));
        if (updateCount != 1 && sqlHandler.isDoExecuteUpdates()) {
            throw new UnitilsException(
                    "Error while registering update succeeded. There should be exactly 1 version record, found "
                            + updateCount);
        }
    }


    /**
     * @return The current version of the database
     */
    private long getCodeScriptsTimestampImpl() {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = sqlHandler.getDataSource().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select " + codeScriptsTimestampColumnName + " from "
                    + defaultDbSupport.qualified(versionTableName));
            rs.next();
            return rs.getLong(codeScriptsTimestampColumnName);

        } catch (SQLException e) {
            throw new UnitilsException("Error while retrieving database version", e);
        } finally {
            DbUtils.closeQuietly(conn, st, rs);
        }
    }

    /**
     * Updates the timestamp of the database code to the given value
     *
     * @param timestamp The new timestamp
     */
    private void setCodeScriptsTimestampImpl(long timestamp) {
        int updateCount = sqlHandler.executeCodeUpdate("update "
                + defaultDbSupport.qualified(versionTableName) + " set "
                + codeScriptsTimestampColumnName + " = " + timestamp + ", "
                + lastCodeUpdateSucceededColumnName + " = 1");
        if (updateCount != 1 && sqlHandler.isDoExecuteUpdates()) {
            throw new UnitilsException(
                    "Error while setting database version. There should be exactly 1 version record, found "
                            + updateCount);
        }
    }


    /**
     * Tells us whether the last database version update succeeded or not
     *
     * @return True if the last database version update succeeded, false otherwise
     */
    private boolean isLastCodeUpdateSucceededImpl() {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = sqlHandler.getDataSource().getConnection();
            st = conn.createStatement();
            rs = st.executeQuery("select " + lastCodeUpdateSucceededColumnName + " from "
                    + defaultDbSupport.qualified(versionTableName));
            if (rs.next()) {
                return (rs.getInt(lastCodeUpdateSucceededColumnName) == 1);
            } else {
                return false;
            }
        } catch (SQLException e) {
            throw new UnitilsException("Error while checking whether last update succeeded", e);
        } finally {
            DbUtils.closeQuietly(conn, st, rs);
        }
    }


    /**
     * Notifies the VersionSource of the fact that the code script update has succeeded or not
     *
     * @param succeeded True for success
     */
    private void registerCodeUpdateSucceededImpl(boolean succeeded) {
        int updateCount = sqlHandler.executeCodeUpdate("update "
                + defaultDbSupport.qualified(versionTableName) + " set "
                + lastCodeUpdateSucceededColumnName + " = " + (succeeded ? "1" : "0"));
        if (updateCount != 1 && sqlHandler.isDoExecuteUpdates()) {
            throw new UnitilsException(
                    "Error while registering update succeeded. There should be exactly 1 version record, found "
                            + updateCount);
        }
    }


    /**
     * Checks if the version table and columns are available and if a record exists in which the version info is stored.
     * If not, the table, columns and record are created.
     *
     * @return False if the version table was not ok and therefore updated or created
     */
    private boolean checkVersionTable() {
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = sqlHandler.getDataSource().getConnection();
            st = conn.createStatement();
            // Check if the version table exists
            DatabaseMetaData metadata = conn.getMetaData();
            String schemaName = defaultDbSupport.getSchemaName();
            rs = metadata.getTables(null, schemaName, versionTableName, null);
            String longDataType = defaultDbSupport.getLongDataType();
            if (!rs.next()) {
                // The version table does not exist. Create it
                logger.info("The table " + defaultDbSupport.qualified(versionTableName) + " doesn't exist yet. It is being created");
                sqlHandler.executeUpdate("create table " + defaultDbSupport.qualified(versionTableName) + " ( " + versionIndexColumnName +
                        " " + longDataType + ", " + versionTimestampColumnName + " " + longDataType + ", " +
                        lastUpdateSucceededColumnName + " " + longDataType + ", " + codeScriptsTimestampColumnName + " " +
                        longDataType + ", " + lastCodeUpdateSucceededColumnName + " " + longDataType + " )");
            } else {
                // Check if the version table has the expected columns
                rs.close();
                rs = metadata.getColumns(null, schemaName, versionTableName, versionIndexColumnName);
                if (!rs.next()) {
                    // The version table exists but the version index column does not. Create it
                    logger.info("Column " + versionIndexColumnName + " is missing on table " +
                            defaultDbSupport.qualified(versionTableName) + ". It is being created");
                    sqlHandler.executeUpdate("alter table " + defaultDbSupport.qualified(versionTableName) + " add " +
                            versionIndexColumnName + " " + longDataType);
                }
                rs.close();
                rs = metadata.getColumns(null, schemaName, versionTableName, versionTimestampColumnName);
                if (!rs.next()) {
                    // The version table exists but the version timestamp column does not. Create it
                    logger.info("Column " + versionTimestampColumnName + " is missing on table " +
                            defaultDbSupport.qualified(versionTableName) + ". It is being created");
                    sqlHandler.executeUpdate("alter table " + defaultDbSupport.qualified(versionTableName) + " add " +
                            versionTimestampColumnName + " " + longDataType);
                }
                rs.close();
                rs = metadata.getColumns(null, schemaName, versionTableName, lastUpdateSucceededColumnName);
                if (!rs.next()) {
                    // The version table exists but the last update succeeded column does not. Create it
                    logger.info("Column " + lastUpdateSucceededColumnName + " is missing on table " +
                            defaultDbSupport.qualified(versionTableName) + ". It is being created");
                    sqlHandler.executeUpdate("alter table " + defaultDbSupport.qualified(versionTableName) + " add " +
                            lastUpdateSucceededColumnName + " " + longDataType);
                }
                rs.close();
                rs = metadata.getColumns(null, schemaName, versionTableName, codeScriptsTimestampColumnName);
                if (!rs.next()) {
                    // The version table exists but the version timestamp column does not. Create it
                    logger.info("Column " + codeScriptsTimestampColumnName + " is missing on table " +
                            defaultDbSupport.qualified(versionTableName) + ". It is being created");
                    sqlHandler.executeUpdate("alter table " + defaultDbSupport.qualified(versionTableName) + " add " +
                            codeScriptsTimestampColumnName + " " + longDataType);
                }
                rs.close();
                rs = metadata.getColumns(null, schemaName, versionTableName, lastCodeUpdateSucceededColumnName);
                if (!rs.next()) {
                    // The version table exists but the last update succeeded column does not. Create it
                    logger.info("Column " + lastCodeUpdateSucceededColumnName + " is missing on table " +
                            defaultDbSupport.qualified(versionTableName) + ". It is being created");
                    sqlHandler.executeUpdate("alter table " + defaultDbSupport.qualified(versionTableName) + " add " +
                            lastCodeUpdateSucceededColumnName + " " + longDataType);
                }
            }
            // The version table and columns exist. Check if a record with the version is available
            rs.close();
            rs = st.executeQuery("select * from " + defaultDbSupport.qualified(versionTableName));
            if (!rs.next()) {
                // The version table is empty. Insert a record with default version numbers.
                sqlHandler.executeUpdate("insert into " + defaultDbSupport.qualified(versionTableName) + " (" + versionIndexColumnName + ", " +
                        versionTimestampColumnName + ", " + lastUpdateSucceededColumnName + ", " +
                        codeScriptsTimestampColumnName + ", " + lastCodeUpdateSucceededColumnName + ") values (0, 0, 0, 0, 0)");
            } else {
                // version table was ok
                return true;
            }
            return false;

        } catch (Exception e) {
            throw new UnitilsException("Error while checking version table", e);
        } finally {
            DbUtils.closeQuietly(conn, st, rs);
        }
    }
}