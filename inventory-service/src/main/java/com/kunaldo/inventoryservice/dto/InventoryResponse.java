package com.kunaldo.inventoryservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryResponse {
    // holds the info on the skuCode and whether it is stock or not
    private String skuCode;
    private boolean isInStock;
}
