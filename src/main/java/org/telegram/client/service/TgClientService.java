package org.telegram.client.service;

import it.tdlight.Init;
import it.tdlight.Log;
import it.tdlight.Slf4JLogMessageHandler;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.config.ScheduledTask;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Data
@PropertySource("application.properties")
public class TgClientService {

    long adminId = Integer.getInteger("adminId", 536034324);
    TgApp app;

    @Getter
    @Setter
    @Value("${tg.bot.chat.id}")
    long botChatId;

    @Getter
    @Setter
    @Value("${api.id}")
    int apiId;

    @Getter
    @Setter
    @Value("${api.hash}")
    String apiHash;

    @Getter
    @Setter
    @Value("${user.number}")
    String userNumber;

    public TgClientService() {

    }

    /**
     * <b>Init</b> - инициализация, сюда нужно вбить все данные пользователя
     */
    @Async
    public void Init() {
        try {
            Init.init();
            Log.setLogMessageHandler(1, new Slf4JLogMessageHandler());

            try (SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory()) {

                APIToken apiToken = new APIToken(apiId, apiHash);

                TDLibSettings settings = TDLibSettings.create(apiToken);

                Path sessionPath = Paths.get("example-tdlight-session");
                settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
                settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

                SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);
                SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.user(userNumber);

                app = new TgApp(clientBuilder, authenticationData, adminId, botChatId);

            }
        } catch (Exception e) {
            System.out.println("Ex" + e);
        }
    }

    /**
     * <b>getChat</b> - возвращает чат пользователя по chatId
     * @param chatId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public TdApi.Chat getChat(long chatId) throws ExecutionException, InterruptedException, TimeoutException {
        var req = new TdApi.GetChat(chatId);
        TdApi.Chat chat = app.getClient().send(req).get(1, TimeUnit.MINUTES);
        return chat;
    }

    /**
     * <b>getChatList</b> - возвращает массив ChatId всех чатов пользователя
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public long[] getChatList() throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.GetChats req = new TdApi.GetChats(new TdApi.ChatListMain(), Integer.MAX_VALUE);

        TdApi.Chats chats = app.getClient().send(req).get(1, TimeUnit.MINUTES);

        return chats.chatIds;
    }


    /**
     * <b>leaveChatByChatId</b> - выходит из чата по chatID
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void leaveChatByChatId(long chatId) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.LeaveChat req = new TdApi.LeaveChat(chatId);
        TdApi.Ok res = app.getClient().send(req).get(1, TimeUnit.MINUTES);
    }

    /**
     * <b>FindAChats</b> - Возвращает массив публичных чатов найденных по запросу
     * @param query - любой поисковой запрос
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public long[] FindAChats(String query) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.SearchPublicChats req = new TdApi.SearchPublicChats(query);
        TdApi.Chats res = app.getClient().send(req).get(1, TimeUnit.MINUTES);
        return res.chatIds;
    }

    /**
     * <b>findAChatByName</b> - находит чат по username, воспринимает формат @userName и username
     * @param username
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public TdApi.Chat findAChatByName(String username) throws ExecutionException, InterruptedException, TimeoutException {
        if (!username.startsWith("@")) {
            StringBuilder sb = new StringBuilder("@" + username);
            username = sb.toString();
        }
        TdApi.SearchPublicChat req = new TdApi.SearchPublicChat(username);
        TdApi.Chat res = app.getClient().send(req).get(1, TimeUnit.MINUTES);
        return res;
    }

    /**
     * <b>muteChat</b> - Отключает любые уведомления в чате
     * @param chatId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void muteChat(long chatId) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.SetChatNotificationSettings notificationSettings = new TdApi.SetChatNotificationSettings();

        TdApi.ChatNotificationSettings notification = new TdApi.ChatNotificationSettings();
        notification.disableMentionNotifications = true;
        notification.disablePinnedMessageNotifications = true;
        notification.soundId = 0;
        notification.muteFor = Integer.MAX_VALUE;

        notificationSettings.chatId = chatId;
        notificationSettings.notificationSettings = notification;

        app.getClient().send(notificationSettings).get(1, TimeUnit.MINUTES);
    }

    /**
     * <b>joinPrivateChat</b> - Присоединяет пользователя в закрытый канал по ссылке приглашению
     * @param inviteLink
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public TdApi.Chat joinPrivateChat(String inviteLink) throws ExecutionException, InterruptedException, TimeoutException {
        inviteLink = inviteLink.replace("\"", "");
        try {

            TdApi.JoinChatByInviteLink joinChatByInviteLink = new TdApi.JoinChatByInviteLink(inviteLink);
            var res = app.getClient().send(joinChatByInviteLink).get(1, TimeUnit.MINUTES);


            muteChat(res.id);
            return res;
        } catch (ExecutionException e) {

        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * <b>joinChat</b> - Присоединяет пользователя в публичный канал по его id
     * @param chatId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public boolean joinChat(long chatId) throws ExecutionException, InterruptedException, TimeoutException {
        long[] chats = getChatList();
        if (Arrays.stream(chats).noneMatch(value -> value == chatId)) {
            try {
                TdApi.JoinChat req = new TdApi.JoinChat(chatId);
                app.getClient().send(req).get(1, TimeUnit.MINUTES);

                muteChat(chatId);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }

    /**
     * <b>forwardMessage</b> - Пересылает сообщение
     * @param chatId - куда пересылаем
     * @param messageId - что пересылаем
     * @param targetChatId - откуда пересылаем
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public void forwardMessage(long chatId, long messageId, long targetChatId) {
        // Создание объекта ForwardMessage
        TdApi.ForwardMessages forwardMessage = new TdApi.ForwardMessages();
        forwardMessage.fromChatId = chatId; // ID исходного чата
        forwardMessage.messageIds = new long[]{messageId}; // ID сообщения для пересылки
        forwardMessage.chatId = targetChatId; // ID целевого чата

        app.getClient().send(forwardMessage);
    }

    /**
     * <b>getLastMessage</b> - Возвращает последнее сообщение из чата
     * @param chatId
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws TimeoutException
     */
    public TdApi.Message getLastMessage(long chatId) throws ExecutionException, InterruptedException, TimeoutException {

        TdApi.Chat chat = getChat(chatId);
        return chat.lastMessage;
    }

    public static class TgApp implements AutoCloseable {

        private final SimpleTelegramClient client;

        private final long adminId;

        private final long botId;

        public TgApp(SimpleTelegramClientBuilder clientBuilder,
                     SimpleAuthenticationSupplier<?> authenticationData,
                     long adminId, long botId) {
            this.adminId = adminId;
            this.botId = botId;

            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onUpdateAuthorizationState);

            clientBuilder.addCommandHandler("stop", this::onStopCommand);

            clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, this::onUpdateNewMessage);

            this.client = clientBuilder.build(authenticationData);
        }

        @Override
        public void close() throws Exception {
            client.close();
        }

        public SimpleTelegramClient getClient() {
            return client;
        }

        /**
         * Выводим статус
         */
        private void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
            TdApi.AuthorizationState authorizationState = update.authorizationState;
            if (authorizationState instanceof TdApi.AuthorizationStateReady) {
                System.out.println("Logged in");
            } else if (authorizationState instanceof TdApi.AuthorizationStateClosing) {
                System.out.println("Closing...");
            } else if (authorizationState instanceof TdApi.AuthorizationStateClosed) {
                System.out.println("Closed");
            } else if (authorizationState instanceof TdApi.AuthorizationStateLoggingOut) {
                System.out.println("Logging out...");
            }
        }

        Map<Long,List<TdApi.Message>> messageMap = new HashMap<>();
        /**
         * Print new messages received via updateNewMessage
         */
        private void onUpdateNewMessage(TdApi.UpdateNewMessage update) {
            // Get the message content
            TdApi.MessageContent messageContent = update.message.content;

            String text;
            if (messageContent instanceof TdApi.MessageText messageText) {
                // Get the text of the text message
                text = messageText.text.text;
            } else {
                // We handle only text messages, the other messages will be printed as their type
                text = String.format("(%s)", messageContent.getClass().getSimpleName());
            }

            long chatId = update.message.chatId;

            // Get the chat title
            client.send(new TdApi.GetChat(chatId))
                    // Use the async completion handler, to avoid blocking the TDLib response thread accidentally
                    .whenCompleteAsync((chatIdResult, error) -> {
                        if (error != null) {
                            // Print error
                            System.err.printf("Can't get chat title of chat %s%n", chatId);
                            error.printStackTrace(System.err);
                        } else {
                            // Get the chat name
                            String title = chatIdResult.title;

                            // Print the message
                            if (chatId != botId) {
                                long albumId = update.message.mediaAlbumId;

                                if (albumId != 0) {

                                        TdApi.Message mes = new TdApi.Message();
                                        mes.chatId = update.message.chatId;
                                        mes.mediaAlbumId = update.message.mediaAlbumId;
                                        mes.id = update.message.id;
                                        mes.content = update.message.content;
                                        if(messageMap.containsKey(mes.mediaAlbumId))
                                        {
                                            try {
                                                List<TdApi.Message> messageList = messageMap.get(mes.mediaAlbumId);
                                                messageList.add(mes);
                                                System.out.println("ss");
                                            }
                                            catch (Exception e)
                                            {
                                                System.out.println("sq");
                                            }

                                        }
                                        else
                                        {
                                            List<TdApi.Message> messageList = new ArrayList<>();
                                            messageList.add(mes);
                                            messageMap.put(mes.mediaAlbumId,messageList);
                                        }
                                }
                                else {

                                    if(!messageMap.isEmpty())
                                    {
                                        for(long key : messageMap.keySet())
                                        {
                                            long[] mesIds = messageMap.get(key).stream().mapToLong(a-> a.id).toArray();

                                            TdApi.ForwardMessages forwardMessage = new TdApi.ForwardMessages();
                                            forwardMessage.fromChatId = messageMap.get(key).get(0).chatId; // ID исходного чата
                                            forwardMessage.messageIds = mesIds; // ID сообщения для пересылки
                                            forwardMessage.messageThreadId = messageMap.get(key).get(0).messageThreadId;
                                            forwardMessage.chatId = botId; // ID целевого чата
                                            client.send(forwardMessage);
                                            System.out.printf("Received new MediaGroupMessage");
                                        }
                                        messageMap.clear();
                                    }

                                    TdApi.ForwardMessages forwardMessage = new TdApi.ForwardMessages();

                                    forwardMessage.fromChatId = chatId; // ID исходного чата
                                    forwardMessage.messageIds = new long[]{update.message.id}; // ID сообщения для пересылки
                                    forwardMessage.messageThreadId = update.message.messageThreadId;
                                    forwardMessage.chatId = botId; // ID целевого чата
                                    client.send(forwardMessage);
                                    System.out.printf("Received new message from chat %s (%s): %s%n", title, chatId, text);

                                }

                            }


                        }
                    });
        }


        /**
         * Close the bot if the /stop command is sent by the administrator
         */
        private void onStopCommand(TdApi.Chat chat, TdApi.MessageSender commandSender, String arguments) {
            // Check if the sender is the admin
            if (isAdmin(commandSender)) {
                // Stop the client
                System.out.println("Received stop command. closing...");
                client.sendClose();
            }
        }

        /**
         * Check if the command sender is admin
         */
        public boolean isAdmin(TdApi.MessageSender sender) {
            if (sender instanceof TdApi.MessageSenderUser messageSenderUser) {
                return messageSenderUser.userId == adminId;
            } else {
                return false;
            }
        }

    }
}
