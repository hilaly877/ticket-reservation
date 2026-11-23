
/* =========================================
   TicketHub - Frontend JavaScript
   ========================================= */

const API_URL = "";

// Store login information
const state = {
    token: localStorage.getItem("token"),
    username: localStorage.getItem("username"),
    fullName: localStorage.getItem("fullName"),
    role: localStorage.getItem("role")
};

/* ---------- HTML ELEMENTS ---------- */

const loginSection = document.getElementById("login-section");
const registerSection = document.getElementById("register-section");
const dashboardSection = document.getElementById("dashboard-section");

const loginForm = document.getElementById("login-form");
const registerForm = document.getElementById("register-form");

const loginMessage = document.getElementById("login-message");
const registerMessage = document.getElementById("register-message");

const eventsList = document.getElementById("events-list");
const reservationsList = document.getElementById("reservations-list");

const welcomeMessage = document.getElementById("welcome-message");
const searchInput = document.getElementById("search-input");

/* ---------- API HELPER ---------- */

// Sends requests to the Spring Boot backend
async function api(path, options = {}) {

    const headers = {
        "Content-Type": "application/json"
    };

    // Attach JWT when available
    if (state.token) {
        headers["Authorization"] = "Bearer " + state.token;
    }

    const response = await fetch(API_URL + path, {
        method: options.method || "GET",
        headers: headers,
        body: options.body
            ? JSON.stringify(options.body)
            : undefined
    });

    const text = await response.text();

    let data = null;

    try {
        data = text ? JSON.parse(text) : null;
    } catch {
        data = text;
    }

    // Handle API errors
    if (!response.ok) {
        const message =
            data?.message || data?.error || "Request failed";

        if (response.status === 401) {
            logout("Your session expired. Please log in again.");
        }

        throw new Error(message);
    }

    return data;
}

/* ---------- SHOW / HIDE SECTIONS ---------- */

function showLogin() {
    loginSection.classList.remove("hidden");
    registerSection.classList.add("hidden");
    dashboardSection.classList.add("hidden");
}

function showRegister() {
    loginSection.classList.add("hidden");
    registerSection.classList.remove("hidden");
    dashboardSection.classList.add("hidden");
}

function showDashboard() {
    loginSection.classList.add("hidden");
    registerSection.classList.add("hidden");
    dashboardSection.classList.remove("hidden");

    welcomeMessage.textContent =
        `Welcome, ${state.fullName || state.username}! (${state.role})`;

    // Show admin navigation only for ADMIN
    const adminButton = document.querySelector('[data-view="admin"]');

    if (adminButton) {
        adminButton.classList.toggle(
            "hidden",
            state.role !== "ADMIN"
        );
    }
}

/* ---------- LOGIN ---------- */

loginForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    loginMessage.textContent = "Logging in...";

    const username =
        document.getElementById("login-username").value.trim();

    const password =
        document.getElementById("login-password").value;

    try {
        const result = await api("/api/auth/login", {
            method: "POST",
            body: { username, password }
        });

        // Save JWT and user details
        state.token = result.token;
        state.username = result.username;
        state.fullName = result.fullName;
        state.role = result.role;

        localStorage.setItem("token", result.token);
        localStorage.setItem("username", result.username);
        localStorage.setItem("fullName", result.fullName);
        localStorage.setItem("role", result.role);

        loginMessage.textContent = "";

        showDashboard();

        await loadEvents();

    } catch (error) {
        loginMessage.textContent = error.message;
    }
});

/* ---------- REGISTER CUSTOMER ---------- */

registerForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    registerMessage.textContent = "Creating account...";

    const username =
        document.getElementById("register-username").value.trim();

    const fullName =
        document.getElementById("register-fullname").value.trim();

    const password =
        document.getElementById("register-password").value;

    try {
        await api("/api/auth/register", {
            method: "POST",
            body: {
                username,
                fullName,
                password,
                role: "CUSTOMER"
            }
        });

        registerMessage.textContent =
            "Account created successfully! You can now log in.";

        registerForm.reset();

    } catch (error) {
        registerMessage.textContent = error.message;
    }
});

