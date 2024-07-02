package site.manage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;


// ServletContextListener 인터페이스를 구현하여 애플리케이션 초기화 시 및 종료 시 실행할 코드를 작성함.
// 또한 Runnable을 구현하여, 우리가 주로 하는 작업 외에도 스레드로 해당 작업이 돌아갈 수 있도록.
public class FileSweeper implements ServletContextListener, Runnable {
	private Thread thread;
	private boolean running = true;
	private ServletContext context;
	private int sweepInterval;
	
	public FileSweeper() {
		System.out.println("This is the constructor!");
	}
	
	// Dead code.
//	public FileSweeper(ServletContext context) {
//		this.context = context;
//	}
	
	
	// 우선은.. DB 내에서 5분이 지난 파일들을 대상으로 삭제하는 작업 (5분 단위로)
	// web.xml 파일에 <listener> 태그 설정함으로써 웹 애플리케이션이 시작될 때 초기화하고, 스레드를 구현하여 5분마다 FileSweeper의 sweepFiles 메서드가 실행되어 오래된 파일들을 제거하도록 함.
	// 주 메서드
	public void sweepFiles() {
	
    // Upload 서버 경로 받아놓기
	String folder = context.getInitParameter("UploadFolder");
    File uploadfolder = new File(context.getRealPath(folder));
		
	// DB 초기화 파라미터
	String driverName = context.getInitParameter("dbDriverClass");
	String driverURL = context.getInitParameter("dbURL");
	   
	// filelist table에서 5분이 경과한 레코드 목록을 조회하는 작업
	// driverName으로부터 class 로딩.
    try {
		Class.forName(driverName);
	} catch (ClassNotFoundException e1) {
		System.out.println("class loading error!");
		// 문자열 중복으로 인해 e1으로 수정
		e1.printStackTrace();
	}
    
     // DB 연결 받고, filelist 테이블로부터 5분이 지난 파일 정보들 삭제.
   try (
  		Connection conn = DriverManager.getConnection(driverURL);
  		Statement state = conn.createStatement();
  	 ) {
	   
	     // 현재 시간과, 5분 전 시간 저장.
	     Date now = new Date();
	     SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	     String formatedNow = formatter.format(now);
	     Calendar cal = Calendar.getInstance();
	     cal.setTime(now);
	     cal.add(Calendar.MINUTE, -5);
	     Date newTime = cal.getTime();
	     String formattedNow = formatter.format(newTime);
	     
	     // ResultSet으로 삭제할 파일리스트들의 이름을 저장
	     String selquery = String.format("SELECT filename FROM filelist WHERE upload_time < '%s'", formattedNow);
	     ResultSet rs = state.executeQuery(selquery);
	     
	     // 해당 조건 만족하는 행들 돌면서 실제 서버 폴더 내에서 파일 삭제 ㄱㄱ
	     while (rs.next()) {
	    	 String filename = rs.getString("filename");
	    	 File file = new File(uploadfolder, filename);
	    	 if (file.exists()) {
	    		 if (file.delete()) {
	    			 System.out.println("파일 삭제 성공(서버 폴더에서)");
	    		 } else {
	    			 System.out.println("파일 삭제 실패(서버 폴더에서)");
	    		 }
	    	 } 

	     }  
	 
  	 // 쿼리문 세팅 
     String str = "";
     str = String.format("DELETE FROM filelist WHERE upload_time < '%s'", formattedNow);
  	 System.out.println("딜리트문은 다음과 같음. " + str);
     state.executeUpdate(str);
     System.out.println("5분이 지난 파일들이 DB에서 삭제되었습니다.");
     
     
     
     
    } catch (SQLException e) {
 	   System.out.println("invalid SQL : check SQL");
 	   e.printStackTrace();
    }
   
    // sweepFiles()가 끝나는 지점
}
	
	// 5분마다 파일을 삭제하는 작업 초기화.
	@Override
    public void contextInitialized(ServletContextEvent sce) {
        context = sce.getServletContext();
        String intervalParam = context.getInitParameter("SweepInterval");
        try {
            sweepInterval = Integer.parseInt(intervalParam) * 60 * 1000; // 분 단위 -> 밀리초 단위 변환 (web.xml파일에는 분 단위로 표기되어 있지만, 밀리초로 인자 전달해야 함)
        } catch (NumberFormatException e) {
            sweepInterval = 5 * 60 * 1000; // 기본값 5분
        }
        System.out.println("FileSweeper의 업무를 처리하는 스레드 실행 전");
        thread = new Thread(this);
        thread.start();
        System.out.println("FileSweeper의 업무를 처리하는 스레드 실행 후");
    }
	
	// app이 끝날 때 실행할 메서드.
	@Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 스레드 중지
		running = false;
        if (thread != null) {
            try {
                thread.join(); // 종료하기 전 대기
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("FileSweeper의 업무를 처리하는 스레드 종료");
    }
	
	@Override
	// 스레드를 통해서 어떤 작업을 할 건지? -> sweepFiles() 메서드를 실행하겠다!
    public void run() {
        while (running) {
            sweepFiles();
            System.out.println("파일 정리 시스템 가동 중 ...");
            try {
            	// 내가 진작에 설정한 sweepInterval (5분)만큼 대기하도록 함(계속 실행x).
                Thread.sleep(sweepInterval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
