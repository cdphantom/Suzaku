package com.cdphantom.suzaku.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface Relation {
    /** 基础对象 */
    Class<?> base() default DefaultBase.class;

    /** 基础对象 field */
    String[] baseColumn();

    /** 关联对象 */
    Class<?> refer();

    /** 关联对象 field */
    String[] referColumn();

    /** 关联类型，默认为内关联 */
    RelationType type() default RelationType.INNER;

    /**
     * 连接条件，优先级高于 column 定义，支持变量。
     * <ul>
     * <li>属性使用“[属性名]”或“[类名.属性名]”，如果没有明确指定类名，则默认使用 base 属性对应的类名；</li>
     * <li>变量使用“{变量名称}”，变量名称即为查询参数 Map 中的 key</li>
     * </ul>
     */
    String on() default "";

    /**
     * Relation使用的默认类，空实现
     * 
     * @author lysming
     * @since 3.0
     * @date 2012-3-20
     */
    class DefaultBase {
    }
}
