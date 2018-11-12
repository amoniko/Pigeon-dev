package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
// 扫描mybatis mapper(那堆接口)包路径
@MapperScan(basePackages="com.pigeon.mapper")
// 扫描 所有需要的包, 包含一些自用的工具类包 所在的路径
@ComponentScan(basePackages= {"com.pigeon", "org.n3r"})
public class PigeonApplication {
	
	public static void main(String[] args) {

		SpringApplication.run(PigeonApplication.class, args);
	}
}
