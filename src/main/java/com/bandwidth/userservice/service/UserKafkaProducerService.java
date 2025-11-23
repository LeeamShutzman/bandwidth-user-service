package com.bandwidth.userservice.service;

import com.bandwidth.userservice.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserKafkaProducerService {
    public void sendUserCreatedEvent(User savedUser) {
    }

    public void sendUserDeletedEvent(Long userId) {
    }
}
