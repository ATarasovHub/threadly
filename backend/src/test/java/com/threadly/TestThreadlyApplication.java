package com.threadly;

import org.springframework.boot.SpringApplication;

public class TestThreadlyApplication {

	public static void main(String[] args) {
		SpringApplication.from(ThreadlyApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
