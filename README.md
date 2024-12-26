# CompressiON

# HTTP Server Explanation

This document provides an overview of the HTTP server, named **CompressiON**, and the functions implemented in its code.

## Features of the Server
- Handles GET and POST requests.
- Serves files from a specified directory.
- Supports gzip compression for faster and smaller responses.
- Handles the `Accept-Encoding` header to determine if the client supports gzip.
- Returns appropriate HTTP status codes and responses for various scenarios.

---

## Server Workflow

1. **Start the Server**: 
   The server listens for incoming connections on port 4221. It accepts client connections and spawns a new thread to handle each connection.

2. **Parse Requests**: 
   Each request is parsed to determine the HTTP method (e.g., `GET`, `POST`) and the requested path.

3. **Handle Responses**: 
   The server processes the request and sends an appropriate HTTP response, including headers and a body if needed.

4. **Support for Compression**: 
   If the client specifies `gzip` in the `Accept-Encoding` header, the server sets `Content-Encoding: gzip` in the response and compresses the response body using the gzip algorithm.

---

## Functions Overview

### 1. **Main Server Setup**
```java
try (ServerSocket serverSocket = new ServerSocket(4221)) {
    serverSocket.setReuseAddress(true);
    while (true) {
        Socket clientSocket = serverSocket.accept();
        new Thread(new ClientHandler(clientSocket)).start();
    }
} catch (IOException e) {
    e.printStackTrace();
}
```
- Listens for incoming connections.
- Creates a new thread for each client to handle requests concurrently.

### 2. **ClientHandler (Runnable)**
```java
class ClientHandler implements Runnable {
    private final Socket clientSocket;
    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }
    @Override
    public void run() {
        // Handles client requests
    }
}
```
- Handles individual client connections.
- Parses and processes incoming requests.
- Sends appropriate HTTP responses.

### 3. **handleGetRequest**
```java
private String handleGetRequest(String path, BufferedReader br) throws IOException {
    if ("/user-agent".equals(path)) {
        return handleUserAgent(br);
    } else if (path.startsWith("/echo")) {
        return handleEcho(path);
    } else {
        return "HTTP/1.1 404 Not Found\r\n\r\n";
    }
}
```
- Processes `GET` requests.
- Supports `/user-agent` and `/echo/{string}` endpoints.

### 4. **handlePostRequest**
```java
private void handlePostRequest(String path, OutputStream outputStream, BufferedReader br) throws IOException {
    String fileName = path.substring(7);
    File file = new File(directoryPath, fileName);
    
    // Read headers and body
    // Write body to the specified file
    
    String httpResponse = "HTTP/1.1 201 Created\r\n\r\n";
    outputStream.write(httpResponse.getBytes());
}
```
- Handles `POST` requests for uploading files to the server.
- Saves the file in the server's directory.

### 5. **handleFileRequest**
```java
private void handleFileRequest(String path, OutputStream outputStream) throws IOException {
    String fileName = path.substring(7);
    File file = new File(directoryPath, fileName);
    
    if (file.exists() && file.isFile()) {
        // Send file contents as HTTP response
    } else {
        outputStream.write("HTTP/1.1 404 Not Found\r\n\r\n".getBytes());
    }
}
```
- Serves files requested by the client using the `/files/{filename}` endpoint.
- Returns a `404 Not Found` if the file does not exist.

### 6. **handleCompressRequest**
```java
private void handleCompressRequest(String path, OutputStream outputStream, BufferedReader br) throws Exception {
    String responseText = path.substring(6);
    boolean isGzip = false;
    
    // Check for gzip support in Accept-Encoding header
    
    String httpResponse = "HTTP/1.1 200 OK\n" +
                          "Content-Type: text/plain\n";
    if (isGzip) {
        httpResponse += "Content-Encoding: gzip\n";
        // Compress the response body
    }
    outputStream.write(httpResponse.getBytes());
    outputStream.write(responseText.getBytes()); // uncompressed body for now
}
```
- Processes requests with gzip compression support.
- Sets the `Content-Encoding` header to `gzip` if the client supports it.
- (Future) Compresses the response body using gzip.

### 7. **gzipCompression** (To Be Implemented)
```java
private byte[] gzipCompression(String data) throws IOException {
    ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
    try (GZIPOutputStream gzipStream = new GZIPOutputStream(byteStream)) {
        gzipStream.write(data.getBytes());
    }
    return byteStream.toByteArray();
}
```
- Compresses a string using gzip and returns the compressed byte array.

---

## Example Usage

### Start the Server
```bash
java Main --directory /path/to/serve
```

### Test GET Request
```bash
curl -v -H "Accept-Encoding: gzip" http://localhost:4221/echo/hello
```
- The server responds with a gzip-compressed body and appropriate headers.

---

## Future Enhancements
- Implement full gzip compression for response bodies.
- Add support for more HTTP methods.
- Enhance file handling with additional MIME types.
- Improve error handling and logging.

---

This HTTP server is designed to be a lightweight and extensible solution for basic HTTP request handling and file serving, with compression support for faster responses.
