package src.controller;

import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import jakarta.servlet.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

import src.classe.*;
import src.annotation.*;

@MultipartConfig
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
                gererPath(path, routesMap, request, response);
            }
        } catch (Exception e) {
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>Erreur interne du serveur + " + e.getMessage() + " </h1>");
            response.getWriter().println("</body></html>");
        }
    }  

    public void gererPath(String path, HashMap<String,Object> routesMap, HttpServletRequest request, HttpServletResponse response) throws IOException{
        try {
            // response.getWriter().println("<html><body>");
            // response.getWriter().println("<h1>Path: " + path + "</h1>");
            // response.getWriter().println("</body></html>");

                // si le path existe (genre egale, exemple: "/hello")  dans le map des routes on execute la methode associee
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
                //si le path n'existe pas genre c'est pas un path pas normale (exemple : "hello/{name}")
            } else {
                String removedValue = path.replaceAll(".*/([^/]+)$", "$1");
                String pathNettoye = path.replaceAll("/[^/]+$", "/");
                // Affiche le chemin nettoyé dans la réponse HTML pour le voir dans le navigateur
                String regex = pathNettoye+"\\{[^/]+}$";
                for (String key : routesMap.keySet()) {
                    // si le path correspond au path nettoye avec une regex genre /hello/{$variable}
                    if (key.matches(regex)) {
                        System.out.println("Matched: " + key);
                        List<Route> routes = (List<Route>) routesMap.get(key);
                        for (Route route : routes) {
                            if (route.getMethodeHttp().equalsIgnoreCase(request.getMethod())) {
                                Route r = route;
                                if (removedValue != null) {
                                    // on ajouute l'argument si ca existe
                                    r.setArg(removedValue);
                                }
                                execute(r, request, response);
                                return;
                            }
                        }
                    }
                }
                // si le path n'est vraiment pas trouve
                response.getWriter().println("<html><body>");
                response.getWriter().println("<h1>Path Not Found</h1>");
                response.getWriter().println("</body></html>");
            }
        } catch (Exception e) {
            response.getWriter().println("<html><body>");
            response.getWriter().println("<h1>Ca marche pas: " + e.getMessage() + "</h1>");
            response.getWriter().println("</body></html>");
            e.printStackTrace();
        }
    }

    // ici on execute la methode du controller associee a la route
    @SuppressWarnings("unused")
    
    public void execute(Route route, HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            Method method = route.getMethod();
            Object controllerInstance = route.getControllerInstance();
            
            // Vérifier si la méthode est annotée avec @Json
            boolean isJsonMethod = method.isAnnotationPresent(Json.class);
            
            if (!isJsonMethod) {
                // response.getWriter().println("<html><body>");
                // response.getWriter().println("<h1>Controller: " + controllerInstance.getClass().getName() + "</h1>");
                // response.getWriter().println("<h1>Method: " + method.getName() + "</h1>");
                // response.getWriter().println("</body></html>");
            }

            Object retour = null;
            // s'il y a des parametres dans la methode
            if (method.getParameters().length > 0) {
                // si la route a un argument (exemple: /hello/{name} ) on lui donne l'argument comme argument
                if (route.getArg() != null) {
                    Typation typation = new Typation(route.getArg(), method.getParameters()[0].getType());
                    retour = method.invoke(controllerInstance, typation.getTypedValue());
                // si la methode a un parametre de type Map on lui donne une map des parametres de la requete
                } else if (method.getParameters().length == 1 && method.getParameters()[0].getType().equals(Map.class)) {
                        Map<String, Object> paramMap = new HashMap<>();
                        Enumeration<String> parameterNames = request.getParameterNames();
                        while (parameterNames.hasMoreElements()) {
                            String paramName = parameterNames.nextElement();
                            paramMap.put(paramName, request.getParameter(paramName));
                        }
                        retour = method.invoke(controllerInstance, paramMap);
                // si la methode a des parametres soit il a le meme nom soit il est annote avec @Request
                } else {
                    int index = 0;
                    Object[] args = new Object[method.getParameters().length];
                    for (Parameter param : method.getParameters()) {
                        Typation typation;
                        Request requestAnnotation = param.getAnnotation(Request.class);
                        Session sessionAnnotation = param.getAnnotation(Session.class);
                        // s'il est pas de type map qui demande un Path et un Byte
                        
                        if (sessionAnnotation != null && param.getType().equals(Map.class)) {
                            HttpSession session = request.getSession();
                            Map<String, Object> sessionMap = new HashMap<>();
                            Enumeration<String> attributeNames = session.getAttributeNames();
                            while (attributeNames.hasMoreElements()) {
                                String attributeName = attributeNames.nextElement();
                                sessionMap.put(attributeName, session.getAttribute(attributeName));
                            }
                            args[index] = sessionMap;
                        }
                        // s'il est annote par Request
                        else if (requestAnnotation != null) {
                            typation = new Typation(request.getParameter(requestAnnotation.value()), param.getType());
                            args[index] = typation.getTypedValue();
                        // s'il est pas annote
                        } else {
                            if (param.getType() == String.class || param.getType().isPrimitive() || isWrapperType(param.getType())) {
                                typation = new Typation(request.getParameter(param.getName()), param.getType());
                                args[index] = typation.getTypedValue();
                            } else if (param.getType() == Map.class) {
                                response.getWriter().println("<html><body>");
                                response.getWriter().println("<h1>Controller c'est un Map, donc on attend un upload de fichier</h1>");
                                response.getWriter().println("</body></html>");
                                // si c'est un upload de fichier
                                String contentType = request.getContentType();
                                if (contentType != null && contentType.startsWith("multipart/form-data")) {
                                    response.getWriter().println("<html><body>");
                                    response.getWriter().println("<h1>Le contentType est okey</h1>");
                                    response.getWriter().println("</body></html>");
                                    Map<String, byte[]> fileMap = new HashMap<>();
                                    try {
                                        String projectPath = request.getServletContext().getRealPath("/");
                                        response.getWriter().println("<html><body>");
                                        response.getWriter().println("<h1>Project Path: " + projectPath + "</h1>");
                                        response.getWriter().println("</body></html>");
                                        File uploadsDir = new File(projectPath, "uploads");
                                        
                                        if (!uploadsDir.exists()) {
                                            uploadsDir.mkdirs();
                                            System.out.println("Dossier uploads créé à: " + uploadsDir.getAbsolutePath());
                                        }

                                        Collection<Part> parts = request.getParts();
                                        if (parts.size() <= 0) {
                                            response.getWriter().println("<html><body>");
                                            response.getWriter().println("<h1>Pas de fichier uploadé</h1>");
                                            response.getWriter().println("</body></html>");
                                            continue;
                                        }
                                        for (Part part : parts) {
                                            response.getWriter().println("<html><body>");
                                            response.getWriter().println("<h1>Part Name: " + part.getName() + "</h1>");
                                            response.getWriter().println("<h1>Part Size: " +    part.getSize() + "</h1>");
                                            response.getWriter().println("</body></html>");
                                            // On récupère le nom du fichier et son contenu
                                            // et on le met dans la map
                                            String fileName = part.getSubmittedFileName();
                                            if (fileName != null) {
                                                byte[] fileContent = part.getInputStream().readAllBytes();
                                                response.getWriter().println("<html><body>");
                                                response.getWriter().println("<h1>File Name: " + fileName + "</h1>");
                                                response.getWriter().println("<h1>File Size: " + fileContent.length + "</h1>");
                                                response.getWriter().println("</body></html>");

                                                File file = new File(uploadsDir, fileName);
                                                try (var fileOutputStream = java.nio.file.Files.newOutputStream(file.toPath())) {
                                                    System.out.println("Enregistrement du fichier: " + file.getAbsolutePath());
                                                    fileOutputStream.write(fileContent);
                                                }
                                                fileMap.put(fileName, fileContent);
                                            }
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                    args[index] = fileMap;
                                } else {
                                    response.getWriter().println("<html><body>");
                                    response.getWriter().println("<h1>Controller c'est un Map, mais pas de fichier upload</h1>");
                                    response.getWriter().println("</body></html>");
                                    args[index] = new HashMap<String, String>();
                                }
                            } else {
                                // Sinon c'est un objet, utiliser convertObject
                                if (!isJsonMethod) {
                                    // response.getWriter().println("<html><body>");
                                    // response.getWriter().println("<h1>Controller c'est un objet</h1>");
                                    // response.getWriter().println("</body></html>");
                                }
                                Param p = new Param(param.getName(), param.getType());
                                Object obj = convertObject(p, request);
                                args[index] = obj;
                            }
                        }
                        index++;
                    }
                    // on lui donne les arguments
                    retour = method.invoke(controllerInstance, args);

                    for (int i = 0; i < method.getParameters().length; i++) {
                        Parameter param = method.getParameters()[i];
                        Session sessionAnnotation = param.getAnnotation(Session.class);
                        if (sessionAnnotation != null) {
                            if (param.getType().equals(Map.class)) {
                                HttpSession session = request.getSession();
                                Map<String, Object> sessionMap = (Map<String, Object>) args[i];
                                for (String key : sessionMap.keySet()) {
                                    session.setAttribute(key, sessionMap.get(key));
                                }
                            }
                        }
                    }
                }
            // sinon il n'y a pas de parametre dans la methode
            } else {
                retour = method.invoke(controllerInstance);
            }

            // Gestion du retour en fonction de l'annotation @Json
            if (isJsonMethod) {
                response.setContentType("application/json");
                String jsonResponse = JsonConverter.createJsonResponse("success", retour);
                response.getWriter().print(jsonResponse);
            } else {
                // Comportement original pour HTML
                // si le retour est une chaine de caractere on l'affiche directement
                if (retour != null && retour.getClass() == String.class) {
                    response.getWriter().println("<html><body>");
                    response.getWriter().println("<h1>Return Value:</h1>");
                    response.getWriter().println("<pre>" + retour.toString() + "</pre>");
                    response.getWriter().println("</body></html>");
                // si le retour est un ModelVue on set les attributs et on fait un forward vers la vue
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
            }
        } catch (Exception e) {
            boolean isJsonMethod = route.getMethod().isAnnotationPresent(Json.class);
            if (isJsonMethod) {
                response.setContentType("application/json");
                String jsonError = JsonConverter.createJsonError("error", e.getMessage());
                response.getWriter().print(jsonError);
            } else {
                response.getWriter().println("<html><body>");
                response.getWriter().println("<h1>Ca marche pas: " + e.getMessage() + "</h1>");
                response.getWriter().println("</body></html>");
                e.printStackTrace();
            }
        }
    }

    public Object convertObject(Param p, HttpServletRequest request) {
        String nomParam = p.getName();
        String concatenateWithField = nomParam + ".";
        try {
            Object instance = p.getType().getDeclaredConstructor().newInstance();
            for (Field field : p.getType().getDeclaredFields()) {
                field.setAccessible(true);
                String fieldNameWithPrefix = concatenateWithField + field.getName();
                
                if (field.getType().isPrimitive() || field.getType() == String.class || isWrapperType(field.getType())) {
                    // Types primitifs, String et types wrapper (Integer, Boolean, etc.)
                    Typation typation = new Typation(request.getParameter(fieldNameWithPrefix), field.getType());
                    field.set(instance, typation.getTypedValue());
                } else {
                    // appel recursif pour les objets imbriques
                    Param paramField = new Param(fieldNameWithPrefix, field.getType());
                    Object nestedObject = convertObject(paramField, request);
                    if (nestedObject != null) {
                        field.set(instance, nestedObject);
                    }
                }
            }
            return instance;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isWrapperType(Class<?> type) {
        return type == Integer.class || type == Long.class || type == Double.class || 
               type == Float.class || type == Boolean.class || type == Byte.class || 
               type == Short.class || type == Character.class;
    }
}
