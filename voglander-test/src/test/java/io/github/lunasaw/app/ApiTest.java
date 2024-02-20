package io.github.lunasaw.app;

import io.github.lunasaw.voglander.repository.mapper.DeviceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alibaba.fastjson.JSON;

import io.github.lunasaw.voglander.manager.manager.ExportTaskManager;
import io.github.lunasaw.voglander.repository.entity.ExportTaskDO;
import io.github.lunasaw.voglander.web.ApplicationWeb;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author luna
 * @version 1.0
 * @date 2023/12/3
 * @description:
 */
@SpringBootTest(classes = ApplicationWeb.class)
public class ApiTest {

    @Autowired
    private ExportTaskManager exportTaskManager;

    @Autowired
    private DeviceMapper      deviceMapper;

    @Autowired
    private DataSource        dataSource;

    @Test
    public void atest() {
        ExportTaskDO taskDO = exportTaskManager.getById(null);
        System.out.println(JSON.toJSONString(taskDO));
    }

    @Test
    public void atest_查询版本() throws SQLException {
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select  version()");

        while (resultSet.next()) {
            String string = resultSet.getString(1);
            System.out.println(string);
        }

        connection.close();
    }

    @Test
    public void atest_查询隔离级别() throws SQLException {
        // 查看系统隔离级别：select @@global.tx_isolation;
        // 查看会话隔离级别(5.0以上版本)：select @@tx_isolation;
        // 查看会话隔离级别(8.0以上版本)：select @@transaction_isolation;

        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();
        ResultSet resultSet = statement.executeQuery("select @@transaction_isolation");

        while (resultSet.next()) {
            String string = resultSet.getString(1);
            System.out.println(string);
        }

        connection.close();
    }

    /**
     * 一、Read Uncommitted【读未提交】
     * 读未提交指的是 ： 一个事务可以读取到另一个事务还未提交的数据
     * 这就会导致脏读 即读取到的是数据库内存中的数据 而并非真正磁盘上的数据
     *
     * 例：
     *
     * 1、开启一个命令行窗口A开始事务 然后查询表中记录
     * 设置当前窗口的事务隔离级别为读未提交
     * 命令：
     *
     * set session transaction isolation level read uncommitted
     * 1.
     * 2、另外再打开一个窗口B 也开启事务 然后执行sql语句 但不提交
     * 3、在A窗口重新执行查询 会看到B窗口没有提交的数据
     * -----------------------------------
     * mysql 设置读未提交的事务隔离级别执行无锁读 mysql 读已提交
     * 
     * @throws SQLException
     */
    @Test
    public void atest_设置隔离级别_未提交读() throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        statement.executeUpdate("set session transaction isolation level read uncommitted");

        ResultSet resultSet = statement.executeQuery("select @@transaction_isolation");

        while (resultSet.next()) {
            String string = resultSet.getString(1);
            System.out.println(string);
        }
        statement.executeUpdate("UPDATE tb_device SET `status` = 2 WHERE device_id = '33010602011187000001';");

        Connection connection1 = dataSource.getConnection();
        connection1.setAutoCommit(false);
        Statement statement1 = connection1.createStatement();
        // 设置隔离级别为读未提交
        statement1.executeUpdate("set session transaction isolation level read uncommitted");

        resultSet = statement1.executeQuery("select @@transaction_isolation");

        while (resultSet.next()) {
            String string = resultSet.getString(1);
            System.out.println(string);
        }

        ResultSet resultSet2 = statement1.executeQuery("SELECT * from tb_device where device_id = '33010602011187000001';");
        while (resultSet2.next()) {
            // 读到2
            String string = resultSet2.getString(6);
            System.out.println(string);
        }

        connection.commit();

