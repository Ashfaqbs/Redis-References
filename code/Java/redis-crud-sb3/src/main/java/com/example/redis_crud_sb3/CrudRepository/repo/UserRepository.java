package com.example.redis_crud_sb3.CrudRepository.repo;


import org.springframework.data.repository.CrudRepository;

import com.example.redis_crud_sb3.CrudRepository.entity.User;

public interface UserRepository extends CrudRepository<User, String> {
}