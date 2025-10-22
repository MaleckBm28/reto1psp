package cliente;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color; // <-- IMPORTANTE
import javax.swing.JTextField;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager; // <-- IMPORTANTE
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionEvent;
import javax.swing.JTextPane;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

public class VentanaCliente extends JFrame {

    private static final long serialVersionUID = 1L;
    private JPanel contentPane;
    private JTextField ipField;
    private JTextField portField;
    private JLabel lblUsuario;
    private JTextField usuField;
    private JButton btnDesconectar;
    private JLabel lblEstado;
    private JTextField txtMensaje;
    private JButton btnConectar;
    private JCheckBox boxPrivado;
    private JComboBox<String> comboUsuarios;
    private JButton btnEnviar;
    private JTextPane textPane;
    private JScrollPane scrollPane;
    private StyledDocument doc;

    // Lógica del cliente
    private ClienteChat cliente;
    private String[] cacheListaUsuarios = new String[0]; // Cache de usuarios

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    // --- APLICA EL LOOK AND FEEL NATIVO ---
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    
                    VentanaCliente frame = new VentanaCliente();
                    frame.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public VentanaCliente() {
        // Inicializar la lógica del cliente
        cliente = new ClienteChat(this);

        setTitle("Chat Cliente");
        setResizable(false); 
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBounds(100, 100, 725, 469); // El tamaño fijo original
        contentPane = new JPanel();
        contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(contentPane);
        contentPane.setLayout(null); 

        // Listener para el cierre de ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cliente.isConectado()) {
                    cliente.desconectar();
                }
                System.exit(0);
            }
        });

        // --- Fila Superior  ---
        
        JLabel ipText = new JLabel("IP : ");
        ipText.setFont(new Font("Tahoma", Font.PLAIN, 14));
        ipText.setBounds(10, 26, 31, 24);
        contentPane.add(ipText);

        ipField = new JTextField();
        ipField.setText("localhost");
        ipField.setToolTipText("");
        ipField.setFont(new Font("Tahoma", Font.PLAIN, 14));
        ipField.setBounds(40, 27, 94, 24);
        contentPane.add(ipField);
        ipField.setColumns(10);

        JLabel portText = new JLabel("Puerto : ");
        portText.setFont(new Font("Tahoma", Font.PLAIN, 14));
        portText.setBounds(143, 26, 62, 24);
        contentPane.add(portText);

        portField = new JTextField();
        portField.setText("9090");
        portField.setToolTipText("");
        portField.setFont(new Font("Tahoma", Font.PLAIN, 14));
        portField.setColumns(10);
        portField.setBounds(203, 27, 74, 24);
        contentPane.add(portField);

        lblUsuario = new JLabel("Usuario : ");
        lblUsuario.setFont(new Font("Tahoma", Font.PLAIN, 14));
        lblUsuario.setBounds(287, 26, 62, 24);
        contentPane.add(lblUsuario);

        usuField = new JTextField();
        usuField.setToolTipText("user");
        usuField.setFont(new Font("Tahoma", Font.PLAIN, 14));
        usuField.setColumns(10);
        usuField.setBounds(346, 27, 106, 24);
        contentPane.add(usuField);

        // --- Botones Superiores  ---
        
        btnConectar = new JButton("Conectar");
        btnConectar.addActionListener(e -> accionConectar());
        btnConectar.setBackground(new Color(144, 238, 144)); // Verde claro
        btnConectar.setOpaque(true);
        btnConectar.setBorderPainted(false);
        btnConectar.setBounds(462, 26, 110, 23);
        contentPane.add(btnConectar);

        btnDesconectar = new JButton("Desconectar");
        btnDesconectar.addActionListener(e -> accionDesconectar());
        btnDesconectar.setBackground(new Color(240, 128, 128)); // Rojo claro
        btnDesconectar.setOpaque(true);
        btnDesconectar.setBorderPainted(false);
        btnDesconectar.setBounds(582, 26, 110, 23);
        contentPane.add(btnDesconectar);

        // --- Label de Estado ---
        
        lblEstado = new JLabel("Usuario Desconectado");
        lblEstado.setFont(new Font("Tahoma", Font.PLAIN, 13));
        lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
        // Centrado debajo de los botones
        lblEstado.setBounds(462, 60, 230, 24); 
        contentPane.add(lblEstado);

        // --- Área de Chat  ---
        
        scrollPane = new JScrollPane();
        scrollPane.setBounds(10, 90, 682, 287);
        contentPane.add(scrollPane);

        textPane = new JTextPane();
        textPane.setEditable(false);
        scrollPane.setViewportView(textPane);
        doc = textPane.getStyledDocument();

        // --- Fila Inferior ---
        
        boxPrivado = new JCheckBox("Privado");
        boxPrivado.setFont(new Font("Tahoma", Font.PLAIN, 14));
        boxPrivado.addActionListener(e -> filtrarComboUsuarios());
        boxPrivado.setBounds(10, 389, 82, 23);
        contentPane.add(boxPrivado);

        JLabel txtPara = new JLabel("Para : ");
        txtPara.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtPara.setBounds(99, 388, 48, 24);
        contentPane.add(txtPara);


        comboUsuarios = new JComboBox<>();
        comboUsuarios.setFont(new Font("Tahoma", Font.PLAIN, 14));
        comboUsuarios.setBounds(143, 390, 180, 21);
        contentPane.add(comboUsuarios);


        txtMensaje = new JTextField();
        txtMensaje.setToolTipText("");
        txtMensaje.setFont(new Font("Tahoma", Font.PLAIN, 14));
        txtMensaje.setColumns(10);
        txtMensaje.setBounds(333, 389, 239, 24); // Posición y ancho ajustados
        contentPane.add(txtMensaje);

  
        btnEnviar = new JButton("Enviar");
        btnEnviar.setFont(new Font("Tahoma", Font.PLAIN, 13));
        btnEnviar.addActionListener(e -> accionEnviar());
        btnEnviar.setBackground(new Color(173, 216, 230)); // Azul claro
        btnEnviar.setOpaque(true);
        btnEnviar.setBorderPainted(false);
        btnEnviar.setBounds(582, 390, 110, 23);
        contentPane.add(btnEnviar);

        // Estado inicial de la interfaz
        setEstadoConectado(false);
    }
   
    private void accionConectar() {
        String ip = ipField.getText().trim();
        String puertoStr = portField.getText().trim();
        String usuario = usuField.getText().trim();

        if (ip.isEmpty() || puertoStr.isEmpty() || usuario.isEmpty()) {
            mostrarError("¡Oye! No te olvides de poner la IP, el Puerto y tu Usuario.");
            return;
        }

        try {
            int puerto = Integer.parseInt(puertoStr);
            cliente.conectar(ip, puerto, usuario);
        } catch (NumberFormatException e) {
            mostrarError("¡Error! El puerto solo puede tener números.");
        }
    }

    private void accionDesconectar() {
        cliente.desconectar();
    }

    private void accionEnviar() {
        String mensaje = txtMensaje.getText().trim();
        if (mensaje.isEmpty()) {
            return;
        }

        if (boxPrivado.isSelected()) {
            String destinatario = (String) comboUsuarios.getSelectedItem();
            if (destinatario == null) {
                mostrarError("¿A quién le vas a mandar eso? Elige un destinatario para el mensaje privado.");
                return;
            }
            cliente.enviarMensajePrivado(destinatario, mensaje);
            agregarMensaje("Tú (Privado a " + destinatario + "): " + mensaje, Color.BLUE);
        } else {
            cliente.enviarMensajePublico(mensaje);
        }
        txtMensaje.setText("");
    }

    public void agregarMensaje(String msg, Color color) {
        SwingUtilities.invokeLater(() -> {
            try {
                SimpleAttributeSet attrs = new SimpleAttributeSet();
                StyleConstants.setForeground(attrs, color);
                StyleConstants.setFontSize(attrs, 14);
                
                doc.insertString(doc.getLength(), msg + "\n", attrs);
                textPane.setCaretPosition(doc.getLength());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }
    
    public void agregarMensaje(String msg) {
        agregarMensaje(msg, Color.BLACK);
    }

    public void actualizarListaUsuarios(String[] usuarios) {
        this.cacheListaUsuarios = usuarios;
        filtrarComboUsuarios();
    }
    
    private void filtrarComboUsuarios() {
        SwingUtilities.invokeLater(() -> {
            String seleccionado = (String) comboUsuarios.getSelectedItem();
            comboUsuarios.removeAllItems();
            
            if (boxPrivado.isSelected()) {
                comboUsuarios.setEnabled(true);
                boolean reSeleccionar = false;
                
                for (String user : cacheListaUsuarios) {
                    if (!user.equals(usuField.getText().trim())) {
                        comboUsuarios.addItem(user);
                        if (user.equals(seleccionado)) {
                            reSeleccionar = true;
                        }
                    }
                }
                
                if (reSeleccionar) {
                    comboUsuarios.setSelectedItem(seleccionado);
                }
                
            } else {
                comboUsuarios.addItem("Todos");
                comboUsuarios.setEnabled(false);
            }
        });
    }

    public void mostrarError(String msg) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, msg, "¡Ups! Algo salió mal", JOptionPane.WARNING_MESSAGE);
        });
    }

    public void setEstadoConectado(boolean conectado) {
        SwingUtilities.invokeLater(() -> {
            ipField.setEnabled(!conectado);
            portField.setEnabled(!conectado);
            usuField.setEnabled(!conectado);
            btnConectar.setEnabled(!conectado);

            btnDesconectar.setEnabled(conectado);
            btnEnviar.setEnabled(conectado);
            txtMensaje.setEnabled(conectado);
            boxPrivado.setEnabled(conectado);
            // 'comboUsuarios' se gestiona en filtrarComboUsuarios()

            if (conectado) {
                lblEstado.setText("Conectado: " + usuField.getText());
                lblEstado.setForeground(new Color(0, 128, 0)); // Verde
                filtrarComboUsuarios(); // Actualiza el combo al conectar
            } else {
                lblEstado.setText("Usuario Desconectado");
                lblEstado.setForeground(Color.RED);
                
                this.cacheListaUsuarios = new String[0]; 
                boxPrivado.setSelected(false);
                filtrarComboUsuarios(); // Limpia y deshabilita el combo
            }
        });
    }
}