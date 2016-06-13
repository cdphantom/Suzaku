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
            throw new IllegalArgumentException("SQL ��䲻��Ϊ��");
        }

        if (sql.matches("(?i)\\s*select\\b.+")) {
            throw new RuntimeException("��֧��ִ�в�ѯ[SELECT] SQL ���");
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
            throw new RuntimeException("�������������ݼ�¼��Ψһ������ѯ���� [" + results.size() + "] ��");
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
        Query query = createQuery(params); // ������ѯ����
        processTopParameter(query, params); // �����ѯ�����е� TOP ����

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
     * ����һ�� SQL ��ѯ����
     * 
     * @param sql
     *            ԭ���� SQL ���
     * @param params
     *            ��ѯ����ӳ���ϵ
     * @return �´����� SQL ��ѯ����
     */
    protected SQLQuery createSQLQuery(final String sql, final Map<String, ?> params) {
        return (SQLQuery) createQuery(sql, params, true);
    }

    /**
     * ����һ�� HQL ��ѯ����
     * 
     * @param queryString
     *            HQL ��ѯ���
     * @param params
     *            ��ѯ����ӳ���ϵ
     * @return �´����� HQL ��ѯ����
     */
    protected Query createHQLQuery(final String queryString, final Map<String, ?> params) {
        return createQuery(queryString, params, false);
    }

    /**
     * ��ѯ SQL �� HQL ��䴴�� Hibernate ��ѯ����
     * 
     * @param queryString
     *            SQL �� HQL ��ѯ���
     * @param params
     *            ��ѯ����
     * @param isSql
     *            ָ�� queryString �Ƿ�Ϊһ��ԭ�� SQL ���
     * @return �´����� Hibernate ��ѯ����
     */
    protected Query createQuery(final String queryString, final Map<String, ?> params, final boolean isSql) {
        return getHibernateTemplate().execute(new HibernateCallback<Query>() {
            @Override
            public Query doInHibernate(Session session) {
                Query query = (isSql ? session.createSQLQuery(queryString) : session.createQuery(queryString));
                processQueryParameters(query, params); // ���ò�ѯ����Ĳ���
                return query;
            }
        });
    }

    /**
     * Ϊ Hibernate �Ĳ�ѯ�������ò�ѯ�����Ĳ���
     * 
     * @param query
     *            Hibernate ��ѯ����
     * @param params
     *            ������ѯ��������ӳ���ϵ�� Map
     */
    protected void processQueryParameters(Query query, Map<String, ?> params) {
        String[] paramNames = query.getNamedParameters();
        if (ArrayUtils.isEmpty(paramNames) || MapUtils.isEmpty(params)) {
            return;
        }

        // ���ò�ѯ����Ĳ���
        for (String name : paramNames) {
            if (!params.containsKey(name)) {
                throw new RuntimeException("û�����ò��� [" + name + "] ��ֵ�� ��ѯ���Ϊ�� " + query.getQueryString());
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
            if (query instanceof SQLQuery) { // ʹ��ԭ�� SQL ���в�ѯ
                if (value instanceof Date) { // ����
                    hibernateType = TimestampType.INSTANCE;
                } else if (value instanceof Enum) { // ö��
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
     * ���� Hibernate ��ѯ����
     * 
     * @param recordType
     *            ��ѯ�����Ӧ�� PO �� VO ���ݶ��������
     * @param params
     *            ��ѯ����
     * @return �´����Ĳ�ѯ����
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
     * �Ӵ���ļ���������ֵ���л�ȡ����ѯ�� PO �� VO �ඨ��
     * 
     * @param params
     *            ���������ļ�ֵ��
     * @return ����ѯ�� PO �� VO ���ඨ�壬�� params �ж�Ӧ�� key Ϊ
     *         {@link QuerySupport#PARAM_TABLENAME}
     * @throws GboatGenerateQLException
     *             ����� map �в����� key Ϊ {@link QuerySupport#PARAM_TABLENAME}
     *             �Ľ�ֵ�Ի��Ӧ��ֵ��Чʱ�׳����쳣
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
     * ��һ����ͨ�� HQL �� SQL ��ѯ���ת��Ϊ��ѯ�ܼ�¼�����Ĳ�ѯ��䣬����<br>
     * select ... from ... --&gt; select count(1) from ...
     * 
     * @param queryString
     *            ��ͨ�� HQL �� SQL ��ѯ���
     * @param isSql
     *            ֵΪ <code>true</code> ʱ��ʾ����Ĳ�ѯ�����һ��ԭ���� SQL�� ֵΪ
     *            <code>false</code> ʱ��ʾ����Ĳ�ѯ�����һ�� HQL
     * @return ת����Ĳ�ѯ�ܼ�¼�����Ĳ�ѯ���
     */
    protected String convertToQueryCountString(String queryString, boolean isSql) {
        // �������Ĳ�ѯ��䱾����Ѿ��� select count(...) from ... ����ʽ����ֱ�ӷ���
        if (isQueryCountString(queryString)) {
            return queryString;
        }

        String regex = "^(select\\s+.+?\\s+)?from\\s+";
        String selectCount = "select count(" + (isSql ? "1" : "*") + ") ";

        String queryStr = StringUtils.trim(queryString);
        Matcher matcher = Pattern.compile(regex, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(queryStr);
        if (!matcher.find()) {
            throw new RuntimeException("��ѯ���ĸ�ʽ����Ϊ [select ... from ...] ����ʽ");
        }

        // ת���� select count(1) from ����ʽ
        if (matcher.group(1) == null) { // ��ѯ����� from ��ͷ����ǰ��û�� select ... ����䣩
            queryStr = selectCount + queryStr;
        } else {
            queryStr = selectCount + " from (" + queryStr + ") COUNT_TEMP";
        }

        return queryStr;
    }

    /**
     * �ж�һ�� HQL �� SQL ��ѯ����Ƿ�Ϊ��ѯ�ܼ�¼�����Ĳ�ѯ��䣬�������� select count(...) from ... �Ĳ�ѯ���
     * 
     * @param queryString
     *            Ҫ�����жϵ� HQL �� SQL ��ѯ���
     * @return �������Ĳ�����һ����ѯ�ܼ�¼�����Ĳ�ѯ��䣬�򷵻� <code>true</code>�����򷵻�
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
     * �����ѯ�����е� TOP ����
     * 
     * @param query
     *            Hibernate ��ѯ����
     * @param params
     *            ��ѯ����
     */
    protected void processTopParameter(Query query, Map<String, ?> params) {
        if (params.containsKey(QuerySupport.PARAM_TOP)) { // top ��ѯʵ��
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
                throw new RuntimeException("��ѯ�����е� top ������ֵ������һ������ 0 ��������");
            }
            query.setFirstResult(0);
            query.setMaxResults(maxResults);
        }
    }

    /**
     * ��ԭ�� SQL ��ѯ�����Ľ����ת���ɶ�Ӧ�� Java ���ͼ���
     * 
     * @param voClass
     *            ���ݼ�¼��Ӧ�� Java �ඨ��
     * @param records
     *            ԭ�� SQL ��ѯ�����Ľ����
     * @return ת����� Java ���ͼ���
     */
    protected <E> List<E> convertResultToVO(List<Object[]> records, Class<E> voClass) {
        List<E> resultList = new ArrayList<E>(records.size()); // ת���õĽ����
        try {
            Collection<Field> queryFields = sqlGenerater.getQueryColumnFieldMap(voClass).values();
            for (Object[] record : records) {
                E entity = voClass.newInstance();// ������ѯ�����Ӧ�� javaBean
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
     * �Բ�ѯ����е��ֶ�ֵ���� javaBean �е����Զ�����д����ת��
     * 
     * @param field
     *            javaBean �е����Զ���
     * @param value
     *            JDBC ��ѯ����е��ֶ�ֵ
     * @return ���������ķ��� javaBean �������͵��ֶ�ֵ
     */
    protected Object processJdbcValue(Field field, Object value) {
        Class<?> fieldType = field.getType();
        if (value == null) {
            return (fieldType == String.class) ? StringUtils.EMPTY : null;
        }
        try {
            return sqlGenerater.castValue(value, fieldType);
        } catch (Exception e) {
            throw new RuntimeException("VO �� [" + field.getDeclaringClass().getName() + "] ���ֶ� [" + field.getName()
                    + "] ������Ϊ [" + fieldType.getName() + "]�� ����ѯ���ֵ [" + value + "] ������Ϊ [" + value.getClass().getName()
                    + "]", e);
        }
    }
}
