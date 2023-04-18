package com.yxm.demo.readlisting.repository;

import java.util.List;

import com.yxm.demo.readlisting.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * @author Yxm
 **/
public interface BookRepository extends JpaRepository<Book, Long> {
    List<Book> findByReader(String reader);
}