/* ---------- LOGOUT ---------- */

function logout(message = "") {

    state.token = null;
    state.username = null;
    state.fullName = null;
    state.role = null;

    localStorage.removeItem("token");
    localStorage.removeItem("username");
    localStorage.removeItem("fullName");
    localStorage.removeItem("role");

    showLogin();

    if (message) {
        loginMessage.textContent = message;
    }
}

document.getElementById("logout-button")
    .addEventListener("click", () => logout());

/* ---------- SWITCH LOGIN / REGISTER ---------- */

document.getElementById("show-register")
    .addEventListener("click", showRegister);

document.getElementById("show-login")
    .addEventListener("click", showLogin);

/* ---------- LOAD EVENTS ---------- */

async function loadEvents(keyword = "") {

    eventsList.innerHTML = "<p>Loading events...</p>";

    try {
        const events = await api("/api/events");

        // Filter events using search text
        const filteredEvents = events.filter(event => {
            const text =
                `${event.name} ${event.description} ${event.venue}`;

            return text.toLowerCase().includes(keyword.toLowerCase());
        });

        displayEvents(filteredEvents);

    } catch (error) {
        eventsList.innerHTML =
            `<p>${escapeHTML(error.message)}</p>`;
    }
}

/* ---------- DISPLAY EVENTS ---------- */

function displayEvents(events) {

    eventsList.innerHTML = "";

    if (events.length === 0) {
        eventsList.innerHTML = "<p>No events found.</p>";
        return;
    }

    events.forEach(event => {

        const card = document.createElement("div");
        card.className = "event-card";

        card.innerHTML = `
            <h3>${escapeHTML(event.name)}</h3>

            <p>${escapeHTML(event.description)}</p>

            <p><strong>Venue:</strong>
                ${escapeHTML(event.venue)}</p>

            <p><strong>Date:</strong>
                ${formatDate(event.eventDate)}</p>

            <p><strong>Available Seats:</strong>
                ${event.availableSeats} / ${event.totalSeats}</p>

            <p><strong>Ticket Price:</strong>
                Rs. ${event.ticketPrice}</p>

            ${
                state.role === "CUSTOMER"
                    ? `<button class="book-button"
                        data-event-id="${event.id}">
                        Book Tickets
                       </button>`
                    : ""
            }
        `;

        eventsList.appendChild(card);
    });

    // Add booking button actions
    document.querySelectorAll(".book-button").forEach(button => {

        button.addEventListener("click", () => {
            bookEvent(button.dataset.eventId);
        });

    });
}

/* ---------- BOOK TICKETS ---------- */

async function bookEvent(eventId) {

    const seats = Number(
        prompt("How many seats do you want to book? (1-10)")
    );

    if (!Number.isInteger(seats) || seats < 1 || seats > 10) {
        alert("Please enter a number between 1 and 10.");
        return;
    }

    try {
        const result = await api("/api/reservations", {
            method: "POST",
            body: {
                eventId: eventId,
                seats: seats
            }
        });

        alert(
            `Booking successful!\nReservation code: ${result.reservationCode}`
        );

        await loadEvents();

    } catch (error) {
        alert("Booking failed: " + error.message);
    }
}

/* ---------- LOAD MY RESERVATIONS ---------- */

async function loadMyReservations() {

    reservationsList.innerHTML = "<p>Loading reservations...</p>";

    try {

        let endpoint;

        if (state.role === "CUSTOMER") {
            endpoint = "/api/reservations/my";
        } else {
            endpoint = "/api/reservations";
        }

        const reservations = await api(endpoint);

        displayReservations(reservations);

    } catch (error) {

        reservationsList.innerHTML =
            `<p>${escapeHTML(error.message)}</p>`;

    }
}

/* ---------- DISPLAY RESERVATIONS ---------- */

