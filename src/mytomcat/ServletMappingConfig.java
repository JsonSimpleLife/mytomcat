package mytomcat;

import java.util.ArrayList;
import java.util.List;

/**
 * 配置请求url和处理的servlet的对应关系
 * 
 * @author wangjun [wangjun8@xiaomi.com]
 * @date 2018-11-06
 * @version 1.0
 */
public class ServletMappingConfig {
	
	public static List<ServletMapping> servletMappingList = new ArrayList<>();;
	
	static {
		servletMappingList.add(new ServletMapping("Book", "/book", "mytomcat.BookServlet"));
		servletMappingList.add(new ServletMapping("Car", "/car", "mytomcat.CarServlet"));
	}

}
