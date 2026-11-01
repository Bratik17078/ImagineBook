package io.github.jumperonjava.imaginebook.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Plans a paste before changing the book, so a full book never silently loses text. */
public final class BookPaste {
    private BookPaste() {}

    public record Result(List<String> pages, int cursorPage, int cursorOffset) {}

    public static Optional<Result> plan(String page, int selectionStart, int selectionEnd,
                                        String paste, int availablePages, int characterLimit,
                                        Predicate<String> fitsOnPage) {
        String text = page.substring(0, selectionStart) + paste + page.substring(selectionEnd);
        int insertionEnd = selectionStart + paste.length();
        List<String> pages = new ArrayList<>();
        int consumed = 0;
        int cursorPage = 0;
        int cursorOffset = 0;
        do {
            if (pages.size() >= availablePages) return Optional.empty();
            String remaining = text.substring(consumed);
            int bound = Math.min(characterLimit, remaining.length());
            // Search by code points so supplementary characters stay intact at page boundaries.
            if (bound > 0 && bound < remaining.length()
                    && Character.isHighSurrogate(remaining.charAt(bound - 1))) bound--;
            int low = 0;
            int high = remaining.codePointCount(0, bound);
            while (low < high) {
                int mid = (low + high + 1) / 2;
                int end = remaining.offsetByCodePoints(0, mid);
                if (fitsOnPage.test(remaining.substring(0, end))) low = mid;
                else high = mid - 1;
            }
            int end = remaining.offsetByCodePoints(0, low);
            if (!remaining.isEmpty() && end == 0) return Optional.empty();
            if (end < remaining.length()) {
                // Prefer a nearby word or paragraph boundary, retaining every whitespace character.
                for (int i = end - 1; i >= end / 2; i--) {
                    if (Character.isWhitespace(remaining.charAt(i))) {
                        end = i + 1;
                        break;
                    }
                }
            }
            pages.add(remaining.substring(0, end));
            if (insertionEnd > consumed && insertionEnd <= consumed + end || insertionEnd == 0 && consumed == 0) {
                cursorPage = pages.size() - 1;
                cursorOffset = insertionEnd - consumed;
            }
            consumed += end;
        } while (consumed < text.length());
        return Optional.of(new Result(List.copyOf(pages), cursorPage, cursorOffset));
    }
}
