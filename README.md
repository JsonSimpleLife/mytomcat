# 实现一个简单的Tomcat

## 1. Tomcat作用

我们的web应用会运行在Tomcat中，那么显然请求必定是先到达Tomcat的，Tomcat对于请求实际上会进行如下的处理：

- 提供Socket服务：Tomcat的启动，必然是Socket服务，支持http协议。
- 进行请求的分发：一个Tomcat可以为多个web应用提供服务，那么就需要把url下发到不同的web应用。
- 需要将请求和响应封装成request和response：我们在写后端代码的时候都是直接使用request和response的，这是因为Tomcat已经做好了。

下面我们就自己来实现这三步。

## 2. 实现代码

项目结构：

```
  src
    └─mytomcat
            BookServlet.java
            CarServlet.java
            MyRequest.java
            MyResponse.java
            MyServlet.java
            MyTomcat.java
            ServletMapping.java
            ServletMappingConfig.java
```

### 2.1 封装http请求和响应

```java
package mytomcat;

import java.io.IOException;
import java.io.InputStream;

/**
 * 封装http请求
 */
public class MyRequest {
	
	private String url;
	private String method;
	
	public MyRequest(InputStream inputStream) throws IOException {
		
		String httpRequest = "";
		byte[] httpRequestBytes = new byte[1024];
		int length = 0;
		if((length = inputStream.read(httpRequestBytes)) > 0) {
			httpRequest = new String(httpRequestBytes, 0, length);
		}
		
		String httpHead = httpRequest.split("\n")[0];
		url = httpHead.split("\\s")[1];
		method = httpHead.split("\\s")[0];
		
		System.out.println(this.toString());
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}
	
	@Override
		public String toString() {
			return "MyRequest -- url:" + url + ",method:" + method;
		}

}

```

```java
package mytomcat;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 封装http响应
 */
public class MyResponse {
	
	private OutputStream outputStream;
	
	public MyResponse (OutputStream outputStream) {
		this.outputStream = outputStream;
	}
	
	public void write(String content) throws IOException {
		StringBuffer httpResponse = new StringBuffer();
		httpResponse.append("HTTP/1.1 200 OK\n")
					.append("Content-Type: text/html\n")
					.append("\r\n")
					.append(content);
		
		outputStream.write(httpResponse.toString().getBytes());
		outputStream.close();
	}

}

```

### 2.2 实现不同的Servlet

```java
package mytomcat;
/**
 * Servlet抽象类
 */
public abstract class MyServlet {
	
	public abstract void doGet(MyRequest myRequest, MyResponse myResponse);
	
	public abstract void doPost(MyRequest myRequest, MyResponse myResponse);
	
	public void service(MyRequest myRequest, MyResponse myResponse) {
		if(myRequest.getMethod().equalsIgnoreCase("POST")) {
			doPost(myRequest, myResponse);
		}else if(myRequest.getMethod().equalsIgnoreCase("GET")) {
			doGet(myRequest, myResponse);
		}
	}
}

```

```java
package mytomcat;

import java.io.IOException;

/**
 * 处理操作'书'的http请求
 */
public class BookServlet extends MyServlet {

	@Override
	public void doGet(MyRequest myRequest, MyResponse myResponse) {
		try {
			myResponse.write("[get] book...");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void doPost(MyRequest myRequest, MyResponse myResponse) {
		try {
			myResponse.write("[post] book...");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

}

```

```java
package mytomcat;

import java.io.IOException;

/**
 * 处理操作'车'的http请求
 */
public class CarServlet extends MyServlet {

	@Override
	public void doGet(MyRequest myRequest, MyResponse myResponse) {
		try {
			myResponse.write("[get] car...");
		}catch(IOException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void doPost(MyRequest myRequest, MyResponse myResponse) {
		try {
			myResponse.write("[post] car...");
		}catch(IOException e) {
			e.printStackTrace();
		}
	}

}

```

### 2.3 定义Servlet映射POJO类

```java\
package mytomcat;

public class ServletMapping {
	
	private String servletName;
	private String url;
	private String className;
	
	public ServletMapping(String servletName, String url, String className) {
		super();
		this.servletName = servletName;
		this.url = url;
		this.className = className;
	}

	public String getServletName() {
		return servletName;
	}

	public void setServletName(String servletName) {
		this.servletName = servletName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}
	
}

```

### 2.4 配置Servlet映射关系

```java
package mytomcat;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置请求url和处理的servlet的对应关系
 */
public class ServletMappingConfig {
	
	public static List<ServletMapping> servletMappingList = new ArrayList<>();;
	
	static {
		servletMappingList.add(new ServletMapping("Book", "/book", "mytomcat.BookServlet"));
		servletMappingList.add(new ServletMapping("Car", "/car", "mytomcat.CarServlet"));
	}

}

```

### 2.5 主类

```java
package mytomcat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class MyTomcat {
	private int port;
	//保存请求url和处理请求servlet的对应关系
	private Map<String, String> urlServletMap = new HashMap<String, String>();
	
	public MyTomcat(int port) {
		this.port = port;
	}
	
	public void start() {
		initServletMapping();
		
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(port);
			System.out.println("MyTomcat is start...\n监听端口：" + port);
			
			while(true) {
				System.out.println("等待请求...");
				Socket socket = serverSocket.accept();
				InputStream inputStream = socket.getInputStream();
				OutputStream outputStream = socket.getOutputStream();
				
				MyRequest myRequest = new MyRequest(inputStream);
				MyResponse myResponse = new MyResponse(outputStream);
				
				//请求分发
				disPatch(myRequest, myResponse);
				socket.close();
			}
		}catch(IOException e) {
			e.printStackTrace();
		}finally {
			if(serverSocket != null) {
				try {
					serverSocket.close();
				}catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	//初始化url和处理的servlet的对应关系
	private void initServletMapping() {
		for(ServletMapping servletMapping: ServletMappingConfig.servletMappingList) {
			urlServletMap.put(servletMapping.getUrl(), servletMapping.getClassName());
		}
	}
	
	//分发处理请求
	private void disPatch(MyRequest myRequest, MyResponse myResponse) {
		String className = urlServletMap.get(myRequest.getUrl());
		
		//反射
		try {
			Class<MyServlet> myServletClass = (Class<MyServlet>) Class.forName(className);
			MyServlet myServlet = myServletClass.newInstance();
			
			myServlet.service(myRequest, myResponse);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		MyTomcat myTomcat = new MyTomcat(8080);
		myTomcat.start();
	}
}

```

## 3. 测试

运行MyTomcat主类，然后在浏览器输入`http://localhost:8080/car`，可以看到返回`[get] car...`，大功告成。