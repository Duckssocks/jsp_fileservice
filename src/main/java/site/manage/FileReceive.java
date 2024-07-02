package site.manage;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Servlet implementation class FileReceive
 */
@WebServlet("/FileReceive")
public class FileReceive extends HttpServlet {
    private static final long serialVersionUID = 1L;
    
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FileReceive() {
        super();
        // TODO Auto-generated constructor stub
    }

    /**
     * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        String passCode = request.getParameter("pass_code");

        // DB 커넥션 파라미터 읽기
        String driverName = this.getServletContext().getInitParameter("dbDriverClass");
        String driverURL = this.getServletContext().getInitParameter("dbURL");
        String uploadFolder = this.getServletContext().getInitParameter("UploadFolder");
        // 지정된 드라이버 클래스를 로드함
        
        //
        try {
            Class.forName(driverName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            try (PrintWriter pw = response.getWriter()) {
                pw.write("<html><body><h3>데이터베이스 드라이버 로딩에 실패했습니다.</h3></body></html>");
            }
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // 데이터베이스 연결
            conn = DriverManager.getConnection(driverURL);
            
            // pass_code 검증 및 파일 정보 가져오기
            String sql = "SELECT * FROM filelist WHERE pass_code = ? AND NOW() <= DATE_ADD(upload_time, INTERVAL 5 MINUTE)";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, passCode);
            rs = pstmt.executeQuery();
            
            //pass_code를 검증하고 파일 정보를 가져오는 SQL 쿼리를 준비하고 실행
            if (rs.next()) {
                String fileName = rs.getString("filename");
                String filePath = this.getServletContext().getRealPath(uploadFolder + fileName);

                File file = new File(filePath);
                System.out.println("파일 경로: " + filePath);
                System.out.println("파일 존재 여부: " + file.exists());  // 파일 존재 여부 출력
                // 
                if (file.exists()) {
                    try (FileInputStream fileIn = new FileInputStream(file); OutputStream out = response.getOutputStream()) {
                        // 파일 다운로드 처리
                        response.setContentType("application/octet-stream");
                        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = fileIn.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }

                        // 파일 및 데이터베이스 레코드 삭제
                        file.delete();

                        String deleteSql = "DELETE FROM filelist WHERE pass_code = ?";
                        pstmt = conn.prepareStatement(deleteSql);
                        pstmt.setString(1, passCode);
                        pstmt.executeUpdate();

                        // 쿠키 무효화
                        Cookie txRequestCookie = new Cookie("TxRequest", null);
                        txRequestCookie.setMaxAge(0); // 쿠키 삭제
                        response.addCookie(txRequestCookie);

                        response.sendRedirect("sendFile.html");
                    } catch (IOException e) {
                        e.printStackTrace();
                        try (PrintWriter pw = response.getWriter()) {
                            pw.write("<html><body><h3>파일을 읽는 중 오류가 발생했습니다: " + e.getMessage() + "</h3></body></html>");
                        }
                    }
                } else {
                    response.sendRedirect("receiveFile.html?error=FileNotFound");
                }
            } else {
                response.sendRedirect("receiveFile.html?error=InvalidPassCode");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.sendRedirect("receiveFile.html?error=Exception");
        } finally {
            try {
                if (rs != null) rs.close();
                if (pstmt != null) pstmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
