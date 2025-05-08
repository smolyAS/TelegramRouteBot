package org.example;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.InputFile;

public class RouteBot extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "MyRoute_v1_bot"; // Замени на имя бота
    }

    @Override
    public String getBotToken() {
        return "7847754007:AAHerJpTiV6bXg8FFdvWS7oPGuiJ9yHw_8s"; // Замени на токен
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String[] parts = update.getMessage().getText().replace("/route ", "").split(";");
            if (parts.length != 2) {
                sendText(update.getMessage().getChatId(), "Неверный формат. Пример: /route Адрес1; Адрес2");
                return;
            }

            String from = parts[0].trim();
            String to = parts[1].trim();

            try {
                RouteService.RouteResult route = RouteService.buildRoute(from, to);

                StringBuilder text = new StringBuilder();
                text.append("🚗 Расстояние: ").append((int) route.distance()).append(" м\n");
                text.append("⏱️ Время: ").append((int) route.duration()).append(" сек\n\n");

                for (String step : route.instructions()) {
                    text.append("; ").append(step).append("\n");
                }

                sendPhoto(update.getMessage().getChatId(), route.mapUrl());
                sendText(update.getMessage().getChatId(), text.toString());

            } catch (Exception e) {
                e.printStackTrace();
                sendText(update.getMessage().getChatId(), "Ошибка при построении маршрута: " + e.getMessage());
            }
        }
    }

    private void sendText(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId.toString()).text(text).build());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendPhoto(Long chatId, String photoUrl) {
        try {
            System.out.println("📷 Отправляю фото по ссылке: " + photoUrl); // лог

            InputFile inputFile = new InputFile(photoUrl); // должен быть публичный URL!
            SendPhoto sendPhoto = SendPhoto.builder()
                    .chatId(chatId.toString())
                    .photo(inputFile)
                    .build();
            execute(sendPhoto);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
