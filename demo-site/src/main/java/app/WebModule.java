package app;

import app.web.IndexController;
import app.web.IndexPage;
import app.web.UploadController;
import app.web.WildcardController;
import app.web.ajax.AJAXController;
import app.web.interceptor.TestInterceptor;
import core.framework.api.Module;
import core.framework.api.http.ContentTypes;
import core.framework.api.http.HTTPStatus;
import core.framework.api.web.Response;

/**
 * @author neo
 */
public class WebModule extends Module {
    @Override
    protected void initialize() {
        http().intercept(bind(TestInterceptor.class));

        route().get("/hello", request -> Response.text("hello", HTTPStatus.CREATED, ContentTypes.TEXT_PLAIN));
        route().get("/hello/", request -> Response.text("hello with ending slash", HTTPStatus.CREATED, ContentTypes.TEXT_PLAIN));
        route().get("/hello/:name", request -> Response.text("hello " + request.pathParam("name"), HTTPStatus.CREATED, ContentTypes.TEXT_PLAIN));

        site().staticContent("/static");
        site().staticContent("/favicon.ico");
        site().staticContent("/robots.txt");
        site().message().loadProperties("messages/main.properties");

        site().template("/template/index.html", IndexPage.class);
        IndexController index = bind(IndexController.class);
        route().get("/", index::index);
        route().get("/css/main.css", index::css);
        route().post("/submit", index::submit);

        UploadController upload = bind(UploadController.class);
        route().get("/upload", upload::get);
        route().post("/upload", upload::post);

        route().post("/ajax", bind(AJAXController.class)::ajax);

        WildcardController wildcardController = bind(WildcardController.class);
        route().get("/:all(*)", wildcardController::wildcard);
    }
}