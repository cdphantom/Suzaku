package com.cdphantom.suzaku.dao.impl;

import java.lang.reflect.Field;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.hibernate.dialect.Dialect;

public class HQLGenerator extends GenerateQL {

    @Override
    public String generateQueryLanguage(Class<?> resultType, Map<String, ?> params, Dialect dialect)
            throws RuntimeException {
        // 检查是否是PO类
        if (!resultType.isAnnotationPresent(Entity.class)) {
            throw new RuntimeException(resultType.getName() + " isn't assigned with @" + Entity.class.getName());
        }
        return super.generateQueryLanguage(resultType, params, dialect);
    }

    @Override
    protected String getQueryColumns(Class<?> resultType) {
        return null;
    }

    @Override
    protected String getQueryTableName(Class<?> resultType, Map<String, ?> params, Dialect dialect) {
        return resultType.getName();
    }

    @Override
    protected String getColumnName(Field field) throws RuntimeException {
        if (isTransient(field)) {
            throw new RuntimeException("字段 [" + field + "] 被标注为 @" + Transient.class.getName());
        }
        return field.getName();
    }
}
