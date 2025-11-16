package fi.natroutter.foxbot.configs.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter @Setter
public class AIRequest {

    private boolean background;
    private List<String> include;
    private String input;
    private String instructions;
    private Text text;
    private int max_output_tokens;
    private int max_tool_calls;
    private Map<String, String> metadata;
    private String model;
    private boolean parallel_tool_calls;
    private String previous_response_id;
    private Prompt prompt;
    private String prompt_cache_key;
    private String prompt_cache_retention;
    private Reasoning reasoning;
    private String safety_identifier;
    private String service_tier;
    private boolean store;
    private boolean stream;
    private StreamOptions stream_options;
    private int temperature;
    //Tools here
    private int top_logprobs;
    private int top_p;
    private String truncation;


    @Getter @Setter
    public static class StreamOptions {
        private boolean include_obfuscation;
    }

    @Getter @Setter
    public static class Prompt {
        private String id;
        private String version;
    }

    @Getter @Setter
    public static class Text {
        private Format format;
    }

    @Getter @Setter
    public static class Reasoning {
        private Format format;
    }

    @Getter @Setter
    public static class Format {
        private String type;
    }

}