package com.yxm.demo.readlisting.repository;

import com.yxm.demo.readlisting.entity.People;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author 徐一杰
 * @date 2022/9/19 17:42
 * @description
 */
@Repository
public interface PeopleMapper extends JpaRepository<People, String> {
}

