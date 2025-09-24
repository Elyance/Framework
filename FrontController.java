import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;


public class FrontController extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        
        String path = request.getRequestURI();

        response.getWriter().println("<html><body>");
        response.getWriter().println("<h1>Path: " + path + "</h1>");
        response.getWriter().println("</body></html>");
    }    
}
