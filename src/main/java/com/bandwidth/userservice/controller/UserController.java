package com.bandwidth.userservice.controller;

import com.bandwidth.userservice.dto.UserCreateRequestDTO;
import com.bandwidth.userservice.dto.UserCredentialDTO;
import com.bandwidth.userservice.dto.UserResponseDTO;
import com.bandwidth.userservice.dto.UserUpdateRequestDTO;
import com.bandwidth.userservice.model.User;
import com.bandwidth.userservice.service.DuplicateUserException;
import com.bandwidth.userservice.service.UserService;
import org.springframework.beans.factory.annotation.Value;
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
    public ResponseEntity<UserResponseDTO> createUser(@RequestBody UserCreateRequestDTO requestDTO) {
        UserResponseDTO responseDTO = userService.createUser(requestDTO);
        // Returns the created user object with a 201 Created status
        return new ResponseEntity<>(responseDTO, HttpStatus.CREATED);
    }

    /****************************************************************************************/

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> getUser(@PathVariable Long id){
        UserResponseDTO responseDTO = userService.getUser(id);
        return ResponseEntity.ok(responseDTO);
    }

    /****************************************************************************************/

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequestDTO requestDTO){
        UserResponseDTO responseDTO = userService.updateUser(id, requestDTO);
        return ResponseEntity.ok(responseDTO);
    }

    /****************************************************************************************/

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        boolean wasDeleted = userService.deleteUser(id);

        if (wasDeleted) {
            // Return 204 No Content for a successful deletion
            return ResponseEntity.noContent().build();
        } else {
            // Return 404 Not Found if the user ID doesn't exist
            return ResponseEntity.notFound().build();
        }
    }

    /****************************************************************************************/

    /** Utils **/

    @ExceptionHandler(DuplicateUserException.class)
    public ResponseEntity<String> handleDuplicateUserException(DuplicateUserException ex) {
        // HTTP 409 is the status code for a resource conflict (e.g., duplicate username)
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @GetMapping("/internal/credentials")
    public ResponseEntity<UserCredentialDTO> getCredentialsByUsername(@RequestHeader("X-Internal-Secret") String token, @RequestParam String username) {
        UserCredentialDTO credentials = userService.getCredentialsByUsername(username);
        if (credentials == null) {
            // Return 404 if user not found, Spring Security will handle the failure
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(credentials);
    }


}