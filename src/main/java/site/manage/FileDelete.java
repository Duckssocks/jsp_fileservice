package site.manage;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class FileDelete extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // HTTP POST 요청을 처리하는 메서드
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 요청에서 파일 ID를 가져옴
        String id = request.getParameter("id");
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        // 파일 업로드 폴더 경로 가져옴
        String folder = getServletContext().getInitParameter("UploadFolder");
        String filePath = getServletContext().getRealPath(folder);

        try {
            // MariaDB 드라이버 로드
            Class.forName(getServletContext().getInitParameter("dbDriverClass"));

            // 데이터베이스 연결 설정
            conn = DriverManager.getConnection(getServletContext().getInitParameter("dbURL"));
            stmt = conn.createStatement();

            // 파일 ID로 파일명 조회 쿼리 실행
            String sql = "SELECT filename FROM filelist WHERE id=" + id;
            rs = stmt.executeQuery(sql);

            // 결과가 있으면 파일 삭제 진행
            if (rs.next()) {
                String fileName = rs.getString("filename");

                // 파일 시스템에서 파일 삭제
                File file = new File(filePath + File.separator + fileName);
                if (file.delete()) {
                    // 데이터베이스에서 해당 레코드 삭제
                    sql = "DELETE FROM filelist WHERE id=" + id;
                    stmt.executeUpdate(sql);

                    // TxRequest 쿠키 무효화
                    Cookie cookie = new Cookie("TxRequest", null);
                    cookie.setMaxAge(0);
                    response.addCookie(cookie);

                    // sendFile.html로 리디렉션
                    response.sendRedirect("sendFile.html");
                } else {
                    // 파일 삭제 실패 시 sendFile.html로 리디렉션
                    response.sendRedirect("sendFile.html");
                }
            } else {
                // 레코드가 없을 경우 sendFile.html로 리디렉션
                response.sendRedirect("sendFile.html");
            }
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            response.sendRedirect("sendFile.html");
        } finally {
            // 리소스 해제
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
