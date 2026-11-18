package com.krekno.tripservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krekno.tripservice.entity.OutboxEvent;
import com.krekno.tripservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveEvent(
            UUID aggregateId,
            String eventType,
            String topic,
            Object event
    ) {
        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .id(UUID.randomUUID())
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .topic(topic)
                    .payload(payload)
                    .createdAt(LocalDateTime.now())
                    .status("PENDING")
                    .build();

            outboxRepository.save(outboxEvent);

        } catch (Exception e) {
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }
}