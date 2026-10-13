package com.example.taskmanager;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Sign-up, sign-in, sign-out, CSRF and per-user data isolation, using real sessions. */
@SpringBootTest
@Transactional
class AuthApiTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void apiRequiresSignIn() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void registerSignsInAndLogoutSignsOut() throws Exception {
        MockHttpSession session = new MockHttpSession();

        mvc.perform(post("/api/auth/register").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "New.User@Example.com", "password": "correct horse", "displayName": "New User"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new.user@example.com"))
                .andExpect(jsonPath("$.displayName").value("New User"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new.user@example.com"));

        mvc.perform(post("/api/auth/logout").session(session).with(csrf()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsDuplicateEmailAndWeakPassword() throws Exception {
        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "DEMO@example.com", "password": "something-long", "displayName": "Copycat"}
                                """))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "short", "displayName": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.displayName").exists());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameError() throws Exception {
        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"demo@example.com\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));

        mvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"nobody@example.com\", \"password\": \"wrong-password\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void usersCannotSeeOrTouchEachOthersData() throws Exception {
        MockHttpSession demo = signIn("demo@example.com", "password123");
        mvc.perform(get("/api/projects").session(demo)).andExpect(jsonPath("$", hasSize(2)));

        MockHttpSession other = new MockHttpSession();
        mvc.perform(post("/api/auth/register").session(other).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "other@example.com", "password": "password456", "displayName": "Other"}
                                """))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/projects").session(other)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/tasks").session(other)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/projects/1").session(other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/tasks/1").session(other)).andExpect(status().isNotFound());
        mvc.perform(post("/api/tasks").session(other).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Sneaky\", \"projectId\": 1}"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/projects").session(other).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Mine\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/projects").session(other)).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/projects").session(demo)).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void repeatedWrongPasswordsAreBlockedEvenIfTheNextOneIsRight() throws Exception {
        RequestPostProcessor attacker = fromIp("192.0.2.10");
        for (int i = 0; i < 5; i++) {
            mvc.perform(login("demo@example.com", "guess-" + i).with(attacker)).andExpect(status().isUnauthorized());
        }

        mvc.perform(login("demo@example.com", "password123").with(attacker))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.detail").value("Too many failed sign-in attempts. Try again later."))
                .andExpect(jsonPath("$.retryAfterSeconds").isNumber());

        // Email case doesn't give the attacker a fresh budget.
        mvc.perform(login("DEMO@example.com", "password123").with(attacker))
                .andExpect(status().isTooManyRequests());

        // The real owner, on their own network, can still sign in.
        mvc.perform(login("demo@example.com", "password123").with(fromIp("192.0.2.99")))
                .andExpect(status().isOk());
    }

    @Test
    void oneIpGuessingManyAccountsIsBlocked() throws Exception {
        RequestPostProcessor sprayer = fromIp("192.0.2.20");
        for (int i = 0; i < 20; i++) {
            mvc.perform(login("victim" + i + "@example.com", "password1").with(sprayer))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(login("demo@example.com", "password123").with(sprayer))
                .andExpect(status().isTooManyRequests());
    }

    private static MockHttpServletRequestBuilder login(String email, String password) {
        return post("/api/auth/login").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password));
    }

    private static RequestPostProcessor fromIp(String ip) {
        return request -> {
            request.setRemoteAddr(ip);
            return request;
        };
    }

    private MockHttpSession signIn(String email, String password) throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)))
                .andExpect(status().isOk());
        return session;
    }
}
