package salatine.pandawan.bot                                                                                                                                               ;

import net.dv8tion.jda.api.JDA                                                                                                                                              ;
import net.dv8tion.jda.api.JDABuilder                                                                                                                                       ;
import net.dv8tion.jda.api.hooks.ListenerAdapter                                                                                                                            ;
import net.dv8tion.jda.api.interactions.commands.Command                                                                                                                    ;
import net.dv8tion.jda.api.interactions.commands.build.CommandData                                                                                                          ;
import net.dv8tion.jda.api.interactions.commands.build.Commands                                                                                                             ;
import net.dv8tion.jda.api.utils.FileUpload                                                                                                                                 ;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent                                                                                          ;
import net.dv8tion.jda.api.entities.*                                                                                                                                       ;
import net.dv8tion.jda.api.EmbedBuilder                                                                                                                                     ;
import java.net.http.HttpClient                                                                                                                                             ;
import java.net.http.HttpRequest                                                                                                                                            ;
import java.net.http.HttpResponse                                                                                                                                           ;
import java.nio.ByteBuffer                                                                                                                                                  ;
import java.net.URL                                                                                                                                                         ;
import java.net.URI                                                                                                                                                         ;
import org.json.JSONObject                                                                                                                                                  ;
import org.json.JSONArray                                                                                                                                                   ;
import java.io.InputStream                                                                                                                                                  ;
import java.io.ByteArrayInputStream                                                                                                                                         ;
import java.io.IOException                                                                                                                                                  ;
import java.lang.InterruptedException                                                                                                                                       ;
import java.util.*                                                                                                                                                          ;

