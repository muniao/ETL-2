package top.wetech.tools.db;

import org.apache.commons.dbcp2.BasicDataSourceFactory;
import org.apache.log4j.Logger;
import top.wetech.tools.util.ExternalPropertyUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.Serializable;
import java.sql.*;
import java.util.*;

/**
 * Company:
 * User: chenzuoli
 * Date: 2018/4/27 10:52
 * Description: 动态获取外部配置文件中的jdbc连接参数，加载数据库连接池工具类
 */
public class ExternalDBCPUtils implements Serializable {
    private static Logger logger = Logger.getLogger(ExternalDBCPUtils.class);
    private static DataSource dataSource = null;
    public Properties props;

    private ExternalDBCPUtils(String filePath) {
        if (dataSource == null) {
            logger.info("---------开始初始化数据库连接池---------");
            try {
                ExternalPropertyUtils propertyUtil = ExternalPropertyUtils.getInstance(filePath);
                props = propertyUtil.props;
                dataSource = BasicDataSourceFactory.createDataSource(props);
                logger.info("---------数据库连接池初始化完成---------");
            } catch (IOException e) {
                logger.error("---------加载配置文件失败---------", e);
            } catch (Exception e) {
                logger.error("----------初始化数据库连接池异常失败---------", e);
            }
        }
    }

    /**
     * description: 获取ExternalDBCPUtils类的实例，传递连接数据库的配置文件参数
     * param: [filePath]
     * return: ExternalDBCPUtils
     * date: 2018/6/13 14:21
     */
    public static ExternalDBCPUtils getInstance(String filePath) {
        return new ExternalDBCPUtils(filePath);
    }

    /**
     * Description: 获取数据库连接
     * Param: []
     * Return: java.sql.Connection
     * Date: 2018/4/27 10:57
     */
    public Connection getConnection() {
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
        } catch (Exception e) {
            logger.error("---------数据库连接池获取连接异常---------", e);
        }
        return conn;
    }

    /**
     * Description: 关闭数据库连接
     * Param: [connection]
     * Return: void
     * Date: 2018/4/27 11:08
     */
    public void close(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                logger.error("---------关闭Connection异常---------", e);
            }
        }
    }

    /**
     * Description: 关闭数据库连接及会话
     * Param: [conn, stat]
     * Return: void
     * Date: 2018/4/27 11:09
     */
    public void close(Connection conn, Statement stat) {
        try {
            if (stat != null) {
                stat.close();
            }
        } catch (Exception e) {
            logger.error("---------关闭Connection、Statement异常---------", e);
        } finally {
            close(conn);
        }
    }

    /**
     * Description: 关闭数据库连接、会话、结果集
     * Param: [conn, stat, rs]
     * Return: void
     * Date: 2018/4/27 11:10
     */
    public void close(Connection conn, Statement stat, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            logger.error("---------关闭ResultSet异常---------", e);
        } finally {
            close(conn, stat);
        }
    }

    /**
     * Description: 执行查询
     * Param: [sql, params]
     * Return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     * Date: 2018/4/27 11:10
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> rowDataList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement stat = null;
        ResultSet resultSet = null;
        try {
            conn = getConnection();
            stat = conn.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            stat.setFetchSize(5000);
            setStatParams(stat, params);
            resultSet = stat.executeQuery();
            rowDataList = getResultList(resultSet);
        } catch (Exception e) {
            logger.error("---------数据查询异常[" + sql + "]---------", e);
        } finally {
            close(conn, stat, resultSet);
        }
        return rowDataList;
    }

    /**
     * Description: 更新数据
     * Param: [sql, params]
     * Return: boolean
     * Date: 2018/4/27 11:11
     */
    public boolean executeUpdate(String sql, Object... params) {
        boolean isUpdated = false;
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(sql);
            setStatParams(stat, params);
            int updatedNum = stat.executeUpdate();
            isUpdated = updatedNum == 1;
            conn.commit();
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            logger.error("---------更新失败! sql:[" + sql + "], params:[" + Arrays.toString(params) + "]---------", e);
        } finally {
            close(conn, stat);
        }
        return isUpdated;
    }

    /**
     * Description: 批量更新
     * Param: [sql, paramList]
     * Return: boolean
     * Date: 2018/4/23 13:36
     */
    public boolean executeBatch(String sql, List<String[]> paramList) {
        boolean isUpdated = false;
        Connection conn = null;
        PreparedStatement stat = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            stat = conn.prepareStatement(sql);
            int count = 0;
            for (String[] params : paramList) {
                setStatParams(stat, params);
                stat.addBatch();
                if ((count + 1) % 1000 == 0) {
                    stat.executeBatch();
                    stat.clearBatch();
                }
            }
            stat.executeBatch();
            conn.commit();
            isUpdated = true;
        } catch (Exception e) {
            try {
                conn.rollback();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            logger.error("---------更新失败! sql:[" + sql + "], params:[" + paramList + "]---------", e);
        } finally {
            close(conn, stat);
        }
        return isUpdated;
    }

    /**
     * Description: 执行批处理
     * Param: [sqlList]
     * Return: boolean
     * Date: 2018/4/27 11:11
     */
    public boolean executeBatch(List<String> sqlList) {
        if (sqlList == null || sqlList.isEmpty()) {
            return true;
        }
        Connection conn = null;
        Statement stat = null;
        try {
            conn = getConnection();
            conn.setAutoCommit(false);
            stat = conn.createStatement();
            for (String sql : sqlList) {
                stat.addBatch(sql);
            }
            stat.executeBatch();
            conn.commit();
            return true;
        } catch (Exception e) {
            try {
                conn.rollback();
                logger.error("---------批处理异常，执行回滚---------");
            } catch (Exception e1) {
                logger.error("---------回滚异常---------", e1);
            }
            logger.error("---------执行批处理异常---------");
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (Exception e) {
                logger.error("---------设置自动提交异常---------", e);
            }
            close(conn, stat);
        }
        return false;
    }

    /**
     * Description: 获取列名及数据
     * Param: [rs]
     * Return: java.util.List<java.util.Map<java.lang.String,java.lang.Object>>
     * Date: 2018/4/27 11:12
     */
    private List<Map<String, Object>> getResultList(ResultSet rs) throws SQLException {
        List<Map<String, Object>> rowDataList = new ArrayList<>();
        List<String> colNameList = getColumnName(rs);
        while (rs.next()) {
            Map<String, Object> rowData = new HashMap<>();
            for (String colName : colNameList) {
                rowData.put(colName, rs.getObject(colName));
            }
            if (!rowData.isEmpty()) {
                rowDataList.add(rowData);
            }
        }
        return rowDataList;
    }

    /**
     * Description: 获取列名
     * Param: [rs]
     * Return: java.util.List<java.lang.String>
     * Date: 2018/4/27 11:12
     */
    private List<String> getColumnName(ResultSet rs) throws SQLException {
        List<String> columnList = new ArrayList<>();
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                columnList.add(metaData.getColumnName(i));
            }
        } catch (Exception e) {
            logger.info("------获取表列表异常------", e);
            throw e;
        }
        return columnList;
    }

    /**
     * Description: 为会话PreparedStatement设置参数
     * Param: [stat, params]
     * Return: void
     * Date: 2018/4/27 11:13
     */
    private void setStatParams(PreparedStatement stat, Object... params) throws SQLException {
        if (stat != null && params != null) {
            try {
                for (int len = params.length, i = 1; i <= len; i++) {
                    stat.setObject(i, params[i - 1]);
                }
            } catch (Exception e) {
                logger.error("------设置sql参数异常---------");
                throw e;
            }
        }
    }

}
