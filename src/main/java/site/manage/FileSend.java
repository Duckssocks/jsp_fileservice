package site.manage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Random;

/**
 * Servlet implementation class FileSend
 */
public class FileSend extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileSend() {
        super();
        // TODO Auto-generated constructor stub
    }

	
	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		   response.setContentType("text/html");
		   boolean filesend = false;
		   String folder = this.getServletContext().getInitParameter("UploadFolder");
		   File uploadfolder = new File(this.getServletContext().getRealPath(folder));
		   if (!uploadfolder.exists()) {
			   uploadfolder.mkdir();
		   }
		   
		   // DB 커넥션 받기
		   String driverName = this.getServletContext().getInitParameter("dbDriverClass");
		   String driverURL = this.getServletContext().getInitParameter("dbURL");
		   
	   
		   PrintWriter pw = response.getWriter();
		   String passcode;
		   Cookie[] cookies = request.getCookies();
		   
		   // 쿠키가 이미 있으면, 기다리라는 html로 sendRedirect 함.
		   if (cookies != null) {
		      for (Cookie c: cookies) {
		    	  if ("TxRequest".equals(c.getName())) {
		    		  response.sendRedirect("sample_wait.html");
		    		  System.out.println("쿠키가 이미 존재합니다.");
		    		  // 쿠키가 이미 존재한다면, sample_wait로 보내고 해당 doPost() 함수는 그대로 리턴.
		    		  return;
		    	  }
		      }
		   } 
		   
		    // 현재 시간과, +5분 시간 저장.
		    Date now = new Date();
		    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    String formatedNow = formatter.format(now);
		    Calendar cal = Calendar.getInstance();
		    cal.setTime(now);
		    cal.add(Calendar.MINUTE, 5);
		    Date newTime = cal.getTime();
		    String formattedNow = formatter.format(newTime);
		    
		    
			// code 생성
			Random random = new Random();
			int createNum;
			StringBuilder resultNum = new StringBuilder();
			int letter = 6;
			for (int i=0; i< letter; i++) {
				  createNum = random.nextInt(9);
				  resultNum.append(createNum);  
			}
			passcode = resultNum.toString();
            System.out.println("코드 생성 완료");
            
            
            // ip 주소를 가져오는 코드
            InetAddress clientAddress = InetAddress.getLocalHost();
            String ip = clientAddress.getHostAddress();
			System.out.println(ip);
			
			// id값을 전역 변수로 설정 (null로 초기화하면 안됨 !!)
            String id = "";
			
            // DB 연결하고, 같은 IP로부터 5분 이내 요청이 있는지? 확인해보는 코드.
            try (
            		Connection conn = DriverManager.getConnection(driverURL);
            		Statement state = conn.createStatement();
            ) {
            	String ipcheck = String.format(
            			"SELECT COUNT(*) AS request_count FROM filelist WHERE ip_addr = '%s' AND upload_time >= DATE_SUB(NOW(), INTERVAL 5 MINUTE)", ip
            			);
            	ResultSet checkResult = state.executeQuery(ipcheck);
            	if (checkResult.next() && checkResult.getInt("request_count") > 0) {
            		System.out.println("5분 이내, 동일한 IP로부터 요청이 이미 존재합니다.");
            		response.sendRedirect("sample_wait.html");
            		return;
            	}
            } catch (SQLException e) {
            	System.out.println("동일한 IP에서 5분 이내에 접속하였는지 확인하는 과정에서 오류가 발생했습니다.");
            	e.printStackTrace();
            }
            
            
			Collection<Part> mParts = null;
			try {
				mParts = request.getParts();
				
				if (mParts != null) {
					 System.out.println("안녕 파츠");
				 // 파일 업로드하는 로직.
				     for (Part part : mParts) {
				           if ("upfile".equals(part.getName())) {
				            // 파일명에서 이름 부분(fileName)과 확장자 부분(fileExt)을 분리한다.
				            String wholeName = part.getSubmittedFileName();
				            String fileName = wholeName.substring(0, wholeName.lastIndexOf("."));
				            String fileExt = wholeName.substring(wholeName.lastIndexOf(".") + 1);
				                     
				            // 파일명이 중복되지 않을 떄까지 반복함.
				            File upfolder = new File(uploadfolder, fileName + "." + fileExt);
				            int fileExists = 1;
				            while (upfolder.exists()) {
				                fileName = fileName + "(" + fileExists + ")";
				                fileExists++;
				                upfolder = new File(uploadfolder, fileName + "." + fileExt);
				            }
				            part.write(upfolder.getAbsolutePath()); 
				            filesend = true;
				            System.out.println("파일 업로드 완료");
				                     
				           
				            // driverName으로부터 class 로딩.
				           try {
							Class.forName(driverName);
						} catch (ClassNotFoundException e) {
							System.out.println("class loading error!");
							e.printStackTrace();
						}
				           
				            // DB 연결 받고, filelist 테이블에 정보 저장
				          try (
				         		Connection conn = DriverManager.getConnection(driverURL);
				         		Statement state = conn.createStatement();
				         	 ) {
				         	  // pass_code, filename, ip_addr, upload_time을 INSERT하기. DB에 !
				            String str = "";
				            str = String.format("INSERT INTO filelist (pass_code, filename, ip_addr, upload_time) VALUES ('%s', '%s', '%s', '%s')", passcode, wholeName, ip, formatedNow);
				         	System.out.println("인서트문은 다음과 같다. " + str);
				            state.executeUpdate(str);
				            
				            String str2 = "SELECT id FROM filelist WHERE pass_code = '" + passcode + "'";
				            ResultSet rs2 = state.executeQuery(str2);			            
				         	while (rs2.next()) {
				         		id = String.valueOf(rs2.getInt("id"));
				         	}
				         	System.out.println("id 값은 " + id + "입니다");
				         	
				           } catch (SQLException e) {
				        	   System.out.println("invalid SQL : check SQL");
				        	   e.printStackTrace();
				           }
				            	// for문이 끝나는 지점.
				           }
				         }
				     
				     // mParts이 null인지에 대한 if문이 끝나는 지점.
				 }
			} catch (IOException e) {
				System.out.println("에러가 발생하긴 했는데, 그냥 아무것도 안 하고 무시. 그리고 mParts = null로 설정한 뒤 sendRedirect할 거임.");
				mParts = null;
				response.sendRedirect("sendFile.html");
			}
			  
			   if (filesend) {
				   // TxRequest 쿠키 설정
				   Cookie cookie = new Cookie("TxRequest", id);
				   cookie.setMaxAge(60 * 5);
				   response.addCookie(cookie);
				   
	            	// 결과 출력하는 코드.
	            	pw.append("<html>");
	            	pw.append("<body>");
	            	pw.append("<hr>");
	            	pw.append("<h2>파일이 성공적으로 업로드 되었습니다.</h2>");
	            	pw.append("<br>");
	            	pw.append("pass_code: " + "<strong style='color:red;'>" + passcode + "</strong>" + "<br>");
	            	pw.append("삭제 예정 시간: " + "<strong style='color:blue;'>" + formattedNow + "</strong>");
	            	pw.append("<br>");
	            	pw.append("다운로드로 이동: " + "<a href='download.do'>다운 받기</a>");
	            	pw.append("</body>");
	            	pw.append("</html>");
	            }
		   
		   pw.close();
		                                  
	}

}
