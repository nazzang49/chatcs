package com.cafe24.network.chat.client;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import javax.swing.JFrame;

//실제 채팅창 by java.awt GUI
public class ChatWindow {

	private String name;
	private JFrame frame;
	private Panel pannel;
	private Button buttonSend;
	//메시지 입력하는 부분
	private TextField textField;
	//메시지 확인하는 부분
	private TextArea textArea;
	private Socket client;
	
	public ChatWindow(String name, Socket client) {
		this.name=name;
		this.client=client;
		frame = new JFrame(name);
		pannel = new Panel();
		buttonSend = new Button("send");
		textField = new TextField();
		textArea = new TextArea(30, 80);
		
		frame.setTitle("채팅방");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//내장 클래스
		new ChatClientReceiveThread(this.client).start();
	}
	
	//GUI 창 설정
	public void show() {
		//전송버튼
		buttonSend.setBackground(Color.GRAY);
		buttonSend.setForeground(Color.WHITE);
		buttonSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				sendMessage();
			}
		});

		//입력부분
		//길이제한
		textField.setColumns(80);
		textField.addKeyListener(new KeyAdapter() {
			//엔터키 누르면 전송
			public void keyReleased(KeyEvent e) {
				char keyCode = e.getKeyChar();
				if (keyCode == KeyEvent.VK_ENTER) {
					sendMessage();
				}
			}
		});

		//패널설정
		pannel.setBackground(Color.LIGHT_GRAY);
		pannel.add(textField);
		pannel.add(buttonSend);
		frame.add(BorderLayout.SOUTH, pannel);

		//출력부분
		textArea.setEditable(false);
		frame.add(BorderLayout.CENTER, textArea);

		//전체 틀
		frame.addWindowListener(new WindowAdapter() {
			//닫기 클릭 시(퇴장 메시지 전송 후 창 종료)
			public void windowClosing(WindowEvent e) {
				PrintWriter pw;
				try {
					pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
					String request = "quit\r\n";
					pw.println(request);
					System.exit(0);
				}
				catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		//창 표시
		frame.setVisible(true);
		frame.pack();
	}
	
	//스레드 생성 및 출력
	private void sendMessage() {
		PrintWriter pw;
		try {
			pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true);
			//엔터키 눌러 입력된 메시지 문자열로 저장
			String msg = textField.getText();
			String data = "msg:"+name+"님의 말 - "+msg+"\r\n";
			pw.println(data);

			//입력 후 다시 빈 입력 창 + 커서 이동
			textField.setText("");
			textField.requestFocus();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private class ChatClientReceiveThread extends Thread{
		Socket client = null;
		
		ChatClientReceiveThread(Socket client){
			this.client=client;
		}

		//각 클라이언트 입력 실행
		public void run() {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(), "utf-8"));
				while(true) {
					String msg = br.readLine();
					//개행설정
					textArea.append(msg);
					textArea.append("\n");
				}
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
