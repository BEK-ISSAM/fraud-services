package org.issam.customer;

import lombok.AllArgsConstructor;
import org.issam.clients.fraud.FraudCheckResponse;
import org.issam.clients.fraud.FraudClient;
import org.issam.clients.notification.NotificationClient;
import org.issam.clients.notification.NotificationRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
    private final NotificationClient notificationClient;

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstname(request.firstname())
                .lastname(request.lastname())
                .email(request.email())
                .build();
        // TODO: check if email is valid
        // TODO: check if email is not taken
        customerRepository.saveAndFlush(customer);

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if(fraudCheckResponse.isFraudster()){
            throw new IllegalStateException("Fraudster");
        }

        // TODO: make it async. (i.e. add it to a queue)
        notificationClient.send(
            new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, Welcome to IssamCode.", customer.getFirstname())
            )
        );
    }

    public ResponseEntity<List<Customer>> getAll() {
        return ResponseEntity.ok(customerRepository.findAll());
    }
}
