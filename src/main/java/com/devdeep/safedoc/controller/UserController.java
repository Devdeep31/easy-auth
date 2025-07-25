package com.devdeep.safedoc.controller;

import com.devdeep.safedoc.entity.User;
import com.devdeep.safedoc.service.UserServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private UserServiceImpl userService;

    UserController(UserServiceImpl userService){
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<User> addUser(@RequestBody User user){
        return ResponseEntity.ok(this.userService.createUser(user));
    }

    @GetMapping("/all")
    public ResponseEntity<List<User>> getAllUsers(){
        return ResponseEntity.ok(this.userService.getAllUsers());
    }
}
