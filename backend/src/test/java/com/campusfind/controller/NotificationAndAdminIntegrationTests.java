package com.campusfind.controller;

import com.campusfind.dto.request.LoginRequest;
import com.campusfind.dto.request.ReportRequest;
import com.campusfind.entity.*;
import com.campusfind.repository.*;
import com.campusfind.security.CustomUserDetails;
import com.campusfind.security.JwtService;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("standalone")
@Transactional
class NotificationAndAdminIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BlockRepository blockRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    private User student;
    private User admin1;
    private User admin2;

    private String studentToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        student = userRepository.save(User.builder()
                .fullName("Normal Student")
                .email("student@college.edu")
                .passwordHash(passwordEncoder.encode("StudentPass1!"))
                .role(Role.STUDENT)
                .banned(false)
                .build());

        admin1 = userRepository.save(User.builder()
                .fullName("Lead Administrator")
                .email("leadadmin@college.edu")
                .passwordHash(passwordEncoder.encode("AdminPass1!"))
                .role(Role.ADMIN)
                .banned(false)
                .build());

        admin2 = userRepository.save(User.builder()
                .fullName("Secondary Administrator")
                .email("secondadmin@college.edu")
                .passwordHash(passwordEncoder.encode("AdminPass1!"))
                .role(Role.ADMIN)
                .banned(false)
                .build());

        studentToken = jwtService.generateToken(new CustomUserDetails(student), student.getId(), student.getRole().name());
        adminToken = jwtService.generateToken(new CustomUserDetails(admin1), admin1.getId(), admin1.getRole().name());
    }

    @Test
    @DisplayName("Notifications: Users only see their own, unread count, and mark as read")
    void testNotificationsIsolationAndReadStatus() throws Exception {
        Notification note1 = notificationRepository.save(Notification.builder()
                .user(student)
                .title("Student Alert")
                .body("You have a match!")
                .read(false)
                .build());

        Notification note2 = notificationRepository.save(Notification.builder()
                .user(admin1)
                .title("Admin Alert")
                .body("System backup ready")
                .read(false)
                .build());

        // Student fetches notifications -> only sees note1
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.content[0].title").value("Student Alert"));

        // Student unread count is 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // Student cannot mark admin's notification as read -> 403
        mockMvc.perform(post("/api/v1/notifications/" + note2.getId() + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // Student marks own notification as read
        mockMvc.perform(post("/api/v1/notifications/" + note1.getId() + "/read")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.read").value(true));

        // Unread count is now 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));
    }

    @Test
    @DisplayName("Admin Endpoints: STUDENT gets 403 on all admin routes; ADMIN can moderate, ban, and unhide")
    void testAdminEndpointsAndGovernance() throws Exception {
        // 1. STUDENT gets 403 on admin stats
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());

        // 2. ADMIN gets stats
        mockMvc.perform(get("/api/v1/admin/stats")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").isNumber());

        // 3. Admin cannot ban themselves -> 400
        mockMvc.perform(post("/api/v1/admin/users/" + admin1.getId() + "/ban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot ban their own account")));

        // 4. Admin cannot ban another admin -> 400
        mockMvc.perform(post("/api/v1/admin/users/" + admin2.getId() + "/ban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("cannot ban another administrator")));

        // 5. Admin bans student -> audit log written
        mockMvc.perform(post("/api/v1/admin/users/" + student.getId() + "/ban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banned").value(true));

        User bannedStudent = userRepository.findById(student.getId()).orElseThrow();
        assertThat(bannedStudent.isBanned()).isTrue();

        // 6. Banned student cannot login -> 403
        LoginRequest loginReq = new LoginRequest(student.getEmail(), "StudentPass1!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("banned")));

        // 7. Admin unbans student -> login restored
        mockMvc.perform(post("/api/v1/admin/users/" + student.getId() + "/unban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.banned").value(false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        // 8. Auto-hidden item unhiding
        Item hiddenItem = itemRepository.save(Item.builder()
                .owner(student)
                .type(ItemType.LOST)
                .title("Reported Wallet")
                .category(ItemCategory.OTHER)
                .eventTime(LocalDateTime.now().minusHours(2))
                .hidden(true)
                .build());

        mockMvc.perform(post("/api/v1/admin/items/" + hiddenItem.getId() + "/unhide")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Item unhidden = itemRepository.findById(hiddenItem.getId()).orElseThrow();
        assertThat(unhidden.isHidden()).isFalse();

        // 9. Verify AuditLog entries exist
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", not(empty())))
                .andExpect(jsonPath("$.data.content[0].adminEmail").value(admin1.getEmail()));
    }
}
