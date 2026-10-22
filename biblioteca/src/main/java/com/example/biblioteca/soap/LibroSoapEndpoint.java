package com.example.biblioteca.soap;

import com.example.biblioteca.configuracion.SoapConfig;
import com.example.biblioteca.modelo.Libro;
import com.example.biblioteca.servicio.LibroService;

import java.util.Locale;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.ws.context.MessageContext;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import org.springframework.ws.soap.SoapMessage;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

@Endpoint
public class LibroSoapEndpoint {

    private final LibroService libroService;

    public LibroSoapEndpoint(LibroService libroService) {
        this.libroService = libroService;
    }

    @PayloadRoot(
            namespace = SoapConfig.NAMESPACE,
            localPart = "consultarLibroRequest"
    )
    @ResponsePayload
    public Element consultarLibro(
            @RequestPayload Element solicitud,
            MessageContext contexto)
            throws ParserConfigurationException {

        // El interceptor ya comprobó la estructura y el tipo del id.
        String textoId = solicitud
                .getElementsByTagNameNS(
                        SoapConfig.NAMESPACE,
                        "id"
                )
                .item(0)
                .getTextContent()
                .trim();

        int id = Integer.parseInt(textoId);

        // Reutilizar el catálogo de la práctica anterior.
        Optional<Libro> resultado =
                libroService.buscarPorId(id);

        if (resultado.isEmpty()) {

            SoapMessage respuesta =
                    (SoapMessage) contexto.getResponse();

            respuesta.getSoapBody()
                    .addClientOrSenderFault(
                            "No existe el libro con identificador " + id,
                            Locale.forLanguageTag("es")
                    );

            return null;
        }

        Libro libro = resultado.get();

        // Crear el documento de respuesta.
        DocumentBuilderFactory fabrica =
                DocumentBuilderFactory.newInstance();

        fabrica.setNamespaceAware(true);

        Document documento = fabrica
                .newDocumentBuilder()
                .newDocument();

        Element respuesta = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "bib:consultarLibroResponse"
        );

        documento.appendChild(respuesta);

        Element elementoLibro = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "bib:libro"
        );

        respuesta.appendChild(elementoLibro);

        agregarElemento(
                documento,
                elementoLibro,
                "id",
                Integer.toString(libro.getId())
        );

        agregarElemento(
                documento,
                elementoLibro,
                "titulo",
                libro.getTitulo()
        );

        agregarElemento(
                documento,
                elementoLibro,
                "autor",
                libro.getAutor()
        );

        agregarElemento(
                documento,
                elementoLibro,
                "disponible",
                Boolean.toString(libro.isDisponible())
        );

        return respuesta;
    }

    private void agregarElemento(
            Document documento,
            Element padre,
            String nombre,
            String valor) {

        Element elemento = documento.createElementNS(
                SoapConfig.NAMESPACE,
                "bib:" + nombre
        );

        elemento.setTextContent(valor);
        padre.appendChild(elemento);
    }
}
