package com.yxm.demo.readlisting.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.yxm.demo.readlisting.gen.Column;
import lombok.Data;

/**
 * @author 徐一杰
 * @date 2022/9/19 17:25
 * @description
 */
@Entity
@Table(name = "people")
@Data
public class People {
    @Id
    @Column
    private long id;
    @Column(index = true, comment = "account", length = 64)
    private String account;
    @Column(comment = "名字", length = 64)
    private String name;
    @Column(comment = "性别", length = 4)
    private String sex;
    @Column(comment = "age")
    private Integer age;
    @Column(comment = "爱好")
    private String lover;
}

