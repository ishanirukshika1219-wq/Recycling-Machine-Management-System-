/*
 * site-info.js
 * Fills the system name, phone, email and location on any page from the
 * admin settings (database). Add it at the end of a page:
 *
 *     <script src="src/site-info.js"></script>
 *
 * and mark the text that should change with a data-site attribute:
 *
 *     <span data-site="systemName">SRMMS</span>
 *     <span data-site="phone">+94 00 000 0000</span>
 *     <span data-site="email">info@srmms.lk</span>
 *     <span data-site="location">Sabaragamuwa University of Sri Lanka</span>
 *
 * The text written in the HTML stays if the server is off or the admin has not
 * entered that detail yet.
 */
(function () {

  var API_URL = "http://localhost:8080/api/contact-info";
  var CACHE_KEY = "srmmsSiteInfo";

  // e.g. "About Us - SRMMS"
  var originalTitle = document.title;

  function apply(info) {

    if (!info) {
      return;
    }

    var values = {
      systemName: (info.systemName || "").trim(),
      phone: (info.phone || "").trim(),
      email: (info.email || "").trim(),
      location: (info.location || "").trim()
    };

    var elements = document.querySelectorAll("[data-site]");

    for (var i = 0; i < elements.length; i++) {

      var value = values[elements[i].getAttribute("data-site")];

      if (value) {
        elements[i].textContent = value;
      }
    }

    // Browser tab title
    if (values.systemName) {
      document.title = originalTitle.replace(/SRMMS/g, values.systemName);
    }
  }

  function run() {

    // 1) Show the last known details straight away (no flicker between pages)
    try {
      var cached = sessionStorage.getItem(CACHE_KEY);
      if (cached) {
        apply(JSON.parse(cached));
      }
    } catch (e) { /* ignore */ }

    // 2) Get the latest details from the database
    fetch(API_URL + "?time=" + new Date().getTime(), { cache: "no-store" })
      .then(function (response) {
        return response.ok ? response.json() : null;
      })
      .then(function (info) {

        if (!info) {
          return;
        }

        apply(info);

        try {
          sessionStorage.setItem(CACHE_KEY, JSON.stringify(info));
        } catch (e) { /* ignore */ }
      })
      .catch(function () {
        /* server off: keep the text written in the HTML */
      });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", run);
  } else {
    run();
  }

})();