package com.yxm.demo.readlisting;

import com.yxm.demo.readlisting.gen.GenDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ReadListingApplication {

    @Autowired
    private GenDB genDB;

    public static void main(String[] args) {
        SpringApplication.run(ReadListingApplication.class, args);
    }

}
