package com.krekno.userservice.service;

import com.krekno.userservice.dto.event.DriverStatusEvent;
import com.krekno.userservice.entity.Driver;
import com.krekno.userservice.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KafkaListenerService {

    private final DriverRepository driverRepository;

    @Transactional
    @KafkaListener(topics = "driver-status", groupId = "DriverStatus")
    public void updateDriverStatus(DriverStatusEvent driverStatusEvent) {
        Driver driver = driverRepository.findByUserId(driverStatusEvent.driverId()).orElseThrow();
        driver.setStatus(driverStatusEvent.status());
        driverRepository.save(driver);
    }

}