function displayReservations(reservations) {

    reservationsList.innerHTML = "";

    if (reservations.length === 0) {
        reservationsList.innerHTML =
            "<p>You have no reservations yet.</p>";
        return;
    }

    reservations.forEach(reservation => {

        const card = document.createElement("div");
        card.className = "reservation-card";

        card.innerHTML = `
            <p><strong>Reservation:</strong>
                ${escapeHTML(reservation.reservationCode)}</p>

            <p><strong>Event ID:</strong>
                ${escapeHTML(reservation.eventId)}</p>

            <p><strong>Seats:</strong>
                ${reservation.seats}</p>

            <p><strong>Total:</strong>
                Rs. ${reservation.totalAmount}</p>

            <p><strong>Status:</strong>
                ${escapeHTML(reservation.status)}</p>

${
    reservation.status === "CONFIRMED" &&
    state.role === "CUSTOMER"
        ? `
            <button class="edit-seats-button"
                data-reservation-id="${reservation.id}"
                data-event-id="${reservation.eventId}"
                data-current-seats="${reservation.seats}">
                Edit Seats
            </button>

            <button class="cancel-button"
                data-reservation-id="${reservation.id}">
                Cancel Reservation
            </button>
        `
        : ""
}
        `;

        reservationsList.appendChild(card);
    });

    // Add cancellation actions
    document.querySelectorAll(".cancel-button").forEach(button => {

        button.addEventListener("click", () => {
            cancelReservation(button.dataset.reservationId);
        });

    });

    // Add edit seat actions
    document.querySelectorAll(".edit-seats-button").forEach(button => {

        button.addEventListener("click", () => {
            updateReservationSeats(
                button.dataset.reservationId,
                button.dataset.eventId,
                Number(button.dataset.currentSeats)
            );
        });

    });
}

/* ---------- UPDATE RESERVATION ---------- */
async function updateReservationSeats(reservationId, eventId, currentSeats) {

    const newSeats = prompt(
        `Currently booked: ${currentSeats} seats.\nEnter the new seat count (1-10):`,
        currentSeats
    );

    // If user clicks Cancel
    if (newSeats === null) {
        return;
    }

    const seats = Number(newSeats);

    // Validate seat count
    if (!Number.isInteger(seats) || seats < 1 || seats > 10) {
        alert("Please enter a valid number of seats between 1 and 10.");
        return;
    }

    // No changes needed
    if (seats === currentSeats) {
        alert("The seat count has not changed.");
        return;
    }

    try {

        const response = await api(`/api/reservations/${reservationId}`, {
            method: "PUT",
            body:{
                eventId: eventId,
                seats: seats
            }
        });

        alert("Reservation updated successfully!");

        // Refresh reservations and event seat availability
        await loadMyReservations();
        await loadEvents();

    } catch (error) {
        alert("Failed to update reservation: " + error.message);
    }
}


/* ---------- CANCEL RESERVATION ---------- */

async function cancelReservation(reservationId) {

    if (!confirm("Are you sure you want to cancel this reservation?")) {
        return;
    }

    try {
        await api(`/api/reservations/${reservationId}`, {
            method: "DELETE"
        });

        alert("Reservation cancelled successfully.");

        await loadMyReservations();
        await loadEvents();

    } catch (error) {
        alert("Cancellation failed: " + error.message);
    }
}

/* ---------- NAVIGATION ---------- */

document.querySelectorAll(".nav-button").forEach(button => {

    button.addEventListener("click", async () => {

        const viewName = button.dataset.view;

        // Active button design
        document.querySelectorAll(".nav-button")
            .forEach(btn => btn.classList.remove("active"));

        button.classList.add("active");

        // Hide all views
        document.querySelectorAll(".view")
            .forEach(view => view.classList.add("hidden"));

        // Show selected view
        document.getElementById(`${viewName}-view`)
            .classList.remove("hidden");

        if (viewName === "events") {
            await loadEvents();
        }

        if (viewName === "reservations") {
            await loadMyReservations();
        }

        if (viewName === "admin") {
            await loadAdminData();
        }
    });

});

/* ---------- SEARCH ---------- */

searchInput.addEventListener("input", () => {
    loadEvents(searchInput.value);
});

