document.addEventListener("DOMContentLoaded", function () {

    const registerForm = document.getElementById("registerForm");
    const successOverlay = document.getElementById("successOverlay");
    const closeSuccessBtn = document.getElementById("closeSuccessBtn");
    const registerBtn = document.getElementById("registerBtn");


    if (!registerForm) {
        console.error("Registration form not found.");
        return;
    }


    registerForm.addEventListener("submit", async function (e) {

        e.preventDefault();


        // Check HTML validation
        if (!registerForm.checkValidity()) {
            registerForm.reportValidity();
            return;
        }


        // Get form values
        const name = document.getElementById("fullName").value.trim();
        const email = document.getElementById("email").value.trim();
        const phone = document.getElementById("phone").value.trim();
        const password = document.getElementById("password").value;


        // Data that will be sent to Spring Boot
        const customerData = {
            name: name,
            email: email,
            phone: phone,
            password: password
        };


        try {

            // Disable button
            registerBtn.disabled = true;
            registerBtn.textContent = "Registering...";


            console.log("Sending customer data:", {
                name,
                email,
                phone
            });


            // Send data to Spring Boot
            const response = await fetch(
                "http://localhost:8080/api/customers/register",
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify(customerData)
                }
            );


            // Get response
            const responseText = await response.text();


            if (!response.ok) {

                throw new Error(
                    responseText || "Registration failed"
                );

            }


            // Convert response to JSON if possible
            let savedCustomer;

            try {
                savedCustomer = JSON.parse(responseText);
            } catch {
                savedCustomer = responseText;
            }


            console.log("Customer saved successfully:", savedCustomer);


            // Show success popup
            successOverlay.classList.add("show");


            // Clear form
            registerForm.reset();


        } catch (error) {

            console.error("Registration error:", error);


            alert(
                "Registration failed!\n\n" +
                error.message
            );

        } finally {

            // Enable button
            registerBtn.disabled = false;
            registerBtn.textContent = "Register";

        }

    });


    // Continue to Login
    if (closeSuccessBtn) {

        closeSuccessBtn.addEventListener("click", function () {

            window.location.href = "login.html";

        });

    }

});