package com.cdphantom.suzaku.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public interface IBaseDAO {

    /**
     * ��ȡ��ָ������������������ת��Ϊ�ַ����� SQL ���
     * 
     * @param columnName �������͵����ݿ�����
     * @param dateFormateOfJava ��Ҫת�������ڸ�ʽ�� Java �б�ʾ��ʽ���磺 yyyy-MM-dd HH:mm:ss��
     *            ��μ� {@link gboat2.base.bridge.util.DateUtil#SUPPORT_FORMATS}
     * @return SQL ��䣬�磺<br>
     *         ʹ�� Oracle ���ݿ�ʱ����Ϊ to_char(COLUMN_NAME, 'YYYY-MM-DD HH24:MI:SS')��<br>
     *         ʹ�� MySQL ���ݿ�ʱ����Ϊ date_format(COLUMN_NAME,'%Y-%m-%d %T')
     */
    String getDate2StringSQL(String columnName, String dateFormateOfJava);

    /**
     * ��ȡ��ָ���������ַ���ת�������ݿ��е����ڶ���� SQL ���
     * 
     * @param dateString �����ַ������磺 2014-12-01 �� 2014-12-01 18:01:47
     * @return SQL ��䣬�磺<br>
     *         ʹ�� Oracle ���ݿ�ʱ����Ϊ to_date(dateString, 'YYYY-MM-DD HH24:MI:SS')��<br>
     *         ʹ�� MySQL ���ݿ�ʱ����Ϊ str_to_date(dateString,'%Y-%m-%d %T')
     */
    String getString2DateSQL(String dateString);

    /**
     * ��ȡ���ݿ����õĲ�ѯ��ǰϵͳʱ��Ĺؼ���
     * 
     * @return ���ݿ����õĲ�ѯ��ǰϵͳʱ��Ĺؼ��ʣ��磺 Oracle ���� current_timestamp�� MySQL ����
     *         current_date
     */
    String getCurrentTimeFuncName();
    
    /**
     * ��� PO ����ĳ־û�״̬
     * 
     * @param po ���־û����Ķ���
     */
    void evict(Object po);

    /**
     * ������̬�ĳ־û��������ºϲ�Ϊ�־�̬
     * 
     * @param entity ��������̬�� PO ����
     * @param <T> PO ������
     * @return �־�̬�� PO ����
     */
    <T> T merge(T entity);
    
    /**
     * Remove all objects from the org.hibernate.Session cache, and cancel all pending saves, updates and deletes
     * @see org.springframework.orm.hibernate3.HibernateTemplate#clear()
     */
    void clear();

    /**
     * ���棨������һ������
     * 
     * @param po Ҫ����Ķ���
     * @return String ��������
     */
    String save(Object po);

    /**
     * ��������һ���������������ֵ������£���֮Ϊ������
     * 
     * @param po Ҫ��������һ������
     */
    void saveOrUpdate(Object po);

    /**
     * ����һ������
     * 
     * @param po Ҫ�޸ĵĶ��� �ö�����������ݿ����Ѿ����ڶ�Ӧ�ļ�¼
     */
    void update(Object po);

    /**
     * �� HQL �����¼�¼
     * 
     * @param queryString HQL �������
     * @param params ����
     */
    void updateByQuery(String queryString, Map<String, ?> params);

    /**
     * ɾ��һ������
     * 
     * @param po Ҫɾ���Ķ��󣬸ö��������һ��Ҫ��ֵ
     */
    void delete(Object po);

    /**
     * ��������ֵɾ��һ����¼
     * 
     * @param clazz Ҫɾ���ļ�¼��Ӧ�� PO ����
     * @param id Ҫɾ���ļ�¼������ֵ
     */
    void delete(Class<?> clazz, Serializable id);

    /**
     * ���ݲ�ѯ����ɾ���������������ݼ�¼
     * 
     * @param queryString HQL ɾ�����
     * @param parameters ��ѯ����
     * @return ��ɾ���ļ�¼����
     */
    int deleteByQuery(String queryString, Map<String, ?> parameters);

    /**
     * ִ��ԭ���� SQL ���
     * 
     * @param sql ԭ���� SQL ���
     * @param params SQL ���Ĳ���
     * @return int �� SQL ���Ӱ�쵽�����ݼ�¼����
     */
    int executeUpdateSql(String sql, Map<String, ?> params);

    /**
     * �������ͺͶ���id�����ݿ�ȡ��һ������
     * 
     * @param clazz ��
     * @param id ����id
     * @param <T> ��Ҫ��ѯ�� PO ������
     * @return Ŀ�����
     */
    <T> T get(Class<T> clazz, Serializable id);

    /**
     * ���ݲ�ѯ���Ͳ�ѯ���������ݿ�ȡ��һ������
     * 
     * @param hql HQL ���
     * @param params ��ѯ��������
     * @param <T> ��Ҫ��ѯ�� PO ������
     * @return ���������ĵ�һ����¼�����û�в�ѯ���κ��������������ݼ�¼���򷵻� <code>null</code>
     */
    <T> T  get(String hql, Map<String, ?> params);

    /**
     * ���ظ��ݲ�����ѯ�Ľ���ĵ�һ�����϶���
     * 
     * @param params ��ѯ����
     * @param <T> Ҫ��ѯ�� PO �� VO ������
     * @return Object ��ѯ���Ķ���
     */
    <T> T get(Map<String, ?> params);

    /**
     * ��ѯ��¼����
     * 
     * @param clazz ��
     * @param field ��ѯ���ֶε���������ò��������������ֶ�
     * @return �ܼ�¼����
     */
    long getCount(Class<?> clazz, String field);

    /**
     * ��ѯ��¼����
     * 
     * @param queryParams ��ѯ�������� map �б������������ key��
     * <ul>
     * <li>TABLENAME - ֵΪһ�� Class�� PO ���ඨ��</li>
     * <li>count - ʹ�� count(...) ��������������</li>
     * </ul>
     * @return ��¼����
     */
    long getCount(Map<String, ?> queryParams);

    /**
     * ���� HQL ����ѯ���������ļ�¼����
     * @param hql HQL ��ѯ��䣬������ from POClassName where ...
     * @param params ��ѯ�����Ĳ���
     * @return ���ϲ�ѯ�������ܼ�¼��
     */
    long getCount(String hql, Map<String, ?> params);
    
    /**
     * ���� SQL ����ѯ���������ļ�¼����
     * @param sql ԭ���� SQL ��ѯ��䣬������ select ... from tableName where ...
     * @param params ��ѯ����
     * @return ���ϲ�ѯ�������ܼ�¼��
     */
    long getCountBySql(String sql, Map<String, ?> params);

    /**
     * ͨ���б��ѯ���÷����� {@link #queryList(Map)} ����Ψһ��������Ǹ÷����ķ�������֧�ַ��ͣ������ڵ��÷�����Բ�ѯ���Ľ�����Ͻ�������ǿת
     * 
     * @param queryParams ��ѯ����
     * @param <T> ��ѯ����е����ݼ�¼��Ӧ�� PO �� VO ������
     * @return ��ѯ����б�
     */
    <T> List<T> queryList(Map<String, ?> queryParams);
    
    /**
     * ���� HQL ��ѯ�������������ݼ�¼���÷����� {@link #queryListByHql(String, Map)}
     * ����Ψһ��������Ǹ÷����ķ�������֧�ַ��ͣ������ڵ��÷�����Բ�ѯ���Ľ�����Ͻ�������ǿת
     * 
     * @param <T> ��ѯ����е����ݼ�¼��Ӧ�� PO ������
     * @param hql Hibernate ��ѯ����
     * @param params ��ѯ����
     * @return �������������ݼ�¼�����û�в�ѯ���κμ�¼���򷵻�һ������Ϊ 0 �Ŀռ���
     */
    <T> List<T> queryListByHql(String hql, Map<String, ?> params);

    /**
     * ���� SQL ��ѯ�������������ݼ�¼���÷����� {@link #queryListBySql(String, Map)}
     * ����Ψһ��������Ǹ÷����ķ�������֧�ַ��ͣ������ڵ��÷�����Բ�ѯ���Ľ�����Ͻ�������ǿת
     * @param <T> ��ѯ����е����ݼ�¼��Ӧ�� PO �� VO ������
     * @param sql ԭ���� SQL ��ѯ���
     * @param params ��ѯ����
     * @return �������������ݼ�¼�����û�в�ѯ���κμ�¼���򷵻�һ������Ϊ 0 �Ŀռ���
     */
    <T> List<T> queryListBySql(String sql, Map<String, ?> params);
}