/* ---------- HELPERS ---------- */

function formatDate(dateString) {

    if (!dateString) return "Date not available";

    return new Date(dateString).toLocaleString();
}

// Escape HTML before placing API data inside HTML strings
function escapeHTML(value) {

    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    }[character]));
}

/* ---------- ADMIN DATA ---------- */

async function loadAdminData() {

    if (state.role !== "ADMIN") {
        return;
    }

    await loadAdminStats();
    await loadAdminUsers();
}

/* ---------- START APPLICATION ---------- */

if (state.token) {
    showDashboard();
    loadEvents();
} else {
    showLogin();
}

// =========================
// ADMIN FUNCTIONS
// =========================

async function loadAdminStats() {

    try {

        const stats = await api("/api/admin/stats");

        document.getElementById("total-users").textContent =
            stats.totalUsers;

        document.getElementById("total-events").textContent =
            stats.totalEvents;

        document.getElementById("total-reservations").textContent =
            stats.totalReservations;

        document.getElementById("confirmed-reservations").textContent =
            stats.confirmedReservations;

        document.getElementById("cancelled-reservations").textContent =
            stats.cancelledReservations;

    } catch (error) {

        alert("Failed to load statistics: " + error.message);

    }
}

// Create Admin or Organizer
document.getElementById("create-user-form")
    .addEventListener("submit", async (event) => {

        event.preventDefault();

        const username =
            document.getElementById("new-username").value.trim();

        const password =
            document.getElementById("new-password").value;

        const fullName =
            document.getElementById("new-fullname").value.trim();

        const role =
            document.getElementById("new-role").value;

        try {

            await api("/api/admin/users", {
                method: "POST",
                body: {
                    username,
                    password,
                    fullName,
                    role
                }
            });

            alert("User created successfully!");

            document.getElementById("create-user-form").reset();

            await loadAdminStats();

        } catch (error) {

            alert("Failed to create user: " + error.message);

        }
    });


    // Load all registered users
async function loadAdminUsers() {

    const usersList = document.getElementById("users-list");

    usersList.innerHTML = "<p>Loading users...</p>";

    try {

        const users = await api("/api/admin/users");

        usersList.innerHTML = "";

        if (users.length === 0) {
            usersList.innerHTML = "<p>No users found.</p>";
            return;
        }

        users.forEach(user => {

            const card = document.createElement("div");
            card.className = "user-card";

        card.innerHTML = `
            <p><strong>Username:</strong>
                ${escapeHTML(user.username)}</p>

            <p><strong>Full Name:</strong>
                ${escapeHTML(user.fullName)}</p>

            <p><strong>Role:</strong>
                ${escapeHTML(user.role)}</p>

            <button class="edit-user-button"
                data-user-id="${user.id}"
                data-full-name="${escapeHTML(user.fullName)}">
                Edit Full Name
            </button>
        `;

            usersList.appendChild(card);
        });

        // Add edit button actions
        document.querySelectorAll(".edit-user-button")
            .forEach(button => {

                button.addEventListener("click", () => {

                    editUser(
                        button.dataset.userId,
                        button.dataset.fullName
                    );

                });

            });




    } catch (error) {

        usersList.innerHTML =
            `<p>${escapeHTML(error.message)}</p>`;
    }
}

// Refresh users button
document.getElementById("refresh-users-button")
    .addEventListener("click", loadAdminUsers);

// Edit user's full name
async function editUser(userId, currentFullName) {

    const newFullName = prompt(
        "Enter the new full name:",
        currentFullName
    );

    if (newFullName === null) {
        return;
    }

    const fullName = newFullName.trim();

    if (fullName.length < 2 || fullName.length > 100) {
        alert("Full name must be between 2 and 100 characters.");
        return;
    }

    try {

        await api(`/api/admin/users/${userId}`, {
            method: "PUT",
            body: { fullName }
        });

        alert("User name updated successfully!");

        await loadAdminUsers();

    } catch (error) {

        alert("Failed to update user: " + error.message);

    }
}