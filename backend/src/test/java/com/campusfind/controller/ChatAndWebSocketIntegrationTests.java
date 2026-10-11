package com.campusfind.controller;

import com.campusfind.dto.request.ChatMessageRequest;
import com.campusfind.entity.*;
import com.campusfind.repository.*;
import com.campusfind.security.CustomUserDetails;
import com.campusfind.security.JwtService;
import com.campusfind.security.WebSocketAuthChannelInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("standalone")
@Transactional
class ChatAndWebSocketIntegrationTests {

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
    private MatchRepository matchRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private WebSocketAuthChannelInterceptor channelInterceptor;

    @Autowired
    private ObjectMapper objectMapper;

    private User studentA; // Lost item owner
    private User studentB; // Found item owner
    private User studentC; // Third party stranger

    private String tokenA;
    private String tokenB;
    private String tokenC;

    private MatchEntity confirmedMatch;
    private MatchEntity returnedMatch;

    @BeforeEach
    void setUp() {
        Block block = blockRepository.save(Block.builder().name("Chat Test Block").build());
        Room room = roomRepository.save(Room.builder().block(block).name("Chat Room 202").build());

        studentA = userRepository.save(User.builder()
                .fullName("Chat User A")
                .email("chata@college.edu")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .role(Role.STUDENT)
                .build());

        studentB = userRepository.save(User.builder()
                .fullName("Chat User B")
                .email("chatb@college.edu")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .role(Role.STUDENT)
                .build());

        studentC = userRepository.save(User.builder()
                .fullName("Chat Stranger C")
                .email("chatc@college.edu")
                .passwordHash(passwordEncoder.encode("Pass1234!"))
                .role(Role.STUDENT)
                .build());

        tokenA = jwtService.generateToken(new CustomUserDetails(studentA), studentA.getId(), studentA.getRole().name());
        tokenB = jwtService.generateToken(new CustomUserDetails(studentB), studentB.getId(), studentB.getRole().name());
        tokenC = jwtService.generateToken(new CustomUserDetails(studentC), studentC.getId(), studentC.getRole().name());

        Item lost1 = itemRepository.save(Item.builder()
                .owner(studentA)
                .type(ItemType.LOST)
                .title("Calculus Book")
                .category(ItemCategory.BOOKS)
                .block(block)
                .room(room)
                .eventTime(LocalDateTime.now().minusHours(1))
                .status(ItemStatus.MATCHED)
                .build());

        Item found1 = itemRepository.save(Item.builder()
                .owner(studentB)
                .type(ItemType.FOUND)
                .title("Found Calculus Book")
                .category(ItemCategory.BOOKS)
                .block(block)
                .room(room)
                .eventTime(LocalDateTime.now().minusHours(1))
                .status(ItemStatus.MATCHED)
                .build());

        confirmedMatch = matchRepository.save(MatchEntity.builder()
                .lostItem(lost1)
                .foundItem(found1)
                .score(90)
                .status(MatchStatus.CONFIRMED)
                .build());

        Item lost2 = itemRepository.save(Item.builder()
                .owner(studentA)
                .type(ItemType.LOST)
                .title("Returned Glasses")
                .category(ItemCategory.OTHER)
                .block(block)
                .room(room)
                .eventTime(LocalDateTime.now().minusHours(5))
                .status(ItemStatus.RETURNED)
                .build());

        Item found2 = itemRepository.save(Item.builder()
                .owner(studentB)
                .type(ItemType.FOUND)
                .title("Found Glasses")
                .category(ItemCategory.OTHER)
                .block(block)
                .room(room)
                .eventTime(LocalDateTime.now().minusHours(5))
                .status(ItemStatus.RETURNED)
                .build());

        returnedMatch = matchRepository.save(MatchEntity.builder()
                .lostItem(lost2)
                .foundItem(found2)
                .score(95)
                .status(MatchStatus.RETURNED)
                .build());
    }

    @Test
    @DisplayName("WebSocket Auth: Unauthenticated CONNECT rejected, authenticated CONNECT accepted")
    void testWebSocketConnectAuth() {
        // Missing Authorization header
        StompHeaderAccessor unauthAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        assertThatThrownBy(() -> channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], unauthAccessor.getMessageHeaders()), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Missing or invalid JWT");

