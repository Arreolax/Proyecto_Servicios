package com.example.integrador.servicio;

import com.example.integrador.modelo.LibroRespuesta;

import java.io.IOException;
import java.io.StringReader;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

@Service
public class BibliotecaSoapClient {

    private static final String NS_BIBLIOTECA =
            "http://ejemplo.com/biblioteca/soap";

    private static final String NS_SOAP =
            "http://schemas.xmlsoap.org/soap/envelope/";

    private final HttpClient cliente;
    private final URI direccion;

    public BibliotecaSoapClient(
            @Value("${biblioteca.soap.url}") String url) {

        direccion = URI.create(url);

        cliente = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public LibroRespuesta consultar(int id) {

        if (id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El identificador debe ser mayor que cero"
            );
        }

        String solicitudSoap = crearSolicitud(id);

        HttpRequest peticion = HttpRequest.newBuilder()
                .uri(direccion)
                .timeout(Duration.ofSeconds(8))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("Accept", "text/xml")
                .header("SOAPAction", "\"\"")
                .POST(HttpRequest.BodyPublishers.ofString(
                        solicitudSoap,
                        StandardCharsets.UTF_8
                ))
                .build();

        HttpResponse<String> respuesta;

        try {
            respuesta = cliente.send(
                    peticion,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

        } catch (HttpTimeoutException error) {
            throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "Biblioteca tardó demasiado en responder"
            );

        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "La consulta fue interrumpida"
            );

        } catch (IOException error) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "No fue posible comunicarse con biblioteca"
            );
        }

        return interpretarRespuesta(
                respuesta.statusCode(),
                respuesta.body(),
                id
        );
    }

    private String crearSolicitud(int id) {

        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope
                    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                    xmlns:bib="http://ejemplo.com/biblioteca/soap">
                    <soapenv:Header/>
                    <soapenv:Body>
                        <bib:consultarLibroRequest>
                            <bib:id>%d</bib:id>
                        </bib:consultarLibroRequest>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(id);
    }

    private LibroRespuesta interpretarRespuesta(
            int estadoHttp,
            String xml,
            int idEsperado) {

        try {
            Document documento = leerXml(xml);

            Element sobre = documento.getDocumentElement();

            if (!NS_SOAP.equals(sobre.getNamespaceURI())
                    || !"Envelope".equals(sobre.getLocalName())) {
                throw new IllegalArgumentException(
                        "La respuesta no es un sobre SOAP 1.1"
                );
            }

            Element cuerpo = requerido(sobre, NS_SOAP, "Body");

            // SOAP puede enviar un Fault con HTTP 500.
            Element fallo = hijo(cuerpo, NS_SOAP, "Fault");

            if (fallo != null) {
                throw traducirFault(fallo);
            }

            if (estadoHttp != 200) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Biblioteca respondió con HTTP " + estadoHttp
                );
            }

            Element respuesta = requerido(
                    cuerpo,
                    NS_BIBLIOTECA,
                    "consultarLibroResponse"
            );

            Element libro = requerido(
                    respuesta,
                    NS_BIBLIOTECA,
                    "libro"
            );

            int id = Integer.parseInt(
                    texto(libro, "id").trim()
            );

            String titulo = texto(libro, "titulo");
            String autor = texto(libro, "autor");

            String disponibilidad =
                    texto(libro, "disponible").trim();

            boolean disponible = switch (disponibilidad) {
                case "true", "1" -> true;
                case "false", "0" -> false;
                default -> throw new IllegalArgumentException(
                        "Disponibilidad inválida"
                );
            };

            if (id != idEsperado) {
                throw new IllegalArgumentException(
                        "Biblioteca devolvió otro identificador"
                );
            }

            return new LibroRespuesta(
                    id,
                    titulo,
                    autor,
                    disponible
            );

        } catch (ParserConfigurationException error) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "No fue posible configurar el lector XML"
            );

        } catch (SAXException
                | IOException
                | IllegalArgumentException error) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Biblioteca devolvió una respuesta incompatible"
            );
        }
    }

    private Document leerXml(String xml)
            throws ParserConfigurationException,
            SAXException,
            IOException {

        DocumentBuilderFactory fabrica =
                DocumentBuilderFactory.newInstance();

        fabrica.setNamespaceAware(true);

        fabrica.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        fabrica.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        fabrica.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        fabrica.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        fabrica.setXIncludeAware(false);
        fabrica.setExpandEntityReferences(false);

        return fabrica.newDocumentBuilder().parse(
                new InputSource(new StringReader(xml))
        );
    }

    private ResponseStatusException traducirFault(Element fallo) {

        Element codigo = requerido(fallo, null, "faultcode");
        Element mensaje = requerido(fallo, null, "faultstring");

        String valor = codigo.getTextContent().trim();

        int separador = valor.indexOf(':');

        String prefijo = separador >= 0
                ? valor.substring(0, separador)
                : null;

        String nombre = separador >= 0
                ? valor.substring(separador + 1)
                : valor;

        String namespace = codigo.lookupNamespaceURI(prefijo);

        boolean rechazoSolicitud =
                NS_SOAP.equals(namespace)
                && "Client".equals(nombre);

        HttpStatus estado = rechazoSolicitud
                ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.BAD_GATEWAY;

        return new ResponseStatusException(
                estado,
                "Biblioteca: " + mensaje.getTextContent().trim()
        );
    }

    private String texto(Element padre, String nombre) {
        return requerido(
                padre,
                NS_BIBLIOTECA,
                nombre
        ).getTextContent();
    }

    private Element requerido(
            Element padre,
            String namespace,
            String nombre) {

        Element elemento = hijo(padre, namespace, nombre);

        if (elemento == null) {
            throw new IllegalArgumentException(
                    "Falta el elemento " + nombre
            );
        }

        return elemento;
    }

    private Element hijo(
            Element padre,
            String namespace,
            String nombre) {

        for (Node nodo = padre.getFirstChild();
                nodo != null;
                nodo = nodo.getNextSibling()) {

            if (nodo instanceof Element elemento) {

                boolean mismoNamespace =
                        java.util.Objects.equals(
                                namespace,
                                elemento.getNamespaceURI()
                        );

                if (mismoNamespace
                        && nombre.equals(elemento.getLocalName())) {
                    return elemento;
                }
            }
        }

        return null;
    }
}
