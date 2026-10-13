package com.example.taskmanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Exercises the CSRF + session cookie handshake over real HTTP with a cookie-keeping client,
 * the same way the browser does it (MockMvc doesn't see cookies written at response commit).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class CsrfHttpTest {

    @LocalServerPort
    private int port;

    private final CookieManager cookies = new CookieManager();
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies).build();

    @Test
    void signInRequiresTheCsrfTokenFromTheCookie() throws Exception {
        assertThat(send(get("/api/auth/me")).statusCode()).isEqualTo(401);
        String token = cookie("XSRF-TOKEN");
        assertThat(token).isNotBlank();

        String credentials = "{\"email\": \"demo@example.com\", \"password\": \"password123\"}";
        assertThat(send(post("/api/auth/login", credentials)).statusCode()).isEqualTo(403);

        HttpResponse<String> login = send(post("/api/auth/login", credentials).header("X-XSRF-TOKEN", token));
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat(login.headers().allValues("Set-Cookie"))
                .anySatisfy(header -> assertThat(header).startsWith("JSESSIONID=").containsIgnoringCase("HttpOnly"));

        HttpResponse<String> me = send(get("/api/auth/me"));
        assertThat(me.statusCode()).isEqualTo(200);
        assertThat(me.body()).contains("demo@example.com");

        assertThat(send(post("/api/auth/logout", "").header("X-XSRF-TOKEN", cookie("XSRF-TOKEN"))).statusCode())
                .isEqualTo(204);
        assertThat(send(get("/api/auth/me")).statusCode()).isEqualTo(401);
    }

    private HttpRequest.Builder get(String path) {
        return HttpRequest.newBuilder(uri(path)).GET();
    }

    private HttpRequest.Builder post(String path, String json) {
        return HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json));
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String cookie(String name) {
        return cookies.getCookieStore().getCookies().stream()
                .filter(c -> c.getName().equals(name))
                .map(HttpCookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
