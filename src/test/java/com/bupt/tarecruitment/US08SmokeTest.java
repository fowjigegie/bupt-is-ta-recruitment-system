package com.bupt.tarecruitment;

import com.bupt.tarecruitment.communication.InquiryMessage;
import com.bupt.tarecruitment.communication.MessageService;
import com.bupt.tarecruitment.communication.TextFileMessageRepository;
import com.bupt.tarecruitment.common.storage.DataFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

public final class US08SmokeTest {
    private US08SmokeTest() {
    }

    public static void main(String[] args) {
        try {
            Path tempDataDirectory = Files.createTempDirectory("us08-smoke");
            Files.write(
                tempDataDirectory.resolve(DataFile.MESSAGES.fileName()),
                List.of(DataFile.MESSAGES.initialLines().getFirst()),
                StandardCharsets.UTF_8
            );

            TextFileMessageRepository repository = new TextFileMessageRepository(tempDataDirectory);
            repository.save(new InquiryMessage(
                "message101",
                "job101",
                "ta101",
                "mo101",
                LocalDateTime.parse("2026-04-04T10:00:00"),
                "Hello organiser",
                false
            ));
            repository.save(new InquiryMessage(
                "message102",
                "job101",
                "mo101",
                "ta101",
                LocalDateTime.parse("2026-04-04T10:05:00"),
                "Please send your availability.",
                false
            ));
            repository.save(new InquiryMessage(
                "message103",
                "job102",
                "ta101",
                "mo101",
                LocalDateTime.parse("2026-04-04T10:10:00"),
                "Another thread",
                false
            ));

            MessageService messageService = new MessageService(repository);

            assertEquals(1L, messageService.countUnreadMessagesForUser("ta101"), "ta101 should start with one unread message.");
            assertEquals(2L, messageService.countUnreadMessagesForUser("mo101"), "mo101 should start with two unread messages.");

            int marked = messageService.markConversationAsRead("job101", "ta101", "mo101");
            assertEquals(1, marked, "Exactly one unread message should be marked as read for ta101 in job101.");
            assertEquals(0L, messageService.countUnreadMessagesForUser("ta101"), "ta101 should have no unread messages after reading.");
            assertEquals(2L, messageService.countUnreadMessagesForUser("mo101"), "mo101 unread count should be unchanged.");

            InquiryMessage newMessage = messageService.sendMessage("job101", "mo101", "ta101", "Follow-up");
            assertEquals("job101", newMessage.jobId(), "Message should keep job context.");
            assertEquals(1L, messageService.countUnreadMessagesForUser("ta101"), "Newly sent message should increment ta101 unread count.");

            System.out.println("US08 smoke test passed.");
            System.out.println("Temp data directory: " + tempDataDirectory);
        } catch (Exception exception) {
            throw new IllegalStateException("US08 smoke test failed.", exception);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException(message + " Expected: " + expected + ", actual: " + actual);
        }
    }
}
