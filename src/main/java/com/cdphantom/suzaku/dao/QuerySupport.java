package com.cdphantom.suzaku.dao;

public interface QuerySupport {
    /** ԭʼ���������Map���� ֵΪ {@value} */
    String PARAM_SOURCE_MAP = "PARAM_SOURCE_MAP";

    /** ������ ֵΪ {@value} */
    String PARAM_TABLENAME = "TABLENAME";

    /** ��ǰҳ�� ֵΪ {@value} */
    String PARAM_PAGE = "PAGE";

    /** ÿҳ�������������Ҫ��ҳ������ֵ����Ϊ -1�� ֵΪ {@value} */
    String PARAM_PAGESIZE = "PAGESIZE";

    /** ÿҳĬ����ʾ�������� ֵΪ {@value} */
    int PAGESIZE_DEFAULT = 20;

    /** ���������� ֵΪ {@value} */
    String PARAM_ORDERBY = "ORDERBY";

    /** �������ƺͲ�������ǰ׺��{@value} */
    String PARAM_PREFIX = "_";
    
    /** �������ƺͲ�������ǰ׺��{@value} */
    String PARAM_SEPARATOR = "#";
    
    /** ��ѯ�飬 ֵΪ {@value} */
    String PARAM_BLOCK = PARAM_PREFIX + "block";

    /** �Ƿ���� DISTINCT �ؼ������أ� ֵΪ {@value} */
    String PARAM_DISTINCT = "DISTINCT";

    /** ��ѯ�������� ֵΪ {@value} */
    String PARAM_TOP = "top";

    /** ��ѯ�������� ֵΪ {@value} */
    String PARAM_COUNT = "count";
    
    /* ----------------------------------- ������ <��ʼ> ----------------------------------- */
    
    /** ������ like��ֵΪ {@value} */
    String OPERA_LIKE = "like";
    
    /** ������ =��ֵΪ {@value} */
    String OPERA_EQ = "eq";
    
    /** ������ �����ڣ�ֵΪ {@value} */
    String OPERA_NE = "ne";
    
    /** ������ ���ڣ�ֵΪ {@value} */
    String OPERA_GT = "gt";
    
    /** ������ ���ڵ��ڣ�ֵΪ {@value} */
    String OPERA_GE = "ge";
    
    /** ������ С�� ��ֵΪ {@value} */
    String OPERA_LT = "lt";

    /** ������ С�ڵ��ڣ�ֵΪ {@value} */
    String OPERA_LE = "le";
    
    /** ������ is null��ֵΪ {@value} */
    String OPERA_NULL = "null";
    
    /** ������ is not null��ֵΪ {@value} */
    String OPERA_NOTNULL = "notnull";
    
    /** ������ in��ֵΪ {@value} */
    String OPERA_IN = "in";
    
    /** ������ not in��ֵΪ {@value} */
    String OPERA_NOTIN = "notin";
    
    /** ������ ��ֵΪ {@value} */
    String OPERA_OR = "or";
    /* ----------------------------------- ������ <����> ----------------------------------- */
}
