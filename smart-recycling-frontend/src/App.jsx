/* =========================================================
   LOGIN FORM
========================================================= */

function LoginForm({ onLoginSuccess }) {

    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    const [showPassword, setShowPassword] = useState(false);

    const [loading, setLoading] = useState(false);


    const handleLogin = async (e) => {

        e.preventDefault();

        setLoading(true);


        const payload = {

            email: email.trim(),

            password: password

        };


        try {

            const response = await fetch(
                `${API_BASE}/auth/login`,
                {
                    method: "POST",

                    headers: {
                        "Content-Type": "application/json"
                    },

                    body: JSON.stringify(payload)
                }
            );


            // =========================================
            // LOGIN ERROR
            // =========================================

            if (!response.ok) {

                const errorText =
                    await response.text();

                throw new Error(
                    errorText ||
                    "Invalid email or password"
                );

            }


            // =========================================
            // CUSTOMER FROM DATABASE
            // =========================================

            const customerData =
                await response.json();


            console.log(
                "Customer data received from database:",
                customerData
            );


            // =========================================
            // GET CUSTOMER ID
            // =========================================

            const customerId =
                customerData.customerId ??
                customerData.CustomerID ??
                customerData.id;


            // =========================================
            // GET CUSTOMER NAME
            // =========================================

            const customerName =
                customerData.name ??
                customerData.Name ??
                "";


            // =========================================
            // CHECK CUSTOMER ID
            // =========================================

            if (!customerId) {

                console.error(
                    "Customer ID missing from login response:",
                    customerData
                );

                alert(
                    "Login successful, but Customer ID was not received from the server."
                );

                return;
            }


            // =========================================
            // CREATE CUSTOMER OBJECT
            // =========================================

            const loggedInCustomer = {

                customerId: customerId,

                name: customerName,

                email:
                    customerData.email ??
                    customerData.Email ??
                    email.trim(),

                phone:
                    customerData.phone ??
                    customerData.Phone ??
                    "",

                registeredDate:
                    customerData.registeredDate ??
                    customerData.RegisteredDate ??
                    customerData.registered_date ??
                    null

            };


            console.log(
                "Logged-in customer:",
                loggedInCustomer
            );


            // =========================================
            // SAVE CUSTOMER
            // =========================================

            localStorage.setItem(
                "customer",
                JSON.stringify(loggedInCustomer)
            );


            // =========================================
            // CHECK SAVED DATA
            // =========================================

            console.log(
                "Saved customer:",
                JSON.parse(
                    localStorage.getItem("customer")
                )
            );


            // =========================================
            // OPEN CUSTOMER DASHBOARD
            // =========================================

            window.location.href =
                "customer-dashboard.html";


        } catch (error) {

            console.error(
                "Login error:",
                error
            );

            alert(
                error.message ||
                "Login failed."
            );

        } finally {

            setLoading(false);

        }

    };


    return (

        <div className="login-container">

            <div className="login-card">

                <div className="login-header">

                    <h2>
                        Welcome Back!
                    </h2>

                    <p>
                        Login to your account
                    </p>

                </div>


                <form
                    onSubmit={handleLogin}
                    className="login-form"
                >

                    {/* Email */}

                    <div className="form-group">

                        <label htmlFor="email">
                            Email
                        </label>

                        <input
                            type="email"
                            id="email"
                            placeholder="Enter your email"
                            value={email}
                            onChange={(e) =>
                                setEmail(e.target.value)
                            }
                            required
                        />

                    </div>


                    {/* Password */}

                    <div className="form-group">

                        <label htmlFor="password">
                            Password
                        </label>


                        <div
                            className="password-input-wrapper"
                            style={{
                                position: "relative"
                            }}
                        >

                            <input
                                type={
                                    showPassword
                                        ? "text"
                                        : "password"
                                }
                                id="password"
                                placeholder="Enter your password"
                                value={password}
                                onChange={(e) =>
                                    setPassword(
                                        e.target.value
                                    )
                                }
                                required
                            />


                            <span
                                onClick={() =>
                                    setShowPassword(
                                        !showPassword
                                    )
                                }
                                style={{
                                    position: "absolute",
                                    right: "12px",
                                    top: "50%",
                                    transform:
                                        "translateY(-50%)",
                                    cursor: "pointer",
                                    userSelect: "none"
                                }}
                            >

                                {showPassword
                                    ? "🙈"
                                    : "👁"}

                            </span>

                        </div>

                    </div>


                    {/* Login Button */}

                    <button
                        type="submit"
                        disabled={loading}
                        className="login-submit-btn"
                        style={{
                            marginTop: "20px"
                        }}
                    >

                        {loading
                            ? "Logging in..."
                            : "Login"}

                    </button>

                </form>

            </div>

        </div>

    );

}