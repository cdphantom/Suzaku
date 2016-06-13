package com.cdphantom.suzaku.service;

import java.util.List;
import java.util.Map;

import com.cdphantom.suzaku.dao.IBaseDAO;

public interface IBaseService {
    /**
     * 获取进行数据库对象访问操作的对象
     * 
     * @return 数据库对象访问操作的对象
     */
    IBaseDAO getBaseDAO();
    
    /**
     * 保存一条记录
     * @param po 要保存的对象
     * @return 保存成功返回 true， 否则返回 false
     */
    String save(Object po);

    /**
     * 根据主键更新一条记录
     * @param po 要更新的记录，主键值必须不为 null
     * @return 更新成功返回 true， 否则返回 false
     */
    boolean update(Object po);

    /**
     * 根据主键删除一条记录
     * @param po 要删除的对象
     * @return 删除成功返回 true， 否则返回 false
     */
    boolean delete(Object po);
    
    /**
     * 根据主键查询一条记录
     * @param poClass PO 类定义
     * @param id 主键值
     * @param <T> 数据记录对应的 javaBean 类型
     * @return 查询到的对象，如果没有匹配记录则返回 null
     */
    <T> T get(Class<T> poClass, String id);
    
    /**
     * 返回根据参数查询的结果的第一个符合对象
     * 
     * @param params 查询参数
     * @param <T> 要查询的 PO 或 VO 的类型
     * @return Object 查询到的对象
     */
    <T> T get(Map<String, ?> params);
    
    /**
     * 通用列表查询。该方法与 {@link #queryList(Map)} 方法唯一的区别就是该方法的返回类型支持泛型，无需在调用方法后对查询到的结果集合进行类型强转
     * 
     * @param queryParams 查询条件
     * @param <T> 查询结果中的数据记录对应的 PO 或 VO 的类型
     * @return 查询结果列表
     */
    <T> List<T> queryList(Map<String, ?> queryParams);
}
