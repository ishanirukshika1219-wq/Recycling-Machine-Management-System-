package com.example.myproject.service;

import com.example.myproject.model.Customer;
import com.example.myproject.repository.CustomerRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class NotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(NotificationService.class);

    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final SmsService smsService;

    public NotificationService(
            CustomerRepository customerRepository,
            EmailService emailService,
            SmsService smsService) {

        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.smsService = smsService;
    }

    // =========================================================
    // TRANSACTION CREATED - PENDING EMAIL + SMS
    // =========================================================

    @Async
    public void sendTransactionNotifications(
            Integer customerId,
            String category,
            double weightKg,
            double amount,
            int points,
            String status) {

        log.info(
                "Starting transaction notification. Customer ID: {}, Status: {}",
                customerId,
                status
        );

        Customer customer =
                customerRepository.findById(customerId).orElse(null);

        if (customer == null) {
            log.warn("Customer not found for transaction notification: {}", customerId);
            return;
        }

        String name = firstName(customer.getName());

        String safeCategory =
                category == null || category.isBlank()
                        ? "Recyclables"
                        : category.trim();

        String safeStatus =
                status == null || status.isBlank()
                        ? "Pending"
                        : status.trim();

        // EMAIL
        boolean emailOn =
                !Boolean.FALSE.equals(customer.getEmailNotifications());

        if (emailOn &&
                customer.getEmail() != null &&
                !customer.getEmail().isBlank()) {

            try {

                emailService.sendHtmlEmail(
                        customer.getEmail(),
                        "Recycling Transaction " + safeStatus + " - SRMMS",
                        buildTransactionEmail(
                                name,
                                safeCategory,
                                weightKg,
                                amount,
                                points,
                                safeStatus
                        )
                );

                log.info(
                        "Transaction email sent successfully to {}",
                        customer.getEmail()
                );

            } catch (Exception e) {

                log.error(
                        "Could not send transaction email to {}",
                        customer.getEmail(),
                        e
                );
            }

        } else {

            log.info(
                    "Pending email skipped. Email notifications disabled or email missing. Customer ID: {}",
                    customerId
            );
        }

        // SMS
        boolean smsOn =
                !Boolean.FALSE.equals(customer.getSmsAlerts());

        if (smsOn &&
                customer.getPhone() != null &&
                !customer.getPhone().isBlank()) {

            try {

                String sms = String.format(
                        Locale.US,
                        "SRMMS: Hi %s, your recycling transaction is %s. %s %.2fkg, Rs.%.2f, %d pts. Thank you!",
                        name,
                        safeStatus,
                        safeCategory,
                        weightKg,
                        amount,
                        points
                );

                smsService.send(customer.getPhone(), sms);

            } catch (Exception e) {

                log.error(
                        "Could not send transaction SMS to {}",
                        customer.getPhone(),
                        e
                );
            }
        }
    }

    // =========================================================
    // STAFF VERIFY / REJECT EMAIL
    // =========================================================

    @Async
    public void sendDepositDecisionEmail(
            Integer customerId,
            Integer transactionId,
            String transactionDate,
            String category,
            double weightKg,
            double amount,
            int points,
            String status,
            String rejectionReason) {

        log.info(
                "Deposit decision email method started. Transaction ID: {}, Customer ID: {}, Status: {}",
                transactionId,
                customerId,
                status
        );

        try {

            Customer customer =
                    customerRepository.findById(customerId).orElse(null);

            if (customer == null) {

                log.warn(
                        "Customer not found. Customer ID: {}, Transaction ID: {}",
                        customerId,
                        transactionId
                );

                return;
            }

            log.info(
                    "Customer loaded. Customer ID: {}, Email: {}, Email notifications: {}",
                    customerId,
                    customer.getEmail(),
                    customer.getEmailNotifications()
            );

            // Check email notification preference
            boolean emailOn =
                    !Boolean.FALSE.equals(customer.getEmailNotifications());

            if (!emailOn) {

                log.warn(
                        "Decision email not sent because email notifications are disabled. Customer ID: {}",
                        customerId
                );

                return;
            }

            if (customer.getEmail() == null ||
                    customer.getEmail().isBlank()) {

                log.warn(
                        "Decision email not sent because customer email is missing. Customer ID: {}",
                        customerId
                );

                return;
            }

            String name = firstName(customer.getName());

            boolean verified =
                    "Completed".equalsIgnoreCase(status);

            String subject;
            String heading;
            String message;

            if (verified) {

                subject =
                        "Your Recycling Deposit Has Been Verified - SRMMS";

                heading =
                        "Deposit Verified Successfully";

                message =
                        "<p>Your recycling deposit has been successfully verified by our staff.</p>";

            } else if ("Rejected".equalsIgnoreCase(status)) {

                subject =
                        "Your Recycling Deposit Has Been Rejected - SRMMS";

                heading =
                        "Deposit Rejected";

                message =
                        "<p>We regret to inform you that your recycling deposit has been rejected.</p>";

            } else {

                log.warn(
                        "Unknown deposit decision status: {}. Transaction ID: {}",
                        status,
                        transactionId
                );

                return;
            }

            // Transaction details
            String rows =
                    row("Transaction ID", String.valueOf(transactionId))
                            +
                            row(
                                    "Date",
                                    transactionDate == null ? "N/A" : transactionDate
                            )
                            +
                            row(
                                    "Category",
                                    category == null ? "Recyclables" : category
                            )
                            +
                            row(
                                    "Weight",
                                    String.format(Locale.US, "%.2f kg", weightKg)
                            )
                            +
                            row(
                                    "Deposit Value",
                                    String.format(Locale.US, "Rs. %,.2f", amount)
                            )
                            +
                            row("Status", status);

            // Show points for verified deposits
            if (verified) {

                rows += row(
                        "Reward Points",
                        String.valueOf(points)
                );
            }

            // Show rejection reason if available
            if (!verified &&
                    rejectionReason != null &&
                    !rejectionReason.isBlank()) {

                rows += row(
                        "Rejection Reason",
                        rejectionReason
                );
            }

            String body =
                    "<p>Hi "
                            + HtmlUtils.htmlEscape(name)
                            + ",</p>"
                            + message
                            + "<table style=\"width:100%;border-collapse:collapse;\">"
                            + rows
                            + "</table>"
                            + "<p>Thank you for using SRMMS.</p>";

            // Send email
            log.info(
                    "Sending {} decision email to {} for transaction ID: {}",
                    status,
                    customer.getEmail(),
                    transactionId
            );

            emailService.sendHtmlEmail(
                    customer.getEmail(),
                    subject,
                    wrap(heading, body)
            );

            log.info(
                    "Deposit decision email sent successfully to {}. Transaction ID: {}",
                    customer.getEmail(),
                    transactionId
            );

        } catch (Exception e) {

            log.error(
                    "FAILED to send deposit decision email. Transaction ID: {}, Customer ID: {}",
                    transactionId,
                    customerId,
                    e
            );
        }
    }

    // =========================================================
    // PROMOTIONAL EMAIL
    // =========================================================

    @Async
    public void sendPromotionalEmail(
            String subject,
            String message) {

        List<Customer> customers = customerRepository.findAll();

        for (Customer customer : customers) {

            boolean optedIn =
                    Boolean.TRUE.equals(customer.getPromotionalOffers());

            if (!optedIn ||
                    customer.getEmail() == null ||
                    customer.getEmail().isBlank()) {

                continue;
            }

            try {

                emailService.sendHtmlEmail(
                        customer.getEmail(),
                        subject,
                        buildPromotionEmail(
                                firstName(customer.getName()),
                                subject,
                                message
                        )
                );

            } catch (Exception e) {

                log.error(
                        "Could not send promotion to {}",
                        customer.getEmail(),
                        e
                );
            }
        }
    }

    // =========================================================
    // TRANSACTION EMAIL TEMPLATE
    // =========================================================

    private String buildTransactionEmail(
            String name,
            String category,
            double weightKg,
            double amount,
            int points,
            String status) {

        String rows =
                row(
                        "Date",
                        LocalDateTime.now().format(DATE_TIME)
                )
                        +
                        row("Category", category)
                        +
                        row(
                                "Weight",
                                String.format(Locale.US, "%.2f kg", weightKg)
                        )
                        +
                        row(
                                "Amount",
                                String.format(Locale.US, "Rs. %,.2f", amount)
                        )
                        +
                        row("Reward Points", String.valueOf(points))
                        +
                        row("Status", status);

        return wrap(
                "Transaction " + HtmlUtils.htmlEscape(status),

                "<p>Hi "
                        + HtmlUtils.htmlEscape(name)
                        + ",</p>"
                        + "<p>Your recycling transaction has been recorded:</p>"
                        + "<table style=\"width:100%;border-collapse:collapse;\">"
                        + rows
                        + "</table>"
                        + "<p>Thank you for recycling with SRMMS.</p>"
        );
    }

    // =========================================================
    // PROMOTIONAL EMAIL TEMPLATE
    // =========================================================

    private String buildPromotionEmail(
            String name,
            String subject,
            String message) {

        return wrap(
                HtmlUtils.htmlEscape(subject),

                "<p>Hi "
                        + HtmlUtils.htmlEscape(name)
                        + ",</p>"
                        + "<p>"
                        + HtmlUtils.htmlEscape(message).replace("\n", "<br>")
                        + "</p>"
                        + "<p style=\"color:#6b7280;font-size:12px;\">"
                        + "You receive this because Promotional Offers is "
                        + "turned on in your SRMMS settings. You can turn it off any time."
                        + "</p>"
        );
    }

    // =========================================================
    // EMAIL TABLE ROW
    // =========================================================

    private String row(
            String label,
            String value) {

        return "<tr>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #e5e7eb;color:#6b7280;\">"
                + HtmlUtils.htmlEscape(label)
                + "</td>"
                + "<td style=\"padding:8px 0;border-bottom:1px solid #e5e7eb;text-align:right;font-weight:600;\">"
                + HtmlUtils.htmlEscape(value == null ? "" : value)
                + "</td>"
                + "</tr>";
    }

    // =========================================================
    // EMAIL WRAPPER
    // =========================================================

    private String wrap(
            String heading,
            String bodyHtml) {

        return "<div style=\"font-family:Arial,sans-serif;max-width:520px;margin:auto;"
                + "border:1px solid #e5e7eb;border-radius:12px;overflow:hidden;\">"

                + "<div style=\"background:#2e9e5b;color:#fff;padding:20px 24px;\">"
                + "<h2 style=\"margin:0;\">♻ SRMMS</h2>"
                + "</div>"

                + "<div style=\"padding:24px;color:#374151;line-height:1.6;\">"
                + "<h3 style=\"margin-top:0;\">"
                + heading
                + "</h3>"
                + bodyHtml
                + "</div>"

                + "</div>";
    }

    // =========================================================
    // GET CUSTOMER FIRST NAME
    // =========================================================

    private String firstName(String fullName) {

        if (fullName == null || fullName.isBlank()) {
            return "Customer";
        }

        return fullName.trim().split("\\s+")[0];
    }
}