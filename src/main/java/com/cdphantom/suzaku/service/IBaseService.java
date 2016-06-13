package com.cdphantom.suzaku.service;

import java.util.List;
import java.util.Map;

import com.cdphantom.suzaku.dao.IBaseDAO;

public interface IBaseService {
    /**
     * ��ȡ�������ݿ������ʲ����Ķ���
     * 
     * @return ���ݿ������ʲ����Ķ���
     */
    IBaseDAO getBaseDAO();
    
    /**
     * ����һ����¼
     * @param po Ҫ����Ķ���
     * @return ����ɹ����� true�� ���򷵻� false
     */
    String save(Object po);

    /**
     * ������������һ����¼
     * @param po Ҫ���µļ�¼������ֵ���벻Ϊ null
     * @return ���³ɹ����� true�� ���򷵻� false
     */
    boolean update(Object po);

    /**
     * ��������ɾ��һ����¼
     * @param po Ҫɾ���Ķ���
     * @return ɾ���ɹ����� true�� ���򷵻� false
     */
    boolean delete(Object po);
    
    /**
     * ����������ѯһ����¼
     * @param poClass PO �ඨ��
     * @param id ����ֵ
     * @param <T> ���ݼ�¼��Ӧ�� javaBean ����
     * @return ��ѯ���Ķ������û��ƥ���¼�򷵻� null
     */
    <T> T get(Class<T> poClass, String id);
    
    /**
     * ���ظ��ݲ�����ѯ�Ľ���ĵ�һ�����϶���
     * 
     * @param params ��ѯ����
     * @param <T> Ҫ��ѯ�� PO �� VO ������
     * @return Object ��ѯ���Ķ���
     */
    <T> T get(Map<String, ?> params);
    
    /**
     * ͨ���б��ѯ���÷����� {@link #queryList(Map)} ����Ψһ��������Ǹ÷����ķ�������֧�ַ��ͣ������ڵ��÷�����Բ�ѯ���Ľ�����Ͻ�������ǿת
     * 
     * @param queryParams ��ѯ����
     * @param <T> ��ѯ����е����ݼ�¼��Ӧ�� PO �� VO ������
     * @return ��ѯ����б�
     */
    <T> List<T> queryList(Map<String, ?> queryParams);
}
