<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*, java.util.*, java.sql.*" %>    
    
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>manage Files</title>
</head>
<body>

<%
	// DB 커넥션 받기
	String driverName = this.getServletContext().getInitParameter("dbDriverClass");
	String driverURL = this.getServletContext().getInitParameter("dbURL");
	
    // ServletContext 객체를 통해 web.xml 파일에 설정되어 있는 StorageFolder 초기화 파라메터의 값을 읽어 온다.
    String rootFolder = this.getServletContext().getInitParameter("UploadFolder");
    File userFolder = new File(this.getServletContext().getRealPath(rootFolder));
    // 만약 Upload 폴더가 없다면 생성한다.
    if (!userFolder.exists())
	    userFolder.mkdir();
    // 서버 폴더 확인용.
    System.out.println("[lookup.jsp] userFolder: " + userFolder);
%>

<%
	// form의 checked를 통해서 전달된 파일 목록들을 서버에서 삭제 후, 관련된 filelist 테이블의 레코드들도 함께 삭제하고 남은 파일들 출력.
	// 만약 요청시에 checked된 파일들이 없는 경우에는, 서버에 업로드된 파일들의 목록 출력.
	
    // 1. 서버 폴더에서 파일 삭제 처리.
    String[] toDelete = request.getParameterValues("checked");
    
    if (toDelete != null) {
    	// 클래스 로딩
    	try {
    		Class.forName(driverName);
    	} catch (ClassNotFoundException e) {
    		System.out.println("class loading error!");
            e.printStackTrace();
    	}
    	// DB 연결 받기.
    	try (
    		Connection conn = DriverManager.getConnection(driverURL);
            Statement state = conn.createStatement();
    		) {
    		for (String fileName : toDelete) {
    			File file = new File(userFolder, fileName);
    			if (file.exists()) {
    			    // 1. 서버 폴더에서 우선 삭제하고 ...
    				file.delete();
    				System.out.println(fileName + " 파일을 서버 폴더에서 삭제함");
    				
    				// 2. 데이터베이스 테이블에서 삭제 처리.
    				String delquery = "DELETE FROM filelist WHERE filename = '" + fileName + "'";
    				state.executeUpdate(delquery);
    				System.out.println(fileName + " 파일에 관한 정보가 DB에서도 삭제됨.");
    			}
    		}
    		
    	} catch (SQLException e) {
    		System.out.println("invalid SQL : check SQL");
    		e.printStackTrace();
    	}
    }
   

%>

<form action='lookup.jsp' method='post'>
  <fieldset style='width:400px'>
    <legend>uploaded files</legend>
<%  
   // 파일의 목록을 보여줌 .. 
   String[] files = userFolder.list();
   if (files == null || files.length == 0)
      out.println("<strong>No uploaded files.</strong><br>");
   else {
      for (String fileName: files) {           
         out.println(String.format("<input type='checkbox' id='%s' name='checked' value='%s'>", fileName, fileName));
         out.println(String.format("<label for='%s'>%s</label>", fileName, fileName));
         		 
         // 여기에 파일의 크기 제시 ..		 
         File file = new File(userFolder, fileName);
         long bytes = file.length();
         String filesize;
         
         
         // KB, MB, GB로 분기하여 제시한다.
         if (bytes < 1024) {
             filesize = bytes + " B";
         } else if (bytes < 1024 * 1024) {
             filesize = (bytes / 1024) + " KB";
         } else if (bytes < 1024 * 1024 * 1024) {
             filesize = (bytes / (1024 * 1024)) + " MB";
         } else {
             filesize = (bytes / (1024 * 1024 * 1024)) + " GB";
         }
         out.println(" [" + filesize + "]");
         out.println("<br>");
      }
   }
%>
    <hr>
    <div style='width:400px;' align='right'> 
      <input type='submit' value='Delete'>&nbsp;&nbsp;
      <input type='reset' value='Reset'>
    </div>  
  </fieldset><br>  
</form>

</body>
</html>