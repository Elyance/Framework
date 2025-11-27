package src.controller;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import src.classe.*;
import src.annotation.*;

public class FrontController extends HttpServlet {
    @Override
    public void init() throws ServletException {
        Scan scan = new Scan(Controller.class);
        HashMap <String, List<Route>> controllers = new HashMap<>();
        try {
            List<Class<?>> classesAnnotated = scan.getClassesAnnotatedWith();
            for (Class<?> c : classesAnnotated) {
                Object controllerInstance = c.getDeclaredConstructor().newInstance();
                Method[] listeMethods = c.getDeclaredMethods();
                for (Method m : listeMethods) {
                    if (m.isAnnotationPresent(Url.class) ) {
                        String httpMethod = ""; // Default HTTP method
                        if (m.isAnnotationPresent(Get.class)) {
                            httpMethod = "GET";
                        } else if (m.isAnnotationPresent(Post.class)) {
                            httpMethod = "POST";
                        }
                        if (controllers.get(m.getAnnotation(Url.class).value()) == null) {
                            controllers.put(m.getAnnotation(Url.class).value(), new ArrayList<>());
                        }
                        controllers.get(m.getAnnotation(Url.class).value()).add(new Route(controllerInstance, m, httpMethod));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Print la liste des URLs dans le map setup
        ServletContext context = getServletContext();
        context.setAttribute("routesMap", controllers);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html");
        try {
            ServletContext context = getServletContext();
            HashMap<String,Object> routesMap=(HashMap<String,Object>) context.getAttribute("routesMap");

            String path = request.getRequestURI().substring(request.getContextPath().length());

            boolean resources = getServletContext().getResource(path) != null;

            if (resources) {
                getServletContext().getNamedDispatcher("default").forward(request, response);
                return;
            } else {
                response.getWriter().println("<html><body>");
                response.getWriter().println("<h1>Path: " + path + "</h1>");
                response.getWriter().println("</body></html>");

                if (routesMap.containsKey(path)) {
                    
                    List<Route> routes = (List<Route>) routesMap.get(path);
                    for (Route route : routes) {
                        if (route.getMethodeHttp().equalsIgnoreCase(request.getMethod())) {
                            execute(route, request, response);
                            return;
                        }
                    }
                    response.getWriter().println("<html><body>");
                    response.getWriter().println("<h1>Method "+  request.getMethod() +" Not Allowed</h1>");
                    response.getWriter().println("</body></html>");
                } else {
                    String removedValue = path.replaceAll(".*/([^/]+)$", "$1");
                    String pathNettoye = path.replaceAll("/[^/]+$", "/");
                    // Affiche le chemin nettoyé dans la réponse HTML pour le voir dans le navigateur
                    String regex = pathNettoye+"\\{[^/]+}$";
                    for (String key : routesMap.keySet()) {
                        if (key.matches(regex)) {
                            System.out.println("Matched: " + key);
                            List<Route> routes = (List<Route>) routesMap.get(key);
                            for (Route route : routes) {
                                if (route.getMethodeHttp().equalsIgnoreCase(request.getMethod())) {
                                    Route r = route;
                                    if (removedValue != null) {
                                        r.setArg(removedValue);
                                    }
                                    execute(r, request, response);
                                    return;
                                }
                            }
                        }
                    }
                    response.getWriter().println("<html><body>");
                    response.getWriter().println("<h1>Path Not Found</h1>");
                    response.getWriter().println("</body></html>");
                }
            }
        } catch (Exception e) {
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>Erreur interne du serveur + " + e.getMessage() + " </h1>");
            response.getWriter().println("</body></html>");
        }
    }  
    
    public void execute(Route route, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Method method = route.getMethod();
            Object controllerInstance = route.getControllerInstance();
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>Controller: " + controllerInstance.getClass().getName() + "</h1>");
            response.getWriter().println("<h1>Method: " + method.getName() + "</h1>");
            response.getWriter().println("</body></html>");

            Object retour = null;
            if (method.getParameters().length > 0) {
                if (route.getArg() != null) {
                    Typation typation = new Typation(route.getArg(), method.getParameters()[0].getType());
                    retour = method.invoke(controllerInstance, typation.getTypedValue());
                } else {
                    int index = 0;
                    Object[] args = new Object[method.getParameters().length];
                    for (Parameter param : method.getParameters()) {
                        Typation typation;
                        Request requestAnnotation = param.getAnnotation(Request.class);
                        if (requestAnnotation != null) {
                            typation = new Typation(request.getParameter(requestAnnotation.value()), param.getType());
                        } else {
                            typation = new Typation(request.getParameter(param.getName()), param.getType());
                        }
                        args[index] = typation.getTypedValue();
                        index++;
                    }
                    retour = method.invoke(controllerInstance, args);
                }
            } else {
                retour = method.invoke(controllerInstance);
            }
            
                    
            // Object retour = method.invoke(controllerInstance, args);
            if (retour != null && retour.getClass() == String.class) {
                response.getWriter().println("<html><body>");
                response.getWriter().println("<h1>Return Value:</h1>");
                response.getWriter().println("<pre>" + retour.toString() + "</pre>");
                response.getWriter().println("</body></html>");
            } else if (retour != null && retour.getClass() == ModelVue.class) {
                if (((ModelVue) retour).getView().isEmpty()) {
                    throw new Exception("View name is null or empty");
                }
                for (String cle : ((ModelVue) retour).getData().keySet()) {
                    request.setAttribute(cle, ((ModelVue) retour).getData().get(cle));
                }
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
    }
}
