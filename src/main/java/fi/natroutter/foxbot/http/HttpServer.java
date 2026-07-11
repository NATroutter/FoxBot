package fi.natroutter.foxbot.http;

import fi.natroutter.foxbot.FoxBot;
import fi.natroutter.foxbot.configs.data.Config;
import fi.natroutter.foxlib.logger.FoxLogger;
import io.javalin.Javalin;
import io.javalin.config.RoutesConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class HttpServer {

    private final String name;
    private Javalin server;

    private final List<Route> routes = new ArrayList<>();
    private int validRoutes = 0;

    @AllArgsConstructor
    private static class httpError {
        public int code;
        public String message;
        public String product;
    }

    public void register(Route route) {
        routes.add(route);
    }
    public void register(Route... route) {
        routes.addAll(List.of(route));
    }

    public HttpServer(String name) {
        this.name = name;
    }

    private void loadEndPoints(RoutesConfig routesConfig) {
        Config.HttpServer conf = FoxBot.getConfigProvider().get().getHttpServer();

        //Create the root page
        routesConfig.get("/", ctx -> {
            ctx.result("Foxbot v"+FoxBot.getVERSION()).status(200);
        });

        //Check for common http errors and sends custom status!
        List<Integer> errorCodes = new ArrayList<>(IntStream.range(400, 599).boxed().toList());
        errorCodes.forEach(code -> {
            routesConfig.error(code, ctx -> {
                ctx.json(new httpError(ctx.statusCode(), ctx.result(), name));
            });
        });

        this.validRoutes = 0;
        for(Route route : routes) {
            String path = route.getPath();
            String name = route.getClass().getSimpleName();

            if (path == null || path.isBlank()) {
                logger().warn("Invalid path for endpoint '"+name+"', skipping.");
                continue;
            }
            path = path.toLowerCase();

            if (path.startsWith("/")) {
                path = path.substring(1);
            }

            routesConfig.get("/"+path, route::get);
            routesConfig.post("/"+path, route::post);
            routesConfig.put("/"+path, route::put);
            routesConfig.patch("/"+path, route::patch);
            routesConfig.delete("/"+path, route::delete);
            routesConfig.head("/"+path, route::head);
            routesConfig.options("/"+path, route::options);

            logger().info("Registered route: " + joinUrl(conf.getPublicAddress(), path));
            this.validRoutes++;
        }
    }

    private String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        String routePath = path == null ? "" : path.trim();

        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        while (routePath.startsWith("/")) {
            routePath = routePath.substring(1);
        }

        if (base.isEmpty()) {
            return "/" + routePath;
        }
        if (routePath.isEmpty()) {
            return base;
        }
        return base + "/" + routePath;
    }

    private boolean started = false;
    public void start() {
        Config.HttpServer conf = FoxBot.getConfigProvider().get().getHttpServer();
        if (conf == null || !conf.isEnabled()) {
            logger().info("HTTP server disabled.");
            return;
        }

        if (!started) {
            this.server = Javalin.create(config -> loadEndPoints(config.routes));
            if (validRoutes > 0) {
                server.start(conf.getHost(), conf.getPort());
                started = true;
                logger().info("HTTP server started on " + conf.getHost() + ":" + server.port());
            } else {
                logger().error("Cannot start the server: no API routes are registered.");
                System.exit(0);
            }
        }
    }

    private FoxLogger logger() {
        return FoxBot.getLogger();
    }
}
