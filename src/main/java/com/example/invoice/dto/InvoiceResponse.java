package com.example.invoice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record InvoiceResponse(
    @JsonProperty("vendor_name") String vendorName,
    @JsonProperty("invoice_number") String invoiceNumber,
    @JsonProperty("invoice_date") String invoiceDate,
    String currency,
    @JsonProperty("line_items") List<LineItem> lineItems,
    @JsonProperty("tax_amount") double taxAmount,
    @JsonProperty("grand_total") double grandTotal
) {}