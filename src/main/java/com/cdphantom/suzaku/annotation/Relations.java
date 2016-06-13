package com.cdphantom.suzaku.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Relations {
    /**
     * @return 主表对应的 PO 类
     */
    Class<?> base();
    
    /**
     * @return 多表之间的关联关系
     */
    Relation[] value();
}
