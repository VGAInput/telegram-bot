package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.entity.NotificationTask;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.time.LocalDateTime.parse;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final Pattern pattern = Pattern.compile(
            "([0-9\\.\\:\\s]{16})(\\s)([\\W+]+)");
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @Autowired
    private final TelegramBot tgBot;
    private final NotificationTaskService ntService;

    public TelegramBotUpdatesListener(TelegramBot tgBot, NotificationTaskService ntService) {
        this.tgBot = tgBot;
        this.ntService = ntService;
    }

    @PostConstruct
    public void init() {
        tgBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.stream().filter(update -> update.message() != null).forEach(update -> {
            logger.info("Processing update: {}", update);
            Message msg = update.message();
            Long chatId = msg.chat().id();
            String text = msg.text();

            if ("/start".equals(text)) {
                startMessage(chatId, "Приветствую, готов к работе.");
            } else if (text != null) {
                    Matcher matcher = pattern.matcher(text);
                    if (matcher.find()) {
                        LocalDateTime dt = parse(matcher.group(1));
                        if (Objects.isNull(dt)) {
                            sendMessage(chatId, "Некорректный формат даты и/или времени.");
                        } else {
                            String text_sec = matcher.group(3);
                            NotificationTask nt = new NotificationTask();
                            nt.setId(chatId);
                            nt.setMessage(text_sec);
                            nt.setNotificationDateTime(dt);
                            ntService.saveNotification(nt);
                            sendMessage(chatId, "Запись сохранена.");
                        }
                    } else {
                        sendMessage(chatId, "Некорректный формат сообщения.");
                    }

            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }
    @Nullable
    private LocalDateTime parse(String dateTime){
      try{
          return LocalDateTime.parse(dateTime,dtf);
      }catch (DateTimeParseException e){
          return null;
      }
    };

    private void startMessage(Long chatId, String message) {
        sendMessage(chatId, message);
    }

    private void sendMessage(Long chatId, String message) {
        SendMessage sendMessage = new SendMessage(chatId, message);
        SendResponse sendResponse = tgBot.execute(sendMessage);
        if (!sendResponse.isOk()) logger.error("ERROR SENDING MESSAGE: {}", sendResponse.description());
    }


}
