"use strict";

// Direcciones de los proveedores.
const API_LIBROS = "http://localhost:8082/api/integracion/libros";
const API_PRESTAMOS = "http://localhost:8081/api/prestamos";

// Forma breve de obtener elementos del HTML.
const elemento = (id) => document.getElementById(id);

const formConsulta = elemento("formConsulta");
const formPrestamo = elemento("formPrestamo");

const btnConsultar = elemento("btnConsultar");
const btnPrestar = elemento("btnPrestar");
const btnListar = elemento("btnListar");

// Mostrar mensajes como texto.
function mostrarMensaje(id, texto, esError = false) {
    const destino = elemento(id);
    destino.textContent = texto;
    destino.className = esError
        ? "mensaje error"
        : "mensaje exito";
}

// Validar identificadores compatibles con int de Java.
function leerId(idCampo) {
    const texto = elemento(idCampo).value.trim();
    const numero = Number(texto);

    if (
        texto === "" ||
        !Number.isInteger(numero) ||
        numero < 1 ||
        numero > 2147483647
    ) {
        throw new Error("Escribe un identificador entero positivo válido.");
    }

    return numero;
}

// Enviar una petición y procesar la respuesta JSON.
async function solicitarJson(url, opciones = {}) {
    const controlador = new AbortController();

    // Un tiempo mayor que el de las llamadas internas de la práctica.
    const temporizador = setTimeout(() => {
        controlador.abort();
    }, 20000);

    const esEscritura = opciones.method === "POST";

    try {
        const respuesta = await fetch(url, {
            ...opciones,
            signal: controlador.signal
        });

        const texto = await respuesta.text();

        let datos = null;
        let jsonValido = true;

        if (texto) {
            try {
                datos = JSON.parse(texto);
            } catch {
                jsonValido = false;
            }
        }

        if (!respuesta.ok) {
            const detalle =
                datos && typeof datos.mensaje === "string"
                    ? datos.mensaje
                    : `El servicio respondió con HTTP ${respuesta.status}.`;

            const error = new Error(detalle);
            error.status = respuesta.status;
            throw error;
        }

        if (!jsonValido || datos === null) {
            throw new Error(
                esEscritura
                    ? "La respuesta no pudo interpretarse. Consulta los " +
                      "préstamos antes de volver a enviar la solicitud."
                    : "El servicio no devolvió un JSON válido."
            );
        }

        return datos;

    } catch (error) {
        if (
            error.name === "AbortError" ||
            error instanceof TypeError
        ) {
            throw new Error(
                esEscritura
                    ? "No se pudo confirmar el resultado. Revisa los " +
                      "préstamos y la disponibilidad antes de repetir. " +
                      "Comprueba también la conexión y CORS."
                    : "No fue posible obtener la respuesta. " +
                      "Comprueba que el servicio esté activo y revisa CORS."
            );
        }

        throw error;

    } finally {
        clearTimeout(temporizador);
    }
}

// Consultar y mostrar un libro.
async function consultarLibro(id) {
    btnConsultar.disabled = true;
    elemento("resultadoLibro").hidden = true;
    mostrarMensaje("mensajeConsulta", "Consultando libro...");

    try {
        const libro = await solicitarJson(`${API_LIBROS}/${id}`);

        if (
            !libro ||
            !Number.isInteger(libro.id) ||
            typeof libro.titulo !== "string" ||
            typeof libro.autor !== "string" ||
            typeof libro.disponible !== "boolean"
        ) {
            throw new Error("El libro recibido no tiene el formato esperado.");
        }

        elemento("idLibro").textContent = libro.id;
        elemento("tituloLibro").textContent = libro.titulo;
        elemento("autorLibro").textContent = libro.autor;
        elemento("disponibilidadLibro").textContent =
            libro.disponible ? "Disponible" : "No disponible";

        elemento("resultadoLibro").hidden = false;

        mostrarMensaje(
            "mensajeConsulta",
            "Consulta realizada correctamente."
        );

    } catch (error) {
        mostrarMensaje("mensajeConsulta", error.message, true);

    } finally {
        btnConsultar.disabled = false;
    }
}

// Consultar y construir la tabla de préstamos.
async function cargarPrestamos() {
    btnListar.disabled = true;
    elemento("tablaPrestamos").hidden = true;
    elemento("cuerpoPrestamos").replaceChildren();

    mostrarMensaje("mensajeLista", "Consultando préstamos...");

    try {
        const prestamos = await solicitarJson(API_PRESTAMOS);

        if (!Array.isArray(prestamos)) {
            throw new Error("El servicio no devolvió una lista de préstamos.");
        }

        if (prestamos.length === 0) {
            mostrarMensaje("mensajeLista", "No hay préstamos registrados.");
            return;
        }

        for (const prestamo of prestamos) {
            const fila = document.createElement("tr");

            const valores = [
                prestamo.id,
                prestamo.tituloLibro,
                prestamo.matricula,
                prestamo.fechaPrestamo,
                prestamo.estado
            ];

            for (const valor of valores) {
                const celda = document.createElement("td");
                celda.textContent = String(valor ?? "");
                fila.appendChild(celda);
            }

            elemento("cuerpoPrestamos").appendChild(fila);
        }

        elemento("tablaPrestamos").hidden = false;

        mostrarMensaje(
            "mensajeLista",
            `Préstamos registrados: ${prestamos.length}.`
        );

    } catch (error) {
        mostrarMensaje("mensajeLista", error.message, true);

    } finally {
        btnListar.disabled = false;
    }
}

// Evento del formulario de consulta.
formConsulta.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    if (btnConsultar.disabled) {
        return;
    }

    try {
        const id = leerId("consultaId");
        await consultarLibro(id);

    } catch (error) {
        elemento("resultadoLibro").hidden = true;
        mostrarMensaje("mensajeConsulta", error.message, true);
    }
});

// Evento del formulario de préstamo.
formPrestamo.addEventListener("submit", async (evento) => {
    evento.preventDefault();

    if (btnPrestar.disabled) {
        return;
    }

    let libroId;
    let matricula;

    // Primero validar, sin enviar ninguna solicitud.
    try {
        libroId = leerId("prestamoLibroId");
        matricula = elemento("matricula").value.trim();

        if (matricula === "") {
            throw new Error("La matrícula es obligatoria.");
        }

    } catch (error) {
        mostrarMensaje("mensajePrestamo", error.message, true);
        return;
    }

    btnPrestar.disabled = true;
    mostrarMensaje("mensajePrestamo", "Registrando préstamo...");

    try {
        const prestamo = await solicitarJson(API_PRESTAMOS, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "application/json"
            },
            body: JSON.stringify({
                libroId: libroId,
                matricula: matricula
            })
        });

        mostrarMensaje(
            "mensajePrestamo",
            `Préstamo ${prestamo.id} registrado: ` +
            `${prestamo.tituloLibro}, matrícula ${prestamo.matricula}.`
        );

        // Limpiar el formulario solo después de recibir confirmación.
        formPrestamo.reset();

        // Mostrar el estado actualizado del libro solicitado.
        elemento("consultaId").value = libroId;
        await consultarLibro(libroId);

        // Actualizar el listado.
        await cargarPrestamos();

    } catch (error) {
        mostrarMensaje("mensajePrestamo", error.message, true);

    } finally {
        btnPrestar.disabled = false;
    }
});

// Evento del botón que consulta todos los préstamos.
btnListar.addEventListener("click", cargarPrestamos);
