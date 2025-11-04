package cliente;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.awt.Color;

/*
 * Clase que maneja la lógica de red del cliente de chat.
 */
public class ClienteChat implements Runnable {

    private VentanaCliente ventana; // Referencia a la GUI
    private Socket socket;
    private PrintWriter salida;
    private BufferedReader entrada;
    private Thread hiloReceptor;
    private String username;
    private volatile boolean conectado; // volatile para seguridad entre hilos

    public ClienteChat(VentanaCliente ventana) {
        this.ventana = ventana;
        this.conectado = false;
    }

    public boolean isConectado() {
        return conectado;
    }

    /*
     * Intenta conectar al servidor.
     */
    public void conectar(String ip, int puerto, String user) {
        if (conectado) return;

        try {
            socket = new Socket(ip, puerto);
            salida = new PrintWriter(socket.getOutputStream(), true);
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.username = user;

            // Enviar el nombre de usuario al servidor
            salida.println("CONECTAR:" + username);

            conectado = true;
            ventana.setEstadoConectado(true);

            // Iniciar hilo para recibir mensajes
            hiloReceptor = new Thread(this);
            hiloReceptor.start();

        } catch (UnknownHostException e) {
            ventana.mostrarError("Error: Host desconocido (" + ip + ")");
        } catch (IOException e) {
            ventana.mostrarError("No se pudo conectar al servidor. " + e.getMessage());
        }
    }

    /*
     * Cierra la conexión y los recursos.
     */
    public void desconectar() {
        if (!conectado) return;

        try {
            // Enviar mensaje de desconexión al servidor
            if (salida != null) {
                salida.println("DESCONECTAR:");
            }
        } finally {
            // Ponemos conectado a false ANTES de cerrar para que el bucle run() termine
            conectado = false; 
            cerrarRecursos();
            ventana.setEstadoConectado(false);
            if (hiloReceptor != null) {
                hiloReceptor.interrupt(); // Interrumpir el hilo si está bloqueado
            }
        }
    }
    
    /*
     * Envía un mensaje público al servidor.
     */
    public void enviarMensajePublico(String msg) {
        if (conectado && salida != null) {
            salida.println("MSG:" + msg);
        }
    }

    /*
     * Envía un mensaje privado a un destinatario.
     */
    public void enviarMensajePrivado(String destinatario, String msg) {
        if (conectado && salida != null) {
            salida.println("MSG_PRIVADO:" + destinatario + ":" + msg);
        }
    }

    /*
     * Hilo de escucha 
     * Escucha permanentemente los mensajes del servidor.
     */
    @Override
    public void run() {
        String msgServidor;
        try {
            // Bucle de lectura mientras esté conectado
            while (conectado && (msgServidor = entrada.readLine()) != null) {
                procesarMensajeServidor(msgServidor);
            }
        } catch (IOException e) {
            if (conectado) { // Si 'conectado' es true, fue una desconexión inesperada
                ventana.mostrarError("Error: Se perdió la conexión con el servidor.");
            }
            // Si 'conectado' es false, es una desconexión normal (llamada a desconectar())
            // y no mostramos error.
        } finally {
            // Asegurarse de limpiar todo si el bucle termina
            if (conectado) { // Si el bucle terminó pero seguíamos "conectados"
                desconectar();
            }
        }
    }

    /*
     * Procesa un mensaje recibido del servidor y actualiza la GUI.
     */
    private void procesarMensajeServidor(String msg) {
        try {
            if (msg.startsWith("MSG:")) {
                // Mensaje público: MSG:<emisor>:<mensaje>
                String[] partes = msg.split(":", 3);
                ventana.agregarMensaje(partes[1] + " (Público): " + partes[2]);

            } else if (msg.startsWith("MSG_PRIVADO:")) {
                // Mensaje privado: MSG_PRIVADO:<emisor>:<mensaje>
                String[] partes = msg.split(":", 3);
                ventana.agregarMensaje(partes[1] + " (Privado): " + partes[2], Color.MAGENTA);

            } else if (msg.startsWith("LISTA_USUARIOS:")) {
                // Lista de usuarios: LISTA_USUARIOS:<user1>,<user2>,...
                String[] usuarios = msg.substring(15).split(",");
                ventana.actualizarListaUsuarios(usuarios);

            } else if (msg.startsWith("INFO:")) {
                // Mensaje del servidor: INFO:<mensaje>
                ventana.agregarMensaje("[SERVIDOR]: " + msg.substring(5), Color.GRAY);
            
            } else if (msg.startsWith("NUEVO_USUARIO:")) {
                // Nuevo usuario conectado: NUEVO_USUARIO:<username>
                ventana.agregarMensaje("[SERVIDOR]: " + msg.substring(14) + " se ha conectado.", new Color(0, 100, 0)); // Verde oscuro
            
            } else if (msg.startsWith("USUARIO_FUERA:")) {
                // Usuario desconectado: USUARIO_FUERA:<username>
                ventana.agregarMensaje("[SERVIDOR]: " + msg.substring(14) + " se ha desconectado.", Color.ORANGE);
            }
        } catch (Exception e) {
            System.err.println("Error procesando mensaje del servidor: " + msg);
            e.printStackTrace();
        }
    }

    /*
     * Método auxiliar para cerrar los streams y el socket.
     */
    private void cerrarRecursos() {
        try { if (salida != null) salida.close(); } catch (Exception e) {}
        try { if (entrada != null) entrada.close(); } catch (Exception e) {}
        try { if (socket != null) socket.close(); } catch (Exception e) {}
    }
}