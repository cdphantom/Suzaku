package com.cdphantom.suzaku.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Relations {
    /**
     * @return �����Ӧ�� PO ��
     */
    Class<?> base();
    
    /**
     * @return ���֮��Ĺ�����ϵ
     */
    Relation[] value();
}
