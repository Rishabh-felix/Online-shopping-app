package com.kunaldo.orderservice.service;

import com.kunaldo.orderservice.dto.InventoryResponse;
import com.kunaldo.orderservice.dto.OrderLineItemsDto;
import com.kunaldo.orderservice.dto.OrderRequest;
import com.kunaldo.orderservice.event.OrderPlacedEvent;
import com.kunaldo.orderservice.model.Order;
import com.kunaldo.orderservice.model.OrderLineItems;
import com.kunaldo.orderservice.repository.OrderRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.mapping.Array;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Transactional = spring framework will automatically take care of the transactions

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;
    private final KafkaTemplate<String,OrderPlacedEvent> kafkaTemplate;
    public String placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        List<OrderLineItems> orderLineItems =  orderRequest.getOrderLineItemsDtoList()
                .stream()
                .map(orderLineItemsDto -> mapToDto(orderLineItemsDto))
                .toList();

        order.setOrderLineItemsList(orderLineItems);

        // getting all the skuCode of the orders
        List<String> skuCodes = order.getOrderLineItemsList().stream().map(orderLineItem -> orderLineItem.getSkuCode()).toList();

        log.info("Calling the inventory service");

        //assigns the span Id for the particular piece of code that is executed inside try
        Span inventoryServiceLookUp=tracer.nextSpan().name("InventoryServiceLookUp");

        try(Tracer.SpanInScope spanInScope = tracer.withSpan(inventoryServiceLookUp.start())) {

            // To read the data comming from Inventory service we have to use bodyToMono function
            // .block() => to specify the call should be synchronous
            // Call Inventory service, and place order if product is in stock
            InventoryResponse[] inventoryResponseArray = webClientBuilder.build().get()
                    .uri("http/inventory-service/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode",skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            boolean allProductsInStock = Arrays.stream(inventoryResponseArray).allMatch(inventoryResponse -> inventoryResponse.isInStock());

            if (allProductsInStock){
                orderRepository.save(order);
                kafkaTemplate.send("notificationTopic",new OrderPlacedEvent(order.getOrderNumber()));
                return "Order Placed Successfully!";
            } else {
                throw new IllegalArgumentException("Product is not in stock, please try again later");
            }

        } finally {
            inventoryServiceLookUp.end();
        }


    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto) {
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }
}
