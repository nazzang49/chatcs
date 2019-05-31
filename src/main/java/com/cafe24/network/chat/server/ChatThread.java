package com.cafe24.network.chat.server;

import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import com.cafe24.network.chat.client.ChatClient;

import java.util.HashMap;
import java.util.Map;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatThread extends Thread{

	private String nickname = null;
	private Socket client = null;
	//클라이언트 명단
	private HashMap<String, PrintWriter> listClients = null;
	
	public ChatThread(Socket client, HashMap<String, PrintWriter> listClients) {
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
					int firstAlpha = msg.indexOf("@");
					int firstBlank = msg.indexOf(" ");
					int len = msg.length();
					//귓속말 (@ 존재, 내용 구분용 공백 존재, 문자열 길이 2 이상, @ 바로 뒤 문자가 공백 아닌 경우에만 인정)
					if(firstAlpha!=-1&&firstBlank!=-1&&msg.charAt(firstAlpha+1)!=' '&&len>=2) {
						//받는 이의 이름만 추출
						String to = msg.substring(firstAlpha+1, firstBlank);
						newMsg = msg.substring(firstBlank+1, len);
						doMsg(newMsg, to, pw);
					}
					//전체메시지
					else {
						newMsg = msg.substring(4, len);
						doMsg(newMsg);
					}
				}
			}
		}catch(IOException e) {
			consoleLog(this.nickname+"님이 퇴장하셨습니다.");
			e.printStackTrace();
		}
	}

	//채팅방 입장(본인 채팅창에는 구문 표시 X)
	private void doJoin(String nickname, PrintWriter pw) {
		this.nickname=nickname;
		//클라이언트 명단에 추가
		addClient(pw);
		String data = this.nickname+"님이 입장하셨습니다.";
		broadcast(data,this.nickname);
	}
	
	//채팅방 퇴장(특정 클라이언트가 오픈한 출력 통로를 기준으로 각 접속자 구분)
	private void doQuit(PrintWriter pw) {
		//클라이언트 명단에서 제외
		removeClient(pw);
		String data = this.nickname+"님이 퇴장하셨습니다.";
		broadcast(data);
	}
	
	//메시지 전송(클 -> 모든 클)
	private void doMsg(String data) {
		broadcast(this.nickname+"님의 말-"+data);
	}
	
	//귓속말 전송(클 -> 특정 클)
	private void doMsg(String data, String to, PrintWriter from) {
		broadcast(this.nickname+"님의 귓속말-"+data, to, from);
	}
	
	//명단 추가
	private void addClient(PrintWriter pw) {
		synchronized (listClients) {
			listClients.put(this.nickname, pw);
		}
	}
	
	//명단 제외
	private void removeClient(PrintWriter pw) {
		synchronized (listClients) {
			//특정 키-값 쌍을 가지는 접속자 연결 종료
			listClients.remove(this.nickname, pw);
		}
	}
	
	//조인 전용 broadcast
	private void broadcast(String msg, String nickname) {
		synchronized (listClients) {
			for(Map.Entry<String, PrintWriter> entry : listClients.entrySet()) {
				//본인 채팅창에는 입장 구문 표시 X
				if(!entry.getKey().equals(nickname)) {
					entry.getValue().println(msg);	
				}
			}
		}
	}
	
	//메시지 전송(클 -> 모든 클)
	private void broadcast(String msg) {
		synchronized (listClients) {
			//각 클라이언트의 스트림을 통해 모두 알아야할 이슈 전달(퇴장 / 입장 등)
			for(Map.Entry<String, PrintWriter> entry : listClients.entrySet()) {
				entry.getValue().println(msg);
			}
		}
	}
	
	//귓속말 전송(클 -> 특정 클)
	private void broadcast(String msg, String to, PrintWriter from) {
		//현재 접속자 명단에 귓속말 대상이 있으면 = 전송
		if(listClients.containsKey(to)) {
			PrintWriter pw = listClients.get(to);
			//서 -> to 클라이언트에게 전달
			pw.println(msg);
		}
		//현재 접속자 명단에 귓속말 대상이 없으면 = 오류 메시지 출력
		else {
			from.println(to+"님은 현재 미접속 상태입니다.");
		}
	}
	
	private void consoleLog(String log) {
		System.out.println(log);
	}
}
