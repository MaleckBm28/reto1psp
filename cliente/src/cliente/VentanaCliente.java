package cliente;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.Color;
import javax.swing.JTextField;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
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

	/*
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					VentanaCliente frame = new VentanaCliente();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/*
	 * Crear la ventana jframe
	 */
	public VentanaCliente() {
		// Inicializar la lógica del cliente
		cliente = new ClienteChat(this);

		setTitle("Chat Cliente");
		setResizable(false);
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // Controlar cierre
		setBounds(100, 100, 725, 469);
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

		JLabel ipText = new JLabel("IP : ");
		ipText.setFont(new Font("Tahoma", Font.PLAIN, 14));
		ipText.setBounds(10, 26, 31, 24);
		contentPane.add(ipText);

		ipField = new JTextField();
		ipField.setText("localhost"); 
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
		portField.setFont(new Font("Tahoma", Font.PLAIN, 14));
		portField.setColumns(10);
		portField.setBounds(203, 27, 74, 24);
		contentPane.add(portField);

		lblUsuario = new JLabel("Usuario : ");
		lblUsuario.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblUsuario.setBounds(287, 26, 62, 24);
		contentPane.add(lblUsuario);

		usuField = new JTextField();
		usuField.setFont(new Font("Tahoma", Font.PLAIN, 14));
		usuField.setColumns(10);
		usuField.setBounds(346, 27, 106, 24);
		contentPane.add(usuField);

		btnConectar = new JButton("Conectar");
		btnConectar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				accionConectar();
			}
		});
		btnConectar.setBounds(462, 26, 110, 23);
		contentPane.add(btnConectar);

		btnDesconectar = new JButton("Desconectar");
		btnDesconectar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				accionDesconectar();
			}
		});
		btnDesconectar.setBounds(582, 26, 110, 23);
		contentPane.add(btnDesconectar);

		lblEstado = new JLabel("Usuario Desconectado");
		lblEstado.setFont(new Font("Tahoma", Font.PLAIN, 13));
		lblEstado.setHorizontalAlignment(SwingConstants.CENTER);
		lblEstado.setBounds(512, 60, 137, 24);
		contentPane.add(lblEstado);

		boxPrivado = new JCheckBox("Privado");
		boxPrivado.setFont(new Font("Tahoma", Font.PLAIN, 14));
		boxPrivado.setBounds(10, 389, 74, 23);
		contentPane.add(boxPrivado);

		JLabel txtPara = new JLabel("Para : ");
		txtPara.setFont(new Font("Tahoma", Font.PLAIN, 14));
		txtPara.setBounds(90, 388, 44, 24);
		contentPane.add(txtPara);

		comboUsuarios = new JComboBox<>();
		comboUsuarios.setFont(new Font("Tahoma", Font.PLAIN, 14));
		comboUsuarios.setBounds(143, 390, 118, 21);
		contentPane.add(comboUsuarios);

		txtMensaje = new JTextField();
		txtMensaje.setToolTipText("");
		txtMensaje.setFont(new Font("Tahoma", Font.PLAIN, 14));
		txtMensaje.setColumns(10);
		txtMensaje.setBounds(271, 389, 296, 24);
		contentPane.add(txtMensaje);

		btnEnviar = new JButton("Enviar");
		btnEnviar.setFont(new Font("Tahoma", Font.PLAIN, 13));
		btnEnviar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				accionEnviar();
			}
		});
		btnEnviar.setBounds(582, 390, 110, 23);
		contentPane.add(btnEnviar);

		// Panel para mostrar mensajes
		scrollPane = new JScrollPane();
		scrollPane.setBounds(10, 90, 682, 287);
		contentPane.add(scrollPane);

		textPane = new JTextPane();
		textPane.setEditable(false);
		scrollPane.setViewportView(textPane);
		doc = textPane.getStyledDocument();

		// Estado inicial de la interfaz
		setEstadoConectado(false);
	}

	/*
	 * Lógica para el botón Conectar
	 */
	private void accionConectar() {
		String ip = ipField.getText().trim();
		String puertoStr = portField.getText().trim();
		String usuario = usuField.getText().trim();

		if (ip.isEmpty() || puertoStr.isEmpty() || usuario.isEmpty()) {
			mostrarError("IP, Puerto y Usuario no pueden estar vacíos.");
			return;
		}

		try {
			int puerto = Integer.parseInt(puertoStr);
			cliente.conectar(ip, puerto, usuario);
		} catch (NumberFormatException e) {
			mostrarError("El puerto debe ser un número válido.");
		}
	}

	/*
	 * Lógica para el botón Desconectar
	 */
	private void accionDesconectar() {
		cliente.desconectar();
	}

	/*
	 * Lógica para el botón Enviar
	 */
	private void accionEnviar() {
		String mensaje = txtMensaje.getText().trim();
		if (mensaje.isEmpty()) {
			return;
		}

		if (boxPrivado.isSelected()) {
			String destinatario = (String) comboUsuarios.getSelectedItem();
			if (destinatario == null || destinatario.equals("Todos")) {
				mostrarError("Debe seleccionar un destinatario para mensaje privado.");
				return;
			}
			cliente.enviarMensajePrivado(destinatario, mensaje);
			agregarMensaje("Tú (Privado a " + destinatario + "): " + mensaje, Color.BLUE);
		} else {
			cliente.enviarMensajePublico(mensaje);
			// El servidor nos devolverá nuestro propio mensaje público,
			// así que no necesitamos agregarlo aquí.
		}
		txtMensaje.setText("");
	}

	/*
	 * Añade un mensaje al JTextPane. Este método es seguro para hilos.
	 * 
	 * @param msg   El mensaje a añadir.
	 * @param color El color del texto.
	 */
	public void agregarMensaje(String msg, Color color) {
		SwingUtilities.invokeLater(() -> {
			try {
				SimpleAttributeSet attrs = new SimpleAttributeSet();
				StyleConstants.setForeground(attrs, color);
				StyleConstants.setFontSize(attrs, 14);

				doc.insertString(doc.getLength(), msg + "\n", attrs);
				// Auto-scroll
				textPane.setCaretPosition(doc.getLength());
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		});
	}

	/*
	 * Sobrecarga para mensajes normales (color negro)
	 */
	public void agregarMensaje(String msg) {
		agregarMensaje(msg, Color.BLACK);
	}

	/*
	 * Actualiza la lista de usuarios en el JComboBox. Seguro para hilos.
	 * 
	 * @param usuarios Array con los nombres de usuario.
	 */
	public void actualizarListaUsuarios(String[] usuarios) {
		SwingUtilities.invokeLater(() -> {
			comboUsuarios.removeAllItems();
			comboUsuarios.addItem("Todos"); // Opción para mensajes públicos
			for (String user : usuarios) {
				if (!user.equals(usuField.getText().trim())) { // No añadirnos a nosotros mismos
					comboUsuarios.addItem(user);
				}
			}
		});
	}

	/*
	 * Muestra un diálogo de error. Seguro para hilos.
	 * 
	 * @param msg El mensaje de error.
	 */
	public void mostrarError(String msg) {
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
		});
	}

	/*
	 * Habilita o deshabilita componentes de la GUI según el estado de conexión.
	 * 
	 * @param conectado true si está conectado, false en caso contrario.
	 */
	public void setEstadoConectado(boolean conectado) {
		SwingUtilities.invokeLater(() -> {
			ipField.setEnabled(!conectado);
			portField.setEnabled(!conectado);
			usuField.setEnabled(!conectado);
			btnConectar.setEnabled(!conectado);

			btnDesconectar.setEnabled(conectado);
			btnEnviar.setEnabled(conectado);
			txtMensaje.setEnabled(conectado);
			comboUsuarios.setEnabled(conectado);
			boxPrivado.setEnabled(conectado);

			if (conectado) {
				lblEstado.setText("Conectado: " + usuField.getText());
				lblEstado.setForeground(new Color(0, 128, 0)); // Verde
			} else {
				lblEstado.setText("Usuario Desconectado");
				lblEstado.setForeground(Color.RED);
				comboUsuarios.removeAllItems(); // Limpiar lista al desconectar
				// No borramos el chat para que pueda leerlo
			}
		});
	}
}