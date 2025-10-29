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
    
    // --- LÍMITE DE CONEXIONES ---
    public static final int MAX_CONEXIONES = 3; 

    // --- CÓDIGOS DE ESTADO DE CONEXIÓN ---
    public static final int AGREGADO_OK = 0;
    public static final int ERROR_NOMBRE_DUPLICADO = 1;
    public static final int ERROR_SERVIDOR_LLENO = 2;
    // ---------------------------------

    // Mapa sincronizado para almacenar los usuarios conectados (Nombre -> Stream de salida)
    private static Map<String, PrintWriter> usuariosConectados = Collections.synchronizedMap(new HashMap<>());

    // Formateador de fecha para los logs
    private static SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        log("Iniciando el servidor de chat en el puerto " + PUERTO + "...");
        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(PUERTO);
            log("Servidor iniciado. Esperando clientes (máximo " + MAX_CONEXIONES + ")...");

            // Bucle infinito para aceptar conexiones de clientes
            while (true) {
                Socket socketCliente = serverSocket.accept();
                
                ManejadorCliente manejador = new ManejadorCliente(socketCliente);
                new Thread(manejador).start();
            }

        } catch (IOException e) {
            logError("Error al iniciar el servidor: " + e.getMessage());
        } finally {
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
     * Devuelve el número actual de usuarios conectados.
     * @return El número de usuarios.
     */
    public static int getNumeroUsuarios() {
        return usuariosConectados.size();
    }

    /**
     * Añade un nuevo usuario a la lista de conectados, comprobando el límite
     * y los nombres duplicados de forma atómica (thread-safe).
     * @param username El nombre del usuario.
     * @param writer El PrintWriter asociado al usuario.
     * @return Un código de estado: AGREGADO_OK, ERROR_SERVIDOR_LLENO, o ERROR_NOMBRE_DUPLICADO.
     */
    public static int agregarUsuario(String username, PrintWriter writer) {
        // Usamos synchronized para asegurar que todas las comprobaciones
        // y la inserción sean ATÓMICAS.
        synchronized (usuariosConectados) {
            
            // 1. Comprobar si está lleno
            if (usuariosConectados.size() >= MAX_CONEXIONES) {
                return ERROR_SERVIDOR_LLENO;
            }
            
            // 2. Comprobar si está duplicado
            if (usuariosConectados.containsKey(username)) {
                return ERROR_NOMBRE_DUPLICADO;
            }
            
            // 3. Si todo está bien, añadirlo
            usuariosConectados.put(username, writer);
            return AGREGADO_OK;
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
     * @param mensaje El mensaje a retransmitir.
     */
    public static void broadcast(String mensaje) {
        synchronized (usuariosConectados) {
            for (PrintWriter writer : usuariosConectados.values()) {
                writer.println(mensaje);
            }
        }
    }

    /**
     * Envía un mensaje privado a un usuario específico.
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