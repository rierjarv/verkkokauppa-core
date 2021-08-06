package fi.hel.verkkokauppa.order.api.data;

import lombok.*;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDto {

    private String orderId;
    private String namespace;
    private String user;
    private String createdAt;
    private String status;
    private String type;
    private String priceNet;
    private String priceVat;
    private String priceTotal;


    @NotBlank(message = "firstname required")
    private String customerFirstName;

    @NotBlank(message = "lastname required")
    private String customerLastName;

    @Email(message = "email must be in correct format")
    @NotBlank(message = "email required")
    private String customerEmail;

    private String customerPhone;

}