        // Invalid token
        StompHeaderAccessor invalidAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        invalidAccessor.setNativeHeader("Authorization", "Bearer invalid.token.payload");
        assertThatThrownBy(() -> channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], invalidAccessor.getMessageHeaders()), null))
                .isInstanceOf(MessageDeliveryException.class);

        // Valid token
        StompHeaderAccessor validAccessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        validAccessor.setNativeHeader("Authorization", "Bearer " + tokenA);
        validAccessor.setLeaveMutable(true);
        org.springframework.messaging.Message<?> res = channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], validAccessor.getMessageHeaders()), null);
        assertThat(res).isNotNull();
    }

    @Test
    @DisplayName("WebSocket Authorization: Third user cannot subscribe; Chat after RETURNED cannot send")
    void testWebSocketSubscriptionAndSendAuthorization() {
        // Authenticate student C (stranger)
        StompHeaderAccessor connectC = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectC.setNativeHeader("Authorization", "Bearer " + tokenC);
        connectC.setLeaveMutable(true);
        org.springframework.messaging.Message<?> authMsgC = channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], connectC.getMessageHeaders()), null);
        java.security.Principal principalC = StompHeaderAccessor.wrap(authMsgC).getUser();

        // Student C tries to subscribe to confirmedMatch -> rejected
        StompHeaderAccessor subC = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subC.setDestination("/topic/match/" + confirmedMatch.getId());
        subC.setUser(principalC);
        assertThatThrownBy(() -> channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], subC.getMessageHeaders()), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Not authorized to subscribe to match");

        // Authenticate student A (participant)
        StompHeaderAccessor connectA = StompHeaderAccessor.create(StompCommand.CONNECT);
        connectA.setNativeHeader("Authorization", "Bearer " + tokenA);
        connectA.setLeaveMutable(true);
        org.springframework.messaging.Message<?> authMsgA = channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], connectA.getMessageHeaders()), null);
        java.security.Principal principalA = StompHeaderAccessor.wrap(authMsgA).getUser();

        // Student A can subscribe to confirmedMatch
        StompHeaderAccessor subA = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        subA.setDestination("/topic/match/" + confirmedMatch.getId());
        subA.setUser(principalA);
        org.springframework.messaging.Message<?> allowedSub = channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], subA.getMessageHeaders()), null);
        assertThat(allowedSub).isNotNull();

        // Student A tries to send to returnedMatch -> rejected (read-only)
        StompHeaderAccessor sendReturned = StompHeaderAccessor.create(StompCommand.SEND);
        sendReturned.setDestination("/app/chat/" + returnedMatch.getId());
        sendReturned.setUser(principalA);
        assertThatThrownBy(() -> channelInterceptor.preSend(MessageBuilder.createMessage(new byte[0], sendReturned.getMessageHeaders()), null))
                .isInstanceOf(MessageDeliveryException.class)
                .hasMessageContaining("Chat is read-only after item is marked RETURNED");
    }

    @Test
    @DisplayName("REST Chat Endpoints: Message persistence, privacy masking (no email/phone), and chronological ordering")
    void testChatRestEndpoints() throws Exception {
        // Send message from student A
        ChatMessageRequest req1 = new ChatMessageRequest("Hey! When can we meet for the textbook handoff?", null);
        mockMvc.perform(post("/api/v1/matches/" + confirmedMatch.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("Hey! When can we meet for the textbook handoff?"))
                .andExpect(jsonPath("$.data.sender.fullName").value("Chat User A"))
                // Ensure privacy: no email or phone in sender DTO
                .andExpect(jsonPath("$.data.sender.email").doesNotExist())
                .andExpect(jsonPath("$.data.sender.phone").doesNotExist());

        // Send response message from student B
        ChatMessageRequest req2 = new ChatMessageRequest("I am at the library ground floor right now!", null);
        mockMvc.perform(post("/api/v1/matches/" + confirmedMatch.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.body").value("I am at the library ground floor right now!"));

        // Stranger student C gets 403 trying to read messages
        mockMvc.perform(get("/api/v1/matches/" + confirmedMatch.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenC))
                .andExpect(status().isForbidden());

        // Retrieve messages (paginated, oldest to newest)
        mockMvc.perform(get("/api/v1/matches/" + confirmedMatch.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(2)))
                .andExpect(jsonPath("$.data.content[0].body").value("Hey! When can we meet for the textbook handoff?"))
                .andExpect(jsonPath("$.data.content[1].body").value("I am at the library ground floor right now!"));

        // Chat on returned match is rejected via REST
        ChatMessageRequest reqReturned = new ChatMessageRequest("Testing message after return", null);
        mockMvc.perform(post("/api/v1/matches/" + returnedMatch.getId() + "/messages")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqReturned)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Chat is read-only")));
    }
}
