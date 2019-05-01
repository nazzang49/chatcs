package com.cafe24.network.chat.server;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ChatThread extends Thread{

	private String nickname = null;
	private Socket client = null;
	//클라이언트 명단
	private ArrayList<PrintWriter> listClients = null;
	
	public ChatThread(Socket client, ArrayList<PrintWriter> listClients) {
		this.client=client;
		this.listClients=listClients;
	}
	
	@Override
	public void run() {
		try {
			//클라이언트
			//1) 읽기
			BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream(),"utf-8"));
			//2) 쓰기 + auto flush
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"utf-8"),true);
			
			//클라이언트의 입력 및 블로킹(무한루프 = 입력하는 순간까지)
			while(true) {
				String msg = br.readLine();
				if(msg==null) {
					consoleLog("연결 끊김 by 클라이언트");
					doQuit(pw);
					break;
				}
				
				//입력 개행 중 맨 앞 명령어만 남기고 나머지는 인코딩 진행
				//Base64 인코딩
				//1) 인코더 생성
				Encoder encoder = Base64.getEncoder();
				//2) 인코딩 하고자 하는 문자열 바이트 배열로 저장
				byte [] tmp = msg.getBytes();
				//3) 인코딩 실행
				byte [] encodeStr = encoder.encode(tmp);
				
				//Base64 디코딩
				//1) 디코더 생성
				Decoder decoder = Base64.getDecoder();
				//2) 디코딩 실행 (인코딩 된 바이트 배열을 다시 디코드)
				byte [] decodeStr = decoder.decode(encodeStr);
				
				//System.out.println("인코딩 전 : "+msg);
				//System.out.println("인코딩 후 : "+new String(encodeStr));
				//System.out.println("디코딩 후 : "+new String(decodeStr));
				
				//index 0 = 행위, index 1 = 실제 메시지
				String [] tokens = msg.split(":");
				//방 입장
				if("join".equals(tokens[0])) {
					doJoin(tokens[1], pw);
				}
				//방 퇴장
				else if("quit".equals(tokens[0])) {
					doQuit(pw);
				}
				//메시지 전송
				else if("msg".equals(tokens[0])) {
					String newMsg = "";
					for(int i=4;i<msg.length();i++) {
						newMsg+=msg.charAt(i);
					}
					doMsg(newMsg);
				}
			}
		}catch(IOException e) {
			consoleLog(this.nickname+"님이 퇴장하셨습니다.");
			e.printStackTrace();
		}
	}
	
	//채팅방 퇴장(특정 클라이언트가 오픈한 출력 통로를 기준으로 각 접속자 구분)
	private void doQuit(PrintWriter pw) {
		//클라이언트 명단에서 제외
		removeClient(pw);
		String data = this.nickname+"님이 퇴장하셨습니다.";
		broadcast(data);
	}
	
	private void doJoin(String nickname, PrintWriter pw) {
		this.nickname=nickname;
		//클라이언트 명단에 추가
		addClient(pw);
		String data = this.nickname+"님이 입장하셨습니다.";
		broadcast(data);
	}
	
	//메시지 전송(클 -> 모든 클)
	private void doMsg(String data) {
		broadcast(data);
	}
	
	//명단 추가
	private void addClient(PrintWriter pw) {
		synchronized (listClients) {
			listClients.add(pw);
		}
	}
	
	//명단 제외
	private void removeClient(PrintWriter pw) {
		synchronized (listClients) {
			listClients.remove(pw);
		}
	}
	
	//이슈 전파(클 -> 모든 클)
	private void broadcast(String msgToAllTheClients) {
		synchronized (listClients) {
			//각 클라이언트의 스트림을 통해 모두 알아야할 이슈 전달(퇴장 / 입장 등)
			for(PrintWriter pw : listClients) {
				pw.println(msgToAllTheClients);
			}
		}
	}
	
	private void consoleLog(String log) {
		System.out.println(log);
	}
}
