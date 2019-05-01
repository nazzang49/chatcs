package com.cafe24.network.chat.client;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import org.omg.CORBA.portable.UnknownException;

//클라이언트 단
public class ChatClient {

	//상수 선언(대문자 변수명 지정) = 접속할 서버 소켓 정보 
	private static final String SERVER_IP = "192.168.1.27";
	private static final int SERVER_PORT = 7000;
	
	public static void main(String[] args) throws UnknownException {

		Scanner sc = null;
		Socket client = null;
		//닉네임
		String name = null;
		
		try {
			//1) 소켓 생성 및 키보드 입력
			client = new Socket();
			sc = new Scanner(System.in);
			
			while(true) {
				System.out.println("대화명 입력 필수");
				System.out.print(">>");
				name = sc.nextLine();
				
				if(!name.isEmpty()) {
					break;
				}
				System.out.println("한글자 이상 입력 필수");
			}
			//키보드 입력 종료
			sc.close();
			//1-1) 소켓 버퍼 사이즈 확인(통상 64K로 기본 설정)및 소켓 버퍼 사이즈 변경
			client.setReceiveBufferSize(1024*10);
			client.setSendBufferSize(1024*10);
			
			//1-2) Nagle 알고리즘 종료 = 조금씩 여러번 보내기 가능 = 딜레이 없이 전송
			client.setTcpNoDelay(true);
			
			//1-3) 읽는 과정에서 Timeout 설정
			//client.setSoTimeout(3000);
			
			//2) 서버 연결
			try {
				client.connect(new InetSocketAddress(SERVER_IP, SERVER_PORT));
				consoleLog("방에 입장하셨습니다.");
				//GUI 창 오픈
				new ChatWindow(name, client).show();
				//입장 알림 메시지 서버에 전송
				PrintWriter pw = new PrintWriter(new OutputStreamWriter(client.getOutputStream(),"utf-8"),true);
				String data = "join:"+name+"\r\n";
				pw.println(data);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void consoleLog(String log) {
		System.out.println(log);
	}
}
