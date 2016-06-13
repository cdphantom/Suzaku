package com.cdphantom.suzaku.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
public @interface Field {
    /**
     * @return 字段所属的 PO 类
     */
    Class<?> clazz();
    
    /**
     * @return PO 类中的属性名
     */
    String column() default "";
    
    /**
     * @return 是否查询该字段，如果值为 false，则表示该字段只作为查询条件，而不查询该字段。默认为 true。
     */
    boolean query() default true;
}
