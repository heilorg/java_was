package was;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class WASMain {

	public static void main(String[] args) {
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(8080); // 8080포트에서 ServerSocket을 생성
			System.out.println("client를 기다립니다.");
			
			while(true) {
				// 클라이언트다 접속할때까지 기다림
				Socket client = listener.accept();
				System.out.println(client);
				
				// listener.accept()가 블러킹 메소드여서 요청이 올때 블로킹이되는 문제가 있기에  thread 처리가 가능한 was 로 변경
//				1. x thread 처리 
//				handleSocket(client);
				
				new Thread(() ->  { // jdk 1.8 lmd 형식
					try {
						handleSocket(client);
					}catch(Exception ex) {
						ex.printStackTrace();
					}
				}).start();
			}
		}catch(Exception ex) {
			ex.printStackTrace();
		}finally {
			try {
				listener.close();
			}catch(Exception e) {}
		}

	}
	
	public static void handleSocket(Socket client) throws IOException {
//		InputStream in = client.getInputStream();
//		BufferedReader br = new BufferedReader(new InputStreamReader(in));
//		
//		// 1. byte로 한번에 읽어서 출력하기
////		byte[] buffer = new byte[1024];
////		int count = 0;
////		while((count = in.readLine()) != null) {
////			System.out.write(buffer, 0, count);
////		}
//		
//		// 2. String변수에 담아서 출력하기
//		
//		String line = "";
//		while((line = br.readLine()) != null) {
//			System.out.println(line);
//		}
//		
//		in.close();
//		client.close(); // 클라이언트와 접속이 종료된다
		
		// httprequest 클래스 생성 이후
		
		OutputStream out = client.getOutputStream(); //클라이언트에게 데이터를 보내기 위한 작업
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));

        InputStream in = client.getInputStream(); //클라이언트에게서 받은 데이터를 처리를 위한 작업
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        HttpRequest request = new HttpRequest();
        line = br.readLine();
        String[] firstLineArgs = line.split(" ");
        request.setMethod(firstLineArgs[0]);
        request.setPath(firstLineArgs[1]);

        while((line = br.readLine()) != null){
            if("".equals(line)){ // 헤더를 읽고 빈줄을 만나면
                break;
            }
            String[] headerArray = line.split(" ");
            if(headerArray[0].startsWith("Host:")){
                request.setHost(headerArray[1].trim());
            }else if(headerArray[0].startsWith("Content-Length:")){
                int length = Integer.parseInt(headerArray[1].trim());
                request.setContentLength(length);
            }else if(headerArray[0].startsWith("User-Agent:")){
                request.setUserAgent(line.substring(12));
            }else if(headerArray[0].startsWith("Content-Type:")){
                request.setContentType(headerArray[1].trim());
            }
        }
        System.out.println(request);

        String baseDir = "/tmp/wasroot";
        String fileName = request.getPath();

        if("/".equals(fileName)){
            fileName = "/index.html";
        }else if(fileName.endsWith(".png")){ //png 이미지일때
            fileName = request.getPath();
        }else{ //잘못된 경로 처리
            fileName = "/error.html";
        }
        fileName = baseDir + fileName;

        String contentType = "text/html; charset=UTF-8";
        if(fileName.endsWith(".png")){
            contentType =  "image/png";
        }

        File file = new File(fileName); // java.io.File
        long fileLength = file.length();

        if(file.isFile()){
            pw.println("HTTP/1.1 200 OK");
            pw.println("Content-Type: " + contentType);
            pw.println("Content-Length: " + fileLength);
            pw.println();
        }else{
            pw.println("HTTP/1.1 404 OK");
            pw.println("Content-Type: " + contentType);
            pw.println("Content-Length: " + fileLength);
            pw.println();
        }

        pw.flush(); // 헤더와 빈줄을 char형식으로 출력

        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int readCount = 0;
        while((readCount = fis.read(buffer)) != -1){
            out.write(buffer,0,readCount);
        }
        out.flush();


        out.close();
        in.close();
        client.close(); // 클라이언트와 접속이 close된다.
	}
}