public class Bot extends ListenerAdapter                                                                                                                                    {
    final static String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN")                                                                                                      ;
    final static String DANBOORU_LOGIN = System.getenv("DANBOORU_LOGIN")                                                                                                    ;
    final static String DANBOORU_API_KEY = System.getenv("DANBOORU_API_KEY")                                                                                                ;
    final static boolean USE_GUILD_COMMANDS = Optional.ofNullable(System.getenv("USE_GUILD_COMMANDS"))
            .map((value) -> value.equals("true"))
            .orElse(false)                                                                                                                                                  ;
    final static Optional<Long> GUILD_ID = Optional.ofNullable(System.getenv("GUILD_ID"))
            .map(Long::parseLong)                                                                                                                                           ;
    final String DANBOORU_URL = "https://danbooru.donmai.us/posts.json?"                                                                                                    ;
    public static void main(String[] args) throws InterruptedException                                                                                                      {
        JDA jda = JDABuilder.createDefault(DISCORD_TOKEN)
            .addEventListeners(new Bot())
            .setActivity(Activity.playing("pandinha"))
            .build()                                                                                                                                                        ;

        var commands = new CommandData[]                                                                                                                                    {
            Commands.slash("ping", "calculate ping of the bot"),
            Commands.slash("kokomi", "get sangonomiya kokomi images from danbooru"),
            Commands.slash("wave", "get a waving image from danbooru"),
            Commands.slash("miku", "get hatsune miku images from danbooru"),
            Commands.slash("sophie", "get sophie images from danbooru")
        }                                                                                                                                                                   ;

        if (USE_GUILD_COMMANDS && GUILD_ID.isPresent())                                                                                                                     {
            var guild = jda.awaitReady().getGuildById(GUILD_ID.get())                                                                                                       ;

            if (guild == null)                                                                                                                                              {
                throw new RuntimeException("could not find guild with id " + GUILD_ID.get())                                                                                ;
                                                                                                                                                                            }

            guild.updateCommands().addCommands(commands).queue()                                                                                                            ;
        } else                                                                                                                                                              {
            jda.updateCommands().addCommands(commands).queue()                                                                                                              ;
                                                                                                                                                                            }
        
                                                                                                                                                                            }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)                                                                                               {
        switch (event.getName())                                                                                                                                            {
            case "ping":
                long time = System.currentTimeMillis()                                                                                                                      ;
                event.reply("pong!").setEphemeral(false)
                    .flatMap(v -> event.getHook().editOriginalFormat("pong: %dms", System.currentTimeMillis() - time)).queue()                                              ;
                break                                                                                                                                                       ;

            case "kokomi":
                sendRandomDanbooruImageWithTags(event, List.of("sangonomiya_kokomi", "rating:general"))                                                                     ;
                break                                                                                                                                                       ;

            case "wave":
                sendRandomDanbooruImageWithTags(event, List.of("waving", "is:gif", "rating:general"), List.of("animated"))                                                  ;
                break                                                                                                                                                       ;

            case "miku":
                sendRandomDanbooruImageWithTags(event, List.of("hatsune_miku", "rating:general"), List.of("solo"))                                                          ;
                break                                                                                                                                                       ;

            case "sophie":
                sendRandomDanbooruImageWithTags(event, List.of("sophia_(p5s)", "rating:general"))                                                                           ;
                break                                                                                                                                                       ;
                                                                                                                                                                            }
                                                                                                                                                                            }

    private void sendRandomDanbooruImageWithTags(SlashCommandInteractionEvent event, Collection<String> tags)                                                               {
        sendRandomDanbooruImageWithTags(event, tags, List.of())                                                                                                             ;
                                                                                                                                                                            }

    private void sendRandomDanbooruImageWithTags(SlashCommandInteractionEvent event, Collection<String> tags, Collection<String> additionalFilters)                         {
        event.deferReply().queue()                                                                                                                                          ;

        var tagsPlusRandomOrder = new ArrayList<>(tags)                                                                                                                     ;
        tagsPlusRandomOrder.add("order:random")                                                                                                                             ;

        try                                                                                                                                                                 {
            var images = getDanbooruImages(tagsPlusRandomOrder, additionalFilters, 1)                                                                                       ;
            var randomImage = images.get((int) (Math.random() * images.size()))                                                                                             ;
            var randomImageInputStream  = new ByteArrayInputStream(randomImage.contents().array())                                                                          ;

            var fileUpload = FileUpload.fromData(randomImageInputStream, "file." + randomImage.extension())                                                                 ;
            var embed = new EmbedBuilder()
                    .setDescription("")
                    .setImage("attachment://file." + randomImage.extension())
                    .build()                                                                                                                                                ;

            event.getHook().sendMessageEmbeds(embed).addFiles(fileUpload).queue()                                                                                           ;
            fileUpload.close()                                                                                                                                              ;
        } catch (DanbooruApiException | IOException e)                                                                                                                      {
            event.getHook().sendMessage("There was an error while fetching the image").queue()                                                                              ;
            e.printStackTrace()                                                                                                                                             ;
                                                                                                                                                                            }
                                                                                                                                                                            }

    private List<DanbooruImage> getDanbooruImages(
        Collection<String> tags,
        Collection<String> additionalTags,
        int limit
    ) throws DanbooruApiException                                                                                                                                           {
        var formattedTags = String.join("+", tags)                                                                                                                          ;
        var client = HttpClient.newHttpClient()                                                                                                                             ;

        var postsToBeRead = 50                                                                                                                                              ;
        var request = HttpRequest.newBuilder()
            .uri(URI.create(DANBOORU_URL + "tags=" + formattedTags + "&login=" + DANBOORU_LOGIN + "&api_key=" + DANBOORU_API_KEY + "&limit=" + postsToBeRead))
            .build()                                                                                                                                                        ;

        try                                                                                                                                                                 {
            var response = client.send(request, HttpResponse.BodyHandlers.ofString())                                                                                       ;
            var posts = new JSONArray(response.body())                                                                                                                      ;
            var images = new ArrayList<DanbooruImage>()                                                                                                                     ;
            var remainingImages = limit                                                                                                                                     ;

            for (int i = 0; i < posts.length(); i++)                                                                                                                        {
                var post = posts.getJSONObject(i)                                                                                                                           ;
                var postTags = Set.of(post.getString("tag_string").split(" "))                                                                                              ;

                if (!postTags.containsAll(additionalTags))                                                                                                                  {
                    continue                                                                                                                                                ;
                                                                                                                                                                            }

                var fileURL = new URL(post.getString("file_url"))                                                                                                           ;

                try (var stream = fileURL.openStream())                                                                                                                     {
                    var extension = getExtension(fileURL.getPath())                                                                                                         ;
                    var contents = ByteBuffer.wrap(stream.readAllBytes())                                                                                                   ;

                    var image = new DanbooruImage(extension, contents)                                                                                                      ;

                    images.add(image)                                                                                                                                       ;
                    remainingImages--                                                                                                                                       ;

                    if (remainingImages == 0)                                                                                                                               {
                        break                                                                                                                                               ;
                                                                                                                                                                            }
                                                                                                                                                                            }
                                                                                                                                                                            }

            return images                                                                                                                                                   ;
        } catch (Exception e)                                                                                                                                               {
            throw new DanbooruApiException("error trying to download image", e)                                                                                             ;
                                                                                                                                                                            }
                                                                                                                                                                            }

    private String getExtension(String path)                                                                                                                                {
        var parts = path.split("\\.")                                                                                                                                       ;

        return parts[parts.length - 1]                                                                                                                                      ;
                                                                                                                                                                            }
                                                                                                                                                                            }
