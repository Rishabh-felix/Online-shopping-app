package com.kunaldo.inventoryservice.controller;

import com.kunaldo.inventoryservice.dto.InventoryResponse;
import com.kunaldo.inventoryservice.model.Inventory;
import com.kunaldo.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode) {
        // List of Inventory objects to a different Type
        return inventoryService.isInStock(skuCode);
    }
}
