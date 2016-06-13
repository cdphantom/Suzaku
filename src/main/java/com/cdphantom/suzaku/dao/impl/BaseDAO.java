package com.cdphantom.suzaku.dao.impl;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Entity;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Projections;
import org.hibernate.dialect.MySQL5Dialect;
import org.hibernate.type.TimestampType;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate4.HibernateCallback;
import org.springframework.orm.hibernate4.HibernateTemplate;
import org.springframework.stereotype.Repository;

import com.cdphantom.suzaku.annotation.Relations;
import com.cdphantom.suzaku.dao.IBaseDAO;
import com.cdphantom.suzaku.dao.QuerySupport;

@Repository("baseDAO")
public class BaseDAO implements IBaseDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private HibernateTemplate hibernateTemplate;

    protected final GenerateQL generater = new HQLGenerator();
    protected final SQLGenerator sqlGenerater = new SQLGenerator();

    public HibernateTemplate getHibernateTemplate() {
        if (hibernateTemplate == null) {
            hibernateTemplate = new HibernateTemplate(sessionFactory);
        }
        return hibernateTemplate;
    }

    public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
        this.hibernateTemplate = hibernateTemplate;
    }

    @Override
    public String getDate2StringSQL(String columnName, String dateFormateOfJava) {
        return generater.getDate2StringSQL(columnName, dateFormateOfJava, new MySQL5Dialect());
    }

    @Override
    public String getString2DateSQL(String dateString) {
        return generater.getString2DateSQL(dateString, new MySQL5Dialect());
    }

    @Override
    public String getCurrentTimeFuncName() {
        return generater.getCurrentTimeFuncName(new MySQL5Dialect());
    }

    @Override
    public void evict(Object po) {
        getHibernateTemplate().evict(po);
    }

    @Override
    public <T> T merge(T entity) {
        return (T) getHibernateTemplate().merge(entity);
    }

    @Override
    public void clear() {
        getHibernateTemplate().clear();
    }

    @Override
    public String save(Object po) {
        Serializable pk = getHibernateTemplate().save(po);
        return (pk == null ? null : pk.toString());
    }

    @Override
    public void saveOrUpdate(Object po) {
        getHibernateTemplate().saveOrUpdate(po);
    }

    @Override
    public void update(Object po) {
        getHibernateTemplate().update(po);
    }

    @Override
    public void updateByQuery(String queryString, Map<String, ?> params) {
        Query query = createHQLQuery(queryString, params);
        query.executeUpdate();
    }

    @Override
    public void delete(Object po) {
        getHibernateTemplate().delete(po);
    }

    @Override
    public void delete(Class<?> clazz, Serializable id) {
        Object object = get(clazz, id);
        if (object != null) {
            getHibernateTemplate().delete(object);
        }
    }

    @Override
    public int deleteByQuery(String queryString, Map<String, ?> parameters) {
        Query query = createHQLQuery(queryString, parameters);
        return query.executeUpdate();
    }

    @Override
    public int executeUpdateSql(String sql, Map<String, ?> params) {
        if (StringUtils.isBlank(sql)) {
            throw new IllegalArgumentException("SQL 语句不能为空");
        }

        if (sql.matches("(?i)\\s*select\\b.+")) {
            throw new RuntimeException("不支持执行查询[SELECT] SQL 语句");
        }

        Query query = createSQLQuery(sql, params);
        return query.executeUpdate();
    }

    @Override
    public <T> T get(Class<T> clazz, Serializable id) {
        return (T) getHibernateTemplate().get(clazz, id);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String hql, Map<String, ?> params) {
        Query q = createHQLQuery(hql, params);
        List<?> list = q.list();
        if (list != null && !list.isEmpty()) {
            return (T) list.get(0);
        }
        return null;
    }

    @Override
    public <T> T get(Map<String, ?> params) {
        List<T> results = queryList(params);
        if (results == null || results.isEmpty()) {
            return null;
        }

        if (results.size() > 1) {
            throw new RuntimeException("符合条件的数据记录不唯一，共查询到了 [" + results.size() + "] 条");
        }

        return results.get(0);
    }

    @Override
    public long getCount(Class<?> clazz, String field) {
        Number count = (Number) (sessionFactory.getCurrentSession()).createCriteria(clazz)
                .setProjection(Projections.count(field)).uniqueResult();
        return (count == null ? 0 : count.longValue());
    }

    @Override
    public long getCount(Map<String, ?> queryParams) {
        Map<String, Object> params = new LinkedHashMap<String, Object>(queryParams);
        if (!params.containsKey(QuerySupport.PARAM_COUNT)) {
            params.put(QuerySupport.PARAM_COUNT, "*");
        }
        Query query = createQuery(params);
        Number count = (Number) query.uniqueResult();
        return (count == null ? 0 : count.longValue());
    }

    @Override
    public long getCount(String hql, Map<String, ?> params) {
        hql = convertToQueryCountString(hql, false);
        Query query = createHQLQuery(hql, params);
        Number count = (Number) query.uniqueResult();
        return (count == null ? 0 : count.longValue());
    }

    @Override
    public long getCountBySql(String sql, Map<String, ?> params) {
        sql = convertToQueryCountString(sql, true);
        Query query = createSQLQuery(sql, params);
        Number count = (Number) query.uniqueResult();
        return (count == null ? 0 : count.longValue());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> queryList(Map<String, ?> queryParams) {
        List<T> resultList = null;
        Map<String, Object> params = new LinkedHashMap<String, Object>(queryParams);
        Query query = createQuery(params); // 创建查询对象
        processTopParameter(query, params); // 处理查询条件中的 TOP 参数

        Class<?> recordClass = getTableClass(params);
        if (recordClass.isAnnotationPresent(Entity.class)) { // PO
            resultList = query.list();
        } else { // VO
            List<?> queryResult = query.list();
            if (CollectionUtils.isEmpty(queryResult) || !(queryResult.get(0) instanceof Object[])) {
                resultList = (List<T>) queryResult;
            } else {
                resultList = (List<T>) convertResultToVO((List<Object[]>) queryResult, recordClass);
            }
        }
        return resultList;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> List<T> queryListByHql(String hql, Map<String, ?> params) {
        Query query = createHQLQuery(hql, params);
        return query.list();
    }

    @Override
    public <T> List<T> queryListBySql(String sql, Map<String, ?> params) {
        return queryListByHql(sql, params);
    }

    /**
     * 创建一个 SQL 查询对象
     * 
     * @param sql
     *            原生的 SQL 语句
     * @param params
     *            查询条件映射关系
     * @return 新创建的 SQL 查询对象
     */
    protected SQLQuery createSQLQuery(final String sql, final Map<String, ?> params) {
        return (SQLQuery) createQuery(sql, params, true);
    }

    /**
     * 创建一个 HQL 查询对象
     * 
     * @param queryString
     *            HQL 查询语句
     * @param params
     *            查询条件映射关系
     * @return 新创建的 HQL 查询对象
     */
    protected Query createHQLQuery(final String queryString, final Map<String, ?> params) {
        return createQuery(queryString, params, false);
    }

    /**
     * 查询 SQL 或 HQL 语句创建 Hibernate 查询对象
     * 
     * @param queryString
     *            SQL 或 HQL 查询语句
     * @param params
     *            查询参数
     * @param isSql
     *            指明 queryString 是否为一个原生 SQL 语句
     * @return 新创建的 Hibernate 查询对象
     */
    protected Query createQuery(final String queryString, final Map<String, ?> params, final boolean isSql) {
        return getHibernateTemplate().execute(new HibernateCallback<Query>() {
            @Override
            public Query doInHibernate(Session session) {
                Query query = (isSql ? session.createSQLQuery(queryString) : session.createQuery(queryString));
                processQueryParameters(query, params); // 设置查询对象的参数
                return query;
            }
        });
    }

    /**
     * 为 Hibernate 的查询对象设置查询条件的参数
     * 
     * @param query
     *            Hibernate 查询对象
     * @param params
     *            包含查询条件参数映射关系的 Map
     */
    protected void processQueryParameters(Query query, Map<String, ?> params) {
        String[] paramNames = query.getNamedParameters();
        if (ArrayUtils.isEmpty(paramNames) || MapUtils.isEmpty(params)) {
            return;
        }

        // 设置查询对象的参数
        for (String name : paramNames) {
            if (!params.containsKey(name)) {
                throw new RuntimeException("没有设置参数 [" + name + "] 的值， 查询语句为： " + query.getQueryString());
            }

            Object value = params.get(name);
            if (value instanceof String[]) {
                String[] arrayVal = (String[]) value;
                if (arrayVal.length == 0) {
                    value = "";
                } else if (arrayVal.length == 1) {
                    value = arrayVal[0];
                }
            }

            Type hibernateType = null;
            if (query instanceof SQLQuery) { // 使用原生 SQL 进行查询
                if (value instanceof Date) { // 日期
                    hibernateType = TimestampType.INSTANCE;
                } else if (value instanceof Enum) { // 枚举
                    value = ((Enum<?>) value).name();
                }
            }

            if (hibernateType == null) {
                if (value instanceof Collection) {
                    query.setParameterList(name, (Collection<?>) value);
                } else if (value instanceof Object[]) {
                    query.setParameterList(name, (Object[]) value);
                } else {
                    query.setParameter(name, value);
                }
            } else {
                if (value instanceof Collection) {
                    query.setParameterList(name, (Collection<?>) value, hibernateType);
                } else if (value instanceof Object[]) {
                    query.setParameterList(name, (Object[]) value, hibernateType);
                } else {
                    query.setParameter(name, value, hibernateType);
                }
            }
        }
    }

    /**
     * 创建 Hibernate 查询对象
     * 
     * @param recordType
     *            查询结果对应的 PO 或 VO 数据对象的类型
     * @param params
     *            查询条件
     * @return 新创建的查询对象
     */
    protected Query createQuery(Map<String, ?> params) {
        Class<?> recordType = getTableClass(params);

        if (recordType.isAnnotationPresent(Entity.class)) { // PO
            String hql = generater.generateQueryLanguage(recordType, params, new MySQL5Dialect());
            return createHQLQuery(hql, params);
        }

        if (recordType.isAnnotationPresent(Relations.class)) { // VO
            String sql = sqlGenerater.generateQueryLanguage(recordType, params, new MySQL5Dialect());
            return createSQLQuery(sql, params);
        }

        throw new RuntimeException(recordType.getName() + " doesn't support by query as domain class.");
    }

    /**
     * 从传入的检索条件键值对中获取被查询的 PO 或 VO 类定义
     * 
     * @param params
     *            检索条件的键值对
     * @return 被查询的 PO 或 VO 的类定义，在 params 中对应的 key 为
     *         {@link QuerySupport#PARAM_TABLENAME}
     * @throws GboatGenerateQLException
     *             传入的 map 中不包含 key 为 {@link QuerySupport#PARAM_TABLENAME}
     *             的健值对或对应的值无效时抛出此异常
     */
    protected Class<?> getTableClass(Map<String, ?> params) {
        Object value = params.get(QuerySupport.PARAM_TABLENAME);
        if (value instanceof java.lang.Class) {
            return (Class<?>) value;
        }

        if (!(value instanceof String)) {
            throw new RuntimeException("PARAM_TABLENAME should be setted by type String or type Class");
        }

        String className = (String) value;
        if (StringUtils.isBlank(className)) {
            throw new RuntimeException(
                    "Table should be setted for Query. use PARAM_TABLENAME as the key to set the param.");
        }

        if (!className.contains(".")) {
            throw new RuntimeException("Class name should include the package");
        }

        try {
            return ClassLoader.getSystemClassLoader().loadClass(className);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class [" + className + "] isn't exist.", e);
        }
    }

    /**
     * 将一个普通的 HQL 或 SQL 查询语句转换为查询总记录条件的查询语句，即：<br>
     * select ... from ... --&gt; select count(1) from ...
     * 
     * @param queryString
     *            普通的 HQL 或 SQL 查询语句
     * @param isSql
     *            值为 <code>true</code> 时表示传入的查询语句是一个原生的 SQL； 值为
     *            <code>false</code> 时表示传入的查询语句是一个 HQL
     * @return 转换后的查询总记录条数的查询语句
     */
    protected String convertToQueryCountString(String queryString, boolean isSql) {
        // 如果传入的查询语句本身就已经是 select count(...) from ... 的形式，则直接返回
        if (isQueryCountString(queryString)) {
            return queryString;
        }

        String regex = "^(select\\s+.+?\\s+)?from\\s+";
        String selectCount = "select count(" + (isSql ? "1" : "*") + ") ";

        String queryStr = StringUtils.trim(queryString);
        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(queryStr);
        if (!matcher.find()) {
            throw new RuntimeException("查询语句的格式必须为 [select ... from ...] 的形式");
        }

        // 转换成 select count(1) from 的形式
        if (matcher.group(1) == null) { // 查询语句以 from 开头（最前面没有 select ... 子语句）
            queryStr = selectCount + queryStr;
        } else {
            queryStr = selectCount + " from (" + queryStr + ") COUNT_TEMP";
        }

        return queryStr;
    }

    /**
     * 判断一个 HQL 或 SQL 查询语句是否为查询总记录条数的查询语句，即类似于 select count(...) from ... 的查询语句
     * 
     * @param queryString
     *            要进行判断的 HQL 或 SQL 查询语句
     * @return 如果传入的参数是一个查询总记录条数的查询语句，则返回 <code>true</code>，否则返回
     *         <code>false</code>
     */
    protected boolean isQueryCountString(String queryString) {
        if (StringUtils.isBlank(queryString)) {
            return false;
        }
        return Pattern.compile("^select\\s+count\\s*\\(.+$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)
                .matcher(queryString.trim()).matches();
    }

    /**
     * 处理查询条件中的 TOP 参数
     * 
     * @param query
     *            Hibernate 查询对象
     * @param params
     *            查询条件
     */
    protected void processTopParameter(Query query, Map<String, ?> params) {
        if (params.containsKey(QuerySupport.PARAM_TOP)) { // top 查询实现
            Object top = params.get(QuerySupport.PARAM_TOP);
            int maxResults = 0;
            if (top instanceof Number) {
                maxResults = ((Number) top).intValue();
            } else if (top instanceof CharSequence) {
                maxResults = NumberUtils.toInt(top.toString());
            } else if (top instanceof String[] && ((String[]) top).length == 1) {
                maxResults = NumberUtils.toInt(((String[]) top)[0]);
            }

            if (maxResults <= 0) {
                throw new RuntimeException("查询条件中的 top 参数的值必须是一个大于 0 的正整数");
            }
            query.setFirstResult(0);
            query.setMaxResults(maxResults);
        }
    }

    /**
     * 将原生 SQL 查询出来的结果集转换成对应的 Java 类型集合
     * 
     * @param voClass
     *            数据记录对应的 Java 类定义
     * @param records
     *            原生 SQL 查询出来的结果集
     * @return 转换后的 Java 类型集合
     */
    protected <E> List<E> convertResultToVO(List<Object[]> records, Class<E> voClass) {
        List<E> resultList = new ArrayList<E>(records.size()); // 转换好的结果集
        try {
            Collection<Field> queryFields = sqlGenerater.getQueryColumnFieldMap(voClass).values();
            for (Object[] record : records) {
                E entity = voClass.newInstance();// 单条查询结果对应的 javaBean
                int index = 0;
                Object value;
                for (Field field : queryFields) {
                    field.setAccessible(true);
                    value = processJdbcValue(field, record[index++]);
                    if (value != null) {
                        field.set(entity, value);
                    }
                }
                resultList.add(entity);
            }
        } catch (InstantiationException e) {
            throw new RuntimeException("can't instance class : [" + voClass.getName() + "]", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("can't instance class : [" + voClass.getName() + "]", e);
        } catch (SecurityException e) {
            throw new RuntimeException("can't set value to " + voClass.getName());
        }
        return resultList;
    }

    /**
     * 对查询结果中的字段值按照 javaBean 中的属性定义进行处理和转换
     * 
     * @param field
     *            javaBean 中的属性定义
     * @param value
     *            JDBC 查询结果中的字段值
     * @return 经过处理后的符合 javaBean 定义类型的字段值
     */
    protected Object processJdbcValue(Field field, Object value) {
        Class<?> fieldType = field.getType();
        if (value == null) {
            return (fieldType == String.class) ? StringUtils.EMPTY : null;
        }
        try {
            return sqlGenerater.castValue(value, fieldType);
        } catch (Exception e) {
            throw new RuntimeException("VO 类 [" + field.getDeclaringClass().getName() + "] 中字段 [" + field.getName()
                    + "] 的类型为 [" + fieldType.getName() + "]， 但查询结果值 [" + value + "] 的类型为 [" + value.getClass().getName()
                    + "]", e);
        }
    }
}
