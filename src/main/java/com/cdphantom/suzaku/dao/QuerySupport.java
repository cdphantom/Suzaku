package com.cdphantom.suzaku.dao;

public interface QuerySupport {
    /** 原始的请求参数Map对象， 值为 {@value} */
    String PARAM_SOURCE_MAP = "PARAM_SOURCE_MAP";

    /** 表名， 值为 {@value} */
    String PARAM_TABLENAME = "TABLENAME";

    /** 当前页， 值为 {@value} */
    String PARAM_PAGE = "PAGE";

    /** 每页条数，如果不需要分页，将该值设置为 -1， 值为 {@value} */
    String PARAM_PAGESIZE = "PAGESIZE";

    /** 每页默认显示多少条， 值为 {@value} */
    int PAGESIZE_DEFAULT = 20;

    /** 排序条件， 值为 {@value} */
    String PARAM_ORDERBY = "ORDERBY";

    /** 参数名称和操作符的前缀：{@value} */
    String PARAM_PREFIX = "_";
    
    /** 参数名称和操作符的前缀：{@value} */
    String PARAM_SEPARATOR = "#";
    
    /** 查询块， 值为 {@value} */
    String PARAM_BLOCK = PARAM_PREFIX + "block";

    /** 是否添加 DISTINCT 关键字排重， 值为 {@value} */
    String PARAM_DISTINCT = "DISTINCT";

    /** 查询多少条， 值为 {@value} */
    String PARAM_TOP = "top";

    /** 查询多少条， 值为 {@value} */
    String PARAM_COUNT = "count";
    
    /* ----------------------------------- 操作符 <开始> ----------------------------------- */
    
    /** 操作符 like，值为 {@value} */
    String OPERA_LIKE = "like";
    
    /** 操作符 =，值为 {@value} */
    String OPERA_EQ = "eq";
    
    /** 操作符 不等于，值为 {@value} */
    String OPERA_NE = "ne";
    
    /** 操作符 大于，值为 {@value} */
    String OPERA_GT = "gt";
    
    /** 操作符 大于等于，值为 {@value} */
    String OPERA_GE = "ge";
    
    /** 操作符 小于 ，值为 {@value} */
    String OPERA_LT = "lt";

    /** 操作符 小于等于，值为 {@value} */
    String OPERA_LE = "le";
    
    /** 操作符 is null，值为 {@value} */
    String OPERA_NULL = "null";
    
    /** 操作符 is not null，值为 {@value} */
    String OPERA_NOTNULL = "notnull";
    
    /** 操作符 in，值为 {@value} */
    String OPERA_IN = "in";
    
    /** 操作符 not in，值为 {@value} */
    String OPERA_NOTIN = "notin";
    
    /** 操作符 ，值为 {@value} */
    String OPERA_OR = "or";
    /* ----------------------------------- 操作符 <结束> ----------------------------------- */
}
