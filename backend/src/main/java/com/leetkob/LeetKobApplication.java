package com.leetkob;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.leetkob", "vn.edu.hcmus.leetkob"})
public class LeetKobApplication {

  public static void main(String[] args) {
    SpringApplication.run(LeetKobApplication.class, args);
  }
}
