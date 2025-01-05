package org.issam.customer;

public record CustomerRegistrationRequest(
        String firstname,
        String lastname,
        String email
) {

}
