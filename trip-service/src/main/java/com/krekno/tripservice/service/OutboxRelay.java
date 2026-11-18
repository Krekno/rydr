package com.krekno.tripservice.service;

import com.krekno.tripservice.entity.OutboxEvent;
import com.krekno.tripservice.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1000)
    public void publishPendingEvents() {

        var events = outboxRepository.findTop100ByStatusOrderByCreatedAtAsc("PENDING");

        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(
                        event.getTopic(),
                        event.getAggregateId().toString(),
                        event.getPayload()
                ).get();

                event.setStatus("PUBLISHED");
                event.setPublishedAt(java.time.LocalDateTime.now());

                outboxRepository.save(event);

            } catch (Exception e) {
                System.err.println(
                        "Failed to publish outbox event: " + event.getId()
                );
            }
        }
    }
}