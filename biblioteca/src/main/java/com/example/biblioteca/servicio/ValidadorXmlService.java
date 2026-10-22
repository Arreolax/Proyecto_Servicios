package com.example.biblioteca.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

@Service
public class ValidadorXmlService {

    private final Schema esquema;

    public ValidadorXmlService() {
        SchemaFactory fabrica = SchemaFactory.newInstance(
                XMLConstants.W3C_XML_SCHEMA_NS_URI
        );

        ClassPathResource recurso =
                new ClassPathResource("contratos/libro.xsd");

        try (InputStream entrada = recurso.getInputStream()) {

            // Limitar el procesamiento a los recursos locales indicados.
            fabrica.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true
            );

            fabrica.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            fabrica.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            // Cargar y comprobar el contrato al iniciar la aplicación.
            esquema = fabrica.newSchema(
                    new StreamSource(entrada)
            );

        } catch (SAXException | IOException error) {
            throw new IllegalStateException(
                    "No se pudo cargar contratos/libro.xsd",
                    error
            );
        }
    }

    public void validar(String contenidoXml)
            throws SAXException, IOException {

        // Cada solicitud utiliza su propio validador.
        Validator validador = esquema.newValidator();

        validador.setProperty(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        validador.setProperty(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        try (StringReader lector = new StringReader(contenidoXml)) {
            validador.validate(
                    new StreamSource(lector)
            );
        }
    }
}