        connection.close();
        connection1.close();
    }

    /**
     * 二、Read Commited 【读已提交】
     * 与读未提交刚好相反 该隔离级别只能读取到其他事务已经提交的数据 那些没有提交的数据是读不出来的
     * 但会造成一个问题：前后读取到的结果不一样 发生了不可重复读
     * 不可重复读 就是不能执行多次读取 否则会出现结果不一
     *
     * 例：
     *
     * 1、开启一个命令行窗口A 开始事务 然后查询表中记录
     * 设置当前窗口的事务隔离级别为读未提交
     * 命令：
     *
     * set session transaction isolation level read committed
     * 1.
     * 2、另外再打开一个窗口B 也开启事务 然后执行 sql 语句 但不提交
     * 3、在A窗口重新执行查询 看到B窗口刚才执行sql语句的结果 因为它还没有提交
     * 4、在B窗口执行提交
     * 5、在A窗口中执行 然后查看 这时候才会看到B窗口已经修改的结果
     * 6、但会造成一个问题：在A窗口中第一次查看的数据和第二次查看的数据结果不一样
     *
     * -----------------------------------
     * mysql 设置读未提交的事务隔离级别执行无锁读 mysql 读已提交
     * https://blog.51cto.com/u_16213613/7781724
     * 
     * @throws SQLException
     */
    @Test
    public void atest_设置隔离级别_读已提交() throws SQLException {

        // B窗口 修改不提交
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        statement.executeUpdate("set session transaction isolation level read committed");

        ResultSet resultSet = statement.executeQuery("select @@transaction_isolation");

        while (resultSet.next()) {
            String string = resultSet.getString(1);
            System.out.println(string);
        }
        statement.executeUpdate("UPDATE tb_device SET `status` = 2 WHERE device_id = '33010602011187000001';");

        Connection connection1 = dataSource.getConnection();
        connection1.setAutoCommit(false);
        Statement statement1 = connection1.createStatement();
        // 设置隔离级别为读未提交
        statement1.executeUpdate("set session transaction isolation level read committed");

        ResultSet resultSet2 = statement1.executeQuery("select @@transaction_isolation");

        while (resultSet2.next()) {
            String string = resultSet2.getString(1);
            System.out.println(string);
        }

        // A 窗口 查询
        resultSet2 = statement1.executeQuery("SELECT * from tb_device where device_id = '33010602011187000001';");
        while (resultSet2.next()) {
            // 读到1
            String string = resultSet2.getString(6);
            System.out.println(string);
        }

        // B 提交
        connection.commit();

        resultSet2 = statement1.executeQuery("SELECT * from tb_device where device_id = '33010602011187000001';");
        while (resultSet2.next()) {
            // A读到2
            String string = resultSet2.getString(6);
            System.out.println(string);
        }

        connection.close();
        connection1.close();
    }

    /**
     * 三、Repeatable Read 【重复读】
     * MySql默认该隔离级别
     * 该隔离级别可以让事务在自己的会话中重复读取数据 并且不会出现结果不一样的状况
     * 即使其他事务已经提交了 也依然还是显示以前的数据
     *
     * 例：
     *
     * 1、开启一个命令行窗口A开始事务 然后查询表中记录
     * 设置当前窗口的事务隔离级别为重复读
     * 命令：
     *
     * set session transaction isolation level repeatable read
     * 1.
     * 2、另外再打开一个窗口B 开启事务 然后执行sql语句 但不提交
     * 3、在A窗口重新执行查询 会看到B窗口刚才执行sql语句的结果 因为它还没有提交
     * 4、在B窗口执行提交
     * 5、在A窗口中执行查看 此时查询结果 结果会和以前的查询结果一致 不会发生改变
     * -----------------------------------
     * mysql 设置读未提交的事务隔离级别执行无锁读 mysql 读已提交
     * https://blog.51cto.com/u_16213613/7781724
     */
    @Test
    public void atest_设置隔离级别_可重复读() throws SQLException {
        // B窗口 修改不提交
        Connection connection = dataSource.getConnection();
        connection.setAutoCommit(false);
        Statement statement = connection.createStatement();
        statement.executeUpdate("set session transaction isolation level repeatable read");

        ResultSet resultSet = statement.executeQuery("select @@transaction_isolation");

        while (resultSet.next()) {
            String string = resultSet.getString(1);
            System.out.println(string);
        }
        statement.executeUpdate("UPDATE tb_device SET `status` = 2 WHERE device_id = '33010602011187000001';");

        Connection connection1 = dataSource.getConnection();
        connection1.setAutoCommit(false);
        Statement statement1 = connection1.createStatement();
        // 设置隔离级别为读未提交
        statement1.executeUpdate("set session transaction isolation level repeatable read");

        ResultSet resultSet2 = statement1.executeQuery("select @@transaction_isolation");

        while (resultSet2.next()) {
            String string = resultSet2.getString(1);
            System.out.println(string);
        }

        // A 窗口 查询
        resultSet2 = statement1.executeQuery("SELECT * from tb_device where device_id = '33010602011187000001';");
        while (resultSet2.next()) {
            // 读到1
            String string = resultSet2.getString(6);
            System.out.println(string);
        }

        // B 提交
        connection.commit();

        resultSet2 = statement1.executeQuery("SELECT * from tb_device where device_id = '33010602011187000001';");
        while (resultSet2.next()) {
            // A读到还是1
            String string = resultSet2.getString(6);
            System.out.println(string);
        }

        connection.close();
        connection1.close();
    }
}
