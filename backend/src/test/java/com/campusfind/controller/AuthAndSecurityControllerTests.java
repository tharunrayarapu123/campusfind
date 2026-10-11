package com.campusfind.controller;

import com.campusfind.dto.request.LoginRequest;
import com.campusfind.dto.request.RegisterRequest;
import com.campusfind.entity.Role;
import com.campusfind.entity.User;
import com.campusfind.repository.UserRepository;
import com.campusfind.security.RateLimiterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("standalone")
@Transactional
class AuthAndSecurityControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Reset rate limiter window for testing IP
        rateLimiterService.reset("127.0.0.1:/api/v1/auth/login");
        rateLimiterService.reset("127.0.0.1:/api/v1/auth/register");
    }

    @Test
    @DisplayName("Register: Success with college domain email, STUDENT role, and hashed password")
    void testRegisterSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Sarah Connor")
                .email("sarah@college.edu")
                .password("Password123")
                .department("Computer Science")
                .year(3)
                .phone("+1234567890")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("sarah@college.edu")))
                .andExpect(jsonPath("$.data.user.role", is("STUDENT")))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());

        // Verify password in DB is hashed and not plaintext
        User saved = userRepository.findByEmail("sarah@college.edu").orElseThrow();
        assertThat(saved.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches("Password123", saved.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("Register: Reject non-institutional email domain")
    void testRegisterWrongDomainRejected() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("John External")
                .email("john@gmail.com")
                .password("Password123")
                .department("Physics")
                .year(2)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("college.edu")));
    }

    @Test
    @DisplayName("Register: Reject duplicate email registration")
    void testRegisterDuplicateEmailRejected() throws Exception {
        userRepository.save(User.builder()
                .fullName("Existing Student")
                .email("existing@college.edu")
                .passwordHash(passwordEncoder.encode("Password123"))
                .department("Math")
                .role(Role.STUDENT)
                .build());

        RegisterRequest request = RegisterRequest.builder()
                .fullName("Another Student")
                .email("existing@college.edu")
                .password("NewPass123")
                .department("Math")
                .year(1)
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("already exists")));
    }

    @Test
    @DisplayName("Register: Reject weak password without number or shorter than 8 chars")
    void testRegisterWeakPasswordRejected() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .fullName("Weak Pass User")
                .email("weak@college.edu")
                .password("short")
                .department("Arts")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    @DisplayName("Login: Success returns JWT token and safe user profile without passwordHash")
    void testLoginSuccess() throws Exception {
        userRepository.save(User.builder()
                .fullName("Alex Chen")
                .email("alex@college.edu")
                .passwordHash(passwordEncoder.encode("SecurePass1"))
                .department("Engineering")
                .year(4)
                .karmaPoints(25)
                .role(Role.STUDENT)
                .build());

        LoginRequest request = LoginRequest.builder()
                .email("alex@college.edu")
                .password("SecurePass1")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("alex@college.edu")))
                .andExpect(jsonPath("$.data.user.karmaPoints", is(25)))
                .andExpect(jsonPath("$.data.user.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("Login: Reject wrong password with generic message")
    void testLoginWrongPassword() throws Exception {
        userRepository.save(User.builder()
                .fullName("Alex Chen")
                .email("alex@college.edu")
                .passwordHash(passwordEncoder.encode("CorrectPass1"))
                .department("Engineering")
                .role(Role.STUDENT)
                .build());

        LoginRequest request = LoginRequest.builder()
                .email("alex@college.edu")
                .password("WrongPass1")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("Login: Banned user returns 403 Forbidden with clear message")
    void testLoginBannedUser() throws Exception {
        userRepository.save(User.builder()
                .fullName("Banned Student")
                .email("banned@college.edu")
                .passwordHash(passwordEncoder.encode("Password123"))
                .department("Law")
                .role(Role.STUDENT)
                .banned(true)
                .build());

        LoginRequest request = LoginRequest.builder()
                .email("banned@college.edu")
                .password("Password123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("banned")));
    }

    @Test
    @DisplayName("Security: Protected endpoint without token returns 401 JSON ApiResponse")
    void testProtectedEndpointWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Authentication required")));
    }

    @Test
    @DisplayName("Security: STUDENT role accessing /api/v1/admin/** endpoint returns 403")
    void testStudentCallingAdminEndpointReturns403() throws Exception {
        // 1. Register student and obtain token
        RegisterRequest registerReq = RegisterRequest.builder()
                .fullName("Student User")
                .email("student@college.edu")
                .password("Student123")
                .department("Bio")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String studentToken = jsonNode.get("data").get("token").asText();

        // 2. Call admin endpoint with student token
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Access denied")));
    }

    @Test
    @DisplayName("Security: ADMIN role accessing /api/v1/admin/** succeeds")
    void testAdminCallingAdminEndpointSuccess() throws Exception {
        // Use seeded admin user
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@college.edu")
                .password("Admin@123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        String adminToken = jsonNode.get("data").get("token").asText();

        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("HEALTHY")));
    }

    @Test
    @DisplayName("Privacy Rule: Fetching another user profile NEVER exposes email or phone")
    void testPrivacyRuleOtherUserNeverSeesEmailOrPhone() throws Exception {
        // Create User A with private contact details
        User userA = userRepository.save(User.builder()
                .fullName("Private Owner")
                .email("privateowner@college.edu")
                .phone("+919876543210")
                .passwordHash(passwordEncoder.encode("Pass12345"))
                .department("Architecture")
                .year(2)
                .karmaPoints(40)
                .role(Role.STUDENT)
                .build());

        // Create User B and login to get JWT
        RegisterRequest userBReq = RegisterRequest.builder()
                .fullName("Finder Student")
                .email("finderstudent@college.edu")
                .password("FinderPass1")
                .department("Civil")
                .build();

        MvcResult bResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userBReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String tokenB = objectMapper.readTree(bResult.getResponse().getContentAsString())
                .get("data").get("token").asText();

        // User B looks up User A via GET /api/v1/users/{id}
        mockMvc.perform(get("/api/v1/users/" + userA.getId())
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(userA.getId().intValue())))
                .andExpect(jsonPath("$.data.fullName", is("Private Owner")))
                .andExpect(jsonPath("$.data.department", is("Architecture")))
                .andExpect(jsonPath("$.data.karmaPoints", is(40)))
                // STRICT PRIVACY CHECK: email and phone MUST NOT BE PRESENT
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @Test
    @DisplayName("Rate Limiting: Exceeding 10 requests per minute triggers 429 Too Many Requests")
    void testRateLimitingOnLogin() throws Exception {
        LoginRequest req = LoginRequest.builder()
                .email("bruteforce@college.edu")
                .password("wrongpassword")
                .build();

        // 10 requests allowed
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)));
        }

        // 11th request must be blocked by RateLimitingFilter
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", containsString("Too many requests")));
    }
}
