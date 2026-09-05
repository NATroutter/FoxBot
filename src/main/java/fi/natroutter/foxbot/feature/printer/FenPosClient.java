package fi.natroutter.foxbot.feature.printer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxlib.logger.FoxLogger;
import fi.natroutter.foxlib.logger.types.LogData;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.List;

/**
 * Submits print jobs to the FenPOS API. The device is fixed by the config, a key is only granted the
 * printers it may address anyway.
 */
public class FenPosClient {

    private final FoxLogger logger = FoxBot.getLogger();
    private final Gson gson = new Gson();
    private final Config.FenPos config;

    public FenPosClient(Config.FenPos config) {
        this.config = config;
    }

    public record Result(boolean success, String jobId, int lines, String error) {
        private static Result ok(String jobId, int lines) {
            return new Result(true, jobId, lines, null);
        }
        private static Result fail(String error) {
            return new Result(false, null, 0, error);
        }
    }

    public Result submit(List<String> data) {
        JsonObject requestBody = new JsonObject();
        requestBody.add("data", gson.toJsonTree(data));

        String url = config.getEndpoint() + "/api/v1/print/" + config.getAgent() + "/" + config.getDevice();

        try {
            Connection.Response response = Jsoup.connect(url)
                    .method(Connection.Method.POST)
                    .ignoreHttpErrors(true)
                    .ignoreContentType(true)
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + config.getToken())
                    .userAgent("FoxBot/1.0 (NATroutter)")
                    .requestBody(gson.toJson(requestBody))
                    .timeout(10000)
                    .execute();

            JsonObject json = gson.fromJson(response.body(), JsonObject.class);

            if (response.statusCode() != 202) {
                logger.error("Failed to submit print job",
                        new LogData("Status", String.valueOf(response.statusCode())),
                        new LogData("Body", response.body())
                );
                return Result.fail(explain(response.statusCode(), json));
            }

            return Result.ok(
                    json.has("jobId") ? json.get("jobId").getAsString() : "unknown",
                    json.has("lines") ? json.get("lines").getAsInt() : data.size()
            );

        } catch (IOException e) {
            logger.error("Failed to reach the print API : " + e.getMessage());
            return Result.fail("Couldn't reach the printer API!");
        } catch (JsonSyntaxException | IllegalStateException e) {
            logger.error("Failed to parse print API response: " + e.getMessage());
            return Result.fail("Received an invalid response from the printer API!");
        }
    }

    /**
     * Turns the API's refusal envelope into something worth showing in Discord. The operating
     * conditions get their own wording, anything else falls back to the message the API sent.
     */
    private String explain(int statusCode, JsonObject json) {
        String code = json != null && json.has("error") ? json.get("error").getAsString() : "";

        return switch (code) {
            case "agent_offline" -> "The printer's agent is offline!";
            case "device_unavailable" -> "The printer is not connected!";
            case "device_paused" -> "The printer is paused!";
            case "queue_full" -> "The printer's queue is full, try again in a moment!";
            case "unknown_device" -> "Printer not found, or the API key isn't granted it!";
            case "rate_limited" -> "The printer API is rate limiting us, try again in a moment!";
            //the image path: the API fetches the picture itself while the job compiles
            case "invalid_image" -> "That image isn't one the printer can read! (an interlaced PNG is the usual cause)";
            case "image_too_large" -> "That image is too large for one print job!";
            case "invalid_tag_argument" -> "Couldn't fetch your image, Discord may have expired the link!";
            case "too_many_output_lines" -> "That would print more paper than one job is allowed!";
            default -> {
                if (statusCode == 401 || statusCode == 403) {
                    yield "The printer API key is missing, invalid or lacks the \"jobs:submit\" permission!";
                }
                if (json != null && json.has("message")) {
                    yield json.get("message").getAsString();
                }
                yield "The printer API refused the job! (HTTP " + statusCode + ")";
            }
        };
    }

}
