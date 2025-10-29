package servidor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

/**
 * Implementa Runnable para manejar cada conexión de cliente en un hilo separado.
 */
public class ManejadorCliente implements Runnable {

    private Socket socketCliente;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String username;
    
    // Bandera para el bugfix del usuario duplicado
    private boolean usuarioAgregadoExitosamente = false; 

    public ManejadorCliente(Socket socket) {
        this.socketCliente = socket;
    }

    @Override
    public void run() {
        try {
            // 1. Configurar streams
            entrada = new BufferedReader(new InputStreamReader(socketCliente.getInputStream()));
            salida = new PrintWriter(socketCliente.getOutputStream(), true); // autoFlush

            // 2. Procesar la conexión
            String primerMensaje = entrada.readLine();
            
            if (primerMensaje != null && primerMensaje.startsWith("CONECTAR:")) {
                this.username = primerMensaje.substring(9).trim();

                // --- MODIFICACIÓN: Comprobar el resultado de agregarUsuario ---
                int resultado = ServidorChat.agregarUsuario(this.username, this.salida);
                
                if (resultado == ServidorChat.AGREGADO_OK) {
                    // Conexión exitosa
                    ServidorChat.log("Usuario conectado: " + this.username + " (" + (ServidorChat.getNumeroUsuarios()) + "/" + ServidorChat.MAX_CONEXIONES + ")");
                    salida.println("INFO:¡Bienvenido al chat, " + this.username + "!");
                    
                    // Bugfix: Marcar como agregado exitosamente
                    this.usuarioAgregadoExitosamente = true; 

                    ServidorChat.broadcast("NUEVO_USUARIO:" + this.username);
                    ServidorChat.actualizarListaUsuariosGlobal();

                } else if (resultado == ServidorChat.ERROR_NOMBRE_DUPLICADO) {
                    // Nombre de usuario ya en uso
                    ServidorChat.log("Intento de conexión fallido (usuario ya existe): " + this.username);
                    salida.println("INFO:Error. Ese nombre de usuario ya está en uso. Desconectando.");
                    throw new IOException("Nombre de usuario duplicado");

                } else if (resultado == ServidorChat.ERROR_SERVIDOR_LLENO) {
                    // Servidor lleno
                    ServidorChat.log("Intento de conexión fallido (servidor lleno): " + this.username);
                    salida.println("INFO:Error. El servidor está lleno (máximo " + ServidorChat.MAX_CONEXIONES + "). Desconectando.");
                    throw new IOException("Servidor lleno");
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

                        boolean enviado = ServidorChat.enviarMensajePrivado(destinatario, "MSG_PRIVADO:" + username + ":" + msg);
                        
                        if (!enviado) {
                            salida.println("INFO:El usuario '" + destinatario + "' no está conectado o no existe.");
                        }
                    } catch (Exception e) {
                        salida.println("INFO:Error en el formato del mensaje privado. Use: MSG_PRIVADO:destinatario:mensaje");
                    }
                    
                } else if (linea.startsWith("DESCONECTAR:")) {
                    break; // Salir del bucle
                }
            }

        } catch (SocketException e) {
            ServidorChat.log("Conexión perdida (SocketException) con " + (username != null ? username : "desconocido") + ": " + e.getMessage());
        } catch (IOException e) {
            // No mostramos el error si fue por "servidor lleno" o "duplicado", ya que es controlado
            if (!e.getMessage().equals("Nombre de usuario duplicado") && !e.getMessage().equals("Servidor lleno")) {
                 ServidorChat.logError("Error de E/S con " + (username != null ? username : "desconocido") + ": " + e.getMessage());
            }
        } finally {
            // 4. Limpieza (sucede siempre)
            
            // Bugfix: Solo eliminar al usuario si se agregó exitosamente
            if (this.username != null && this.usuarioAgregadoExitosamente) { 
                ServidorChat.log("Usuario desconectado: " + this.username + " (" + (ServidorChat.getNumeroUsuarios() - 1) + "/" + ServidorChat.MAX_CONEXIONES + ")");
                ServidorChat.eliminarUsuario(this.username);
                ServidorChat.broadcast("USUARIO_FUERA:" + this.username);
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