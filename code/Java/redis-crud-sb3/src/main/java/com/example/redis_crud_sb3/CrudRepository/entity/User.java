package com.example.redis_crud_sb3.CrudRepository.entity;


import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@RedisHash("users")
public class User {
    @Id//We can pass our ID if not passed then it will be auto-generated
    private String id;
    private String name;
    private int age;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}