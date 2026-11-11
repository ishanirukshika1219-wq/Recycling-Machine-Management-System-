package com.example.myproject.controller;

import com.example.myproject.model.Customer;
import com.example.myproject.repository.CustomerRepository;
import com.example.myproject.service.EmailService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/customers")
@CrossOrigin(origins = "*")
public class CustomerController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final CustomerRepository customerRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailService emailService;                       // <-- NEW

    public CustomerController(CustomerRepository customerRepository,
                              EmailService emailService) {        // <-- NEW
        this.customerRepository = customerRepository;
        this.emailService = emailService;                          // <-- NEW
        this.passwordEncoder = new BCryptPasswordEncoder();
    }


    // =========================================================
    // CUSTOMER REGISTER (+ AUTO WELCOME EMAIL)                  <-- NEW
    // =========================================================

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody Map<String, String> request) {

        String name = request.get("name");
        String email = request.get("email");
        String phone = request.get("phone");
        String password = request.get("password");


        // =====================================================
        // CHECK REQUIRED FIELDS
        // =====================================================

        if (name == null ||
                email == null ||
                phone == null ||
                password == null ||
                name.trim().isEmpty() ||
                email.trim().isEmpty() ||
                phone.trim().isEmpty() ||
                password.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Name, email, phone and password are required."
                    ));
        }


        // =====================================================
        // VALIDATE VALUES
        // =====================================================

        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Enter a valid email address."
                    ));
        }

        if (password.length() < 6) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Password must contain at least 6 characters."
                    ));
        }

        if (name.trim().length() > 100 || phone.trim().length() > 15) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Name or phone number is too long."
                    ));
        }


        // =====================================================
        // CHECK DUPLICATE EMAIL
        // =====================================================

        if (customerRepository.findByEmail(email.trim()).isPresent()) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "This email is already registered."
                    ));
        }


        // =====================================================
        // SAVE CUSTOMER
        // =====================================================

        Customer customer = new Customer();

        customer.setName(name.trim());
        customer.setEmail(email.trim());
        customer.setPhone(phone.trim());
        customer.setPassword(passwordEncoder.encode(password));
        customer.setRegisteredDate(LocalDateTime.now());
        customer.setStatus("Active");

        Customer savedCustomer =
                customerRepository.saveAndFlush(customer);


        // =====================================================
        // AUTO EMAIL TO THE CUSTOMER'S OWN EMAIL ADDRESS
        // (runs in the background, never breaks registration)
        // =====================================================

        emailService.sendRegistrationEmailAsync(
                savedCustomer.getEmail(),
                savedCustomer.getName()
        );


        HashMap<String, Object> response = new HashMap<>();

        response.put("message", "Registration successful.");
        response.put("customerId", savedCustomer.getCustomerId());
        response.put("name", savedCustomer.getName());
        response.put("email", savedCustomer.getEmail());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // =========================================================
    // CUSTOMER LOGIN
    // =========================================================

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        if (email == null ||
                password == null ||
                email.trim().isEmpty() ||
                password.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Email and password are required."
                    ));
        }


        Optional<Customer> customerOptional =
                customerRepository.findByEmail(
                        email.trim()
                );


        if (customerOptional.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid email or password."
                    ));
        }


        Customer customer =
                customerOptional.get();


        boolean passwordCorrect = false;


        if (customer.getPassword() != null) {

            try {

                passwordCorrect =
                        passwordEncoder.matches(
                                password,
                                customer.getPassword()
                        );

            } catch (Exception e) {

                passwordCorrect = false;

            }


            // =================================================
            // OLD PLAINTEXT PASSWORD SUPPORT
            // =================================================

            if (!passwordCorrect &&
                    password.equals(
                            customer.getPassword())) {

                passwordCorrect = true;


                customer.setPassword(
                        passwordEncoder.encode(password)
                );


                customerRepository.save(customer);

            }

        }


        if (!passwordCorrect) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid email or password."
                    ));
        }


        // =====================================================
        // LOGIN RESPONSE
        // =====================================================

        HashMap<String, Object> loginResponse =
                new HashMap<>();


        loginResponse.put(
                "customerId",
                customer.getCustomerId()
        );

        loginResponse.put(
                "name",
                customer.getName()
        );

        loginResponse.put(
                "email",
                customer.getEmail()
        );

        loginResponse.put(
                "phone",
                customer.getPhone()
        );

        loginResponse.put(
                "registeredDate",
                customer.getRegisteredDate()
        );

        loginResponse.put(
                "address",
                customer.getAddress()
        );

        loginResponse.put(
                "city",
                customer.getCity()
        );

        loginResponse.put(
                "postalCode",
                customer.getPostalCode()
        );

        loginResponse.put(
                "profilePhoto",
                customer.getProfilePhoto()
        );


        return ResponseEntity.ok(
                loginResponse
        );
    }



    // =========================================================
    // GET CUSTOMER BY ID
    // =========================================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(
            @PathVariable Integer id) {

        Optional<Customer> customer =
                customerRepository.findById(id);


        if (customer.isPresent()) {

            return ResponseEntity.ok(
                    customer.get()
            );

        }


        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "message",
                        "Customer not found."
                ));
    }



    // =========================================================
    // UPDATE CUSTOMER PROFILE
    // =========================================================

    @PutMapping("/profile/{id}")
    public ResponseEntity<?> updateProfile(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {


        // =====================================================
        // FIND CUSTOMER
        // =====================================================

        Optional<Customer> customerOptional =
                customerRepository.findById(id);


        if (customerOptional.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Customer not found."
                    ));
        }


        Customer customer =
                customerOptional.get();



        // =====================================================
        // GET REQUEST DATA
        // =====================================================

        String name =
                request.get("name");

        String email =
                request.get("email");

        String phone =
                request.get("phone");

        String address =
                request.get("address");

        String city =
                request.get("city");

        String postalCode =
                request.get("postalCode");

        String profilePhoto =
                request.get("profilePhoto");



        // =====================================================
        // REQUIRED FIELDS
        // =====================================================

        if (name == null ||
                email == null ||
                phone == null ||
                name.trim().isEmpty() ||
                email.trim().isEmpty() ||
                phone.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Name, email and phone are required."
                    ));
        }



        // =====================================================
        // CHECK DUPLICATE EMAIL
        // =====================================================

        Optional<Customer> existingCustomer =
                customerRepository.findByEmail(
                        email.trim()
                );


        if (existingCustomer.isPresent() &&
                !existingCustomer
                        .get()
                        .getCustomerId()
                        .equals(id)) {

            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            "This email is already used by another customer."
                    ));
        }



        // =====================================================
        // UPDATE BASIC INFORMATION
        // =====================================================

        customer.setName(
                name.trim()
        );


        customer.setEmail(
                email.trim()
        );


        customer.setPhone(
                phone.trim()
        );


        customer.setAddress(
                address != null
                        ? address.trim()
                        : ""
        );


        customer.setCity(
                city != null
                        ? city.trim()
                        : ""
        );


        customer.setPostalCode(
                postalCode != null
                        ? postalCode.trim()
                        : ""
        );



        // =====================================================
        // UPDATE PROFILE PHOTO
        // =====================================================

        if (profilePhoto != null &&
                !profilePhoto.trim().isEmpty()) {

            System.out.println(
                    "Incoming profile photo length: "
                            + profilePhoto.length()
            );


            customer.setProfilePhoto(
                    profilePhoto
            );

        }



        // =====================================================
        // SAVE + FLUSH TO DATABASE
        // =====================================================

        Customer savedCustomer =
                customerRepository.saveAndFlush(
                        customer
                );



        // =====================================================
        // READ CUSTOMER AGAIN FROM DATABASE
        // =====================================================

        Optional<Customer> refreshedCustomer =
                customerRepository.findById(id);


        if (refreshedCustomer.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Customer could not be loaded after update."
                    ));
        }


        Customer updatedCustomer =
                refreshedCustomer.get();



        // =====================================================
        // DEBUG PHOTO SAVE
        // =====================================================

        System.out.println(
                "Saved profile photo length: "
                        + (
                        updatedCustomer.getProfilePhoto() == null
                                ? 0
                                : updatedCustomer.getProfilePhoto().length()
                )
        );



        // =====================================================
        // RESPONSE
        // =====================================================

        HashMap<String, Object> response =
                new HashMap<>();


        response.put(
                "message",
                "Profile updated successfully."
        );


        response.put(
                "customerId",
                updatedCustomer.getCustomerId()
        );


        response.put(
                "name",
                updatedCustomer.getName()
        );


        response.put(
                "email",
                updatedCustomer.getEmail()
        );


        response.put(
                "phone",
                updatedCustomer.getPhone()
        );


        response.put(
                "address",
                updatedCustomer.getAddress()
        );


        response.put(
                "city",
                updatedCustomer.getCity()
        );


        response.put(
                "postalCode",
                updatedCustomer.getPostalCode()
        );


        response.put(
                "profilePhoto",
                updatedCustomer.getProfilePhoto()
        );


        return ResponseEntity.ok(
                response
        );
    }



    // =========================================================
    // NOTIFICATION SETTINGS (Settings page toggles)             <-- NEW
    // =========================================================

    @PutMapping("/notification-settings/{id}")
    public ResponseEntity<?> updateNotificationSettings(
            @PathVariable Integer id,
            @RequestBody Map<String, Object> request) {

        Optional<Customer> customerOptional =
                customerRepository.findById(id);


        if (customerOptional.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Customer not found."
                    ));
        }


        Customer customer =
                customerOptional.get();


        if (request.containsKey("emailNotifications")) {
            customer.setEmailNotifications(
                    Boolean.TRUE.equals(request.get("emailNotifications")));
        }

        if (request.containsKey("smsAlerts")) {
            customer.setSmsAlerts(
                    Boolean.TRUE.equals(request.get("smsAlerts")));
        }

        if (request.containsKey("promotionalOffers")) {
            customer.setPromotionalOffers(
                    Boolean.TRUE.equals(request.get("promotionalOffers")));
        }


        Customer saved =
                customerRepository.saveAndFlush(customer);


        HashMap<String, Object> response =
                new HashMap<>();

        response.put("message", "Notification settings saved.");
        response.put("emailNotifications", saved.getEmailNotifications());
        response.put("smsAlerts", saved.getSmsAlerts());
        response.put("promotionalOffers", saved.getPromotionalOffers());

        return ResponseEntity.ok(response);
    }



    // =========================================================
    // CHANGE CUSTOMER PASSWORD
    // =========================================================

    @PutMapping("/change-password/{id}")
    public ResponseEntity<?> changePassword(
            @PathVariable Integer id,
            @RequestBody Map<String, String> request) {

        Optional<Customer> customerOptional =
                customerRepository.findById(id);


        if (customerOptional.isEmpty()) {

            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "message",
                            "Customer not found."
                    ));
        }


        Customer customer =
                customerOptional.get();


        String currentPassword =
                request.get("currentPassword");

        String newPassword =
                request.get("newPassword");


        // =====================================================
        // CHECK REQUIRED FIELDS
        // =====================================================

        if (currentPassword == null ||
                newPassword == null ||
                currentPassword.trim().isEmpty() ||
                newPassword.trim().isEmpty()) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "Current password and new password are required."
                    ));
        }


        // =====================================================
        // CHECK CURRENT PASSWORD
        // =====================================================

        boolean passwordCorrect = false;


        if (customer.getPassword() != null) {

            try {

                passwordCorrect =
                        passwordEncoder.matches(
                                currentPassword,
                                customer.getPassword()
                        );

            } catch (Exception e) {

                passwordCorrect = false;

            }


            // =================================================
            // SUPPORT OLD PLAINTEXT PASSWORD
            // =================================================

            if (!passwordCorrect &&
                    currentPassword.equals(
                            customer.getPassword())) {

                passwordCorrect = true;

            }

        }


        if (!passwordCorrect) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Current password is incorrect."
                    ));
        }


        // =====================================================
        // CHECK NEW PASSWORD
        // =====================================================

        if (newPassword.length() < 6) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "message",
                            "New password must be at least 6 characters."
                    ));
        }


        // =====================================================
        // ENCODE NEW PASSWORD
        // =====================================================

        String encodedPassword =
                passwordEncoder.encode(
                        newPassword
                );


        customer.setPassword(
                encodedPassword
        );


        // =====================================================
        // SAVE TO DATABASE
        // =====================================================

        customerRepository.saveAndFlush(
                customer
        );


        // =====================================================
        // SUCCESS RESPONSE
        // =====================================================

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Password changed successfully."
                )
        );
    }

}