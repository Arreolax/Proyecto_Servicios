package com.example.biblioteca.configuracion;

import java.util.List;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurer;
import org.springframework.ws.server.EndpointInterceptor;
import org.springframework.ws.server.endpoint.adapter.method.MethodArgumentResolver;
import org.springframework.ws.server.endpoint.adapter.method.MethodReturnValueHandler;
import org.springframework.ws.soap.server.endpoint.interceptor.PayloadValidatingInterceptor;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

@EnableWs
@Configuration
public class SoapConfig implements WsConfigurer {

    public static final String NAMESPACE =
            "http://ejemplo.com/biblioteca/soap";

    // Recibe las solicitudes dirigidas a /ws.
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet>
            messageDispatcherServlet(ApplicationContext applicationContext) {

        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);

        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    // Carga el contrato de mensajes.
    @Bean
    public XsdSchema bibliotecaSoapSchema() {
        return new SimpleXsdSchema(
                new ClassPathResource("contratos/biblioteca-soap.xsd")
        );
    }

    // Publica /ws/biblioteca.wsdl.
    @Bean(name = "biblioteca")
    public DefaultWsdl11Definition bibliotecaWsdl() {

        DefaultWsdl11Definition definicion = new DefaultWsdl11Definition();
        definicion.setPortTypeName("BibliotecaPort");
        definicion.setLocationUri("/ws");
        definicion.setTargetNamespace(NAMESPACE);
        definicion.setSchema(bibliotecaSoapSchema());

        return definicion;
    }

    // Comprueba solicitudes y respuestas contra el XSD.
    @Bean
    public PayloadValidatingInterceptor validadorSoap() {

        PayloadValidatingInterceptor interceptor = new PayloadValidatingInterceptor();
        interceptor.setXsdSchema(bibliotecaSoapSchema());
        interceptor.setValidateRequest(true);
        interceptor.setValidateResponse(true);

        return interceptor;
    }

    @Override
    public void addInterceptors(List<EndpointInterceptor> interceptores) {
        interceptores.add(validadorSoap());
    }

    @Override
    public void addArgumentResolvers(List<MethodArgumentResolver> argumentResolvers) {
        // Método requerido por la interfaz WsConfigurer
    }

    @Override
    public void addReturnValueHandlers(List<MethodReturnValueHandler> returnValueHandlers) {
        // Método requerido por la interfaz WsConfigurer
    }
}
