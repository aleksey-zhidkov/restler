package org.restler.integration

import com.fasterxml.jackson.module.paranamer.ParanamerModule
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.session.HashSessionManager
import org.eclipse.jetty.server.session.SessionHandler
import org.eclipse.jetty.servlet.FilterHolder
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.restler.integration.security.SecurityConfig
import org.restler.integration.springdata.PersonsRepository
import org.restler.integration.springdata.SpringDataRestConfig
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.context.ContextLoaderListener
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.filter.DelegatingFilterProxy
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter
import java.util.EnumSet
import javax.servlet.DispatcherType

EnableWebMvc
Import(SecurityConfig::class, SpringDataRestConfig::class)
open class WebConfig : WebMvcConfigurerAdapter() {

    override fun extendMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
        val paranamerModule = ParanamerModule()
        converters.filterIsInstance(javaClass<MappingJackson2HttpMessageConverter>()).forEach {
            it.getObjectMapper().registerModule(paranamerModule)
        }
    }

    @Bean open fun controller(personsRepo: PersonsRepository) = Controller(personsRepo)

}

fun main(args: Array<String>) {
    val server = server()

    server.start()
    server.join()
}

fun server(): Server {
    val applicationContext = AnnotationConfigWebApplicationContext()
    applicationContext.register(javaClass<WebConfig>())

    val servletHolder = ServletHolder(DispatcherServlet(applicationContext))
    val context = ServletContextHandler()
    context.setSessionHandler(SessionHandler(HashSessionManager()))
    context.setContextPath("/")
    context.addServlet(servletHolder, "/*")
    context.addFilter(FilterHolder(DelegatingFilterProxy("springSecurityFilterChain")), "/*", EnumSet.allOf(javaClass<DispatcherType>()))
    context.addEventListener(ContextLoaderListener(applicationContext))

    val webPort = System.getenv("PORT") ?: "8080"

    val server = Server(Integer.valueOf(webPort))

    server.setHandler(context)
    return server
}

