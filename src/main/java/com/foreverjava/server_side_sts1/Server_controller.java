/**
 * 
 */
package com.foreverjava.server_side_sts1;

import java.io.IOException;
import java.time.LocalDate;

import org.apache.tomcat.util.json.JSONParser;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.scheduling.annotation.*;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * @author GAURAV
 *
 */
@CrossOrigin
@RestController
public class Server_controller {
	@GetMapping("/Hello")
	public String welcome() {
		System.out.println("..................getting a request....");
	    java.util.Date date = new java.util.Date();
		return new String(date + " APi is working as expected - Gaurav");
	}
	
	@GetMapping("/Error")
	public String error() {
		return "error 1";
	}
	
//	@PostMapping("/jc")
//	public StringBuilder save(@RequestBody String code) throws IOException {
//		System.out.println(code);
//		FileWriterClass f = new FileWriterClass(code);
//		return f.write();
//	}
	
	//@PostMapping("/java")
	//@Async
	@RequestMapping("/java")
	public StringBuilder runnable(@RequestBody String s) throws IOException {
		JSONObject j = new JSONObject(s);
		System.out.println(j.getString("code") + " : " +j.getString("input"));
		Executor executor = Executors.newFixedThreadPool(10);
		StringBuilder result = new StringBuilder();
		try {
			Calendar c1 = Calendar.getInstance();
			Date dateOne = c1.getTime();
			long time = dateOne.getTime();
			String timestamp = String.valueOf(time);
			
			result = ((ExecutorService) executor).submit(() -> {
			    //return "Hell_" + timestamp;
				FileWriterClass f = new FileWriterClass(j.getString("code"),j.getString("input"), timestamp);
				return f.write();
			}).get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//FileWriterClass f = new FileWriterClass(j.getString("code"),j.getString("input"));
		//return f.write();
		return new StringBuilder(result);
	}
}




