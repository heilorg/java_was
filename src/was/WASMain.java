package was;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;


public class WASMain {

	public static void main(String[] args) {
		ServerSocket listener = null;
		try {
			listener = new ServerSocket(8080); // 8080��Ʈ���� ServerSocket�� ����
			System.out.println("client�� ��ٸ��ϴ�.");
			
			while(true) {
				// Ŭ���̾�Ʈ�� �����Ҷ����� ��ٸ�
				Socket client = listener.accept();
				System.out.println(client);
				
				// listener.accept()�� ��ŷ �޼ҵ忩�� ��û�� �ö� ���ŷ�̵Ǵ� ������ �ֱ⿡  thread ó���� ������ was �� ����
//				1. x thread ó�� 
//				handleSocket(client);
				
				new Thread(() ->  { // jdk 1.8 lmd ����
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
//		// 1. byte�� �ѹ��� �о ����ϱ�
////		byte[] buffer = new byte[1024];
////		int count = 0;
////		while((count = in.readLine()) != null) {
////			System.out.write(buffer, 0, count);
////		}
//		
//		// 2. String������ ��Ƽ� ����ϱ�
//		
//		String line = "";
//		while((line = br.readLine()) != null) {
//			System.out.println(line);
//		}
//		
//		in.close();
//		client.close(); // Ŭ���̾�Ʈ�� ������ ����ȴ�
		
		// httprequest Ŭ���� ���� ����
		
		OutputStream out = client.getOutputStream(); //Ŭ���̾�Ʈ���� �����͸� ������ ���� �۾�
        PrintWriter pw = new PrintWriter(new OutputStreamWriter(out));

        InputStream in = client.getInputStream(); //Ŭ���̾�Ʈ���Լ� ���� �����͸� ó���� ���� �۾�
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = null;
        HttpRequest request = new HttpRequest();
        line = br.readLine();
        String[] firstLineArgs = line.split(" ");
        request.setMethod(firstLineArgs[0]);
        request.setPath(firstLineArgs[1]);

        while((line = br.readLine()) != null){
            if("".equals(line)){ // ����� �а� ������ ������
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
        }else if(fileName.endsWith(".png")){ //png �̹����϶�
            fileName = request.getPath();
        }else{ //�߸��� ��� ó��
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

        pw.flush(); // ����� ������ char�������� ���

        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        int readCount = 0;
        while((readCount = fis.read(buffer)) != -1){
            out.write(buffer,0,readCount);
        }
        out.flush();


        out.close();
        in.close();
        client.close(); // Ŭ���̾�Ʈ�� ������ close�ȴ�.
	}
}
