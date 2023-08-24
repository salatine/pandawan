package salatine.pandawan.bot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.EmbedBuilder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.net.URL;
import java.net.URI;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.InterruptedException;

public class Bot extends ListenerAdapter {
    final static String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");
    final static String DANBOORU_LOGIN = System.getenv("DANBOORU_LOGIN");
    final static String DANBOORU_API_KEY = System.getenv("DANBOORU_API_KEY");
    final String DANBOORU_URL = "https://danbooru.donmai.us/posts.json?";
    public static void main(String[] args){
        JDA jda = JDABuilder.createDefault(DISCORD_TOKEN)
            .addEventListeners(new Bot())
            .setActivity(Activity.playing("pandinha"))
            .build();
        
        jda.updateCommands().addCommands(
            Commands.slash("ping", "calculate ping of the bot"),
            Commands.slash("kokomi", "get sangonomiya kokomi images from danbooru")
        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "ping":
                long time = System.currentTimeMillis();
                event.reply("pong!").setEphemeral(false)
                    .flatMap(v -> event.getHook().editOriginalFormat("pong: %dms", System.currentTimeMillis() - time)).queue();
                break;

            case "kokomi":
                event.deferReply().queue();
                String[] tags = {"sangonomiya_kokomi", "rating:general", "order:random"};
                try {
                    ByteBuffer[] images = getDanbooruImages(tags, 1);
                    ByteBuffer randomImageBytes = images[(int) (Math.random() * images.length)];
                    InputStream randomImage  = new ByteArrayInputStream(randomImageBytes.array());
                    FileUpload fileUpload = FileUpload.fromData(randomImage, "file.png");
                    MessageEmbed embed = new EmbedBuilder()
                        .setDescription("")
                        .setImage("attachment://file.png")
                        .build();
                        
                    event.getHook().sendMessageEmbeds(embed).addFiles(fileUpload).queue();
                    fileUpload.close();
                    
                } catch (DanbooruApiException | IOException e) {
                    event.getHook().sendMessage("There was an error while fetching the image");
                    e.printStackTrace();
                }

            default:
                break;
        }
    }

    public ByteBuffer[] getDanbooruImages(String[] tags, int limit) throws DanbooruApiException {
        String formattedTags = String.join("+", tags);
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(DANBOORU_URL + "tags=" + formattedTags + "&login=" + DANBOORU_LOGIN + "&api_key=" + DANBOORU_API_KEY + "&limit=" + limit))
            .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JSONArray posts = new JSONArray(response.body());
            ByteBuffer[] images = new ByteBuffer[posts.length()];
            for (int i = 0; i < posts.length(); i++) {
                JSONObject post = posts.getJSONObject(i);
                URL fileURL = new URL(post.getString("file_url"));
                byte[] image = fileURL.openStream().readAllBytes();
                images[i] = ByteBuffer.wrap(image);
            }

        return images;
        } catch (IOException | InterruptedException e) {
            throw new DanbooruApiException("error trying to download image", e);
        }
    }
}
