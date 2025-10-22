package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implementa Runnable para manejar cada conexión de cliente en un hilo separado.
 * (Requisito: Crear un hilo independiente para cada cliente)
 */
public class ManejadorCliente implements Runnable {

    private Socket socketCliente;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String username;

    public ManejadorCliente(Socket socket) {
        this.socketCliente = socket;
    }

    @Override
    public void run() {
        try {
            // 1. Configurar streams de entrada y salida
            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            salida = new PrintWriter(socketCliente.getOutputStream(), true); // 'true' para autoFlush

            // 2. Procesar la conexión (espera el mensaje CONECTAR:)
            String primerMensaje = entrada.readLine();
            if (primerMensaje != null && primerMensaje.startsWith("CONECTAR:")) {
                this.username = primerMensaje.substring(9).trim();

                // Intentar agregar al usuario. Si falla, el nombre está en uso.
                if (ServidorChat.agregarUsuario(this.username, this.salida)) {
                    // Conexión exitosa
                    ServidorChat.log("Usuario conectado: " + this.username + " desde " + socketCliente.getInetAddress().getHostAddress());
                    salida.println("INFO:¡Bienvenido al chat, " + this.username + "!");

                    // Informar a todos (menos al nuevo) que alguien se unió
                    // (Requisito: Informar al resto de usuarios)
                    ServidorChat.broadcast("NUEVO_USUARIO:" + this.username);
                    
                    // Actualizar la lista de usuarios para todos
                    ServidorChat.actualizarListaUsuariosGlobal();

                } else {
                    // Nombre de usuario ya en uso
                    ServidorChat.log("Intento de conexión fallido (usuario ya existe): " + this.username);
                    salida.println("INFO:Error. Ese nombre de usuario ya está en uso. Desconectando.");
                    // Cerramos todo y terminamos el hilo
                    throw new IOException("Nombre de usuario duplicado"); 
                }
            } else {
                // Protocolo incorrecto
                ServidorChat.log("Conexión rechazada. Protocolo incorrecto de " + socketCliente.getInetAddress().getHostAddress());
                salida.println("INFO:Error. Protocolo de conexión incorrecto.");
                throw new IOException("Protocolo incorrecto");
            }

            // 3. Bucle principal: Leer mensajes del cliente
            String linea;
            while ((linea = entrada.readLine()) != null) {
                
                if (linea.startsWith("MSG:")) {
                    // Mensaje Público
                    String msg = linea.substring(4);
                    ServidorChat.log("Mensaje público de " + username + ": " + msg);
                    ServidorChat.broadcast("MSG:" + username + ":" + msg);

                } else if (linea.startsWith("MSG_PRIVADO:")) {
                    // Mensaje Privado: MSG_PRIVADO:<destinatario>:<mensaje>
                    try {
                        String[] partes = linea.split(":", 3);
                        String destinatario = partes[1];
                        String msg = partes[2];
                        
                        ServidorChat.log("Mensaje privado de " + username + " para " + destinatario + ": " + msg);

                        // (Requisito: Enviar mensajes privados solo al destinatario)
                        boolean enviado = ServidorChat.enviarMensajePrivado(destinatario, "MSG_PRIVADO:" + username + ":" + msg);
                        
                        if (!enviado) {
                            salida.println("INFO:El usuario '" + destinatario + "' no está conectado o no existe.");
                        }
                    } catch (Exception e) {
                        salida.println("INFO:Error en el formato del mensaje privado. Use: MSG_PRIVADO:destinatario:mensaje");
                    }
                    
                } else if (linea.startsWith("DESCONECTAR:")) {
                    // El cliente inició la desconexión
                    break; // Salir del bucle
                }
            }

        } catch (SocketException e) {
            // Esto suele pasar si el cliente cierra la aplicación de golpe (ej. con la 'X')
            ServidorChat.log("Conexión perdida (SocketException) con " + (username != null ? username : "desconocido") + ": " + e.getMessage());
        } catch (IOException e) {
            ServidorChat.logError("Error de E/S con " + (username != null ? username : "desconocido") + ": " + e.getMessage());
        } finally {
            // 4. Limpieza (sucede siempre, al salir del bucle o por error)
            
            // Si el usuario logró conectarse (tiene username), lo eliminamos y avisamos
            if (this.username != null) {
                ServidorChat.log("Usuario desconectado: " + this.username);
                ServidorChat.eliminarUsuario(this.username);
                // (Requisito: Informar usuario que sale)
                ServidorChat.broadcast("USUARIO_FUERA:" + this.username);
                // Actualizar la lista de usuarios para los que quedan
                ServidorChat.actualizarListaUsuariosGlobal();
            }

            // Cerrar streams y socket
            try {
                if (salida != null) salida.close();
                if (entrada != null) entrada.close();
                if (socketCliente != null) socketCliente.close();
            } catch (IOException e) {
                ServidorChat.logError("Error al cerrar recursos del cliente: " + e.getMessage());
            }
        }
    }
}