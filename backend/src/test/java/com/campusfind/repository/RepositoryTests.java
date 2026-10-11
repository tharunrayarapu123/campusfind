package com.campusfind.repository;

import com.campusfind.entity.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RepositoryTests {

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
    private NotificationRepository notificationRepository;

    @Test
    @DisplayName("Verify Flyway seed data for Blocks, Rooms, and Admin User")
    void testSeedDataPresence() {
        // Blocks: Block A, Block B, Block C, Library, Canteen
        List<Block> blocks = blockRepository.findAll();
        assertThat(blocks).hasSize(5);

        // Rooms: 5 rooms each = 25 rooms
        List<Room> rooms = roomRepository.findAll();
        assertThat(rooms).hasSize(25);

        // Seeded Admin user
        Optional<User> adminOpt = userRepository.findByEmail("admin@college.edu");
        assertThat(adminOpt).isPresent();
        User admin = adminOpt.get();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getKarmaPoints()).isEqualTo(100);
        assertThat(admin.isBanned()).isFalse();
    }

    @Test
    @DisplayName("Verify Paginated Item Feed with optional filters and keyword search")
    void testItemFeedWithFilters() {
        User student = userRepository.save(User.builder()
                .fullName("John Doe")
                .email("john@college.edu")
                .passwordHash("hashed")
                .role(Role.STUDENT)
                .build());

        Block blockA = blockRepository.findByName("Block A").orElseThrow();
        Room room101 = roomRepository.findByBlockId(blockA.getId()).get(0);

        // Item 1: Lost Fossil Watch in Block A, Room 101
        Item lostWatch = itemRepository.save(Item.builder()
                .owner(student)
                .type(ItemType.LOST)
                .title("Fossil Chronograph Watch")
                .description("Silver strap with black dial lost during lecture")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(2))
                .status(ItemStatus.OPEN)
                .build());

        // Item 2: Found Blue Bottle in Block A
        Item foundBottle = itemRepository.save(Item.builder()
                .owner(student)
                .type(ItemType.FOUND)
                .title("Milton Water Bottle")
                .description("Dark blue steel thermos bottle")
                .category(ItemCategory.BOTTLE)
                .block(blockA)
                .room(room101)
                .eventTime(LocalDateTime.now().minusHours(1))
                .status(ItemStatus.OPEN)
                .build());

        // 1. Filter by type = LOST
        Page<Item> lostItems = itemRepository.findItemsWithFilters(
                ItemType.LOST, null, null, null, null, null, PageRequest.of(0, 10));
        assertThat(lostItems.getContent()).extracting(Item::getId).contains(lostWatch.getId());
        assertThat(lostItems.getContent()).extracting(Item::getId).doesNotContain(foundBottle.getId());

        // 2. Filter by keyword = 'chronograph'
        Page<Item> keywordMatch = itemRepository.findItemsWithFilters(
                null, null, null, null, null, "chronograph", PageRequest.of(0, 10));
        assertThat(keywordMatch.getContent()).hasSize(1);
        assertThat(keywordMatch.getContent().get(0).getTitle()).isEqualTo("Fossil Chronograph Watch");

        // 3. Filter by category = BOTTLE
        Page<Item> bottleItems = itemRepository.findItemsWithFilters(
                null, ItemCategory.BOTTLE, null, null, null, null, PageRequest.of(0, 10));
        assertThat(bottleItems.getContent()).hasSize(1);
        assertThat(bottleItems.getContent().get(0).getTitle()).isEqualTo("Milton Water Bottle");
    }

    @Test
    @DisplayName("Verify finding matching candidates of opposite type with same block or category")
    void testMatchingCandidates() {
        User alice = userRepository.save(User.builder()
                .fullName("Alice Smith")
                .email("alice@college.edu")
                .passwordHash("hashed")
                .build());

        User bob = userRepository.save(User.builder()
                .fullName("Bob Jones")
                .email("bob@college.edu")
                .passwordHash("hashed")
                .build());

        Block blockA = blockRepository.findByName("Block A").orElseThrow();
        Block blockB = blockRepository.findByName("Block B").orElseThrow();

        // Alice posted LOST Watch in Block A
        Item aliceLost = itemRepository.save(Item.builder()
                .owner(alice)
                .type(ItemType.LOST)
                .title("Casio Vintage Watch")
                .category(ItemCategory.WATCH)
                .block(blockA)
                .eventTime(LocalDateTime.now().minusHours(3))
                .status(ItemStatus.OPEN)
                .build());

        // Bob posted FOUND Watch in Block B (matches by category)
        Item bobFound1 = itemRepository.save(Item.builder()
                .owner(bob)
                .type(ItemType.FOUND)
                .title("Gold digital watch")
                .category(ItemCategory.WATCH)
                .block(blockB)
                .eventTime(LocalDateTime.now().minusHours(2))
                .status(ItemStatus.OPEN)
                .build());

        // Bob posted FOUND Bag in Block A (matches by block)
        Item bobFound2 = itemRepository.save(Item.builder()
                .owner(bob)
                .type(ItemType.FOUND)
                .title("Black backpack")
                .category(ItemCategory.BAG)
                .block(blockA)
                .eventTime(LocalDateTime.now().minusHours(1))
                .status(ItemStatus.OPEN)
                .build());

        // Search candidates for Alice's lost watch (opposite = FOUND, block = Block A, category = WATCH)
        List<Item> candidates = itemRepository.findMatchingCandidates(
                ItemStatus.OPEN,
                ItemType.FOUND,
                blockA.getId(),
                ItemCategory.WATCH,
                aliceLost.getId()
        );

        assertThat(candidates).hasSize(2);
        assertThat(candidates).extracting(Item::getId)
                .containsExactlyInAnyOrder(bobFound1.getId(), bobFound2.getId());
    }

    @Test
    @DisplayName("Verify user match retrieval where user is lost item owner or found item owner")
    void testUserMatches() {
        User loser = userRepository.save(User.builder()
                .fullName("Loser Student")
                .email("loser@college.edu")
                .passwordHash("hashed")
                .build());

        User finder = userRepository.save(User.builder()
                .fullName("Finder Student")
                .email("finder@college.edu")
                .passwordHash("hashed")
                .build());

        Item lost = itemRepository.save(Item.builder()
                .owner(loser)
                .type(ItemType.LOST)
                .title("College ID Card")
                .category(ItemCategory.ID_CARD)
                .eventTime(LocalDateTime.now())
                .status(ItemStatus.MATCHED)
                .build());

        Item found = itemRepository.save(Item.builder()
                .owner(finder)
                .type(ItemType.FOUND)
                .title("Found Student ID")
                .category(ItemCategory.ID_CARD)
                .eventTime(LocalDateTime.now())
                .status(ItemStatus.MATCHED)
                .build());

        MatchEntity match = matchRepository.save(MatchEntity.builder()
                .lostItem(lost)
                .foundItem(found)
                .score(92)
                .status(MatchStatus.CONFIRMED)
                .build());

        // Both loser and finder should see this match
        List<MatchEntity> loserMatches = matchRepository.findMatchesByUserId(loser.getId());
        List<MatchEntity> finderMatches = matchRepository.findMatchesByUserId(finder.getId());

        assertThat(loserMatches).hasSize(1);
        assertThat(loserMatches.get(0).getId()).isEqualTo(match.getId());

        assertThat(finderMatches).hasSize(1);
        assertThat(finderMatches.get(0).getId()).isEqualTo(match.getId());
    }

    @Test
    @DisplayName("Verify unread notification count and user notifications query")
    void testNotifications() {
        User user = userRepository.save(User.builder()
                .fullName("Notify User")
                .email("notify@college.edu")
                .passwordHash("hashed")
                .build());

        notificationRepository.save(Notification.builder()
                .user(user)
                .title("New Match Found!")
                .body("A matching watch was found in Block A")
                .read(false)
                .build());

        notificationRepository.save(Notification.builder()
                .user(user)
                .title("Claim Verified")
                .body("Your item claim has been confirmed")
                .read(true)
                .build());

        long unreadCount = notificationRepository.countByUserIdAndReadFalse(user.getId());
        assertThat(unreadCount).isEqualTo(1);

        List<Notification> allNotifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        assertThat(allNotifications).hasSize(2);
    }

    @Test
    @DisplayName("Verify Chat history ordered chronologically by createdAt")
    void testChatHistory() {
        User user1 = userRepository.save(User.builder()
                .fullName("User One")
                .email("user1@college.edu")
                .passwordHash("hashed")
                .build());

        User user2 = userRepository.save(User.builder()
                .fullName("User Two")
                .email("user2@college.edu")
                .passwordHash("hashed")
                .build());

        Item lost = itemRepository.save(Item.builder()
                .owner(user1)
                .type(ItemType.LOST)
                .title("Lost Phone")
                .category(ItemCategory.PHONE)
                .eventTime(LocalDateTime.now())
                .build());

        Item found = itemRepository.save(Item.builder()
                .owner(user2)
                .type(ItemType.FOUND)
                .title("Found Phone")
                .category(ItemCategory.PHONE)
                .eventTime(LocalDateTime.now())
                .build());

        MatchEntity match = matchRepository.save(MatchEntity.builder()
                .lostItem(lost)
                .foundItem(found)
                .score(85)
                .build());

        chatMessageRepository.save(ChatMessage.builder()
                .match(match)
                .sender(user1)
                .body("Hi, where can we meet?")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .build());

        chatMessageRepository.save(ChatMessage.builder()
                .match(match)
                .sender(user2)
                .body("Let's meet at Library entrance.")
                .createdAt(LocalDateTime.now().minusMinutes(2))
                .build());

        List<ChatMessage> chat = chatMessageRepository.findByMatchIdOrderByCreatedAtAsc(match.getId());
        assertThat(chat).hasSize(2);
        assertThat(chat.get(0).getBody()).isEqualTo("Hi, where can we meet?");
        assertThat(chat.get(1).getBody()).isEqualTo("Let's meet at Library entrance.");
    }

    @Test
    @DisplayName("Verify Karma Leaderboard ordering")
    void testKarmaLeaderboard() {
        userRepository.save(User.builder()
                .fullName("Hero Student")
                .email("hero@college.edu")
                .passwordHash("hashed")
                .karmaPoints(250)
                .build());

        userRepository.save(User.builder()
                .fullName("Helper Student")
                .email("helper@college.edu")
                .passwordHash("hashed")
                .karmaPoints(150)
                .build());

        List<User> topUsers = userRepository.findTop10ByBannedFalseOrderByKarmaPointsDesc();
        assertThat(topUsers).isNotEmpty();
        // First should be Hero Student (250 pts)
        assertThat(topUsers.get(0).getEmail()).isEqualTo("hero@college.edu");
    }

    @Test
    @DisplayName("Verify admin stats item counts (total lost, found, returned)")
    void testAdminStats() {
        User student = userRepository.save(User.builder()
                .fullName("Stats Student")
                .email("stats@college.edu")
                .passwordHash("hashed")
                .build());

        itemRepository.save(Item.builder()
                .owner(student)
                .type(ItemType.LOST)
                .title("Lost 1")
                .category(ItemCategory.OTHER)
                .eventTime(LocalDateTime.now())
                .status(ItemStatus.OPEN)
                .build());

        itemRepository.save(Item.builder()
                .owner(student)
                .type(ItemType.FOUND)
                .title("Found 1")
                .category(ItemCategory.OTHER)
                .eventTime(LocalDateTime.now())
                .status(ItemStatus.RETURNED)
                .build());

        long totalLost = itemRepository.countByType(ItemType.LOST);
        long totalFound = itemRepository.countByType(ItemType.FOUND);
        long totalReturned = itemRepository.countByStatus(ItemStatus.RETURNED);

        assertThat(totalLost).isGreaterThanOrEqualTo(1);
        assertThat(totalFound).isGreaterThanOrEqualTo(1);
        assertThat(totalReturned).isGreaterThanOrEqualTo(1);
    }
}
