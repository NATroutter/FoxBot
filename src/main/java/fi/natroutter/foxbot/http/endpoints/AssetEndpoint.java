package fi.natroutter.foxbot.http.endpoints;

import fi.natroutter.foxbot.http.AssetRegistry;
import fi.natroutter.foxbot.http.Route;
import io.javalin.http.Context;
import io.javalin.http.NotFoundResponse;

public class AssetEndpoint extends Route {
    public AssetEndpoint() {
        super("assets/<path>");
    }

    @Override
    public void get(Context ctx) {
        AssetRegistry.Asset asset = AssetRegistry.find(ctx.pathParam("path"))
                .orElseThrow(() -> new NotFoundResponse("Asset not found"));

        ctx.contentType(asset.contentType());
        ctx.header("Cache-Control", "public, max-age=3600");
        ctx.result(asset.data());
    }
}
