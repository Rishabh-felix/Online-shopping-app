package com.kunaldo.inventoryservice;

import com.kunaldo.inventoryservice.model.Inventory;
import com.kunaldo.inventoryservice.repository.InventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class InventoryServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(InventoryServiceApplication.class, args);
	}

	// As initially the inventory database is empty, so it will return NULL
	@Bean
	public CommandLineRunner loadData(InventoryRepository inventoryRepository) {
		return args -> {
			Inventory inventory = new Inventory();
			inventory.setSkuCode("iphone_13");
			inventory.setQuantity(100);

			Inventory inventory1 = new Inventory();
			inventory.setSkuCode("iphone_14");
			inventory.setQuantity(0);

			inventoryRepository.save(inventory);
			inventoryRepository.save(inventory1);

		};
	}
}
