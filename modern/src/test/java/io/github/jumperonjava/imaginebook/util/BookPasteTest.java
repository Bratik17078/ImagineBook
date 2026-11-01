package io.github.jumperonjava.imaginebook.util;

import org.junit.jupiter.api.Test;
import java.util.function.Predicate;
import static org.junit.jupiter.api.Assertions.*;

class BookPasteTest {
    private static final Predicate<String> FITS = text -> text.length() <= 140;

    @Test
    void pastesThousandsOfCharactersWithoutLosingWhitespace() {
        String paste = "A paragraph of text.\n\n".repeat(300);
        var result = BookPaste.plan("", 0, 0, paste, 100, 1024, FITS).orElseThrow();
        assertTrue(result.pages().size() > 1);
        assertEquals(paste, String.join("", result.pages()));
        assertTrue(result.pages().stream().allMatch(FITS));
        assertEquals(paste.length(), cursorPosition(result));
    }

    @Test
    void replacesSelectionAndLeavesCursorBetweenPasteAndSuffix() {
        String paste = "a long sentence ".repeat(70);
        var result = BookPaste.plan("prefix selected suffix", 7, 15, paste, 100, 1024, FITS).orElseThrow();
        assertEquals("prefix " + paste + " suffix", String.join("", result.pages()));
        assertEquals(7 + paste.length(), cursorPosition(result));
    }

    @Test
    void refusesPasteThatExceedsRemainingBookCapacity() {
        assertTrue(BookPaste.plan("original", 0, 8, "x".repeat(1000), 2, 1024, FITS).isEmpty());
    }

    @Test
    void respectsServerCharacterLimitEvenWhenTextFitsVisually() {
        var result = BookPaste.plan("", 0, 0, "i".repeat(3000), 100, 1024, text -> true).orElseThrow();
        assertEquals(3, result.pages().size());
        assertTrue(result.pages().stream().allMatch(text -> text.length() <= 1024));
        assertEquals(3000, String.join("", result.pages()).length());
    }

    @Test
    void neverSplitsSupplementaryCharactersBetweenPages() {
        String text = "x".repeat(1023) + "\ud83d\ude80" + "z".repeat(20);
        var result = BookPaste.plan("", 0, 0, text, 100, 1024, value -> true).orElseThrow();
        assertEquals(text, String.join("", result.pages()));
        assertTrue(result.pages().stream().noneMatch(value -> Character.isHighSurrogate(value.charAt(value.length() - 1))));
    }

    @Test
    void insertionAtStartKeepsCursorBeforeExistingText() {
        var result = BookPaste.plan("x".repeat(500), 0, 0, "", 100, 1024, FITS).orElseThrow();
        assertEquals(0, result.cursorPage());
        assertEquals(0, result.cursorOffset());
    }

    private int cursorPosition(BookPaste.Result result) {
        return result.pages().subList(0, result.cursorPage()).stream().mapToInt(String::length).sum()
                + result.cursorOffset();
    }
}
