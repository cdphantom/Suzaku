package com.cdphantom.suzaku.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.ANNOTATION_TYPE })
public @interface Relation {
    /** �������� */
    Class<?> base() default DefaultBase.class;

    /** �������� field */
    String[] baseColumn();

    /** �������� */
    Class<?> refer();

    /** �������� field */
    String[] referColumn();

    /** �������ͣ�Ĭ��Ϊ�ڹ��� */
    RelationType type() default RelationType.INNER;

    /**
     * �������������ȼ����� column ���壬֧�ֱ�����
     * <ul>
     * <li>����ʹ�á�[������]����[����.������]�������û����ȷָ����������Ĭ��ʹ�� base ���Զ�Ӧ��������</li>
     * <li>����ʹ�á�{��������}�����������Ƽ�Ϊ��ѯ���� Map �е� key</li>
     * </ul>
     */
    String on() default "";

    /**
     * Relationʹ�õ�Ĭ���࣬��ʵ��
     * 
     * @author lysming
     * @since 3.0
     * @date 2012-3-20
     */
    class DefaultBase {
    }
}
