package com.cdphantom.suzaku.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.FIELD, ElementType.ANNOTATION_TYPE })
public @interface Field {
    /**
     * @return �ֶ������� PO ��
     */
    Class<?> clazz();
    
    /**
     * @return PO ���е�������
     */
    String column() default "";
    
    /**
     * @return �Ƿ��ѯ���ֶΣ����ֵΪ false�����ʾ���ֶ�ֻ��Ϊ��ѯ������������ѯ���ֶΡ�Ĭ��Ϊ true��
     */
    boolean query() default true;
}
