package com.cdphantom.suzaku.service.impl;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.cdphantom.suzaku.dao.IBaseDAO;
import com.cdphantom.suzaku.service.IBaseService;

public class BaseService implements IBaseService {

    /** 进行数据库对象访问操作的对象 */
    @Resource(name = "baseDAO")
    protected IBaseDAO baseDAO;

    @Override
    public IBaseDAO getBaseDAO() {
        return this.baseDAO;
    }

    @Override
    public String save(Object po) {
        return getBaseDAO().save(po);
    }

    @Override
    public boolean update(Object po) {
        getBaseDAO().update(po);
        return true;
    }

    @Override
    public boolean delete(Object po) {
        getBaseDAO().delete(po);
        return true;
    }

    @Override
    public <T> T get(Class<T> poClass, String id) {
        return (T) getBaseDAO().get(poClass, id);
    }

    @Override
    public <T> T get(Map<String, ?> params) {
        return getBaseDAO().get(params);
    }

    @Override
    public <T> List<T> queryList(Map<String, ?> queryParams) {
        return getBaseDAO().queryList(queryParams);
    }
}
