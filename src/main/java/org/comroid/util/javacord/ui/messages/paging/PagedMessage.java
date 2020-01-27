package org.comroid.util.javacord.ui.messages.paging;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.Messageable;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

public class PagedMessage {
    private final static ConcurrentHashMap<Messageable, PagedMessage> selfMap = new ConcurrentHashMap<>();
    private final static String PREV_PAGE_EMOJI = "⬅";
    private final static String NEXT_PAGE_EMOJI = "➡";
    private final static int SUITABLE_MAX_LENGTH = 1700;

    private Messageable parent;
    private Supplier<String> head, body;

    private Message lastMessage = null;
    private List<String> pages = new ArrayList<>();
    private int page;

    private PagedMessage(Messageable inParent, Supplier<String> head, Supplier<String> body) {
        this.parent = inParent;
        this.head = head;
        this.body = body;

        this.page = 0;

        resend();
    }

    public void refresh() {
        page = 0;

        refreshPage();
    }

    public void refreshPage() {
        refreshPages();

        if (lastMessage != null) {
            lastMessage.edit(
                    getPageContent()
            );
        }
    }

    public void resend() {
        refreshPages();

        if (lastMessage != null) {
            lastMessage.delete("Outdated");
        }

        parent.sendMessage(
                getPageContent()
        ).thenAcceptAsync(msg -> {
            lastMessage = msg;
            msg.addReactionAddListener(this::onPageClick);
            msg.addReactionRemoveListener(this::onPageClick);
            msg.addReaction(PREV_PAGE_EMOJI);
            msg.addReaction(NEXT_PAGE_EMOJI);
        });
    }

    private void onPageClick(SingleReactionEvent event) {
        if (!event.getUser().isYourself()) {
            switch (event.getEmoji().asUnicodeEmoji().orElse("")) {
                case PREV_PAGE_EMOJI:
                    if (page > 0)
                        page--;

                    this.refreshPage();
                    break;
                case NEXT_PAGE_EMOJI:
                    if (page < pages.size() - 1)
                        page++;

                    this.refreshPage();
                    break;
            }
        }
    }

    private String getPageContent() {
        return new StringBuilder()
                .append(pages.get(page))
                .append("\n\n")
                .append("`Page ")
                .append(page + 1)
                .append(" of ")
                .append(pages.size())
                .append(" | ")
                .append("Last Refresh: ")
                .append(new SimpleDateFormat("HH:mm:ss").format(new Timestamp(System.currentTimeMillis())))
                .append(" [")
                .append(Calendar.getInstance().getTimeZone().getDisplayName())
                .append("]`")
                .toString();
    }

    private void refreshPages() {
        String completeHead = head.get();
        String completeBody = body.get();
        String completeMessage = completeHead + completeBody;
        List<String> bodyLines = Arrays.asList(completeBody.split("\n"));
        StringBuilder pageBuilder;

        pages.clear();

        if (completeMessage.length() < SUITABLE_MAX_LENGTH) {
            pages.add(completeMessage);
        } else {
            pageBuilder = new StringBuilder(completeHead);

            for (int i = 0; i < bodyLines.size(); i++) {
                pageBuilder.append(bodyLines.get(i));
                pageBuilder.append("\n");

                if (i == bodyLines.size() - 1 || pageBuilder.length() + bodyLines.get(i + 1).length() >= SUITABLE_MAX_LENGTH) {
                    pages.add(pageBuilder.toString());
                    pageBuilder = new StringBuilder(completeHead);
                }
            }
        }
    }

    public final static PagedMessage get(Messageable forParent, Supplier<String> defaultHead, Supplier<String> defaultBody) {
        if (selfMap.containsKey(forParent)) {
            PagedMessage val = selfMap.get(forParent);
            val.resend();

            return val;
        } else {
            return selfMap.put(forParent,
                    new PagedMessage(
                            forParent,
                            defaultHead,
                            defaultBody
                    )
            );
        }
    }

    public final static Optional<PagedMessage> get(Messageable forParent) {
        if (selfMap.containsKey(forParent)) {
            PagedMessage val = selfMap.get(forParent);
            val.resend();

            return Optional.of(val);
        } else return Optional.empty();
    }
}
