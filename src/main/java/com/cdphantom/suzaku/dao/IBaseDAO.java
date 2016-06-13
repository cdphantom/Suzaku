package com.cdphantom.suzaku.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface IBaseDAO {

    /**
     * 获取将指定的日期类型数据列转换为字符串的 SQL 语句
     * 
     * @param columnName 日期类型的数据库列名
     * @param dateFormateOfJava 想要转换的日期格式在 Java 中表示方式，如： yyyy-MM-dd HH:mm:ss，
     *            请参见 {@link gboat2.base.bridge.util.DateUtil#SUPPORT_FORMATS}
     * @return SQL 语句，如：<br>
     *         使用 Oracle 数据库时可能为 to_char(COLUMN_NAME, 'YYYY-MM-DD HH24:MI:SS')；<br>
     *         使用 MySQL 数据库时可能为 date_format(COLUMN_NAME,'%Y-%m-%d %T')
     */
    String getDate2StringSQL(String columnName, String dateFormateOfJava);

    /**
     * 获取将指定的日期字符串转换成数据库中的日期对象的 SQL 语句
     * 
     * @param dateString 日期字符串，如： 2014-12-01 或 2014-12-01 18:01:47
     * @return SQL 语句，如：<br>
     *         使用 Oracle 数据库时可能为 to_date(dateString, 'YYYY-MM-DD HH24:MI:SS')；<br>
     *         使用 MySQL 数据库时可能为 str_to_date(dateString,'%Y-%m-%d %T')
     */
    String getString2DateSQL(String dateString);

    /**
     * 获取数据库内置的查询当前系统时间的关键词
     * 
     * @return 数据库内置的查询当前系统时间的关键词，如： Oracle 返回 current_timestamp， MySQL 返回
     *         current_date
     */
    String getCurrentTimeFuncName();
    
    /**
     * 解除 PO 对象的持久化状态
     * 
     * @param po 被持久化过的对象
     */
    void evict(Object po);

    /**
     * 将游离态的持久化对象重新合并为持久态
     * 
     * @param entity 处于游离态的 PO 对象
     * @param <T> PO 的类型
     * @return 持久态的 PO 对象
     */
    <T> T merge(T entity);
    
    /**
     * Remove all objects from the org.hibernate.Session cache, and cancel all pending saves, updates and deletes
     * @see org.springframework.orm.hibernate3.HibernateTemplate#clear()
     */
    void clear();

    /**
     * 保存（新增）一个对象
     * 
     * @param po 要保存的对象
     * @return String 对象主键
     */
    String save(Object po);

    /**
     * 保存或更新一个对象（如果主键有值，则更新，反之为新增）
     * 
     * @param po 要保存或更新一个对象
     */
    void saveOrUpdate(Object po);

    /**
     * 更新一个对象
     * 
     * @param po 要修改的对象， 该对象必须在数据库中已经存在对应的记录
     */
    void update(Object po);

    /**
     * 用 HQL 语句更新记录
     * 
     * @param queryString HQL 更新语句
     * @param params 参数
     */
    void updateByQuery(String queryString, Map<String, ?> params);

    /**
     * 删除一个对象
     * 
     * @param po 要删除的对象，该对象的主键一定要有值
     */
    void delete(Object po);

    /**
     * 根据主键值删除一条记录
     * 
     * @param clazz 要删除的记录对应的 PO 类型
     * @param id 要删除的记录的主键值
     */
    void delete(Class<?> clazz, Serializable id);

    /**
     * 根据查询条件删除符合条件的数据记录
     * 
     * @param queryString HQL 删除语句
     * @param parameters 查询参数
     * @return 被删除的记录条数
     */
    int deleteByQuery(String queryString, Map<String, ?> parameters);

    /**
     * 执行原生的 SQL 语句
     * 
     * @param sql 原生的 SQL 语句
     * @param params SQL 语句的参数
     * @return int 该 SQL 语句影响到的数据记录条数
     */
    int executeUpdateSql(String sql, Map<String, ?> params);

    /**
     * 根据类型和对象id从数据库取得一个对象
     * 
     * @param clazz 类
     * @param id 对象id
     * @param <T> 需要查询的 PO 的类型
     * @return 目标对象
     */
    <T> T get(Class<T> clazz, Serializable id);

    /**
     * 根据查询语句和查询参数从数据库取得一个对象
     * 
     * @param hql HQL 语句
     * @param params 查询条件参数
     * @param <T> 需要查询的 PO 的类型
     * @return 满足条件的第一条记录，如果没有查询到任何满足条件的数据记录，则返回 <code>null</code>
     */
    <T> T  get(String hql, Map<String, ?> params);

    /**
     * 返回根据参数查询的结果的第一个符合对象
     * 
     * @param params 查询参数
     * @param <T> 要查询的 PO 或 VO 的类型
     * @return Object 查询到的对象
     */
    <T> T get(Map<String, ?> params);

    /**
     * 查询记录条数
     * 
     * @param clazz 类
     * @param field 查询此字段的条数，最好采用主键或索引字段
     * @return 总记录条数
     */
    long getCount(Class<?> clazz, String field);

    /**
     * 查询记录条数
     * 
     * @param queryParams 查询参数，该 map 中必须包含有如下 key：
     * <ul>
     * <li>TABLENAME - 值为一个 Class， PO 的类定义</li>
     * <li>count - 使用 count(...) 函数的属性名称</li>
     * </ul>
     * @return 记录条数
     */
    long getCount(Map<String, ?> queryParams);

    /**
     * 根据 HQL 语句查询符合条件的记录条数
     * @param hql HQL 查询语句，类似于 from POClassName where ...
     * @param params 查询条件的参数
     * @return 符合查询条件的总记录数
     */
    long getCount(String hql, Map<String, ?> params);
    
    /**
     * 根据 SQL 语句查询符合条件的记录条数
     * @param sql 原生的 SQL 查询语句，类似于 select ... from tableName where ...
     * @param params 查询条件
     * @return 符合查询条件的总记录数
     */
    long getCountBySql(String sql, Map<String, ?> params);

    /**
     * 通用列表查询。该方法与 {@link #queryList(Map)} 方法唯一的区别就是该方法的返回类型支持泛型，无需在调用方法后对查询到的结果集合进行类型强转
     * 
     * @param queryParams 查询条件
     * @param <T> 查询结果中的数据记录对应的 PO 或 VO 的类型
     * @return 查询结果列表
     */
    <T> List<T> queryList(Map<String, ?> queryParams);
    
    /**
     * 根据 HQL 查询符合条件的数据记录。该方法与 {@link #queryListByHql(String, Map)}
     * 方法唯一的区别就是该方法的返回类型支持泛型，无需在调用方法后对查询到的结果集合进行类型强转
     * 
     * @param <T> 查询结果中的数据记录对应的 PO 的类型
     * @param hql Hibernate 查询语言
     * @param params 查询条件
     * @return 符合条件的数据记录，如果没有查询到任何记录，则返回一个长度为 0 的空集合
     */
    <T> List<T> queryListByHql(String hql, Map<String, ?> params);

    /**
     * 根据 SQL 查询符合条件的数据记录。该方法与 {@link #queryListBySql(String, Map)}
     * 方法唯一的区别就是该方法的返回类型支持泛型，无需在调用方法后对查询到的结果集合进行类型强转
     * @param <T> 查询结果中的数据记录对应的 PO 或 VO 的类型
     * @param sql 原生的 SQL 查询语句
     * @param params 查询条件
     * @return 符合条件的数据记录，如果没有查询到任何记录，则返回一个长度为 0 的空集合
     */
    <T> List<T> queryListBySql(String sql, Map<String, ?> params);
}
