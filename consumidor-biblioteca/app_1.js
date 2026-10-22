// Dirección del servicio proveedor.
const API_URL = "http://localhost:8080/api/libros";

// Elementos de la página.
const formulario = document.getElementById("formulario");
const entradaId = document.getElementById("libroId");
const boton = document.getElementById("botonConsultar");

const mensaje = document.getElementById("mensaje");
const resultado = document.getElementById("resultado");

const tituloLibro = document.getElementById("tituloLibro");
const identificadorLibro = document.getElementById("identificadorLibro");
const autorLibro = document.getElementById("autorLibro");
const disponibilidadLibro =
    document.getElementById("disponibilidadLibro");

// Escuchar el envío del formulario.
formulario.addEventListener("submit", async function (evento) {

    // Evitar que el navegador recargue la página.
    evento.preventDefault();

    // Ocultar información de una consulta anterior.
    resultado.hidden = true;
    mensaje.textContent = "";
    mensaje.className = "";

    const textoId = entradaId.value.trim();
    const id = Number(textoId);

    // Validación local: el proveedor también valida los datos.
    if (
        textoId === "" ||
        !Number.isInteger(id) ||
        id <= 0 ||
        id > 2147483647
    ) {
        mostrarError("Escribe un identificador entero positivo válido.");
        return;
    }

    boton.disabled = true;
    mensaje.textContent = "Consultando...";

    // Cancelar la espera si tarda más de cinco segundos.
    const controlador = new AbortController();

    const temporizador = setTimeout(function () {
        controlador.abort();
    }, 5000);

    try {
        const respuesta = await fetch(`${API_URL}/${id}`, {
            method: "GET",
            headers: {
                "Accept": "application/json"
            },
            signal: controlador.signal
        });

        // Una respuesta HTTP de error no siempre provoca un catch.
        if (!respuesta.ok) {
            let textoError = `Error HTTP ${respuesta.status}`;

            try {
                const detalle = await respuesta.json();

                if (typeof detalle.mensaje === "string") {
                    textoError = detalle.mensaje;
                }
            } catch {
                // Conservar el mensaje HTTP si no existe JSON válido.
            }

            mostrarError(textoError);
            return;
        }

        const libro = await respuesta.json();

        // Detectar una respuesta incompatible con el contrato.
        if (
            !Number.isInteger(libro.id) ||
            typeof libro.titulo !== "string" ||
            typeof libro.autor !== "string" ||
            typeof libro.disponible !== "boolean"
        ) {
            mostrarError("La respuesta no cumple el contrato esperado.");
            return;
        }

        // Usar textContent para mostrar los datos como texto.
        identificadorLibro.textContent = libro.id;
        tituloLibro.textContent = libro.titulo;
        autorLibro.textContent = libro.autor;

        disponibilidadLibro.textContent = libro.disponible
            ? "Disponible para préstamo"
            : "Actualmente prestado";

        resultado.hidden = false;

        mensaje.textContent = "Consulta realizada correctamente.";
        mensaje.className = "exito";

    } catch (error) {

        if (error.name === "AbortError") {
            mostrarError("El servicio tardó demasiado en responder.");
        } else {
            mostrarError(
                "No fue posible completar la consulta. " +
                "Revisa el proveedor, la dirección y la consola del navegador."
            );
        }

    } finally {
        clearTimeout(temporizador);
        boton.disabled = false;
    }
});

function mostrarError(texto) {
    mensaje.textContent = texto;
    mensaje.className = "error";
}
