package com.yxm.demo.readlisting.gen;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Column {
	/** 表中字段属性名称常量 */
	String NAME = "name";
	String TYPE = "type";
	String LENGTH = "length";
	String INDEX = "index";
	String NULLABLE = "nullable";
	String DEFAULTS = "defaults";
	String COMMENT = "comment";
	
	/** 不同类型字段的长度默认值 */
	int TYPE_DEFAULT_INT = 11;
	int TYPE_DEFAULT_LONG = 20;
	int TYPE_DEFAULT_STRING = 255;
	int TYPE_DEFAULT_DOUBLE = 0;
	
	// 表中字段约束信息，（长度，索引，是否为空，默认值）
	/**  字段注释 */
	String comment() default "";
	/**  字段长度 */
	int length() default 0;
	/**  字段是否为索引 */
	boolean index() default false;
	/**  字段是否为空 */
	boolean nullable() default false;
	/**  字段默认值 */
	String defaults() default "";
}
