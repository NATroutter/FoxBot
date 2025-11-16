package fi.natroutter.foxbot.data;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class AIResponse {

    private String id;
    private String object;
    private long created_at;
    private String status;
    private Object error;
    private Object incomplete_details;
    private Object instructions;
    private Object max_output_tokens;
    private String model;
    private List<Output> output;
    private boolean parallel_tool_calls;
    private Object previous_response_id;
    private Reasoning reasoning;
    private boolean store;
    private double temperature;
    private Text text;
    private String tool_choice;
    private List<Object> tools;
    private double top_p;
    private String truncation;
    private Usage usage;
    private Object user;
    private Map<String, Object> metadata;

    @Getter @Setter
    public static class Output {
        private String type;
        private String id;
        private String status;
        private String role;
        private List<Content> content;
    }

    @Getter @Setter
    public static class Content {
        private String type;
        private String text;
        private List<Object> annotations;
    }

    @Getter @Setter
    public static class Reasoning {
        private Object effort;
        private Object summary;
    }

    @Getter @Setter
    public static class Text {
        private Format format;
    }

    @Getter @Setter
    public static class Format {
        private String type;
    }

    @Getter @Setter
    public static class Usage {
        private int input_tokens;
        private InputTokensDetails input_tokens_details;
        private int output_tokens;
        private OutputTokensDetails output_tokens_details;
        private int total_tokens;
    }

    @Getter @Setter
    public static class InputTokensDetails {
        private int cached_tokens;
    }

    @Getter @Setter
    public static class OutputTokensDetails {
        private int reasoning_tokens;
    }
}