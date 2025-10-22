package servidor;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * Clase principal del Servidor de Chat.
 * Es una aplicación de consola que acepta múltiples clientes.
 */
public class ServidorChat {

    // Puerto en el que el servidor escuchará
    private static final int PUERTO = 9090;

    // Mapa sincronizado para almacenar los usuarios conectados (Nombre -> Stream de salida)
    // Esto es crucial para la concurrencia (thread-safety)
    private static Map<String, PrintWriter> usuariosConectados = Collections.synchronizedMap(new HashMap<>());

    // Formateador de fecha para los logs
    private static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        log("Iniciando el servidor de chat en el puerto " + PUERTO + "...");
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PUERTO);
            log("Servidor iniciado. Esperando clientes...");

            // Bucle infinito para aceptar conexiones de clientes
            while (true) {
                // accept() es bloqueante: espera hasta que un cliente se conecte
                Socket socketCliente = serverSocket.accept();
                
                // Cuando un cliente se conecta, crea un nuevo hilo para manejarlo
                ManejadorCliente manejador = new ManejadorCliente(socketCliente);
                new Thread(manejador).start();
            }

        } catch (IOException e) {
            logError("Error al iniciar el servidor: " + e.getMessage());
        } finally {
            // Cerrar el socket principal si algo sale mal
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                    log("Servidor detenido.");
                } catch (IOException e) {
                    logError("Error al cerrar el socket del servidor: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Registra un mensaje de log en la consola con marca de tiempo.
     * @param mensaje El mensaje a registrar.
     */
    public static void log(String mensaje) {
        String hora = formatter.format(new Date());
        System.out.println("[" + hora + " LOG] " + mensaje);
    }

    /**
     * Registra un mensaje de error en la consola.
     * @param mensaje El mensaje de error.
     */
    public static void logError(String mensaje) {
        String hora = formatter.format(new Date());
        System.err.println("[" + hora + " ERROR] " + mensaje);
    }

    /**
     * Añade un nuevo usuario a la lista de conectados.
     * @param username El nombre del usuario.
     * @param writer El PrintWriter asociado al usuario.
     * @return true si se añadió, false si el nombre ya existía.
     */
    public static boolean agregarUsuario(String username, PrintWriter writer) {
        // Usamos synchronized para asegurar que la comprobación y la inserción
        // sean atómicas, evitando que dos hilos registren el mismo nombre.
        synchronized (usuariosConectados) {
            if (usuariosConectados.containsKey(username)) {
                return false; // El usuario ya existe
            }
            usuariosConectados.put(username, writer);
            return true;
        }
    }

    /**
     * Elimina un usuario de la lista de conectados.
     * @param username El nombre del usuario a eliminar.
     */
    public static void eliminarUsuario(String username) {
        if (username != null) {
            usuariosConectados.remove(username);
        }
    }

    /**
     * Envía un mensaje a TODOS los usuarios conectados.
     * (Requisito: Retransmitir mensajes públicos)
     * @param mensaje El mensaje a retransmitir.
     */
    public static void broadcast(String mensaje) {
        // Sincronizamos para evitar problemas si un usuario se añade/elimina
        // mientras estamos iterando la lista.
        synchronized (usuariosConectados) {
            for (PrintWriter writer : usuariosConectados.values()) {
                writer.println(mensaje);
            }
        }
    }

    /**
     * Envía un mensaje privado a un usuario específico.
     * (Requisito: Enviar mensajes privados)
     * @param destinatario El nombre del usuario destino.
     * @param mensaje El mensaje a enviar.
     * @return true si se envió, false si el destinatario no se encontró.
     */
    public static boolean enviarMensajePrivado(String destinatario, String mensaje) {
        synchronized (usuariosConectados) {
            PrintWriter writer = usuariosConectados.get(destinatario);
            if (writer != null) {
                writer.println(mensaje);
                return true;
            }
            return false;
        }
    }

    /**
     * Envía la lista actualizada de usuarios a todos.
     */
    public static void actualizarListaUsuariosGlobal() {
        synchronized (usuariosConectados) {
            String listaUsuarios = "LISTA_USUARIOS:" + String.join(",", usuariosConectados.keySet());
            broadcast(listaUsuarios);
        }
    }
}