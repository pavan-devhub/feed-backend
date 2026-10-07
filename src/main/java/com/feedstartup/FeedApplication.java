package com.feedstartup;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

// Scheduling: the midnight live updates in LiveUpdateBroadcaster.
@SpringBootApplication
@EnableScheduling
public class FeedApplication {
	public static void main(String[] args) {
		SpringApplication.run(FeedApplication.class, args);
	}   
};


