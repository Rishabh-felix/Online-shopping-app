package com.kunaldo.inventoryservice.service;

import com.kunaldo.inventoryservice.dto.InventoryResponse;
import com.kunaldo.inventoryservice.model.Inventory;
import com.kunaldo.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    @Transactional(readOnly = true)
    @SneakyThrows
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode) {

        log.info("Wait Started");
        // Stoping the execution for 10s
        // Simulating the slow behaviour for testing
        Thread.sleep(1000);
        log.info("Wait Ended");

        // List of Inventory objects to a different Type
        // Mapping the inventory object to inventoryResponse object as we only need this info
        return inventoryRepository.findBySkuCodeIn(skuCode).stream()
                .map(inventory ->
                        InventoryResponse.builder()
                                .skuCode(inventory.getSkuCode())
                                .isInStock(inventory.getQuantity() > 0)
                                .build()
                ).toList();
    }
}
