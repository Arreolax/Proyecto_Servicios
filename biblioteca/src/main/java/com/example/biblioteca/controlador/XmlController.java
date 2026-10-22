package com.example.biblioteca.controlador;

import com.example.biblioteca.servicio.ValidadorXmlService;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.xml.sax.SAXException;

@RestController
@RequestMapping("/api/xml")
public class XmlController {

    private final ValidadorXmlService validadorXmlService;

    public XmlController(ValidadorXmlService validadorXmlService) {
        this.validadorXmlService = validadorXmlService;
    }

    @PostMapping(
            value = "/validar",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<Map<String, Object>> validar(
            @RequestBody String contenidoXml) {

        try {
            validadorXmlService.validar(contenidoXml);

            return ResponseEntity.ok(
                    Map.<String, Object>of(
                            "valido", true,
                            "mensaje", "El XML cumple el contrato libro.xsd"
                    )
            );

        } catch (SAXException error) {
            return ResponseEntity.badRequest().body(
                    Map.<String, Object>of(
                            "valido", false,
                            "mensaje",
                            "XML mal formado o incompatible con libro.xsd",
                            "detalle", error.getMessage() != null
                                    ? error.getMessage()
                                    : "Revisa la estructura y los valores"
                    )
            );

        } catch (IOException error) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(
                            Map.<String, Object>of(
                                    "valido", false,
                                    "mensaje",
                                    "No fue posible procesar el documento"
                            )
                    );
        }
    }
}
