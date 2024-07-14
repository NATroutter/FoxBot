package fi.natroutter.foxbot.configs.data;

import com.mongodb.client.model.Variable;
import fi.natroutter.foxbot.commands.Embed;
import fi.natroutter.foxbot.utilities.Utils;
import fi.natroutter.foxlib.FoxLib;
import lombok.Getter;
import lombok.Setter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter @Setter
public class EmbedData implements Cloneable{


    private Embed embed = new Embed();
    private List<Variable> variables = new ArrayList<>();

    @Getter @Setter
    public static class Embed {

        private Author author = new Author();
        private String title;
        private String url;
        private String description;
        private List<Field> fields = new ArrayList<>();
        private Image image = new Image();
        private Thumbnail thumbnail = new Thumbnail();
        private String color;
        private Long timestamp;
        private Boolean useCurrentTime;
        private Footer footer = new Footer();


        @Getter @Setter
        public static class Author {
            private String name;
            private String icon_url;
            private String url;
        }

        @Getter @Setter
        public static class Field {
            private String name;
            private String value;
            private Boolean inline;
            private Boolean blank;
        }

        @Getter @Setter
        public static class Image {
            private String url;
        }

        @Getter @Setter
        public static class Thumbnail {
            private String url;
        }

        @Getter @Setter
        public static class Footer {
            private String text;
            private String icon_url;
        }
    }

    @Getter @Setter
    public static class Variable {
        private String name;
        private String value;
    }

    @Override
    public String toString() {
        return String.format("EmbedData{embed=%s, variables=%s}",
                new StringBuilder()
                        .append("Embed{")
                        .append("author=").append(embed.getAuthor().getName()).append("/").append(embed.getAuthor().getIcon_url()).append("/").append(embed.getAuthor().getUrl())
                        .append(", title='").append(embed.getTitle()).append("'")
                        .append(", url='").append(embed.getUrl()).append("'")
                        .append(", description='").append(embed.getDescription()).append("'")
                        .append(", fields=").append(embed.getFields().stream().map(f -> String.format("Field{%s, %s, %b, %b}", f.getName(), f.getValue(), f.getInline(), f.getBlank())).collect(Collectors.joining(", ")))
                        .append(", image=").append(embed.getImage().getUrl())
                        .append(", thumbnail=").append(embed.getThumbnail().getUrl())
                        .append(", color='").append(embed.getColor()).append("'")
                        .append(", timestamp=").append(embed.getTimestamp())
                        .append(", useCurrentTime=").append(embed.getUseCurrentTime())
                        .append(", footer=").append(embed.getFooter().getText()).append("/").append(embed.getFooter().getIcon_url())
                        .append("}"),
                variables.stream().map(v -> String.format("Variable{%s, %s}", v.getName(), v.getValue())).collect(Collectors.joining(", "))
        );
    }

    @Override
    public EmbedData clone() {
        try {
            EmbedData cloned = (EmbedData) super.clone();

            // Deep copy of embed
            cloned.embed = new Embed();
            cloned.embed.setAuthor(new Embed.Author());
            cloned.embed.getAuthor().setName(this.embed.getAuthor().getName());
            cloned.embed.getAuthor().setIcon_url(this.embed.getAuthor().getIcon_url());
            cloned.embed.getAuthor().setUrl(this.embed.getAuthor().getUrl());
            cloned.embed.setTitle(this.embed.getTitle());
            cloned.embed.setUrl(this.embed.getUrl());
            cloned.embed.setDescription(this.embed.getDescription());

            // Deep copy of fields
            cloned.embed.setFields(new ArrayList<>());
            for (Embed.Field field : this.embed.getFields()) {
                Embed.Field clonedField = new Embed.Field();
                clonedField.setName(field.getName());
                clonedField.setValue(field.getValue());
                clonedField.setInline(field.getInline());
                clonedField.setBlank(field.getBlank());
                cloned.embed.getFields().add(clonedField);
            }

            // Deep copy of image, thumbnail, and footer
            cloned.embed.setImage(new Embed.Image());
            cloned.embed.getImage().setUrl(this.embed.getImage().getUrl());
            cloned.embed.setThumbnail(new Embed.Thumbnail());
            cloned.embed.getThumbnail().setUrl(this.embed.getThumbnail().getUrl());
            cloned.embed.setColor(this.embed.getColor());
            cloned.embed.setTimestamp(this.embed.getTimestamp());
            cloned.embed.setUseCurrentTime(this.embed.getUseCurrentTime());
            cloned.embed.setFooter(new Embed.Footer());
            cloned.embed.getFooter().setText(this.embed.getFooter().getText());
            cloned.embed.getFooter().setIcon_url(this.embed.getFooter().getIcon_url());

            // Deep copy of variables
            cloned.variables = new ArrayList<>();
            for (Variable variable : this.variables) {
                Variable clonedVariable = new Variable();
                clonedVariable.setName(variable.getName());
                clonedVariable.setValue(variable.getValue());
                cloned.variables.add(clonedVariable);
            }

            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cloning not supported for EmbedData", e);
        }
    }

