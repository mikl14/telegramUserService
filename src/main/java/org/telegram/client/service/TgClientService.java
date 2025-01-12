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
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
    static ArrayList<Long> lastChats = new ArrayList<Long>();
    public TgClientService() {

    }


    @Async
    public void Init()
    {
        try {
            Init.init();
            Log.setLogMessageHandler(1, new Slf4JLogMessageHandler());

            try (SimpleTelegramClientFactory clientFactory = new SimpleTelegramClientFactory()) {
                // Obtain the API token
                //
                APIToken apiToken = new APIToken(24861413, "6d2e6183de423431c34486f0bdd92371");
                //
                //  apiToken = APIToken.example();


                // Configure the client
                TDLibSettings settings = TDLibSettings.create(apiToken);

                // Configure the session directory.
                // After you authenticate into a session, the authentication will be skipped from the next restart!
                // If you want to ensure to match the authentication supplier user/bot with your session user/bot,
                //   you can name your session directory after your user id, for example: "tdlib-session-id12345"
                Path sessionPath = Paths.get("example-tdlight-session");
                settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
                settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

                // Prepare a new client builder
                SimpleTelegramClientBuilder clientBuilder = clientFactory.builder(settings);

                // Configure the authentication info
                // Replace with AuthenticationSupplier.consoleLogin(), or .user(xxx), or .bot(xxx);
                SimpleAuthenticationSupplier<?> authenticationData = AuthenticationSupplier.user("+79771183130");
                // This is an example, remove this line to use the real telegram datacenters!
                //  settings.setUseTestDatacenter(true);
                app = new TgApp(clientBuilder, authenticationData, adminId,botChatId);

            }
        } catch (Exception e) {
            System.out.println("Ex" + e);
        }
    }


    public void sendTxtMessage(long chatId, String message) throws ExecutionException, InterruptedException, TimeoutException {
        var req = new TdApi.SendMessage();
        req.chatId = chatId;
        var txt = new TdApi.InputMessageText();
        txt.text = new TdApi.FormattedText(message, new TdApi.TextEntity[0]);
        req.inputMessageContent = txt;
        app.getClient().sendMessage(req, true).get(1, TimeUnit.MINUTES);
    }

    public TdApi.Chat getChat(long chatId) throws ExecutionException, InterruptedException, TimeoutException {
        var req = new TdApi.GetChat(chatId);
        TdApi.Chat chat = app.getClient().send(req).get(1, TimeUnit.MINUTES);
        return chat;
    }
    public long[] getChatList() throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.GetChats req = new TdApi.GetChats(new TdApi.ChatListMain(),50);

        TdApi.Chats chats = app.getClient().send(req).get(1, TimeUnit.MINUTES);

        return chats.chatIds;
    }
    public long[] FindAChats(String query) throws ExecutionException, InterruptedException, TimeoutException {
        TdApi.SearchPublicChats req = new TdApi.SearchPublicChats(query);
        TdApi.Chats res = app.getClient().send(req).get(1, TimeUnit.MINUTES);
        return res.chatIds;
    }

    public TdApi.Chat findAChatByName(String username) throws ExecutionException, InterruptedException, TimeoutException {
        if(!username.startsWith("@"))
        {
            StringBuilder sb = new StringBuilder("@"+username);
            username = sb.toString();
        }
        TdApi.SearchPublicChat req = new TdApi.SearchPublicChat(username);
        TdApi.Chat res = app.getClient().send(req).get(1, TimeUnit.MINUTES);
        return res;
    }

    public boolean joinChat(long chatId) throws ExecutionException, InterruptedException, TimeoutException {
        long[] chats = getChatList();
        if(Arrays.stream(chats).noneMatch(value -> value == chatId)) {
            try {
                TdApi.JoinChat req = new TdApi.JoinChat(chatId);
                app.getClient().send(req).get(1, TimeUnit.MINUTES);
                return true;
            }
            catch (Exception e)
            {
                return false;
            }
        }
        return true;
    }

    public void forwardMessage(long chatId, long messageId, long targetChatId) {
        // Создание объекта ForwardMessage
        TdApi.ForwardMessages forwardMessage = new TdApi.ForwardMessages();
        forwardMessage.fromChatId = chatId; // ID исходного чата
        forwardMessage.messageIds = new long[]{messageId}; // ID сообщения для пересылки
        forwardMessage.chatId = targetChatId; // ID целевого чата

        app.getClient().send(forwardMessage);
    }

    public TdApi.Message getLastMessage(long chatId) throws ExecutionException, InterruptedException, TimeoutException {

        TdApi.Chat chat = getChat(chatId);
        return chat.lastMessage;
    }
    public static class TgApp implements AutoCloseable {

        private final SimpleTelegramClient client;

        /**
         * Admin user id, used by the stop command example
         */
        private final long adminId;

        private final long botId;
        public TgApp(SimpleTelegramClientBuilder clientBuilder,
                          SimpleAuthenticationSupplier<?> authenticationData,
                          long adminId, long botId) {
            this.adminId = adminId;
            this.botId = botId;
            // Add an example update handler that prints when the bot is started
            clientBuilder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onUpdateAuthorizationState);

            // Add an example command handler that stops the bot
            clientBuilder.addCommandHandler("stop", this::onStopCommand);

            // Add an example update handler that prints every received message
            clientBuilder.addUpdateHandler(TdApi.UpdateNewMessage.class, this::onUpdateNewMessage);

            // Build the client
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
         * Print the bot status
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

        /**
         * Print new messages received via updateNewMessage
         */
        private void onUpdateNewMessage(TdApi.UpdateNewMessage update) {
            // Get the message content
            TdApi.MessageContent messageContent = update.message.content;

            // Get the message text
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
                            if(chatId != 7613327420L) {
                                lastChats.add(chatId);
                                TdApi.ForwardMessages forwardMessage = new TdApi.ForwardMessages();
                                forwardMessage.fromChatId = chatId; // ID исходного чата
                                forwardMessage.messageIds = new long[]{update.message.id}; // ID сообщения для пересылки
                                forwardMessage.chatId = botId; // ID целевого чата

                                client.send(forwardMessage);
                                System.out.printf("Received new message from chat %s (%s): %s%n", title, chatId, text);
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
