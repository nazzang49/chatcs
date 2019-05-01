package com.cafe24.network.chat.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

//서버 단
public class ChatServer {

	//IP 및 PORT 정보 입력
	private static final String HOST_IP = "0.0.0.0";
	private static final int PORT = 7000;
	
	public static void main(String[] args) throws SocketException, IOException {
		
		//서버에 접속하는 클라이언트 정보 저장할 리스트 배열
		ArrayList<PrintWriter> listClients = new ArrayList<>();
		//1) 서버 소켓 생성
		ServerSocket ss = null;
		try {
			ss = new ServerSocket();
			//1-1) Time-Wait(아직 통신 중인 소켓들이 완전히 닫히지 않은 과정)중 소켓에 포트번호 할당을 가능케 하기 위한 조치 = 재사용 부여
			//ss.setReuseAddress(true);
			//2) 바인딩 작업(IP+port)
			ss.bind(new InetSocketAddress(HOST_IP,PORT));
			consoleLog("연결 기다리는 중 - " +HOST_IP+":"+PORT);
			
			//3) 어셉트 작업 (여러개의 클라이언트 요청 수락 by 스레드 활용)
			while(true) {
				Socket client = ss.accept();
				//클라이언트 구분할 스레드 선언 및 실행
				new ChatThread(client,listClients).start();
			}
		}catch(SocketException e) {
			System.out.println("예상치 못한 오류로 서버가 종료되었습니다.");
			e.printStackTrace();
		}finally {
			//6) 연결 종료
			if(!ss.isClosed()&&ss!=null) {
				ss.close();
			}	
		}
	}
	
	//현재 접속 중인 클라이언트의 요청 콘솔에 표시
	public static void consoleLog(String log) {
		System.out.println("[server] "+Thread.currentThread().getName()+log);
	}
}