    private String repl(String text, Placeholder placeholder){
        if (text == null) return null;
        if (placeholder == null) return text;

        String target = "{"+placeholder.getName()+"}";
        String replacement = placeholder.getValue().toString();
        return text.replace(target, replacement);
    }

    public MessageEmbed asEmbed() {
        return asEmbed((Placeholder) null);
    }
    public MessageEmbed asEmbed(Placeholder... placeholders) {
        EmbedBuilder builder;
        if (placeholders != null && placeholders.length > 0) {
            builder = asEmbedBuilder(placeholders);
        } else {
            builder = asEmbedBuilder();
        }
        if (!builder.isEmpty()) {
            return builder.build();
        }
        return null;
    }

    public EmbedBuilder asEmbedBuilder() {
        return asEmbedBuilder((Placeholder) null);
    }

    public EmbedBuilder asEmbedBuilder(Placeholder... placeholders) {
        EmbedBuilder em = new EmbedBuilder();

        Embed data = this.clone().getEmbed();

        //Parse placeholders
        if (placeholders != null) {
            for(Placeholder ph : placeholders) {
                Embed.Author author = data.author;
                if (author != null) {
                    author.name = repl(author.name, ph);
                    author.url = repl(author.url, ph);
                    author.icon_url = repl(author.icon_url, ph);
                }
                data.title = repl(data.title, ph);
                data.description = repl(data.description, ph);
                data.url = repl(data.url, ph);
                if (data.fields != null) {
                    for (Embed.Field field : data.fields) {
                        field.name = repl(field.name, ph);
                        field.value = repl(field.value, ph);
                    }
                }
                if (data.image != null) {
                    data.image.url = repl(data.image.url, ph);
                }
                if (data.thumbnail != null) {
                    data.thumbnail.url = repl(data.thumbnail.url, ph);
                }
                data.color = repl(data.color, ph);
                if (data.footer != null) {
                    data.footer.text = repl(data.footer.text, ph);
                    data.footer.icon_url = repl(data.footer.icon_url, ph);
                }
            }
        }


        //Build embed!!
        Embed.Author author = data.author;
        if (author != null) {
            if (author.name != null) {
                if (author.url != null) {
                    if (author.icon_url != null) {
                        em.setAuthor(author.name, author.url, author.icon_url);
                    } else {
                        em.setAuthor(author.name, author.url);
                    }
                } else {
                    em.setAuthor(author.name);
                }
            }
        }

        if (data.title != null) {
            em.setTitle(data.title);
        }
        if (data.url != null) {
            em.setUrl(data.url);
        }
        if (data.description != null) {
            em.setDescription(data.description);
        }
        if (data.fields != null) {
            for (Embed.Field field : data.fields) {
                if (field.blank != null && field.blank) {
                    em.addBlankField(Objects.requireNonNullElse(field.inline, false));
                } else {
                    if (field.name != null && field.value != null && field.inline != null) {
                        em.addField(field.name, field.value, field.inline);
                    }
                }
            }
        }
        if (data.image != null && data.image.url != null) {
            em.setImage(data.image.url);
        }
        if (data.thumbnail != null && data.thumbnail.url != null) {
            em.setThumbnail(data.thumbnail.url);
        }
        if (data.color != null) {
            em.setColor(Color.decode(data.color));
        }

        Embed.Footer footer = data.footer;
        if (footer != null) {
            if (footer.text != null) {
                if (footer.icon_url != null) {
                    em.setFooter(footer.text, footer.icon_url);
                } else {
                    em.setFooter(footer.text);
                }
            }
        }
        if (data.timestamp != null) {
            if (data.useCurrentTime) {
                em.setTimestamp(Utils.unix());
            } else {
                em.setTimestamp(Utils.unix(data.timestamp));
            }
        }
        return em;
    }


}