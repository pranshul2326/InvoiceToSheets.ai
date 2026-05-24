package com.example.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LineItem(
    String sku,
    String description,
    int quantity,
    @JsonProperty("unit_price") double unitPrice,
    @JsonProperty("total_amount") double totalAmount
) {}