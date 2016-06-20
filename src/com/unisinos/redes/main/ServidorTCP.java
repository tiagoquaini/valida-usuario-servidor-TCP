package com.unisinos.redes.main;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;

import com.unisinos.redes.model.Usuario;
import com.unisinos.redes.util.Constants;

public class ServidorTCP {
	
	private static Hashtable<String, Usuario> bancoUsuarios = new Hashtable<String, Usuario>();
	
	public static void main(String[] args) {
		try {
			// Criado o servidor na porta determinada
			ServerSocket servidor = new ServerSocket(12345);
			System.out.println("Servidor executando na porta '12345'");
			
			// Servidor executa infinitamente esperando por algum cliente 
			while(true) {
				System.out.println("Aguardando conexão de cliente...");
				
				// Execução é interrompida neste ponto até que algum cliente solicite conexão 
				Socket cliente = servidor.accept();
				
				// cliente se conecta
				System.out.println("Cliente conectado: " + cliente.getInetAddress().getHostAddress());

				// recebe os dados do cliente
				String[] dados;
				try {
					dados = recebeDadosCliente(cliente.getInputStream());
				} catch (Exception e) {
					// dados incorretos - usuario recebe NOK como resposta e execução para este cliente é interrompida
					enviaRetornoNOK(cliente);
					cliente.close();
					continue;
				}
				
				// executa o processamento e envia uma resposta
				processaDadosCliente(dados, cliente);
				
				// Fecha a conexão com o cliente
				cliente.close();
			}
		} catch (IOException e) {
			System.out.println("Ocorreu um erro durante a execução: " + e.getMessage());
		}
	}
	
	/**
	 * Recebe os dados do cliente.
	 * @param inputStream
	 * @return
	 * @throws Exception
	 */
	private static String[] recebeDadosCliente(InputStream inputStream) throws Exception {
		// converte a entrada de dados
		ObjectInputStream objInputStream = new ObjectInputStream(inputStream);
		String entrada = (String) objInputStream.readObject();
		String[] dados = entrada.split(",");
		
		verificaDadosCorretos(dados);
		
		return dados;
	}
	
	/**
	 * Processa os dados recebidos do cliente.
	 * @param dados
	 * @param cliente
	 * @throws IOException
	 */
	private static void processaDadosCliente(String[] dados, Socket cliente) throws IOException {
		Usuario usuario = new Usuario(dados[1], dados[2]);
		
		switch (dados[0]) {
		case Constants.ACAO_SERVIDOR_VALIDAR:
			validaUsuario(usuario, cliente);
			break;
		case Constants.ACAO_SERVIDOR_CADASTRAR:
			criaUsuario(usuario, cliente);
			break;
		}
	}
	
	/**
	 * Valida usuário e senha recebidos.
	 * @param usuario
	 * @param cliente
	 * @throws IOException
	 */
	private static void validaUsuario(Usuario usuario, Socket cliente) throws IOException {
		if (bancoUsuarios.containsKey(usuario.getUsername())) {
			Usuario hashUser = bancoUsuarios.get(usuario.getUsername());
			
			if (hashUser.getSenha().equals(usuario.getSenha()) && 
					hashUser.getUsername().equals(usuario.getUsername()))
				enviaRetornoOK(cliente);
			else 
				enviaRetornoNOK(cliente);
		}
		else 
			enviaRetornoNOK(cliente);
	}
	
	/**
	 * Cria um novo usuário. Caso o mesmo já exista, retorna um erro.
	 * @param usuario
	 * @param cliente
	 * @throws IOException
	 */
	private static void criaUsuario(Usuario usuario, Socket cliente) throws IOException {
		if (bancoUsuarios.containsKey(usuario.getUsername()))
			enviaRetornoNOK(cliente);
		else {
			bancoUsuarios.put(usuario.getUsername(), usuario);
			enviaRetornoOK(cliente);
		}
	}
	
	/**
	 * Envia uma resposta ao cliente - OK
	 * Usuário e senha corretos / Usuário cadastrado com sucesso / A execução foi processada sem problemas
	 * @param cliente
	 * @throws IOException
	 */
	private static void enviaRetornoOK(Socket cliente) throws IOException {
		ObjectOutputStream resposta = criaOutputStreamObj(cliente); 
		resposta.writeObject("OK");
		resposta.close();
	}
	
	/**
	 * Envia uma resposta ao cliente - NOK
	 * Usuário e senha incorretos / Usuário já cadastrado / Ocorreu um erro durante a execução
	 * @param cliente
	 * @throws IOException
	 */
	private static void enviaRetornoNOK(Socket cliente) throws IOException {
		ObjectOutputStream resposta = criaOutputStreamObj(cliente); 
		resposta.writeObject("NOK");
		resposta.close();
	}
	
	/**
	 * Cria um objeto output stream.
	 * @param cliente
	 * @return
	 * @throws IOException
	 */
	private static ObjectOutputStream criaOutputStreamObj(Socket cliente) throws IOException {
		ObjectOutputStream resposta = new ObjectOutputStream(cliente.getOutputStream());
		resposta.flush();
		return resposta;
	}
	
	/**
	 * Valida se os dados recebidos pelo servidor estão corretos. Caso não estejam, retorna NOK.
	 * @param dados
	 * @throws Exception
	 */
	private static void verificaDadosCorretos(String[] dados) throws Exception {
		if ((dados.length != 3) || (!dados[0].equals("1") && !dados[0].equals("2"))) 
			throw new Exception("Entrada mal formada");
	}
}
