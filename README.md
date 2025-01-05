# Context
> I will use Open Feign for communication between services in an application that handles fraud detection for customers.
> The services are:
>- eureka-server
> - customer
> - fraud
> - notification


> When a customer is created they will go through fraud detection by the fraud service. If they are not a fraudster, a notification saying: *'Welcome to our organisation...'* will be sent to them.

## Steps
> Eureka server has already been set up in the previous step (check ***Service Discovery with Eureka*** note).
> All 3 services (customer, fraud, notification) should be registered as eureka clients.

- The following dependency needs to be added to the global dependency file of the project
```xml
<dependency>  
	<groupId>org.springframework.cloud</groupId>  
	<artifactId>spring-cloud-starter-openfeign</artifactId>  
</dependency>
```

> Create Feign Client interfaces and DTOs to transfer data through REST.
- Create a module named `clients` inside the project.
- create 2 packages named **fraud** and **notification** inside this module.
##### Inside fraud client (not the service !)
```java
package org.issam.clients.fraud;  
  
import org.springframework.cloud.openfeign.FeignClient;  
import org.springframework.web.bind.annotation.GetMapping;  
import org.springframework.web.bind.annotation.PathVariable;  
  
@FeignClient(value = "fraud")  
public interface FraudClient {  
// The targetted method should match the declaration in the fraud controller, but this one is abstract.
// The path provided should be the full path for the target method including parent paths declared in RequestMapping (for the controller class).
	@GetMapping(path = "api/v1/fraud-check/{customerId}")  
	FraudCheckResponse isFraudster(@PathVariable("customerId") Integer customerId);  
}

// DTO
package org.issam.clients.fraud;  
  
public record FraudCheckResponse(Boolean isFraudster) { }
```

##### Inside Notification client (not the service !)
```java
package org.issam.clients.notification;  
  
import org.springframework.cloud.openfeign.FeignClient;  
import org.springframework.web.bind.annotation.PostMapping;  
import org.springframework.web.bind.annotation.RequestBody;  
  
@FeignClient(value = "notification")  
public interface NotificationClient {  
// The targeted method should match the declaration in the notification controller, but this one is abstract.
// The path provided should be the full path for the target method including parent paths declared in RequestMapping (for the controller class).
	@PostMapping(path = "api/v1/notifications")
	void send(@RequestBody NotificationRequest req);
}

// DTO
package org.issam.clients.notification;  

public record NotificationRequest(Integer toCustomerId, String toCustomerEmail, String message) { }
```

> The service that will be using these clients is `customer`
- Add the following dependency to customer service
```xml
<dependency>  
	<groupId>com.netflix.eureka</groupId>  
	<artifactId>eureka-client</artifactId>  
	<version>2.0.4</version>  
</dependency>
```

>customer, fraud, and notification services depend on DTOs from clients service module.
- add this dependency to the 3 services
```xml
<dependency>  
	<groupId>org.issam</groupId>  
	<artifactId>clients</artifactId>  
	<version>1.0-SNAPSHOT</version>  
	<scope>compile</scope>  
</dependency>
```
___
### Fraud
**Fraud Controller**
```java
@RestController  
@RequestMapping("api/v1/fraud-check")  
@AllArgsConstructor  
@Slf4j  
public class FraudController {  
	private final FraudCheckService fraudCheckService;  
	  
	@GetMapping(path = "{customerId}")  // This is the method targeted by fraud feign client
	public FraudCheckResponse isFraudster(@PathVariable("customerId") Integer customerId) {  
		boolean fraudulentCustomer = fraudCheckService.isFraudulentCustomer(customerId);  
		log.info("fraud check request for customer {}", customerId);  
		return new FraudCheckResponse(fraudulentCustomer);  
	}  
}
```

**Fraud Service**
```java
@Service  
@AllArgsConstructor  
public class FraudCheckService {  
	private final FraudCheckHistoryRepository fraudCheckHistoryRepository;  
	  
	public boolean isFraudulentCustomer(Integer customerId){  
		fraudCheckHistoryRepository.save(  
				FraudCheckHistory.builder()  
					.customerId(customerId)  
					.isFraudster(false)  
					.createdAt(LocalDateTime.now())  
					.build()  
		);  
		return false;  
	}  
}
```
___
### Notification
**Notification Controller**
```java
@RestController @AllArgsConstructor  
@RequestMapping("api/v1/notifications")  
@Slf4j  
public class NotificationController {  
	NotificationService notificationService;  
	  
	@PostMapping  // This is the method targeted by notification feign client
	public void send(@RequestBody NotificationRequest req){  
		log.info("New notification... {}", req);  
		notificationService.send(req);  
	}  
	  
	@GetMapping("/all")  
	public ResponseEntity<List<Notification>> getAllNotifications(){  
		log.info("Getting all notifications {}...", notificationService.getAllNotifications().getBody());  
		return notificationService.getAllNotifications();  
	}  
}
```

**Notification Service**
```java
@Service @AllArgsConstructor  
public class NotificationService {  
	NotificationRepository notificationRepository;  
	  
	public void send(NotificationRequest req){  
		notificationRepository.save(
			Notification.builder()  
				.sender("IssamCode")
				.sentAt(LocalDateTime.now())  
				.targetCustomerId(req.toCustomerId())  // comes from feign
				.targetCustomerEmail(req.toCustomerEmail())  // comes from feign
				.message(req.message())  // comes from feign
				.build()  
		);  
	}  
	  
	public ResponseEntity<List<Notification>> getAllNotifications() {  
		return ResponseEntity.ok(notificationRepository.findAll());  
	}  
}
```
___
### Customer
> Annotate the main application of the customer service with `@EnableFeignClients(basePackages = "org.issam.clients")`

**Customer Controller**
```java
@Slf4j  
@RestController  
@RequestMapping("api/v1/customers")  
@AllArgsConstructor  
public class CustomerController {  
	private final CustomerService customerService;  

	@PostMapping  
	public void registerCustomer(@RequestBody CustomerRegistrationRequest customerRegistrationRequest){  
		log.info("New customer has been registered {}", customerRegistrationRequest);  
		customerService.registerCustomer(customerRegistrationRequest);  
	}  
	  
	@GetMapping("/all")  
	public ResponseEntity<List<Customer>> getAll(){  
		return customerService.getAll();  
	}  
}
```

**Customer Service**
> Inject feign clients in the `CustomerService`
```java
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
```
___
