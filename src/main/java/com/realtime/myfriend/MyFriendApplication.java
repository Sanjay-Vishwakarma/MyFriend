package com.realtime.myfriend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.HashMap;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@CrossOrigin("*")
public class MyFriendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MyFriendApplication.class, args);
	}

}
