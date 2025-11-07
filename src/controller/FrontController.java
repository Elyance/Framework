package src.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import src.classe.*;
import src.annotation.*;



public class FrontController extends HttpServlet {
    @Override
    public void init() throws ServletException {
        Scan scan = new Scan(Controller.class);
        HashMap <String, Object> controllers = new HashMap<>();
        try {
            List<Class<?>> classesAnnotated = scan.getClassesAnnotatedWith();
            for (Class<?> c : classesAnnotated) {
                Object controllerInstance = c.getDeclaredConstructor().newInstance();
                Method[] listeMethods = c.getDeclaredMethods();
                for (Method m : listeMethods) {
                    if (m.isAnnotationPresent(Url.class)) {
                        controllers.put(m.getAnnotation(Url.class).value(), new Route(controllerInstance, m));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        ServletContext context = getServletContext();
        context.setAttribute("routesMap", controllers);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        
        ServletContext context = getServletContext();
        HashMap<String,Object> routesMap=(HashMap<String,Object>) context.getAttribute("routesMap");

        String path = request.getRequestURI().substring(request.getContextPath().length());

        boolean resources = getServletContext().getResource(path) != null;

        if (resources) {
            getServletContext().getNamedDispatcher("default").forward(request, response);
            
            return;
        } else {
            response.getWriter  ().println("<html><body>");
            response.getWriter().println("<h1>Path: " + path + "</h1>");
            response.getWriter().println("</body></html>");

            if (routesMap.containsKey(path)) {
                Route route = (Route) routesMap.get(path);
                
                try {
                    Method method = route.getMethod();
                    Object controllerInstance = route.getControllerInstance();
                    response.getWriter().println("<html><body>");
                    response.getWriter().println("<h1>Controller: " + controllerInstance.getClass().getName() + "</h1>");
                    response.getWriter().println("<h1>Method: " + method.getName() + "</h1>");
                    response.getWriter().println("</body></html>");

                    
                    Object retour = method.invoke(controllerInstance);
                    if (retour != null && retour.getClass() == String.class) {
                        response.getWriter().println("<html><body>");
                        response.getWriter().println("<h1>Return Value:</h1>");
                        response.getWriter().println("<pre>" + retour.toString() + "</pre>");
                        response.getWriter().println("</body></html>");
                    } else if (retour != null && retour.getClass() == ModelVue.class) {
                        request.getRequestDispatcher(((ModelVue) retour).getView()).forward(request, response);
                    } else { 
                        response.getWriter().println("<html><body>");
                        response.getWriter().println("<h1>No Return Value</h1>");
                        response.getWriter().println("</body></html>");
                    }
                } catch (Exception e) {
                    response.getWriter().println("<html><body>");
                    response.getWriter().println("<h1>Ca marche pas: " + e.getMessage() + "</h1>");
                    response.getWriter().println("</body></html>");
                    e.printStackTrace();
                }
            } else {
                response.getWriter().println("<html><body>");
                response.getWriter().println("<h1>404 Not Found</h1>");
                response.getWriter().println("</body></html>");
            }
        }
    }    
}
