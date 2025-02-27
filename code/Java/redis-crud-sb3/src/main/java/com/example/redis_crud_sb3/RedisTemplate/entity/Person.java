package com.example.redis_crud_sb3.RedisTemplate.entity;

import java.io.Serializable;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import lombok.Getter;
import lombok.Setter;

@RedisHash("Person")
@Getter
@Setter
public class Person implements Serializable {
    @Id
    private String id;
    private String name;
    private int age;
    // Getters and setters
}
