package com.yxm.demo.readlisting.gen;

import static com.yxm.demo.readlisting.gen.Column.*;

import javax.persistence.Table;
import javax.persistence.Transient;
import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.message.ParameterizedMessage;

/**
 * @author Yxm
 **/
public class GenDB {
    /**
     * 获取不同类型默认长度
     * @param type
     * @return
     * @throws Exception
     */
    private static int getDefaultLength(String type) {
        switch (type) {
            case "int":
                return TYPE_DEFAULT_INT;
            case "long":
                return TYPE_DEFAULT_LONG;
            case "String":
                return TYPE_DEFAULT_STRING;
            case "double":
                return TYPE_DEFAULT_DOUBLE;
            default:
                return 0;
        }
    }

    /**
     * java类型转sql类型
     * @param type
     * @return
     * @throws Exception
     */
    private static String getSqlType(String type) throws Exception {
        switch (type) {
            case "int":
            case "Integer":
                return "integer";
            case "long":
            case "Long":
                return "bigint";
            case "String":
                return "varchar";
            case "boolean":
            case "Boolean":
                return "tinyint";
            case "double":
            case "Double":
                return "double";
            case "float":
            case "Float":
                return "float";
            default:
                throw new Exception("不能识别的类型:" + type);
        }
    }

    /**
     * 从clazz中获取其中的所有枚举实例
     * @param clazz
     * @return
     * @throws Exception
     */
    private static List<Field> getAllFieldInfo(Class<?> clazz) throws Exception {
        List<Field> result = new ArrayList<>();

        // 获得所有字段成员（id, account, name, profession...）,排除有@Transient注解的字段成员
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (!field.isAnnotationPresent(Transient.class)) {
                result.add(field);
            }
        }
        return result;
    }

    /**
     * 获取配置类中的所有字段名
     * @param clazz
     * @return
     * @throws Exception
     */
    private static List<String> getColumns(Class<?> clazz) throws Exception {
        List<String> result = new ArrayList<>();

        // 获得所有枚举字段成员（id, account, name, profession...）
        List<Field> allFieldInfo = getAllFieldInfo(clazz);

        // 遍历获取字段名，id是默认添加的
        result.add("id");
        for (Field e : allFieldInfo) {
            result.add(e.getName().toLowerCase());
        }
        return result;
    }

    /**
     * 获取所有约束信息
     * @param obj
     * @return
     * @throws Exception
     */
    private static Map<String, Object> getFieldInfo(Object obj) throws Exception {
        Map<String, Object> result = new HashMap<>();

        // 获取所有约束信息
        String name = ((Field) obj).getName();
        if ("Id".equals(name) || "ID".equals(name)) {
            name = "id";
        }
        String typeName = ((Field) obj).getType().getSimpleName();
        String type = getSqlType(typeName);

        int length = (Integer) getColumnField(obj, LENGTH);
        boolean index = (Boolean) getColumnField(obj, INDEX);
        String nullable = (Boolean) getColumnField(obj, NULLABLE) ? "NULL" : "NOT NULL";
        String comment = (String) getColumnField(obj, COMMENT);

        // 默认值
        String def = (String) getColumnField(obj, DEFAULTS);
        if ("boolean".equals(typeName) && !StringUtils.isEmpty(def)) {
            def = Boolean.parseBoolean(def) ? "1" : "0";
        }
        String defaults = def.equals("") ? "" : " DEFAULT '" + def + "'";

        // 如果长度为0，即没设长度，则提取默认值
        if (length == 0) {
            length = getDefaultLength(typeName);
        }

        result.put(NAME, name);
        result.put(TYPE, type);
        result.put(LENGTH, length);
        result.put(INDEX, index);
        result.put(NULLABLE, nullable);
        result.put(DEFAULTS, defaults);
        result.put(COMMENT, comment);
        return result;
    }

    static Object getColumnField(Object e, String name) throws Exception {
        return getEnumAnnotationField(e, Column.class, name);
    }
    static Object getEnumAnnotationField(Object o, Class<? extends Annotation> annotationClazz, String name) throws Exception {
        Field field = (Field) o;
        Annotation annotation = field.getAnnotation(annotationClazz);
        Method method = annotationClazz.getMethod(name);
        return method.invoke(annotation);
    }

    /**
     * 获取表中所有字段信息
     * @param clazz
     * @return
     * @throws Exception
     */
    private static List<Map<String, Object>> getTableInfo(Class<?> clazz) throws Exception {
        List<Map<String, Object>> tableInfo = new ArrayList<>();

        // 获得所有枚举字段成员（id, account, name, profession...）
        List<Field> allFieldInfo = getAllFieldInfo(clazz);

        // 遍历字段成员获取信息
        for (Field e : allFieldInfo) {
            tableInfo.add(getFieldInfo(e));
        }

        return tableInfo;
    }

    /**
     * 获取某个字段的约束信息
     * @param clazz
     * @param name
     * @return
     * @throws Exception
     */
    private static Map<String, Object> getOneFieldInfo(Class<?> clazz, String name) throws Exception {
        List<Field> fields = getAllFieldInfo(clazz);
        Optional<Field> field = fields.stream().filter(e -> e.getName().equalsIgnoreCase(name)).findFirst();
        if (field.isPresent()) {
            return getFieldInfo(field.get());
        } else {
            return Collections.emptyMap();
        }
    }

    /**
     * 获取配置表中需要创建索引的字段
     * @param clazz
     * @return
     * @throws Exception
     */
    private static List<String> getIndexInfo(Class<?> clazz) throws Exception {
        String tableName = clazz.getAnnotation(Table.class).name();
        List<String> result = new ArrayList<>();
        List<Field> fields = getAllFieldInfo(clazz);
        for (Field e : fields) {
            boolean index = (Boolean) getColumnField(e, INDEX);
            if (index) {
                result.add(tableName + "_" + e.getName());
            }
        }
        return result;
    }

    /**
     * 在表上创建索引
     * @param conn
     * @param tableName
     * @param clazz
     * @throws Exception
     */
    private static void checkCreateIndex(Connection conn, String tableName, Class<?> clazz) throws Exception {
        // 反射获取配置中待创建索引
        List<String> indexConfs = getIndexInfo(clazz);

        // 表中加索引的列信息
        List<String> indexTables = new ArrayList<>();
        DatabaseMetaData dbMeta = conn.getMetaData();

        // 获取表中索引信息
        ResultSet indexs = dbMeta.getIndexInfo(null, null, tableName, false, true);
        while (indexs.next()) {
            indexTables.add(indexs.getString("INDEX_NAME"));
        }
        indexs.close();

        // 若数据表索引包含配置类中全部索引，则不用建索引，直接返回
        if (indexTables.containsAll(indexConfs)) {
            return ;
        }

        // 找出配置中有，数据表中没有的索引
        indexConfs.removeAll(indexTables);

        // 创建索引
        try (Statement st = conn.createStatement()) {
            for (String index : indexConfs) {
                if (index.equals("id")) {
                    continue;
                }
                String column = index.replaceAll(tableName + "_", "");
                String indexSql = "CREATE INDEX " + index + " ON " + tableName +"(" + column + ")";
                System.out.println("建索引: " + indexSql);
                st.executeUpdate(indexSql);
            }
        }
    }

    /**
     * 检查字段长度是否改变（只允许改长，不允许改短）,不要修改index字段,只修改varchar
     * 如：news 表里的title  字段 原来长度是 100个字符，现长度要改成130个字符 : alter table news modify column title varchar(130);
     * @param conn
     * @param tableName
     * @param clazz
     * @throws Exception
     */
    private static void checkFieldLen(Connection conn, String tableName, Class<?> clazz) throws Exception {
        // 获取表中列信息
        DatabaseMetaData dBMetaData = conn.getMetaData();
        ResultSet colSet = dBMetaData .getColumns(null, "%", tableName, "%");

        // 表中已有的列名，长度。
        Map<String, Integer> colTable = new HashMap<>();
        while (colSet.next()) {
            if(colSet.getInt("DATA_TYPE") == Types.VARCHAR){
                colTable.put(colSet.getString("COLUMN_NAME"), colSet.getInt("COLUMN_SIZE"));
            }
        }
        colSet.close();

        // 获取并遍历配置表字段
        List<Map<String, Object>> tableInfo = getTableInfo(clazz);
        Map<String, Integer> colEntity = new HashMap<>();
        for(Map<String, Object> t : tableInfo) {
            colEntity.put(t.get(NAME).toString(), (Integer)t.get(LENGTH));
        }

        // 获得字段变长了的字段信息
        Map<String, Integer> colResult = new HashMap<>();
        for (Entry<String, Integer> entry : colTable.entrySet()) {
            String colName = entry.getKey();
            int oldLen = entry.getValue();
            // 字段已删除
            if(!colEntity.containsKey(colName)){
                continue;
            }
            int newLen = colEntity.get(colName);
            if (newLen > oldLen) {
                colResult.put(colName, newLen);
            }
        }

        // 没有改变
        if(colResult.isEmpty()){
            return;
        }

        // 拼成SQL语句
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("` ");
        for (Entry<String, Integer> entry : colResult.entrySet()) {
            sql.append("MODIFY COLUMN `" + entry.getKey() + "` varchar(" + entry.getValue() + "),");
        }
        sql.deleteCharAt(sql.length() - 1);
        System.out.println("\n更新表长度: " + sql.toString());

        // 更新表操作
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql.toString());
        }
    }

    /**
     * 检查字段int->long,不要修改index字段,只修改int->long
     * 如：human 表  combat字段 int 改 long: alter table news modify column title varchar(130);
     * @param conn
     * @param tableName
     * @param clazz
     * @throws Exception
     */
    private static void checkFieldIntToLong(Connection conn, String tableName, Class<?> clazz) throws Exception {
        // 获取表中列信息
        DatabaseMetaData dBMetaData = conn.getMetaData();
        ResultSet resultSet = dBMetaData .getColumns(null, "%", tableName, "%");

        // 表中已有的列名，长度。
        Set<String> intCols = new HashSet<>();
        while(resultSet.next()) {
            if(resultSet.getInt("DATA_TYPE") == Types.INTEGER){
                intCols.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        resultSet.close();

        // 获取并遍历配置表字段
        List<Map<String, Object>> tableInfo = getTableInfo(clazz);
        Set<String> longCols = new HashSet<>();
        for(Map<String, Object> t : tableInfo) {
            String type = (String)t.get(TYPE);
            if("bigint".equals(type)) {
                longCols.add(t.get("name").toString());
            }
        }

        // 获得int改为long字段信息
        intCols.retainAll(longCols);
        // 没有改变
        if(intCols.isEmpty()){
            return;
        }

        // 拼成SQL语句
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("` ");
        for(String colName : intCols){
            sql.append("MODIFY COLUMN `" + colName + "` bigint,");
        }
        sql.deleteCharAt(sql.length() - 1);
        System.out.println("\n更新表字段长度int -> long: " + sql.toString());

        // 更新表操作
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql.toString());
        }
    }

    /**
     * 建表操作
     * @param conn
     * @param tableName
     * @param clazz
     * @throws Exception
     */
    private static void createTable(Connection conn, String tableName, Class<?> clazz) throws Exception {
        // 拼成SQL语句
        StringBuilder sql = new StringBuilder();
        /**  建表 */
        sql.append("CREATE TABLE `").append(tableName).append("`");
        sql.append("(");
        // 获取并遍历配置表字段
        List<Map<String, Object>> tableInfo = getTableInfo(clazz);
        if (tableInfo.stream().noneMatch(v -> "id".equals(v.get(NAME)))) {
            /**  创建默认主键 */
            sql.append("`id` bigint(20) NOT NULL,");
        }
        for (Map<String, Object> t : tableInfo) {
            /**  字段名 */
            sql.append("`").append(t.get(NAME)).append("` ");
            /**  类型 */
            sql.append(t.get(TYPE));
            //这里区分一下，创建表的时候double，float，decimal需要指定两个参数(可以默认不指定)，这里区分下
            if (t.get(TYPE).equals("double") || "float".equals(t.get(TYPE)) || "decimal".equals(t.get(TYPE))) {
                /**  长度 */
                sql.append(" ");
            } else {
                /**  长度 */
                sql.append("(").append(t.get(LENGTH)).append(") ");
            }

            /**  是否为空 */
            sql.append(t.get(NULLABLE));
            /**  默认值 */
            sql.append(t.get(DEFAULTS));
            /** 注释 */
            sql.append(" COMMENT '").append(t.get(COMMENT)).append("'");
            sql.append(",");
        }
        /**  设置主键 */
        sql.append("PRIMARY KEY (`id`)");
        sql.append(")");
        System.out.println("\n建表: " + sql);

        // 执行建表操作
        try (Statement st = conn.createStatement()) {
            st.executeUpdate(sql.toString());
        }

        // 建索引
        checkCreateIndex(conn, tableName, clazz);
    }

    /**
     * 更新表操作
     * @param con
     * @param tableName
     * @param clazz
     * @throws Exception
     */
    private static void updateTable(Connection con, String tableName, Class<?> clazz) throws Exception {
        // 获取表中列信息
        DatabaseMetaData dBMetaData = con.getMetaData();
        ResultSet colSet = dBMetaData .getColumns(null, "%", tableName, "%");

        // 表中已有的列名
        List<String> colTables = new ArrayList<>();
        while (colSet.next()) {
            colTables.add(colSet.getString("COLUMN_NAME").toLowerCase());
        }
        colSet.close();

        // 配置中的列名
        List<String> colConfs = getColumns(clazz);

        // 如果数据表中列名包含配置表中全部列名, 则检查创建索引，不用更新表，直接返回
        if (colTables.containsAll(colConfs)) {
            return;
        }

        // 找出两表列名不同
        colConfs.removeAll(colTables);

        // 取得配置中的表字段信息, 拼成SQL语句
        StringBuilder sql = new StringBuilder();
        /**  更新表 */
        sql.append("ALTER TABLE `").append(tableName).append("` ");
        for (int i = 0; i < colConfs.size(); i++) {
            String col = colConfs.get(i);
            Map<String, Object> field = getOneFieldInfo(clazz, col);

            if (i > 0) sql.append(", ");

            /**  增加列名 */
            sql.append("ADD `").append(field.get(NAME)).append("` ");
            /**  类型 */
            sql.append(field.get(TYPE));
            //这里区分一下，创建表的时候double，float，decimal需要指定两个参数(可以默认不指定)，这里区分下
            if (field.get(TYPE).equals("double") || "float".equals(field.get(TYPE)) || "decimal".equals(field.get(TYPE))) {
                /**  长度 */
                sql.append(" ");
            } else {
                /**  长度 */
                sql.append("(").append(field.get(LENGTH)).append(") ");
            }
            /**  是否为空 */
            sql.append(field.get(NULLABLE));
            /**  默认值 */
            sql.append(field.get(DEFAULTS));
        }

        System.out.println("\n更新表: " + sql.toString());

        // 更新表操作
        try (Statement st = con.createStatement()) {
            st.executeUpdate(sql.toString());
        }
    }

    private static Connection getDBConnection(String driver, String urlDB, String user, String pwd) throws Exception {
        Class.forName(driver);
        return DriverManager.getConnection(urlDB, user, pwd);
    }

    private static void genDB(Connection conn, String sourceDir) {
        try {
            // 获取源文件夹下的所有类
            List<Class<?>> sources = getAllClass(sourceDir);

            // 遍历所有类，取出有注解的生成实体类
            for (Class<?> clazz : sources) {
                // 过滤没有EntityConfig注解的类, 并建表
                if (clazz.isAnnotationPresent(Table.class)) {
                    checkAndCreat(clazz, conn);
                }
            }
            // 关闭连接
            conn.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 检查并建表或更新表结构
     * @param clazz
     * @param conn
     * @throws Exception
     */
    private static void checkAndCreat(Class<?> clazz, Connection conn) throws Exception {
        // 获取表的信息
        String catalog = null;
        String schema = "%";
        String tableName = clazz.getSimpleName().toLowerCase();

        String[] types = new String[] { "TABLE" };
        DatabaseMetaData dBMetaData = conn.getMetaData();

        // 从databaseMetaData获取表信息
        ResultSet tableSet = dBMetaData.getTables(catalog, schema, tableName, types);

        // 如果表不存在, 则建表
        if (!tableSet.next()) {
            createTable(conn, tableName, clazz);
            // 表存在, 则更新表
        } else {
            updateTable(conn, tableName, clazz);
            checkCreateIndex(conn, tableName, clazz);
            checkFieldLen(conn, tableName, clazz);
            checkFieldIntToLong(conn, tableName, clazz);
        }
        // 关闭数据库连接
        tableSet.close();
    }
    public static String createStr(String str, Object...params) {
        return ParameterizedMessage.format(str, params);
    }
    /**
     * 将从配置文件中读取的形如org.jow转成org/jow路径格式
     * @param packageName
     * @return
     */
    public static String packageToPath(String packageName) {
        return packageName.replace('.', '/');
    }

    /**
     * 读取包内所有的类获取class对象，并根据指定的条件过滤
     * @param packageName
     * @param excludePackages
     * @return
     */
    public static List<Class<?>> getAllClass(String packageName, List<String> excludePackages) {
        Set<Class<?>> classes = new HashSet<>();
        ClassLoader cl = GenDB.class.getClassLoader();
        String packagePath = packageToPath(packageName);
        try {
            Enumeration<URL> dirs = cl.getResources(packagePath);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    findByFile(cl, packageName, excludePackages, URLDecoder.decode(url.getFile(), "utf-8"), classes);
                } else if ("jar".equals(protocol)) {
                    findInJar(cl, packageName, excludePackages, url, classes);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<Class<?>> result = new ArrayList<>(classes);
        Collections.sort(result, Comparator.comparing(Class::getName));
        return result;
    }

    /**
     * 读取包内所有的类获取class对象，并根据指定的条件过滤
     * @param packageName
     * @return
     */
    public static List<Class<?>> getAllClass(String packageName) {
        return getAllClass(packageName, Collections.emptyList());
    }

    /**
     * 从文件获取java类
     * @param cl
     * @param packageName
     * @param excludePackages
     * @param filePath
     * @param classes
     */
    private static void findByFile(ClassLoader cl, String packageName, List<String> excludePackages, String filePath, Set<Class<?>> classes) {
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        if (excludePackages.stream().anyMatch(packageName::startsWith)) {
            return;
        }

        File[] dirFiles = dir.listFiles(f -> f.isDirectory() || f.getName().endsWith(".class"));
        for (File file : dirFiles) {
            if (file.isDirectory()) {
                findByFile(cl, packageName + "." + file.getName(), excludePackages, file.getAbsolutePath(), classes);
            } else {
                try {
                    String className = packageName + "." + file.getName().substring(0, file.getName().length() - 6);
                    Class<?> clazz = cl.loadClass(className);
                    classes.add(clazz);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取jar中的java类
     * @param cl
     * @param packageName
     * @param excludePackages
     * @param url
     * @param classes
     */
    private static void findInJar(ClassLoader cl, String packageName, List<String> excludePackages, URL url, Set<Class<?>> classes) {
        if (excludePackages.stream().anyMatch(packageName::startsWith)) {
            return;
        }

        String packagePath = packageToPath(packageName) + '/';
        try {
            JarFile jar = ((JarURLConnection) url.openConnection()).getJarFile();
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (name.startsWith(packagePath) && name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6).replace('/', '.');
                    try {
                        Class<?> clazz = cl.loadClass(name);
                        classes.add(clazz);
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 给部署服务器时调用新建数据库
     * @param args 一个参数要搜索的源文件夹
     * @throws Exception
     */
    public static void genDB(String[] args) throws Exception {
        String sourceDir = args[0];
        String dbUrl = "";
        String dbUser = "";
        String dbPwd = "";
        if (args.length >= 5) {
            // 通过命令行指定了DB
            dbUrl = createStr("jdbc:mysql://{}/{}?useUnicode=true&characterEncoding=utf8&createDatabaseIfNotExist=true", args[1], args[4]);
            dbUser = args[2];
            dbPwd = args[3];
        }
        Connection conn = getDBConnection("com.mysql.cj.jdbc.Driver", dbUrl, dbUser, dbPwd);
        genDB(conn, sourceDir);
        System.out.println("执行完毕，如果没有输出则说明无需建表或者无需更新表结构。");
        // 正常退出
        System.exit(0);
    }

    public static void main(String[] args) throws Exception {
        GenDB.genDB(args);
    }
}
