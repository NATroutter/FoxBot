package fi.natroutter.foxbot.handlers;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.commands.Fox;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ConsoleClient {

    private Scanner scanner = new Scanner(System.in);
    private BotHandler bot;

    private Guild guild = null;
    private TextChannel channel = null;

    private List<Guild> guilds = new ArrayList<>();
    private List<TextChannel> channels = new ArrayList<>();

    public ConsoleClient(BotHandler bot) {
        this.bot = bot;
        loop();
    }

    public void loop() {
        print("> ");
        String input = scanner.nextLine();

        if (bot.isConnected()) {
            guilds = bot.getJda().getGuilds();
            if (guilds.size() > 0 && guild != null) {
                Guild g = bot.getJda().getGuildById(guild.getIdLong());
                if (g != null) {
                    channels = g.getTextChannels();
                }
            }
        }

        String[] args = input.split(" ");
        String command = args[0];
        if (args.length > 1) {
            args = Arrays.copyOfRange(args, 1, args.length);
        } else {
            args = new String[0];
        }

        switch (command.toLowerCase()) {
            case "help" -> {
                println("======= FoxBot Console Client =======");
                println("Version: " + FoxBot.getVer());
                println("Author: NATroutter");
                println(" ");
                println("Selected:");
                println("  Guild: " + (guild != null ? guild.getName() + " (" + guild.getId() + ")" : "None"));
                println("  Channel: " + (channel != null ? channel.getName() + " (" + channel.getId() + ")" : "None"));
                println(" ");
                println("Commands:");
                println("  help - Shows this message");
                println("  stop - Stops the bot");
                println("  exit - Disconnects the bot and shutdowns the client");
                println("  guilds/gs - Shows list of guilds");
                println("  channels/chs - Shows list of channels");
                println("  select/sel - Changes channel or guild");
                println("    - guild/g <num> - Changes guild");
                println("    - channel/c <num> - Changes channel");
                println("  say <message> - Sends message to current channel");
            }
            case "guilds","gs" -> {
                if (guilds.size() > 0) {
                    println("Guilds:");
                    for (int i = 0; i < guilds.size(); i++) {
                        println("  " + i + ": " + guilds.get(i).getName());
                    }
                } else {
                    println("No guilds found!");
                }
            }
            case "channels","chs" -> {
                if (guild == null) {
                    println("No guild selected!");
                    break;
                }
                if (channels.size() > 0) {
                    println("Channels:");
                    for (int i = 0; i < channels.size(); i++) {
                        println("  " + i + ": " + channels.get(i).getName());
                    }
                } else {
                    println("No channels found!");
                }
            }
            case "select","sel" -> {
                if (args.length < 2) {
                    println("Usage: select <channel/guild> <num>");
                    break;
                }
                String type = args[0];
                int num = Integer.parseInt(args[1]);

                if (type.equalsIgnoreCase("guild") || type.equalsIgnoreCase("g")) {
                    if (guilds.size() > 0) {
                        if (num < guilds.size()) {
                            guild = guilds.get(num);
                            println("Selected guild: " + guild.getName() + " (" + guild.getId() + ")");
                        } else {
                            println("Invalid guild number!");
                        }
                    } else {
                        println("No guilds found!");
                    }
                } else if (type.equalsIgnoreCase("channel") || type.equalsIgnoreCase("c")) {
                    if (guild == null) {
                        println("No guild selected!");
                        break;
                    }
                    if (channels.size() > 0) {
                        if (num < channels.size()) {
                            channel = channels.get(num);
                            println("Selected channel: " + channel.getName() + " (" + channel.getId() + ")");
                        } else {
                            println("Invalid channel number!");
                        }
                    } else {
                        println("No channels found!");
                    }
                } else {
                    println("Unknown type: " + type);
                }
            }
            case "say" -> {
                if (args.length < 1) {
                    println("Usage: say <message>");
                    break;
                }
                if (guild == null) {
                    println("No guild selected!");
                    break;
                }
                if (channel == null) {
                    println("No channel selected!");
                    break;
                }
                String msg = String.join(" ", args);
                channel.sendMessage(msg).queue();
            }
            case "stop" -> {
                if (bot.isConnected()) {
                    bot.shutdown();
                    println("Bot has been stopped!");
                } else {
                    println("Bot is not connected!");
                }
            }
            case "exit" -> {
                if (bot.isConnected()) {
                    bot.shutdown();
                    println("Bot has been stopped!");
                }
                System.exit(0);
            }
            default -> System.out.println("Unknown command: " + input);
        }

        loop();
    }


    private void print(String msg) {
        System.out.print(msg);
    }
    private void println(String msg) {
        System.out.println(msg);
    }

}
