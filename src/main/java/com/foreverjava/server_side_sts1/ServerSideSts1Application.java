package com.foreverjava.server_side_sts1;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.*;

@SpringBootApplication
@EnableAsync
public class ServerSideSts1Application {

	public static void main(String[] args) {
		SpringApplication.run(ServerSideSts1Application.class, args);
	}
}
