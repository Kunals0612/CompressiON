import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.util.zip.GZIPOutputStream;
public class Main {
   private static String directoryPath;
    public static void main(String[] args) {
        System.out.println("Server started...");
        if(args.length != 2 && !args[0].equals("--directory")){
            System.err.println("No Args Passed! ");
        }
        directoryPath = args[1];
        try (ServerSocket serverSocket = new ServerSocket(4221)) {
            serverSocket.setReuseAddress(true);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted New Connection from " + clientSocket.getInetAddress().getHostAddress() + ":" + clientSocket.getPort());
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.out.println("IOException: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                OutputStream outputStream = clientSocket.getOutputStream()
            ) {
                String requestLine = br.readLine();
                System.out.println("RequestLine: "+requestLine);
                String httpResponse="";

                if (requestLine != null) {
                    String[] tokens = requestLine.split(" ");
                    String method = tokens[0];
                    String path = tokens.length > 1 ? tokens[1] : "";
                    System.out.println("Path:"+path);
                    if(method.equals("GET") && path.startsWith("/files/")){
                       handleFileRequest(path,outputStream);
                    }else if(method.equals("POST") && path.startsWith("/files/")){
                        handlePostRequest(path, outputStream, br);
                    }else if ("GET".equals(method)) {
                        httpResponse = handleGetRequest(path, br, outputStream);
                    } else {
                        httpResponse = "HTTP/1.1 405 Method Not Allowed\r\n\r\n";
                    }
                } else {
                    httpResponse = "HTTP/1.1 400 Bad Request\r\n\r\n";
                }

                outputStream.write(httpResponse.getBytes());
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private String handleGetRequest(String path, BufferedReader br, OutputStream outputStream) throws IOException {
            if ("/user-agent".equals(path)) {
                return handleUserAgent(br);
            } else if (path.startsWith("/echo")) {
                handleCompressRequest(path,outputStream, br);
                return "";
            } else {
                return "HTTP/1.1 404 Not Found\r\n\r\n";
            }
        }

        private String handleUserAgent(BufferedReader br) throws IOException {
            String userAgent = null;
            String headerLine;
            while ((headerLine = br.readLine()) != null && !headerLine.isEmpty()) {
                if (headerLine.startsWith("User-Agent:")) {
                    userAgent = headerLine.substring(12).trim();
                    break;
                }
            }
            if (userAgent != null) {
                return "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + userAgent.length() + "\r\n" +
                        "\r\n" +
                        userAgent;
            }
            return "HTTP/1.1 400 Bad Request\r\n\r\n";
        }

        // private String handleEcho(String path) {
        //     String content = path.substring(6); // Remove "/echo" from the start
        //     return "HTTP/1.1 200 OK\r\n" +
        //             "Content-Type: text/plain\r\n" +
        //             "Content-Length: " + content.length() + "\r\n" +
        //             "\r\n" +
        //             content;
        // }

        private void handlePostRequest(String Path, OutputStream outputStream, BufferedReader br) throws IOException{
              String fileName = Path.substring(7);
              File file = new File(directoryPath, fileName);

              int contentLength = 0;
              String headerLine;
              while((headerLine = br.readLine()) != null && !headerLine.isEmpty()){
                   if(headerLine.startsWith("Content-Length:")){
                      contentLength = Integer.parseInt(headerLine.substring(16).trim());
                   }
              }
              if(contentLength <= 0){
                outputStream.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
                return;
              }
              char[] body = new char[contentLength];
              int bodyLen = br.read(body,0,contentLength);
              if(bodyLen != contentLength){
                outputStream.write("HTTP/1.1 400 Bad Request\r\n\r\n".getBytes());
                return;
              } 
              String requestBody = new String(body);
              try(FileWriter fileWriter = new FileWriter(file)){
                fileWriter.write(requestBody);
              }
              String httpResponse = "HTTP/1.1 201 Created\r\n\r\n";
              outputStream.write(httpResponse.getBytes());
        }
        private void handleFileRequest(String Path, OutputStream outputStream) throws IOException{
           String fileName = Path.substring(7);
           File file = new File(directoryPath,fileName);

           if(file.exists() && file.isFile()){
              long fileLength = file.length();
              try(FileInputStream fis = new FileInputStream(file)){
                 String httpResponse = "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: text/plain\r\n" +
                        "Content-Length: " + fileLength + "\r\n" +
                        "\r\n";
                 outputStream.write(httpResponse.getBytes());
                 byte[] fileBuffer = new byte[1024];
                 int bytes;
                 while((bytes = fis.read(fileBuffer)) != -1){
                      outputStream.write(fileBuffer,0,bytes);
                 }
                 outputStream.flush();
              }
           }else{
               outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
           }
        }
        private void handleCompressRequest(String Path, OutputStream outputStream, BufferedReader br) throws IOException{
            String responseText = Path.substring(6);
            boolean isgzip = false;
            String headerLine;
            while((headerLine = br.readLine()) != null && !headerLine.isEmpty()){
                if(headerLine.startsWith("Accept-Encoding:")){
                    String[] encodings = headerLine.substring(16).trim().split(",\\s*");
                    for(String encoding : encodings){
                        if(encoding.equals("gzip")){
                            isgzip = true;
                            break;
                        }
                    }
                }
            }
            ByteArrayOutputStream compressedOutput = new ByteArrayOutputStream();
            byte[] compressedBody;
            if(isgzip){
                try(GZIPOutputStream gzipOutputStream = new GZIPOutputStream(compressedOutput)){
                    gzipOutputStream.write(responseText.getBytes());
                }
                compressedBody = compressedOutput.toByteArray();
            }else{
                compressedBody = responseText.getBytes();
            }
            String httpsResponse = "";
            httpsResponse = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n";
            if(isgzip){
                httpsResponse += "Content-Encoding: gzip\r\n";
            }
            httpsResponse += "Content-Length: " + compressedBody.length + "\r\n";

            outputStream.write(httpsResponse.getBytes());
            outputStream.write(compressedBody);
            outputStream.flush();
        }
    }
}
