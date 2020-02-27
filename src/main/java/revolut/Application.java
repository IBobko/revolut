package revolut;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.swagger.jaxrs.config.BeanConfig;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import revolut.jaxrs.GsonMessageBodyHandler;
import revolut.jaxrs.mapper.WebApplicationExceptionMapper;
import revolut.resource.HolderResource;
import revolut.resource.TransactionResource;
import revolut.service.HolderService;
import revolut.service.impl.HolderServiceImpl;

import javax.inject.Singleton;

public class Application {
    public final static String CONTEXT_PATH = "/api/v1";
    public static Server server;

    public static void serverInitialization() throws Exception {
        Injector injector = Guice.createInjector(new InitModule());

        server = new Server(8080);
        ServletContextHandler servletHandler = new ServletContextHandler();
        servletHandler.addEventListener(injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));
        ServletHolder sh = new ServletHolder(HttpServletDispatcher.class);

        //servletHandler.addServlet(DefaultServlet.class, "/*");
        servletHandler.addServlet(sh, "/*");
        servletHandler.setContextPath(CONTEXT_PATH);
        server.setHandler(servletHandler);
        server.start();
    }

    public static void main(String[] args) throws Exception {
        try {
            serverInitialization();
            server.join();
        } finally {
            server.destroy();
        }
    }

    private static class InitModule extends RequestScopeModule {
        @Provides
        @Singleton
        public HolderService holderService() {
            return new HolderServiceImpl();
        }

        @SuppressWarnings("PointlessBinding")
        @Override
        protected void configure() {
            super.configure();
            bind(GsonMessageBodyHandler.class);
            bind(HolderResource.class);
            bind(TransactionResource.class);
            bind(WebApplicationExceptionMapper.class);
            swagger();
        }

        public void swagger() {
            bind(io.swagger.jaxrs.listing.SwaggerSerializers.class);
            bind(io.swagger.jaxrs.listing.ApiListingResource.class);
            BeanConfig beanConfig = new BeanConfig();
            beanConfig.setVersion("1.0.2");
            beanConfig.setSchemes(new String[]{"http"});
            beanConfig.setHost("localhost:8080");
            beanConfig.setBasePath(Application.CONTEXT_PATH);
            beanConfig.setResourcePackage("revolut.resource");
//            beanConfig.setScan(true);
        }
    }
}
