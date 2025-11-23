package com.bandwidth.userservice.controller;

import com.bandwidth.userservice.model.User;
import com.bandwidth.userservice.service.DuplicateUserException;
import com.bandwidth.userservice.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing user registration and deletion.
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /****************************************************************************************/

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User savedUser = userService.createUser(user);
        // Returns the created user object with a 201 Created status
        return new ResponseEntity<>(savedUser, HttpStatus.CREATED);
    }

    /****************************************************************************************/

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean wasDeleted = userService.deleteUser(id);

        if (wasDeleted) {
            // Return 204 No Content for a successful deletion (standard REST practice)
            return ResponseEntity.noContent().build();
        } else {
            // Return 404 Not Found if the user ID doesn't exist
            return ResponseEntity.notFound().build();
        }
    }

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUserException(DuplicateUserException ex) {
        // HTTP 409 is the status code for a resource conflict (e.g., duplicate email)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }
}